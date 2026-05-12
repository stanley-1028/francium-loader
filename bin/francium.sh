#!/bin/bash
# LF line endings enforced by .gitattributes
# ============================================
#  Francium Mod Loader — macOS/Linux CLI
#  Usage: ./francium.sh [options]
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Find Java
if [ -n "$FRANCIUM_JAVA_HOME" ]; then
    JAVA="$FRANCIUM_JAVA_HOME/bin/java"
elif [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"  
elif command -v java &> /dev/null; then
    JAVA="java"
else
    echo "⚠ Java not found! Please install JDK 21+ from https://adoptium.net/"
    exit 1
fi

# Find JAR
JAR=$(find build/libs lib -name "francium-*-all.jar" -o -name "francium-*.jar" 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "⚠ Francium JAR not found! Run: ./gradlew shadowJar"
    exit 1
fi

echo "Francium Mod Loader — $@"
exec "$JAVA" -Xmx2g -jar "$JAR" "$@"
