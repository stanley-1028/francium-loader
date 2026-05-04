# ═══════════════════════════════════════════════════════════
#  Francium Server — Docker Image
#
#  一條命令部署 Francium 模組同步伺服器。
#  Build: docker build -t francium-server:1.0.0-alpha .
#  Run:   docker compose up -d
# ═══════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY francium-core/ francium-core/
COPY francium-resolver/ francium-resolver/
COPY francium-ai-bridge/ francium-ai-bridge/
COPY francium-profiler/ francium-profiler/
COPY francium-server/ francium-server/

# Build the server module (skip tests that need Minecraft)
RUN chmod +x gradlew && \
    ./gradlew :francium-server:build -x test -x javadoc && \
    cp francium-server/build/libs/francium-server-*.jar francium-server.jar

# ── Stage 2: Runtime ────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Francium Server"
LABEL org.opencontainers.image.description="Mod sync server for Francium Mod Loader"
LABEL org.opencontainers.image.source="https://github.com/stanley-1028/francium-loader"
LABEL org.opencontainers.image.licenses="MIT"

# Create non-root user
RUN addgroup -S francium && adduser -S francium -G francium

# Create directories
RUN mkdir -p /app/data /app/config /app/mods /app/cache && \
    chown -R francium:francium /app

WORKDIR /app

# Copy server JAR from build stage
COPY --from=builder /app/francium-server.jar /app/francium-server.jar
COPY --from=builder /app/docker/config/server.toml /app/config/server.toml 2>/dev/null || true

# Health check script
RUN echo '#!/bin/sh\n\
wget -q -O- http://localhost:25566/health || exit 1' > /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh && \
    chown francium:francium /app/healthcheck.sh

USER francium

EXPOSE 25566 25567

# JVM tuning for server environments
# ZGC: low-latency GC, ideal for long-running server processes
# -XX:MaxDirectMemorySize: prevent native memory leaks from Netty
ENV JAVA_OPTS="-Xms256M -Xmx1G -XX:+UseZGC -XX:+ZGenerational -XX:MaxDirectMemorySize=128M"

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD ["/app/healthcheck.sh"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/francium-server.jar"]
