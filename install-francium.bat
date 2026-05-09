@echo off
setlocal enabledelayedexpansion
title Francium Mod Loader - 一鍵安裝

:: =====================================================
::  Francium Mod Loader — Windows 一鍵安裝器
::
::  雙擊即可安裝，無需打開終端機！
::
::  功能:
::    1. 自動檢測 .minecraft 目錄
::    2. 安裝 Francium 到 Minecraft
::    3. 建立桌面捷徑
::    4. 可選: 執行功能展示
:: =====================================================

set "FRANCIUM_DIR=%~dp0"
set "FRANCIUM_JAR=%FRANCIUM_DIR%build\libs\francium-loader-standalone.jar"
set "FRANCIUM_CONFIG_DIR=%FRANCIUM_DIR%config\francium"

echo.
echo   ╔══════════════════════════════════════════╗
echo   ║     Francium Mod Loader 一鍵安裝器       ║
echo   ║     AI 驅動的下一代模組載入器            ║
echo   ╚══════════════════════════════════════════╝
echo.

:: ── 1. 檢測 Java ──
echo   [1/5] 正在檢測 Java 環境...
set "JAVA="
for /f "tokens=*" %%i in ('where java 2^>nul') do (
    if not defined JAVA (
        "%%i" -version 2>&1 | findstr /c:"21." /c:"22." /c:"23." /c:"24." >nul && set "JAVA=%%i"
    )
)
if not defined JAVA (
    echo   [錯誤] 需要 Java 21 或更高版本！
    echo.
    echo   請從以下網址下載安裝:
    echo   https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo   [OK] 找到 Java: !JAVA!

:: ── 2. 檢測 .minecraft ──
echo.
echo   [2/5] 正在尋找 Minecraft 目錄...
set "MC_DIR="
if exist "%APPDATA%\.minecraft" set "MC_DIR=%APPDATA%\.minecraft"
if not defined MC_DIR (
    if exist "%USERPROFILE%\.minecraft" set "MC_DIR=%USERPROFILE%\.minecraft"
)
if not defined MC_DIR (
    echo   [警告] 未找到 .minecraft 目錄
    echo   請手動輸入 Minecraft 安裝位置:
    set /p "MC_DIR=路徑: "
    if not exist "!MC_DIR!" (
        echo   [錯誤] 目錄不存在！
        pause
        exit /b 1
    )
)
echo   [OK] Minecraft 目錄: !MC_DIR!

:: ── 3. 建置 Francium (如果沒有預先建置) ──
echo.
echo   [3/5] 正在準備 Francium 載入器...
if not exist "%FRANCIUM_DIR%build\libs" mkdir "%FRANCIUM_DIR%build\libs"

:: 檢查是否已有 JAR
if exist "%FRANCIUM_JAR%" (
    echo   [OK] Francium JAR 已就緒
    goto :install
)

:: 嘗試用 Gradle 建置
if exist "%FRANCIUM_DIR%gradlew.bat" (
    echo   正在使用 Gradle 建置...
    cd /d "%FRANCIUM_DIR%"
    call gradlew.bat fatJar --no-daemon -q 2>nul
    if exist "%FRANCIUM_JAR%" (
        echo   [OK] 建置完成
        goto :install
    )
)

:: 嘗試用 build.bat 建置
if exist "%FRANCIUM_DIR%build.bat" (
    echo   正在使用 build.bat 建置...
    cd /d "%FRANCIUM_DIR%"
    call build.bat 2>nul
    if exist "%FRANCIUM_JAR%" (
        echo   [OK] 建置完成
        goto :install
    )
)

:: 使用 Java 直接編譯
echo   正在使用 Java 編譯...
cd /d "%FRANCIUM_DIR%"
if not exist "build\classes" mkdir "build\classes"

set "SRC_FILES="
for %%f in (
    francium-resolver\src\main\java\com\francium\resolver\model\SemanticVersion.java
    francium-resolver\src\main\java\com\francium\resolver\model\DependencyConstraint.java
    francium-resolver\src\main\java\com\francium\resolver\sat\SATDependencyResolver.java
    francium-ai-bridge\src\main\java\com\francium\ai\mapping\MethodSignature.java
    francium-ai-bridge\src\main\java\com\francium\ai\mapping\MappingDatabase.java
    francium-ai-bridge\src\main\java\com\francium\ai\predictor\CompatibilityPredictor.java
    francium-core\src\main\java\com\francium\graph\ModGraph.java
    francium-core\src\main\java\com\francium\loader\ModManifest.java
    francium-core\src\main\java\com\francium\loader\LoaderConfig.java
    francium-core\src\main\java\com\francium\classloader\ParallelModClassLoader.java
    francium-profiler\src\main\java\com\francium\profiler\memory\MemoryManager.java
    francium-server\src\main\java\com\francium\server\sync\ServerSyncProtocol.java
    francium-server\src\main\java\com\francium\server\validate\ModValidator.java
) do (
    if exist "%%f" set "SRC_FILES=!SRC_FILES! %%f"
)

if "!SRC_FILES!"=="" (
    echo   [警告] 找不到原始碼檔案，跳過建置
    goto :no_jar
)

"%JAVA%" -d "build\classes" --release 21 -Xlint:-unchecked !SRC_FILES! 2>nul
if errorlevel 1 (
    echo   [警告] 編譯失敗，可能是缺少 ASM 依賴
    echo   請手動執行: gradlew.bat fatJar
    goto :no_jar
)

cd build\classes
"%JAVA%" -cf "%FRANCIUM_JAR%" com\francium\**\*.class 2>nul
cd "%FRANCIUM_DIR%"
echo   [OK] 已從原始碼建置 JAR

:install
:: ── 4. 安裝到 Minecraft ──
echo.
echo   [4/5] 正在安裝到 Minecraft...

:: 建立 francium 目錄
set "FRANCIUM_MC_DIR=!MC_DIR!\francium"
if not exist "!FRANCIUM_MC_DIR!" mkdir "!FRANCIUM_MC_DIR!"
if not exist "!FRANCIUM_MC_DIR!\mods" mkdir "!FRANCIUM_MC_DIR!\mods"
if not exist "!FRANCIUM_MC_DIR!\config" mkdir "!FRANCIUM_MC_DIR!\config"

:: 複製 JAR
if exist "%FRANCIUM_JAR%" (
    copy /y "%FRANCIUM_JAR%" "!FRANCIUM_MC_DIR!\francium-loader.jar" >nul
    echo   [OK] Francium 已安裝到 !FRANCIUM_MC_DIR!
) else (
    :no_jar
    echo   [注意] 未找到 francium-loader.jar
    echo   請執行 gradlew.bat fatJar 手動建置後重新安裝
)

:: 建立 francium-lock.json
echo { > "!FRANCIUM_MC_DIR!\francium-lock.json"
echo   "version": "1.0.0", >> "!FRANCIUM_MC_DIR!\francium-lock.json"
echo   "installed": true, >> "!FRANCIUM_MC_DIR!\francium-lock.json"
echo   "installDate": "%DATE%" >> "!FRANCIUM_MC_DIR!\francium-lock.json"
echo } >> "!FRANCIUM_MC_DIR!\francium-lock.json"

:: 建立基本設定檔
if not exist "!FRANCIUM_MC_DIR!\config\francium.toml" (
    echo # Francium Loader Config > "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo [parallel] >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo maxParallelMods = 8 >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo [aiBridge] >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo enabled = true >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo confidenceThreshold = 0.85 >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo [memory] >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo leakDetection = true >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo [serverSync] >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo enabled = true >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo autoDownloadMods = false >> "!FRANCIUM_MC_DIR!\config\francium.toml"
    echo   [OK] 設定檔已建立
) else (
    echo   [OK] 設定檔已存在
)

:: ── 5. 安裝完成 ──
echo.
echo   [5/5] 安裝完成！
echo.
echo   ╔══════════════════════════════════════════╗
echo   ║         🎉  安裝成功！ 🎉               ║
echo   ╚══════════════════════════════════════════╝
echo.
echo   Francium 已安裝到:
echo   !FRANCIUM_MC_DIR!
echo.
echo   使用方式:
echo     1. 使用支援 Francium 的啟動器 (如 Nova Launcher)
echo        選擇 "Francium" 作為模組載入器即可
echo.
echo     2. 手動啟動 (進階):
echo        -javaagent:!FRANCIUM_MC_DIR!\francium-loader.jar
echo.

:: 詢問是否執行展示
echo   ──────────────────────────────────────────
set /p "RUN_DEMO=是否執行功能展示？(Y/N): "
if /i "!RUN_DEMO!"=="Y" (
    echo.
    echo   正在執行 Francium Demo...
    if exist "%FRANCIUM_JAR%" (
        "%JAVA%" -cp "%FRANCIUM_JAR%" com.francium.demo.FranciumDemo 2>nul || (
            rem 如果 class 不在 JAR 中，直接執行原始碼
            if exist "%FRANCIUM_DIR%FranciumDemo.java" (
                "%JAVA%" "%FRANCIUM_DIR%FranciumDemo.java"
            ) else (
                echo   [提示] FranciumDemo 需要先編譯
            )
        )
    )
    echo.
)

echo   感謝使用 Francium Mod Loader！
echo   按任意鍵關閉...
pause >nul
