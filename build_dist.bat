@echo off
REM ============================================
REM  Francium Mod Loader — Distribution Builder (Windows)
REM  Builds: fat JAR + portable ZIP + .exe launcher
REM ============================================
setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Java\jdk-23"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

echo =====================================
echo  Francium Mod Loader — Windows Dist
echo =====================================
echo.

REM Clean previous dist
echo Cleaning previous distributions...
if exist "build\distributions" (
    rmdir /s /q "build\distributions"
)
mkdir "build\distributions" 2>nul

REM Step 1: Run core build (standalone, no deps)
echo =====================================
echo Step 1: Running core build + tests
echo =====================================
call build.bat
if %ERRORLEVEL% NEQ 0 (
    echo FAILED at core build step!
    pause
    exit /b %ERRORLEVEL%
)

REM Step 2: Build fat JAR via Gradle (with all deps)
echo.
echo =====================================
echo Step 2: Building fat JAR (all subprojects)
echo =====================================
call gradlew.bat shadowJar -x test --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo FAILED at shadowJar step!
    pause
    exit /b %ERRORLEVEL%
)

REM Find the fat jar
set FAT_JAR=
for %%f in (build\libs\*-all.jar) do set "FAT_JAR=%%f"
if not defined FAT_JAR (
    echo ✗ No fat JAR found in build\libs\
    pause
    exit /b 1
)
echo  ✓ Fat JAR: %FAT_JAR%

REM Step 3: Create portable distribution folder
echo.
echo =====================================
echo Step 3: Creating portable distribution
echo =====================================
set DIST_DIR=build\distributions\francium-loader
if exist "!DIST_DIR!" rmdir /s /q "!DIST_DIR!"
mkdir "!DIST_DIR!"
mkdir "!DIST_DIR!\bin"
mkdir "!DIST_DIR!\lib"
mkdir "!DIST_DIR!\examples"
mkdir "!DIST_DIR!\docs"

REM Copy fat JAR
copy /y "%FAT_JAR%" "!DIST_DIR!\lib\francium-loader.jar" >nul

REM Create launcher scripts (using temp file to avoid batch escaping issues)
set "LF_DIR=!DIST_DIR!"
set "LF_BIN=!DIST_DIR!\bin"

REM bin\francium.bat
echo @echo off > "%LF_BIN%\francium.bat"
echo REM Francium Mod Loader - Windows Launcher >> "%LF_BIN%\francium.bat"
echo set "SCRIPT_DIR=%%~dp0" >> "%LF_BIN%\francium.bat"
echo set "LIB_DIR=%%SCRIPT_DIR%%..\lib" >> "%LF_BIN%\francium.bat"
echo if not defined JAVA_HOME ( >> "%LF_BIN%\francium.bat"
echo     echo Java not found. Please install JDK 21+. >> "%LF_BIN%\francium.bat"
echo     pause >> "%LF_BIN%\francium.bat"
echo     exit /b 1 >> "%LF_BIN%\francium.bat"
echo ) >> "%LF_BIN%\francium.bat"
echo "%%JAVA_HOME%%\bin\java" -javaagent:"%%LIB_DIR%%\francium-loader.jar" -jar "%%LIB_DIR%%\francium-loader.jar" %%* >> "%LF_BIN%\francium.bat"
echo pause >> "%LF_BIN%\francium.bat"

REM francium.bat at root
echo @echo off > "%LF_DIR%\francium.bat"
echo REM Francium Mod Loader - Quick Launch >> "%LF_DIR%\francium.bat"
echo set "SCRIPT_DIR=%%~dp0" >> "%LF_DIR%\francium.bat"
echo java -javaagent:"%%SCRIPT_DIR%%lib\francium-loader.jar" -jar "%%SCRIPT_DIR%%lib\francium-loader.jar" %%* >> "%LF_DIR%\francium.bat"
echo pause >> "%LF_DIR%\francium.bat"

echo  ?OK Launcher scripts created

REM Copy README, LICENSE, examples
copy /y "README.md" "!DIST_DIR!" >nul 2>&1
copy /y "LICENSE" "!DIST_DIR!" >nul 2>&1
copy /y "CHANGELOG.md" "!DIST_DIR!" >nul 2>&1
copy /y "examples\*.*" "!DIST_DIR!\examples" >nul 2>&1
copy /y "docs\*.*" "!DIST_DIR!\docs" >nul 2>&1

REM Copy standalone JAR from core build (no deps version) if exists
if exist "build\libs\francium-loader-standalone.jar" (
    copy /y "build\libs\francium-loader-standalone.jar" "!DIST_DIR!\lib\francium-loader-core.jar" >nul
)

REM Step 4: Create portable ZIP
echo.
echo =====================================
echo Step 4: Creating portable ZIP
echo =====================================
cd build\distributions
powershell -Command "Compress-Archive -Path francium-loader -DestinationPath francium-loader-windows-x64.zip -Force"
if %ERRORLEVEL% NEQ 0 (
    echo ⚠ PowerShell Compress-Archive failed, trying 7z...
    if exist "C:\Program Files\7-Zip\7z.exe" (
        "C:\Program Files\7-Zip\7z.exe" a -tzip francium-loader-windows-x64.zip francium-loader >nul
    ) else (
        echo Using Java to create ZIP...
        cd ..\..
        call gradlew.bat distZip --no-daemon 2>nul
        cd build\distributions
    )
)
cd ..\..

echo.
echo =====================================
echo  ✅ DISTRIBUTION BUILD COMPLETE!
echo =====================================
echo.
echo 📦 Outputs:
if exist "build\distributions\francium-loader-windows-x64.zip" (
    for %%f in (build\distributions\francium-loader-windows-x64.zip) do echo  ✅ Portable ZIP: %%~ff (%%~zf bytes)
)
if exist "build\distributions\francium-loader\" (
    dir /s /b "build\distributions\francium-loader\" | find /c /v "" > temp_count.txt
    set /p FILE_COUNT=<temp_count.txt
    del temp_count.txt
    echo  ✅ Distribution folder: build\distributions\francium-loader\ (!FILE_COUNT! files)
)
echo.
echo 📦 Quick launcher: build\distributions\francium-loader\francium.bat
echo.
endlocal
pause
