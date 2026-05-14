# ═══════════════════════════════════════════════════════════
#  Francium Server — Dockerfile
#
#  構建: docker build -f docker/Dockerfile -t francium-server:1.0.0-alpha .
#  「不要在伺服器上手動裝 Java。讓 Docker 替你打工。」
# ═══════════════════════════════════════════════════════════

# ─── Stage 1: Build ─────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Cache Gradle dependencies first (layer optimization)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --no-daemon --version

# Copy source and build
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :francium-server:build -x test

# ─── Stage 2: Runtime ───────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Francium Server"
LABEL org.opencontainers.image.description="Minecraft mod sync server — DAG parallel, SAT resolver, AI bridge"
LABEL org.opencontainers.image.source="https://github.com/stanley-1028/francium-loader"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.version="1.0.0-alpha"

# Security: non-root user
RUN addgroup -S francium && adduser -S francium -G francium

# Create data directories
RUN mkdir -p /app/data /app/config /app/mods && \
    chown -R francium:francium /app

# Copy the built JAR from builder stage
COPY --from=builder /workspace/francium-server/build/libs/francium-server-*.jar /app/francium-server.jar

# Copy entrypoint
COPY docker/docker-entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Optional: copy default config
# COPY docker/config/ /app/config/

WORKDIR /app
USER francium

# JVM tuning for Minecraft mod sync workloads:
# - ZGC: low pause times, important for real-time game servers
# - Generational ZGC: better throughput for young-gen-heavy workloads
ENV JAVA_OPTS="-Xms256M -Xmx1G -XX:+UseZGC -XX:+ZGenerational"
ENV FRANCIUM_DATA_DIR=/app/data
ENV FRANCIUM_CONFIG_DIR=/app/config
ENV FRANCIUM_MODS_DIR=/app/mods

EXPOSE 25566 25567

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=15s \
    CMD wget -q -O- http://localhost:25566/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
