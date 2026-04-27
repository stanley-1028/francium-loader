#!/bin/bash
# ============================================
#  Francium Mod Loader - Core Build & Test
#  Compiles only modules without external deps
#  (no ASM, Gson, Netty)
#  For full build: ./gradlew build
# ============================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build/classes"
TEST_DIR="$PROJECT_DIR/build/test-classes"

echo "=========================================="
echo "  Francium Mod Loader - Core Build"
echo "=========================================="
echo "  Modules: resolver, ai-bridge(core),"
echo "  core(graph/classloader/manifest/config),"
echo "  profiler, server"
echo "=========================================="
echo ""

rm -rf "$PROJECT_DIR/build"
mkdir -p "$BUILD_DIR" "$TEST_DIR"

# Core source files (no external deps)
CORE_FILES=(
    # Resolver (pure Java)
    francium-resolver/src/main/java/com/francium/resolver/model/SemanticVersion.java
    francium-resolver/src/main/java/com/francium/resolver/model/DependencyConstraint.java
    francium-resolver/src/main/java/com/francium/resolver/sat/SATDependencyResolver.java
    # AI Bridge - core types (pure Java, no ASM)
    francium-ai-bridge/src/main/java/com/francium/ai/mapping/MethodSignature.java
    francium-ai-bridge/src/main/java/com/francium/ai/mapping/MappingDatabase.java
    francium-ai-bridge/src/main/java/com/francium/ai/predictor/CompatibilityPredictor.java
    # Core - foundation classes (pure Java)
    francium-core/src/main/java/com/francium/graph/ModGraph.java
    francium-core/src/main/java/com/francium/loader/ModManifest.java
    francium-core/src/main/java/com/francium/loader/LoaderConfig.java
    francium-core/src/main/java/com/francium/classloader/ParallelModClassLoader.java
    # Profiler (pure Java, java.lang.management.*)
    francium-profiler/src/main/java/com/francium/profiler/memory/MemoryManager.java
    # Server (pure Java, java.security.*)
    francium-server/src/main/java/com/francium/server/sync/ServerSyncProtocol.java
    francium-server/src/main/java/com/francium/server/validate/ModValidator.java
)

# Test files
TEST_FILES=(
    francium-resolver/src/test/java/com/francium/SemanticVersionTest.java
    francium-resolver/src/test/java/com/francium/DependencyConstraintTest.java
    francium-resolver/src/test/java/com/francium/SATDependencyResolverTest.java
    francium-core/src/test/java/com/francium/ModGraphTest.java
    francium-ai-bridge/src/test/java/com/francium/CompatibilityPredictorTest.java
)

SUCCESS=0
FAILURES=0

echo "Compiling ${#CORE_FILES[@]} core source files..."
javac -d "$BUILD_DIR" --release 21 -Xlint:-unchecked "${CORE_FILES[@]}" 2>&1
echo "  OK"
echo ""

echo "Compiling ${#TEST_FILES[@]} test files..."
javac -d "$TEST_DIR" --release 21 -cp "$BUILD_DIR" -Xlint:-unchecked "${TEST_FILES[@]}" 2>&1
echo "  OK"
echo ""

echo "=========================================="
echo "  Running Core Tests"
echo "=========================================="
echo ""

for testClass in \
    com.francium.SemanticVersionTest \
    com.francium.DependencyConstraintTest \
    com.francium.ModGraphTest \
    com.francium.CompatibilityPredictorTest \
    com.francium.SATDependencyResolverTest; do
    echo "--- $testClass ---"
    if java -cp "$BUILD_DIR:$TEST_DIR" "$testClass" 2>&1; then
        SUCCESS=$((SUCCESS + 1))
    else
        FAILURES=$((FAILURES + 1))
    fi
    echo ""
done

TOTAL=$((SUCCESS + FAILURES))
echo "=========================================="
echo "  Test Suites: $SUCCESS/$TOTAL passed"
if [ $FAILURES -eq 0 ]; then
    echo "  ALL PASS"
else
    echo "  $FAILURES suite(s) FAILED"
fi
echo "=========================================="

[ $FAILURES -eq 0 ]
