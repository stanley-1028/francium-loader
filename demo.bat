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

:: ===== 方法1: 使用 Gradle 已編譯的 class =====
if exist "%FRANCIUM_DIR%build\classes\java\main" (
    echo   使用 Gradle 已編譯的 class 執行展示...
    "%JAVA%" -cp "%FRANCIUM_DIR%build\classes\java\main" com.francium.demo.FranciumDemo 2>nul
    if !ERRORLEVEL! EQU 0 goto :done
)

:: ===== 方法2: 使用 build.bat 先編譯核心模組 =====
echo   正在編譯核心模組...
call "%FRANCIUM_DIR%build.bat" 2>nul
if exist "%FRANCIUM_DIR%build\classes" (
    echo   執行展示...
    "%JAVA%" -cp "%FRANCIUM_DIR%build\classes" com.francium.demo.FranciumDemo 2>nul
    if !ERRORLEVEL! EQU 0 goto :done
)

:: ===== 方法3: 用 Java 直接執行 FranciumDemo.java =====
if exist "%FRANCIUM_DIR%FranciumDemo.java" (
    echo   使用 Java 直接執行展示...
    "%JAVA%" "%FRANCIUM_DIR%FranciumDemo.java" 2>nul
    if !ERRORLEVEL! EQU 0 goto :done
)

:: ===== 所有方法都失敗 =====
echo   [注意] 無法自動執行展示。
echo   請先執行 gradlew.bat shadowJar 建置後再試。
echo   或直接執行: java FranciumDemo.java

:done
echo.
echo   展示結束。按任意鍵關閉...
pause >nul
endlocal
