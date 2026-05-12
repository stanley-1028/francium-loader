#!/bin/bash
# LF line endings enforced by .gitattributes
# ============================================
#  Francium Mod Loader — Distribution Builder (Linux/macOS)
#  Builds: fat JAR + portable ZIP + shell launcher
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "====================================="
echo "  Francium Mod Loader — Dist Build"
echo "====================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "✗ Java not found. Please install JDK 21+."
    exit 1
fi
echo "Using: $(java -version 2>&1 | head -1)"

# Clean previous dist
echo "Cleaning previous distributions..."
rm -rf build/distributions
mkdir -p build/distributions

# Detect platform
OS_NAME="$(uname -s)"
case "$OS_NAME" in
    Darwin*)  PLATFORM="mac-x64" ;;
    Linux*)   PLATFORM="linux-x64" ;;
    *)        PLATFORM="unknown" ;;
esac
echo "Platform: $PLATFORM"
echo ""

# Step 1: Run core standalone build
echo "====================================="
echo "Step 1: Running core build + tests"
echo "====================================="
bash build.sh
echo "  ✓ Core build done"
echo ""

# Step 2: Build fat JAR via Gradle
echo "====================================="
echo "Step 2: Building fat JAR (all subprojects)"
echo "====================================="
./gradlew shadowJar -x test --no-daemon
echo "  ✓ shadowJar done"

# Find the fat jar
FAT_JAR=$(ls build/libs/*-all.jar 2>/dev/null | head -1)
if [ -z "$FAT_JAR" ]; then
    echo "✗ No fat JAR found in build/libs/"
    exit 1
fi
echo "  ✓ Fat JAR: $(basename $FAT_JAR)"
echo ""

# Step 3: Create portable distribution folder
echo "====================================="
echo "Step 3: Creating portable distribution"
echo "====================================="
DIST_DIR="build/distributions/francium-loader"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"/{bin,lib,examples,docs}

# Copy fat JAR
cp "$FAT_JAR" "$DIST_DIR/lib/francium-loader.jar"

# Create shell launcher
cat > "$DIST_DIR/bin/francium" << 'LAUNCHER_EOF'
#!/bin/bash
# Francium Mod Loader — CLI Launcher
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/../lib"

if ! command -v java &> /dev/null; then
    echo "Java not found. Please install JDK 21+."
    exit 1
fi

exec java -javaagent:"$LIB_DIR/francium-loader.jar" \
          -jar "$LIB_DIR/francium-loader.jar" "$@"
LAUNCHER_EOF
chmod +x "$DIST_DIR/bin/francium"

# Create root-level convenience launcher
cat > "$DIST_DIR/francium" << 'LAUNCHER2_EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/bin/francium" "$@"
LAUNCHER2_EOF
chmod +x "$DIST_DIR/francium"

# Copy docs
cp README.md "$DIST_DIR/" 2>/dev/null || true
cp LICENSE "$DIST_DIR/" 2>/dev/null || true
cp CHANGELOG.md "$DIST_DIR/" 2>/dev/null || true
cp -r examples/* "$DIST_DIR/examples/" 2>/dev/null || true
cp -r docs/* "$DIST_DIR/docs/" 2>/dev/null || true

# Copy standalone core JAR if exists
if [ -f "build/libs/francium-loader-standalone.jar" ]; then
    cp "build/libs/francium-loader-standalone.jar" "$DIST_DIR/lib/francium-loader-core.jar"
fi

# Step 4: Create portable ZIP
echo ""
echo "====================================="
echo "Step 4: Creating portable ZIP"
echo "====================================="
cd build/distributions
zip -r "francium-loader-${PLATFORM}.zip" francium-loader/
cd "$SCRIPT_DIR"

echo ""
echo "====================================="
echo "  ✅ DISTRIBUTION BUILD COMPLETE!"
echo "====================================="
echo ""

# Show outputs
echo "📦 Outputs:"
echo "   📁 $DIST_DIR/"
ls -lh "$DIST_DIR/lib/"
echo ""
echo "   🗜️ Portable ZIP:"
ls -lh "build/distributions/francium-loader-${PLATFORM}.zip"
echo ""
echo "👉 Quick launch: $DIST_DIR/francium"
echo "   Install:  $DIST_DIR/bin/francium install sodium"
echo "   Search:   $DIST_DIR/bin/francium search shader"
echo ""
