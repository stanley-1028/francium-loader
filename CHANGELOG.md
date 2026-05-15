# Changelog

All notable changes to Francium Mod Loader will be documented in this file.

## [1.2.3] — 2026-05-15

### Fixed
- Version bump from 1.2.1 to 1.2.3 to match release tagging

## [1.2.2] — 2026-05-15

### Fixed
- CI: standalone-build fallback with retry + curl pre-install
- ServerSyncProtocol.fromJson() returning empty objects; toJson() manual JSON security

## [1.2.1] — 2026-05-14

### Changed
- **Dockerfile rewritten** — Multi-stage build with layer caching optimization
  - Builder stage: cache Gradle dependencies first, then copy and build source
  - Runtime stage: security-hardened with non-root user, ZGC tuning, health check
  - Proper OCI labels (version, source, description)
- **Docker entrypoint** — Moved JVM opts/config to external `entrypoint.sh`
- **JUnit 5 upgrade** — 5.10.2 → 5.11.3 with explicit `useJUnitPlatform()` in build.gradle
- **Build quality** — ModGraphComprehensiveTest uses explicit types for better IDE support

### Fixed
- CI: chmod +x gradlew for Linux/macOS package jobs
- CI: standalone-build fallback with retry + curl pre-install
- CI: ASM cache + jpackage system deps for macOS/Linux
- ServerSyncProtocol.fromJson() returning empty objects; toJson() manual JSON security

## [1.0.1] — 2026-05-11

### Fixed
- README.md: git clone URL updated for clarity
- Documentation improvements and minor typo fixes

## [1.0.1] — 2026-05-12

### 🏗️ Foundation & Build System Overhaul
- **Shadow plugin**: Replaced custom `fatJar` task with official `shadowJar` plugin for reliable fat JARs
- **gradle-wrapper.jar**: Added to repository (was missing, blocking CI builds)
- **gradle.properties**: Added with optimized JVM args and build caching
- **Cross-platform jpackage builds**: New `buildNative` task generates native executables:
  - Windows: `.exe` installer via `jpackageExe`
  - macOS: `.dmg` installer via `jpackageDmg`
  - Linux: AppImage via `jpackageAppImage`
- **Portable ZIP**: New `distPortableZip` task for standalone distribution
- **Launcher scripts**: Added `bin/francium.bat` (Windows) and `bin/francium.sh` (macOS/Linux)

### 🚀 CI/CD Pipeline Enhancement
- **macOS added** to build matrix (previously only Ubuntu + Windows)
- **Native package job**: Automatic per-platform native builds on tag push
- **Unified release**: All JARs + native installers attached to GitHub Releases
- **Standalone build preserved**: Gradle-free `build.sh` still supported and tested

### 🔧 Fixed
- #F3: Missing `gradle-wrapper.jar` — now committed to repo
- #F1: No cross-platform native builds — jpackage pipeline operational
- Install script (`install-francium.bat`) updated to use new `shadowJar` task

### ⚠️ Known Issues
- ASM-dependent modules require Gradle build (no change)
- SAT solver O(n²) above 500 mods
- No end-to-end Minecraft integration test yet

## [1.0.0] — 2026-05-10

### Added
- GitHub Release v1.0.0 with fat JAR artifact
- JitPack publishing support (tag-based auto-build)

### Updated
- ROADMAP milestones: P1 developer experience, P2 distribution marked complete
- All module tests passing (70/70)

## [1.0.0-alpha] — Unreleased

### Added
- DAG-based parallel mod loading (ModGraph with Kahn+BFS topological layering)
- SAT dependency resolver (DPLL backtracking with MRV/LCV heuristics)
- AI version bridge (MethodSignature 5-factor similarity + CompatibilityPredictor with RL)
- MappingDatabase for cross-version method lookups
- MemoryManager with leak detection via WeakReference + ObjectPool
- ServerSyncProtocol with ECDSA mod list signing
- PackageManager with npm-like install/search/update
- Per-mod ClassLoader isolation
- ModManifest supporting francium-mod.json, fabric.mod.json, META-INF/mods.toml
- LaunchWrapper ITweaker integration point
- Build script (build.sh / build.bat) for dependency-free core compilation
- JUnit 5 test suite (70 assertions, 100% pass)
- Cross-version method matching test with real Minecraft 1.20.4 → 1.21 API
- Stress test benchmark up to 1000 mods
- Standalone demo (FranciumDemo.java)
- Example mod (francium-mod.json format)
- CI pipeline (GitHub Actions: JDK 21/22/23)
- Comprehensive documentation (README, ARCHITECTURE, CONTRIBUTING)

### Changed
- Renamed from "Hanako Mod Loader" to "Francium Mod Loader" (tribute to Sodium)
- Removed JGraphT dependency; ModGraph is now self-contained
- MethodSignature descriptor parser rewritten for correctness

### Fixed
- ModGraph Kahn algorithm edge direction (dependency→depender)
- CompatibilityPredictorTest returnType assertion (java.lang.String vs String)
- PackageManager import and method reference bugs
- Circular dependency between francium-core and francium-ai-bridge
- HML/hml residual references throughout codebase
