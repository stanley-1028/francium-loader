package com.francium.ai.adapter;

import com.francium.ai.adapter.VersionBridge.MethodMapping;
import org.objectweb.asm.*;

import java.util.*;
import java.util.jar.*;

/**
 * 自動生成 Adaptor 類別。
 * 
 * 當 AI 橋接器發現來源版本的方法在目標版本不存在時，
 * 此生成器會創建一層中間適配類別:
 * 
 * - 對於已映射的方法: 生成一個靜態 wrapper，轉發呼叫到新的目標方法
 * - 對於複雜的 API 變更: 生成一個完整的 adapter class
 * 
 * 生成的 adapter 會被注入到 mod 的 classpath 中，
 * 使 mod 可以透明地使用新版本的 API。
 */
public class AdapterGenerator {
    private final String modId;
    private final List<MethodMapping> mappings;
    private final String adapterClassName;

    public AdapterGenerator(String modId, List<MethodMapping> mappings) {
        this.modId = modId;
        this.mappings = mappings;
        // 生成唯一 adapter 類名
        this.adapterClassName = "com/francium/bridge/" 
            + modId.replaceAll("[^a-zA-Z0-9]", "_") 
            + "_Francium_Adapter";
    }

    /**
     * 生成適配器的位元組碼。
     * 返回一個新的 JAR 檔案內容 (位元組陣列)。
     */
    public byte[] generate() {
        // 為簡化，此處生成單一類別的位元組
        // 完整實現會生成完整的 JAR
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        cw.visit(Opcodes.V21,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            adapterClassName,
            null,
            "java/lang/Object",
            null);
        
        // 添加版本註解
        AnnotationVisitor av = cw.visitAnnotation("Lcom/francium/bridge/GeneratedAdapter;", true);
        av.visit("modId", modId);
        av.visit("mappingCount", mappings.size());
        av.visitEnd();
        
        // ★ BUG FIX: 生成私有建構函數，防止實例化，同時滿足 JVM 類別驗證需求
        MethodVisitor constructor = cw.visitMethod(
            Opcodes.ACC_PRIVATE,
            "<init>",
            "()V",
            null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        
        // 為每個映射生成靜態橋接方法
        for (MethodMapping mapping : mappings) {
            generateBridgeMethod(cw, mapping);
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * 生成單個橋接方法。
     * 
     * 策略:
     * 1. 相同簽名: 直接轉發
     * 2. 參數類型變更: 插入類型轉換
     * 3. 參數順序變更: 重排參數
     * 4. 返回值變更: 插入轉換程式碼
     */
    private void generateBridgeMethod(ClassWriter cw, MethodMapping mapping) {
        String sourceName = mapping.source().name();
        String sourceDesc = mapping.source().descriptor();
        String targetOwner = mapping.target().owner().replace('.', '/');
        String targetName = mapping.target().name();
        String targetDesc = mapping.target().descriptor();
        
        // 決定生成的橋接方法簽名
        String bridgeName = "bridge$" + sourceName;
        
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            bridgeName,
            sourceDesc,  // 保持來源簽名作為橋接入口
            null,
            null);
        
        mv.visitCode();
        
        // 策略 1: 簽名完全相同 → 簡單轉發
        if (sourceDesc.equals(targetDesc)) {
            generateSimpleForward(mv, targetOwner, targetName, sourceDesc, mapping.confidence());
        }
        // 策略 2: 參數不同 → 插入轉換
        else {
            generateAdaptedForward(mv, mapping, targetOwner, targetName);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateSimpleForward(MethodVisitor mv, String targetOwner, 
                                        String targetName, String desc, float confidence) {
        // 使用 parseParams 正確解析參數型別（支援 L 物件、[ 陣列等），
        // 取代逐字元解析（逐字元無法正確處理物件型別如 Ljava/lang/String;）
        List<String> params = parseParams(desc);
        int slot = 0;
        for (String type : params) {
            mv.visitVarInsn(getLoadOpcode(type), slot);
            if (type.equals("J") || type.equals("D")) {
                slot += 2;
            } else {
                slot++;
            }
        }
        
        // 呼叫目標方法
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner, targetName, desc, false);
        
        // 返回
        char returnType = desc.charAt(desc.indexOf(')') + 1);
        switch (returnType) {
            case 'V' -> mv.visitInsn(Opcodes.RETURN);
            case 'I', 'Z', 'C', 'B', 'S' -> mv.visitInsn(Opcodes.IRETURN);
            case 'J' -> mv.visitInsn(Opcodes.LRETURN);
            case 'F' -> mv.visitInsn(Opcodes.FRETURN);
            case 'D' -> mv.visitInsn(Opcodes.DRETURN);
            default -> mv.visitInsn(Opcodes.ARETURN);
        }
    }

    private void generateAdaptedForward(MethodVisitor mv, MethodMapping mapping,
                                         String targetOwner, String targetName) {
        String sourceDesc = mapping.source().descriptor();
        String targetDesc = mapping.target().descriptor();
        
        // 解析參數
        List<String> sourceParams = parseParams(sourceDesc);
        List<String> targetParams = parseParams(targetDesc);
        
        // 簡單策略: 計算每個參數在區域變數表中的 slot 位置
        int slot = 0;
        Map<Integer, Integer> paramSlots = new HashMap<>();
        
        for (int i = 0; i < sourceParams.size(); i++) {
            paramSlots.put(i, slot);
            String type = sourceParams.get(i);
            if (type.equals("J") || type.equals("D")) {
                slot += 2;
            } else {
                slot++;
            }
        }
        
        // 依照目標順序載入並推入堆疊（處理型別轉換）
        slot = 0;
        for (int i = 0; i < targetParams.size(); i++) {
            String targetType = targetParams.get(i);
            if (i < sourceParams.size()) {
                String sourceType = sourceParams.get(i);
                mv.visitVarInsn(getLoadOpcode(sourceType), paramSlots.get(i));
                
                // 如果需要類型轉換
                if (!sourceType.equals(targetType)) {
                    insertTypeConversion(mv, sourceType, targetType);
                }
            } else {
                // 目標需要更多參數，載入預設值
                insertDefaultValue(mv, targetType);
            }
        }
        
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner, targetName, targetDesc, false);
        
        String sourceReturn = parseReturnType(sourceDesc);
        String targetReturn = parseReturnType(targetDesc);
        
        if (!sourceReturn.equals(targetReturn)) {
            insertTypeConversion(mv, targetReturn, sourceReturn);
        }
        
        mv.visitInsn(getReturnOpcode(sourceReturn));
    }

    // --- ASM helpers ---
    private List<String> parseParams(String desc) {
        List<String> params = new ArrayList<>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            if (desc.charAt(i) == 'L') {
                int end = desc.indexOf(';', i);
                params.add(desc.substring(i, end + 1));
                i = end + 1;
            } else if (desc.charAt(i) == '[') {
                int start = i;
                while (desc.charAt(i) == '[') i++;
                if (desc.charAt(i) == 'L') {
                    i = desc.indexOf(';', i) + 1;
                } else {
                    i++;
                }
                params.add(desc.substring(start, i));
            } else {
                params.add(String.valueOf(desc.charAt(i)));
                i++;
            }
        }
        return params;
    }

    private String parseReturnType(String desc) {
        int parenClose = desc.indexOf(')');
        return desc.substring(parenClose + 1);
    }

    private int getLoadOpcode(String jvmType) {
        return switch (jvmType.charAt(0)) {
            case 'I', 'Z', 'C', 'B', 'S' -> Opcodes.ILOAD;
            case 'J' -> Opcodes.LLOAD;
            case 'F' -> Opcodes.FLOAD;
            case 'D' -> Opcodes.DLOAD;
            default -> Opcodes.ALOAD;
        };
    }

    private int getReturnOpcode(String jvmType) {
        return switch (jvmType.charAt(0)) {
            case 'V' -> Opcodes.RETURN;
            case 'I', 'Z', 'C', 'B', 'S' -> Opcodes.IRETURN;
            case 'J' -> Opcodes.LRETURN;
            case 'F' -> Opcodes.FRETURN;
            case 'D' -> Opcodes.DRETURN;
            default -> Opcodes.ARETURN;
        };
    }

    private void insertTypeConversion(MethodVisitor mv, String from, String to) {
        // 基本類型轉換
        if (from.equals("I") && to.equals("J")) {
            mv.visitInsn(Opcodes.I2L);
        } else if (from.equals("I") && to.equals("F")) {
            mv.visitInsn(Opcodes.I2F);
        } else if (from.equals("I") && to.equals("D")) {
            mv.visitInsn(Opcodes.I2D);
        } else if (from.equals("J") && to.equals("I")) {
            mv.visitInsn(Opcodes.L2I);
        } else if (from.equals("F") && to.equals("I")) {
            mv.visitInsn(Opcodes.F2I);
        } else if (from.equals("D") && to.equals("I")) {
            mv.visitInsn(Opcodes.D2I);
        }
    }

    private void insertDefaultValue(MethodVisitor mv, String jvmType) {
        switch (jvmType.charAt(0)) {
            case 'I', 'Z', 'C', 'B', 'S' -> mv.visitInsn(Opcodes.ICONST_0);
            case 'J' -> mv.visitInsn(Opcodes.LCONST_0);
            case 'F' -> mv.visitInsn(Opcodes.FCONST_0);
            case 'D' -> mv.visitInsn(Opcodes.DCONST_0);
            default -> mv.visitInsn(Opcodes.ACONST_NULL);
        }
    }
}
