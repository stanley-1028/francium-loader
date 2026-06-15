package com.francium;

import com.francium.ai.adapter.AdapterGenerator;
import com.francium.ai.adapter.VersionBridge.MethodMapping;
import com.francium.ai.mapping.MethodSignature;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 驗證 AdapterGenerator 產生的 bytecode 是有效的。
 *
 * 測試策略:
 * 1. 產生各種簽名型別的 adapter（基本型別、物件、陣列、多參數組合）
 * 2. 用 ASM ClassReader 載入 bytecode → 若格式錯誤會丟例外
 * 3. 驗證 generated class 的基礎結構（public/final、版本、註解）
 */
public class AdapterGeneratorTest {

    /** 產生一個簡單的 MethodSignature 用於測試。 */
    private static MethodSignature sig(String owner, String name, String desc) {
        return new MethodSignature(owner, name, desc);
    }

    @Test
    void generatedBytecodeIsValidClassFormat() {
        // 最簡單的 case: (I)I
        AdapterGenerator gen = new AdapterGenerator("test-mod", List.of(
            new MethodMapping(
                sig("com/example/Source", "add", "(I)I"),
                sig("com/example/Target", "add", "(I)I"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        assertNotNull(bytes);
        assertTrue(bytes.length > 50, "Bytecode should be at least 50 bytes");

        // 驗證 ASM 可正確解析 → 若格式錯誤會丟例外
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        assertEquals(Opcodes.V21, cn.version, "Should target Java 21");
        assertTrue((cn.access & Opcodes.ACC_PUBLIC) != 0, "Should be public");
        assertTrue((cn.access & Opcodes.ACC_FINAL) != 0, "Should be final");
    }

    @Test
    void generatedBytecodeWithObjectParamsIsValid() {
        // (Ljava/lang/String;Lnet/minecraft/core/BlockPos;)Z
        AdapterGenerator gen = new AdapterGenerator("block-mod", List.of(
            new MethodMapping(
                sig("com/example/Source", "isValid", "(Ljava/lang/String;Lnet/minecraft/core/BlockPos;)Z"),
                sig("com/example/Target", "isValid", "(Ljava/lang/String;Lnet/minecraft/core/BlockPos;)Z"),
                0.95f
            )
        ));

        byte[] bytes = gen.generate();
        assertNotNull(bytes);

        // 確認 bytecode 可被 ASM 正確解析（不會丟 ClassFormatError）
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

        assertEquals(1, cn.methods.size(), "Should have one bridge method");
        assertTrue(cn.methods.get(0).name.startsWith("bridge$"),
            "Method name should start with 'bridge$'");
    }

    @Test
    void generatedBytecodeWithMultipleMappingsIsValid() {
        List<MethodMapping> mappings = new ArrayList<>();
        mappings.add(new MethodMapping(
            sig("com/example/A", "getX", "()I"),
            sig("com/example/B", "getX", "()I"),
            1.0f
        ));
        mappings.add(new MethodMapping(
            sig("com/example/A", "getName", "()Ljava/lang/String;"),
            sig("com/example/B", "getName", "()Ljava/lang/String;"),
            1.0f
        ));
        mappings.add(new MethodMapping(
            sig("com/example/A", "setName", "(Ljava/lang/String;)V"),
            sig("com/example/B", "setName", "(Ljava/lang/String;)V"),
            1.0f
        ));

        AdapterGenerator gen = new AdapterGenerator("multi-mod", mappings);

        byte[] bytes = gen.generate();
        assertNotNull(bytes);

        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

        assertEquals(3, cn.methods.size(), "Should have 3 bridge methods");
    }

    @Test
    void generatedBytecodeWithLongAndDoubleParamsIsValid() {
        // (JI)D → long 和 double 各佔 2 slot
        AdapterGenerator gen = new AdapterGenerator("math-mod", List.of(
            new MethodMapping(
                sig("com/example/Calc", "compute", "(JI)D"),
                sig("com/example/Calc", "compute", "(JI)D"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        assertNotNull(bytes);

        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

        assertEquals(1, cn.methods.size());
    }

    @Test
    void generatedBytecodeWithArrayParamsIsValid() {
        // ([BII)[B
        AdapterGenerator gen = new AdapterGenerator("array-mod", List.of(
            new MethodMapping(
                sig("com/example/Buf", "slice", "([BII)[B"),
                sig("com/example/Buf", "slice", "([BII)[B"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        assertNotNull(bytes);

        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

        assertEquals(1, cn.methods.size());
    }

    @Test
    void generatedBytecodeAnnotationIsPresent() {
        AdapterGenerator gen = new AdapterGenerator("annotated-mod", List.of(
            new MethodMapping(
                sig("com/example/Source", "foo", "()V"),
                sig("com/example/Target", "foo", "()V"),
                0.9f
            )
        ));

        byte[] bytes = gen.generate();
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE);

        // 檢查 @GeneratedAdapter 註解
        assertFalse(cn.visibleAnnotations.isEmpty(),
            "Should have @GeneratedAdapter annotation");
        assertEquals("Lcom/francium/bridge/GeneratedAdapter;",
            cn.visibleAnnotations.get(0).desc,
            "Annotation descriptor should match GeneratedAdapter");
    }

    @Test
    void generatedBytecodeClassNameContainsModId() {
        AdapterGenerator gen = new AdapterGenerator("my_cool_mod", List.of(
            new MethodMapping(
                sig("com/example/Source", "foo", "()V"),
                sig("com/example/Target", "foo", "()V"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        ClassReader cr = new ClassReader(bytes);

        assertTrue(cr.getClassName().contains("my_cool_mod"),
            "Class name should contain the sanitized modId");
        assertTrue(cr.getClassName().contains("Francium_Adapter"),
            "Class name should end with 'Francium_Adapter'");
    }

    @Test
    void generatedBytecodeDoesNotThrowOnEmptyMappings() {
        AdapterGenerator gen = new AdapterGenerator("empty-mod", List.of());
        byte[] bytes = gen.generate();

        assertNotNull(bytes);

        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE);

        assertTrue(cn.methods.isEmpty(),
            "No mappings should mean no bridge methods");
    }

    @Test
    void generatedBytecodeReturnsCorrectType() {
        // String -> String (ARETURN)
        AdapterGenerator gen = new AdapterGenerator("string-mod", List.of(
            new MethodMapping(
                sig("com/example/Source", "greet", "()Ljava/lang/String;"),
                sig("com/example/Target", "greet", "()Ljava/lang/String;"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE);

        var method = cn.methods.get(0);
        assertEquals("()Ljava/lang/String;", method.desc,
            "Bridge method descriptor should match source descriptor");
    }

    @Test
    void generatedBytecodeWithEmptyReturnIsValid() {
        // ()V
        AdapterGenerator gen = new AdapterGenerator("void-mod", List.of(
            new MethodMapping(
                sig("com/example/Util", "reset", "()V"),
                sig("com/example/Util", "reset", "()V"),
                1.0f
            )
        ));

        byte[] bytes = gen.generate();
        ClassReader cr = new ClassReader(bytes);
        // 只需確認不拋例外即表示格式正確
        cr.accept(new ClassNode(), ClassReader.SKIP_CODE);
    }
}
