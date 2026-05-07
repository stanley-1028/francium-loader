#!/bin/bash
# ============================================
#  Francium Mod Loader - Standalone Build & Test
#
#  Compiles all modules including ASM-dependent ones.
#  ASM jars are auto-downloaded if not present.
#
#  For full Gradle build: ./gradlew build
# ============================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build/classes"
TEST_DIR="$PROJECT_DIR/build/test-classes"
LIB_DIR="$PROJECT_DIR/build/lib"

echo "=========================================="
echo "  Francium Mod Loader - Standalone Build"
echo "=========================================="
echo ""

# ─── Resolve ASM dependency ──────────────────
ASM_VERSION="9.7"
ASM_JAR="$LIB_DIR/asm-$ASM_VERSION.jar"
ASM_TREE_JAR="$LIB_DIR/asm-tree-$ASM_VERSION.jar"
ASM_COMMONS_JAR="$LIB_DIR/asm-commons-$ASM_VERSION.jar"
ASM_URL="https://repo1.maven.org/maven2/org/ow2/asm"

rm -rf "$PROJECT_DIR/build"
mkdir -p "$BUILD_DIR" "$TEST_DIR" "$LIB_DIR"

download_if_missing() {
    local jar="$1"
    local url="$2"
    if [ ! -f "$jar" ]; then
        echo "  ↓ Downloading $(basename $jar)..."
        if command -v curl &>/dev/null; then
            curl -sL "$url" -o "$jar"
        elif command -v wget &>/dev/null; then
            wget -q "$url" -O "$jar"
        else
            echo "  ✗ Cannot download: need curl or wget"
            exit 1
        fi
        if [ -f "$jar" ]; then
            echo "    ✓ Downloaded $(stat -f%z "$jar" 2>/dev/null || stat -c%s "$jar" 2>/dev/null || echo "?") bytes"
        fi
    fi
}

download_if_missing "$ASM_JAR" "$ASM_URL/$ASM_VERSION/asm-$ASM_VERSION.jar"
download_if_missing "$ASM_TREE_JAR" "$ASM_URL/$ASM_VERSION/asm-tree-$ASM_VERSION.jar"
download_if_missing "$ASM_COMMONS_JAR" "$ASM_URL/$ASM_VERSION/asm-commons-$ASM_VERSION.jar"

ASM_CP="$ASM_JAR:$ASM_TREE_JAR:$ASM_COMMONS_JAR"

# ─── Source files ─────────────────────────────
CORE_FILES=(
    # Resolver (pure Java)
    francium-resolver/src/main/java/com/francium/resolver/model/SemanticVersion.java
    francium-resolver/src/main/java/com/francium/resolver/model/DependencyConstraint.java
    francium-resolver/src/main/java/com/francium/resolver/sat/SATDependencyResolver.java
    # AI Bridge - all modules (including ASM-dependent)
    francium-ai-bridge/src/main/java/com/francium/ai/mapping/MethodSignature.java
    francium-ai-bridge/src/main/java/com/francium/ai/mapping/MappingDatabase.java
    francium-ai-bridge/src/main/java/com/francium/ai/predictor/CompatibilityPredictor.java
    francium-ai-bridge/src/main/java/com/francium/ai/analysis/BytecodeAnalyzer.java
    francium-ai-bridge/src/main/java/com/francium/ai/adapter/AdapterGenerator.java
    francium-ai-bridge/src/main/java/com/francium/ai/adapter/VersionBridge.java
    # Core
    francium-core/src/main/java/com/francium/graph/ModGraph.java
    francium-core/src/main/java/com/francium/loader/ModManifest.java
    francium-core/src/main/java/com/francium/loader/LoaderConfig.java
    francium-core/src/main/java/com/francium/loader/FranciumLoader.java
    francium-core/src/main/java/com/francium/classloader/ParallelModClassLoader.java
    francium-core/src/main/java/com/francium/launch/FranciumTweaker.java
    francium-core/src/main/java/com/francium/bootstrap/FranciumBootstrap.java
    # Profiler
    francium-profiler/src/main/java/com/francium/profiler/memory/MemoryManager.java
    # Server
    francium-server/src/main/java/com/francium/server/sync/ServerSyncProtocol.java
    francium-server/src/main/java/com/francium/server/validate/ModValidator.java
    # Manager
    francium-manager/src/main/java/com/francium/manager/package/PackageManager.java
)

TEST_FILES=(
    francium-resolver/src/test/java/com/francium/SemanticVersionTest.java
    francium-resolver/src/test/java/com/francium/DependencyConstraintTest.java
    francium-resolver/src/test/java/com/francium/SATDependencyResolverTest.java
    francium-core/src/test/java/com/francium/ModGraphTest.java
    francium-ai-bridge/src/test/java/com/francium/CompatibilityPredictorTest.java
    francium-ai-bridge/src/test/java/com/francium/CrossVersionTest.java
)

# ─── Compile ──────────────────────────────────
echo "Compiling ${#CORE_FILES[@]} source files (including ASM)..."
javac -d "$BUILD_DIR" --release 21 \
    -cp "$ASM_CP" \
    -Xlint:-unchecked -Xlint:-deprecation \
    "${CORE_FILES[@]}" 2>&1
echo "  ✓ Compilation OK"
echo ""

echo "Compiling ${#TEST_FILES[@]} test files..."
javac -d "$TEST_DIR" --release 21 \
    -cp "$BUILD_DIR:$ASM_CP" \
    -Xlint:-unchecked -Xlint:-deprecation \
    "${TEST_FILES[@]}" 2>&1
echo "  ✓ Compilation OK"
echo ""

# ─── Package JAR ──────────────────────────────
echo "Packaging francium-loader-standalone.jar..."
cd "$BUILD_DIR"
jar cf "$PROJECT_DIR/build/libs/francium-loader-standalone.jar" \
    $(find . -name "*.class")
cd "$PROJECT_DIR"
echo "  ✓ $(stat -f%z "$PROJECT_DIR/build/libs/francium-loader-standalone.jar" 2>/dev/null || stat -c%s "$PROJECT_DIR/build/libs/francium-loader-standalone.jar" 2>/dev/null || echo "?") bytes"
echo ""

# ─── Run Tests ────────────────────────────────
echo "=========================================="
echo "  Running Tests (JUnit 5 console)"
echo "=========================================="
echo ""

# Download JUnit console launcher if missing
JUNIT_JAR="$LIB_DIR/junit-platform-console-standalone-1.10.2.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
download_if_missing "$JUNIT_JAR" "$JUNIT_URL"

java -jar "$JUNIT_JAR" \
    --class-path "$BUILD_DIR:$TEST_DIR:$ASM_CP" \
    --select-package "com.francium" \
    --details=tree \
    --fail-if-no-tests \
    2>&1 || true

echo ""
echo "=========================================="
echo "  Build Summary"
echo "=========================================="
ls -lh "$PROJECT_DIR/build/libs/" 2>/dev/null || echo "  No JARs in build/libs/"
echo "=========================================="
