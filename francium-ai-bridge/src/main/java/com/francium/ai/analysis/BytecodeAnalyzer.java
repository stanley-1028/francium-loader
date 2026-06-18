package com.francium.ai.analysis;

import com.francium.api.PublicApi;
import com.francium.ai.mapping.MethodSignature;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字節碼分析器，用 ASM 解析模組 JAR 中的所有類別。
 * 
 * 功能:
 * 1. 提取所有外部方法呼叫 (INVOKEVIRTUAL, INVOKESTATIC, etc.)
 * 2. 提取欄位存取 (GETFIELD, PUTFIELD, etc.)
 * 3. 記錄呼叫的結構特徵 (供結構匹配使用)
 * 4. 檢測可能的不相容呼叫模式
 */
@PublicApi
public class BytecodeAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BytecodeAnalyzer.class);

    /**
     * 提取模組中所有的外部方法呼叫。
     * 「外部」指不屬於該模組的類別的方法，典型為 Minecraft 或其它模組的 API。
     */
    public Set<MethodSignature> extractExternalCalls(Path modJar) throws IOException {
        Set<MethodSignature> calls = new HashSet<>();
        Set<String> internalClasses = new HashSet<>();
        
        // Pass 1: 收集所有內部類別名稱
        try (JarFile jar = new JarFile(modJar.toFile())) {
            jar.stream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(e -> {
                    String className = e.getName()
                        .replace('/', '.')
                        .replace(".class", "");
                    internalClasses.add(className);
                });
        }
        
        // Pass 2: 分析每個類別的外部呼叫
        try (JarFile jar = new JarFile(modJar.toFile())) {
            jar.stream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(e -> {
                    try {
                        analyzeClass(jar.getInputStream(e), internalClasses, calls);
                    } catch (IOException ex) {
                        LOGGER.error("Failed to analyze: " + e.getName());
                    }
                });
        }
        
        return calls;
    }

    private void analyzeClass(InputStream classStream, Set<String> internalClasses,
                             Set<MethodSignature> calls) throws IOException {
        ClassReader reader = new ClassReader(classStream);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        
        String className = classNode.name.replace('/', '.');
        
        for (MethodNode method : classNode.methods) {
            analyzeMethod(method, classNode.name, internalClasses, calls);
        }
    }

    private void analyzeMethod(MethodNode method, String owner, 
                               Set<String> internalClasses, Set<MethodSignature> calls) {
        InsnList instructions = method.instructions;
        
        for (AbstractInsnNode insn : instructions) {
            if (insn instanceof MethodInsnNode methodInsn) {
                String targetClass = methodInsn.owner.replace('/', '.');
                
                // 只記錄外部呼叫
                if (!isInternalClass(targetClass, internalClasses) && !isJavaStdLib(targetClass)) {
                    MethodSignature sig = new MethodSignature(
                        targetClass,
                        methodInsn.name,
                        methodInsn.desc
                    );
                    // ★ BUG FIX: 避免重複 add 時覆蓋 instructionCount
                    if (!calls.contains(sig)) {
                        sig.setInstructionCount(instructions.size());
                        calls.add(sig);
                    }
                }
            } else if (insn instanceof FieldInsnNode fieldInsn) {
                // 記錄欄位存取作為結構特徵
                String targetClass = fieldInsn.owner.replace('/', '.');
                if (!isInternalClass(targetClass, internalClasses) && !isJavaStdLib(targetClass)) {
                    // 將欄位存取與其所在方法關聯 (用於後續結構匹配)
                    // 此處簡化處理
                }
            }
        }
    }

    /**
     * 分析類別的呼叫圖 (call graph)。
     * 用於結構匹配時比對方法的呼叫模式。
     */
    public Map<String, Set<String>> buildCallGraph(Path modJar) throws IOException {
        Map<String, Set<String>> callGraph = new HashMap<>();
        Set<String> internalClasses = new HashSet<>();
        
        try (JarFile jar = new JarFile(modJar.toFile())) {
            jar.stream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(e -> {
                    String className = e.getName()
                        .replace('/', '.')
                        .replace(".class", "");
                    internalClasses.add(className);
                });
            
            jar.stream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(e -> {
                    try {
                        ClassReader reader = new ClassReader(jar.getInputStream(e));
                        ClassNode cn = new ClassNode();
                        reader.accept(cn, ClassReader.EXPAND_FRAMES);
                        
                        for (MethodNode mn : cn.methods) {
                            String methodKey = cn.name + "." + mn.name + mn.desc;
                            Set<String> callees = new HashSet<>();
                            
                            for (AbstractInsnNode insn : mn.instructions) {
                                if (insn instanceof MethodInsnNode min) {
                                    callees.add(min.owner + "." + min.name + min.desc);
                                }
                            }
                            
                            if (!callees.isEmpty()) {
                                callGraph.put(methodKey, callees);
                            }
                        }
                    } catch (IOException ignored) {}
                });
        }
        
        return callGraph;
    }

    private boolean isInternalClass(String className, Set<String> internalClasses) {
        // ★ BUG FIX: startsWith 必須以 "." 為分隔，否則 "ModA" 會誤匹配 "ModABuilder"
        //   正確的包前綴比對應該用 internal + "." 作為前綴
        //   同時支援 / 和 . 兩種分隔符（JVM 內部使用 /，外部使用 .）
        if (internalClasses.contains(className)) return true;
        String dotSep = className.replace('/', '.');
        String slashSep = className.replace('.', '/');
        for (String internal : internalClasses) {
            String normalized = internal.replace('/', '.');
            if (dotSep.equals(normalized) || dotSep.startsWith(normalized + ".")) return true;
            if (dotSep.startsWith(internal + "/")) return true;
        }
        return false;
    }

    private boolean isJavaStdLib(String className) {
        return className.startsWith("java.") 
            || className.startsWith("javax.")
            || className.startsWith("jdk.")
            || className.startsWith("sun.")
            || className.startsWith("com.sun.");
    }
}
