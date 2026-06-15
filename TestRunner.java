package com.francium;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * 輕量級測試執行器 (無需 JUnit/Gradle)。
 * 支援兩種測試格式：
 * 1. 獨立 main() 方法 (新格式，輸出 pass/fail 統計)
 * 2. @Test 標記方法 (舊格式，反射呼叫)
 */
public class TestRunner {
    private int passed = 0;
    private int failed = 0;
    private final List<String> failures = new ArrayList<>();

    // 測試類列表及其類名
    private static final String[] TEST_CLASSES = {
        "com.francium.SemanticVersionTest",
        "com.francium.DependencyConstraintTest",
        "com.francium.ModGraphTest",
        "com.francium.CompatibilityPredictorTest",
        "com.francium.SATDependencyResolverTest",
    };

    public static void main(String[] args) throws Exception {
        TestRunner runner = new TestRunner();

        System.out.println("========================================");
        System.out.println("  Francium Mod Loader - Test Suite");
        System.out.println("========================================");
        System.out.println();

        long start = System.currentTimeMillis();

        // 執行所有測試類別
        for (String className : TEST_CLASSES) {
            try {
                runner.runClass(className);
            } catch (Exception e) {
                System.err.println("  Failed to load test class: " + className);
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - start;

        // 報告
        System.out.println();
        System.out.println("========================================");
        System.out.printf("  Results: %d passed, %d failed (%.0fms)%n",
            runner.passed, runner.failed, (double) elapsed);
        System.out.println("========================================");

        if (!runner.failures.isEmpty()) {
            System.out.println();
            System.out.println("Failures:");
            for (String f : runner.failures) {
                System.out.println("  \u2717 " + f);
            }
        }

        // Throw instead of System.exit so the JVM can shut down gracefully
        // when TestRunner is called programmatically or as part of a larger suite.
        if (runner.failed > 0) {
            throw new RuntimeException("TestRunner: " + runner.failed + " test(s) failed");
        }
    }

    void runClass(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        System.out.println("--- " + clazz.getSimpleName() + " ---");

        // 策略 1: 嘗試呼叫 main() 方法 (新格式測試)
        try {
            Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
            if (mainMethod != null) {
                // 重新導向 System.out 以擷取輸出
                PrintStream originalOut = System.out;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream captureStream = new PrintStream(baos);
                System.setOut(captureStream);

                try {
                    mainMethod.invoke(null, (Object) new String[0]);
                } catch (InvocationTargetException e) {
                    System.setOut(originalOut);
                    Throwable cause = e.getCause();
                    failed++;
                    failures.add(clazz.getSimpleName() + ": " + cause.getMessage());
                    System.out.println("  \u2717 Exception: " + cause.getMessage());
                    return;
                }

                System.setOut(originalOut);

                // 解析擷取的輸出以統計 pass/fail
                String output = baos.toString();
                int classPassed = countMatches(output, "PASS");
                int classFailed = countMatches(output, "FAIL");

                passed += classPassed;
                failed += classFailed;

                // 顯示測試輸出
                for (String line : output.split("\\n")) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        System.out.println("  " + line);
                    }
                }

                System.out.printf("  \u2192 %d passed, %d failed%n", classPassed, classFailed);
                return;
            }
        } catch (NoSuchMethodException e) {
            // 沒有 main()，嘗試舊格式
        }

        // 策略 2: 舊格式 — 反射呼叫 test* 方法
        int classPassed = 0;
        int classFailed = 0;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("test") && method.getParameterCount() == 0) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                try {
                    method.invoke(instance);
                    classPassed++;
                    System.out.println("  \u2713 " + method.getName());
                } catch (InvocationTargetException e) {
                    classFailed++;
                    String msg = clazz.getSimpleName() + "." + method.getName() + ": "
                        + e.getCause().getMessage();
                    failures.add(msg);
                    System.out.println("  \u2717 " + method.getName() + " - " + e.getCause().getMessage());
                }
            }
        }

        passed += classPassed;
        failed += classFailed;
    }

    private int countMatches(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }
}
