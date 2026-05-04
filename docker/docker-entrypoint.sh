#!/bin/sh
# ═══════════════════════════════════════════════════════════
#  Francium Server — Docker Entrypoint
# ═══════════════════════════════════════════════════════════

set -e

echo "  ⚡ Francium Server starting..."
echo "  ├─ JVM:    $(java -version 2>&1 | head -1)"
echo "  ├─ Data:   ${FRANCIUM_DATA_DIR:-/app/data}"
echo "  ├─ Config: ${FRANCIUM_CONFIG_DIR:-/app/config}"
echo "  └─ Mods:   ${FRANCIUM_MODS_DIR:-/app/mods}"

# ─── Auto-generate config if missing ─────────────────────
CONFIG_FILE="${FRANCIUM_CONFIG_DIR:-/app/config}/server.toml"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "  📝 Generating default config..."
    cat > "$CONFIG_FILE" << 'TOML'
# Francium Server Configuration
[server]
host = "0.0.0.0"
modSyncPort = 25566
registryPort = 25567
modsDir = "/app/mods"

[sync]
requireSignature = true
autoDownloadClientMods = true
allowOptionalMods = true
syncIntervalMinutes = 60

[security]
verifySha256 = true
minimumSigningKeyBits = 256
allowUnsignedMods = false

[registry]
enabled = true
cacheTtlMinutes = 60
mirrors = []

[logging]
level = "INFO"
format = "text"
TOML
fi

# ─── Launch ──────────────────────────────────────────────
exec java ${JAVA_OPTS} -jar /app/francium-server.jar \
    --data-dir "${FRANCIUM_DATA_DIR:-/app/data}" \
    --config "${CONFIG_FILE}"
