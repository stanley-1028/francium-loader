@echo off
setlocal enabledelayedexpansion
title Francium Mod Loader - 功能展示

:: =====================================================
::  Francium Mod Loader — 一鍵 Demo
::  雙擊即可看到 Francium 核心功能展示！
:: =====================================================

set "FRANCIUM_DIR=%~dp0"

echo.
echo   ╔══════════════════════════════════════════╗
echo   ║     Francium Mod Loader                  ║
echo   ║     AI 驅動的下一代模組載入器 — Demo     ║
echo   ╚══════════════════════════════════════════╝
echo.

:: 檢測 Java
for /f "tokens=*" %%i in ('where java 2^>nul') do (
    "%%i" -version 2>&1 | findstr /c:"21." /c:"22." /c:"23." /c:"24." >nul && set "JAVA=%%i"
)
if not defined JAVA (
    echo   [錯誤] 需要 Java 21+ 才能執行展示
    pause
    exit /b 1
)

:: 執行 FranciumDemo
set "DEMO_FILE=%FRANCIUM_DIR%FranciumDemo.java"

if exist "%DEMO_FILE%" (
    echo   正在編譯並啟動展示...
    echo.

    :: First compile the demo (it depends on other francium classes)
    set "BUILD_DIR=%FRANCIUM_DIR%build\classes"
    set "SRC_DIR=%FRANCIUM_DIR%build\demo-src"
    if not exist "%SRC_DIR%\com\francium\demo" mkdir "%SRC_DIR%\com\francium\demo"
    copy /y "%DEMO_FILE%" "%SRC_DIR%\com\francium\demo\FranciumDemo.java" >nul

    :: Compile demo + dependency sources
    "%JAVA%" -d "%BUILD_DIR%" --release 21 -Xlint:-unchecked ^
        -sourcepath "%SRC_DIR%" ^
        francium-resolver\src\main\java\com\franciumesolver\model\SemanticVersion.java ^
        francium-resolver\src\main\java\com\franciumesolver\model\DependencyConstraint.java ^
        francium-resolver\src\main\java\com\franciumesolver\sat\SATDependencyResolver.java ^
        francium-ai-bridge\src\main\java\com\francium\ai\mapping\MethodSignature.java ^
        francium-ai-bridge\src\main\java\com\francium\ai\mapping\MappingDatabase.java ^
        francium-ai-bridge\src\main\java\com\francium\ai\predictor\CompatibilityPredictor.java ^
        francium-core\src\main\java\com\francium\graph\ModGraph.java ^
        francium-core\src\main\java\com\francium\loader\ModManifest.java ^
        francium-core\src\main\java\com\francium\loader\LoaderConfig.java ^
        francium-core\src\main\java\com\francium\classloader\ParallelModClassLoader.java ^
        francium-profiler\src\main\java\com\francium\profiler\memory\MemoryManager.java ^
        francium-server\src\main\java\com\francium\server\sync\ServerSyncProtocol.java ^
        francium-server\src\main\java\com\francium\server\validate\ModValidator.java ^
        "%SRC_DIR%\com\francium\demo\FranciumDemo.java" 2>nul

    if errorlevel 1 (
        echo   [注意] 編譯過程有警告，但可能仍可執行
    )

    "%JAVA%" -cp "%BUILD_DIR%" com.francium.demo.FranciumDemo
) else (
    echo   [錯誤] 找不到 FranciumDemo.java
)

echo.
echo   展示結束。按任意鍵關閉...
pause >nul
