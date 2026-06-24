@echo off
REM ============================================
REM  Francium Mod Loader - Core Build & Test
REM  No external deps (no ASM, Gson, Netty)
REM ============================================
setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0
set BUILD_DIR=%PROJECT_DIR%build\classes
set TEST_DIR=%PROJECT_DIR%build\test-classes

echo ==========================================
echo   Francium Mod Loader - Core Build
echo ==========================================
echo.

rmdir /s /q "%PROJECT_DIR%build" 2>nul
mkdir "%BUILD_DIR%" 2>nul
mkdir "%TEST_DIR%" 2>nul

echo Compiling 15 core source files...
javac -d "%BUILD_DIR%" --release 21 -Xlint:-unchecked ^
    francium-api\src\main\java\com\francium\api\PublicApi.java ^
    build-stubs\org\slf4j\Logger.java ^
    build-stubs\org\slf4j\LoggerFactory.java ^
    francium-resolver\src\main\java\com\francium\resolver\model\SemanticVersion.java ^
    francium-resolver\src\main\java\com\francium\resolver\model\DependencyConstraint.java ^
    francium-resolver\src\main\java\com\francium\resolver\sat\SATDependencyResolver.java ^
    francium-ai-bridge\src\main\java\com\francium\ai\mapping\MethodSignature.java ^
    francium-ai-bridge\src\main\java\com\francium\ai\mapping\MappingDatabase.java ^
    francium-ai-bridge\src\main\java\com\francium\ai\predictor\CompatibilityPredictor.java ^
    francium-core\src\main\java\com\francium\graph\ModGraph.java ^
    francium-core\src\main\java\com\francium\loader\ModManifest.java ^
    francium-core\src\main\java\com\francium\loader\LoaderConfig.java ^
    francium-core\src\main\java\com\francium\classloader\ParallelModClassLoader.java ^
    francium-profiler\src\main\java\com\francium\profiler\memory\MemoryManager.java ^
    francium-server\src\main\java\com\francium\server\sync\ServerSyncProtocol.java ^
    francium-server\src\main\java\com\francium\server\validate\ModValidator.java
if errorlevel 1 (echo FAILED & exit /b 1)
echo   OK

echo Compiling 5 test files...
javac -d "%TEST_DIR%" --release 21 -cp "%BUILD_DIR%" -Xlint:-unchecked ^
    francium-resolver\src\test\java\com\francium\SemanticVersionTest.java ^
    francium-resolver\src\test\java\com\francium\DependencyConstraintTest.java ^
    francium-resolver\src\test\java\com\francium\SATDependencyResolverTest.java ^
    francium-core\src\test\java\com\francium\ModGraphTest.java ^
    francium-ai-bridge\src\test\java\com\francium\CompatibilityPredictorTest.java
if errorlevel 1 (echo FAILED & exit /b 1)
echo   OK

echo.
echo ==========================================
echo   Running Core Tests
echo ==========================================
echo.

set CP=%BUILD_DIR%;%TEST_DIR%

echo --- SemanticVersionTest ---
java -cp "%CP%" com.francium.SemanticVersionTest
if errorlevel 1 (echo   SUITE FAILED) else (echo   SUITE PASSED)
echo.

echo --- DependencyConstraintTest ---
java -cp "%CP%" com.francium.DependencyConstraintTest
if errorlevel 1 (echo   SUITE FAILED) else (echo   SUITE PASSED)
echo.

echo --- ModGraphTest ---
java -cp "%CP%" com.francium.ModGraphTest
if errorlevel 1 (echo   SUITE FAILED) else (echo   SUITE PASSED)
echo.

echo --- CompatibilityPredictorTest ---
java -cp "%CP%" com.francium.CompatibilityPredictorTest
if errorlevel 1 (echo   SUITE FAILED) else (echo   SUITE PASSED)
echo.

echo --- SATDependencyResolverTest ---
java -cp "%CP%" com.francium.SATDependencyResolverTest
if errorlevel 1 (echo   SUITE FAILED) else (echo   SUITE PASSED)
echo.

echo ==========================================
echo   Build ^& Test Complete
echo ==========================================
