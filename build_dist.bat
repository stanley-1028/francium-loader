@echo off
REM ============================================
REM  Francium Loader - Distribution Builder
REM  Builds: JAR + Windows EXE + Portable ZIP
REM ============================================
setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0
set DIST_DIR=%PROJECT_DIR%build\dist

for /f "tokens=2 delims=:" %%v in ('call "%PROJECT_DIR%gradlew.bat" properties -q --no-daemon ^| findstr /b "version:"') do (
    set "VERSION=%%v"
)
set "VERSION=!VERSION: =!"
if not defined VERSION (
    echo FAILED: Unable to determine Gradle project version
    exit /b 1
)

echo ==========================================
echo   Francium Loader v%VERSION% - Distribution
echo ==========================================
echo.

REM Step 1: Clean
echo [1/6] Cleaning build directory...
if exist "%PROJECT_DIR%build" (
    rmdir /s /q "%PROJECT_DIR%build"
)
mkdir "%DIST_DIR%" 2>nul
echo   OK

REM Step 2: Run standalone build (core tests)
echo [2/6] Running core build & tests...
call "%PROJECT_DIR%build.bat"
if errorlevel 1 (
    echo   FAILED: Core build failed
    exit /b 1
)
echo   OK

REM Step 3: Build shadow JAR via Gradle
echo [3/6] Building shadow JAR...
cd "%PROJECT_DIR%"
call "%PROJECT_DIR%gradlew.bat" shadowJar --no-daemon
if errorlevel 1 (
    echo   FAILED: Shadow JAR build failed
    exit /b 1
)
echo   OK

REM Step 4: Find shadow JAR
echo [4/6] Locating shadow JAR...
for /r "%PROJECT_DIR%build\libs" %%f in (*-all.jar) do set SHADOW_JAR=%%f
if not defined SHADOW_JAR (
    echo   FAILED: Shadow JAR not found
    exit /b 1
)
echo   Found: %SHADOW_JAR%

REM Step 5: Copy to dist
echo [5/6] Copying to distribution...
copy "%SHADOW_JAR%" "%DIST_DIR%\" >nul
copy "%PROJECT_DIR%README.md" "%DIST_DIR%\" >nul
copy "%PROJECT_DIR%LICENSE" "%DIST_DIR%\" >nul
copy "%PROJECT_DIR%CHANGELOG.md" "%DIST_DIR%\" >nul

REM Build portable ZIP
cd "%DIST_DIR%"
mkdir francium-loader-%VERSION% 2>nul
copy *.jar francium-loader-%VERSION%\ >nul
copy *.md francium-loader-%VERSION%\ >nul

REM Check if jpackage is available for EXE
where jpackage >nul 2>nul
if !errorlevel! equ 0 (
    echo [6/6] Building jpackage EXE installer...
    cd "%PROJECT_DIR%"
    call "%PROJECT_DIR%gradlew.bat" jpackageExe --no-daemon
    if !errorlevel! equ 0 (
        for /r "%PROJECT_DIR%build\jpackage-output" %%f in (*.exe) do (
            copy "%%f" "%DIST_DIR%\" >nul
        )
        echo   ✅ Windows EXE installer built
    ) else (
        echo   ⚠ jpackage EXE build skipped (may require WiX Toolset)
    )
) else (
    echo [6/6] jpackage not found, skipping native installer
    echo   ℹ  Install JDK 21+ with jpackage to build native installers
)

echo.
echo ==========================================
echo   Distribution Complete!
echo   Output: %DIST_DIR%
echo ==========================================
dir "%DIST_DIR%"

endlocal
