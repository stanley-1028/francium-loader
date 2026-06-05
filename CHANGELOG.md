# Changelog

All notable changes to francium-loader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.3.0] — 2026-06-05

### 🐛 Bug Fixes
- **System.exit 移除**: FranciumBootstrap 改用 `throw RuntimeException`
- **異常處理強化**: 多處異常加入 LOGGER 日誌記錄
- **PrintStackTrac 移除**: 全部改為 SLF4J LOGGER

### 📄 文件
- 新增 Checkstyle CI (`.github/workflows/checkstyle.yml`)
- README 加入 Release badge
- build.gradle 版本號 v2.2.0 → v2.3.0

## [2.1.0] — 2026-05-17

### Changed
- Updated JUnit Jupiter to 6.0.3 (compatible with Gradle 9.x)
- Updated JUnit Platform Launcher to 1.12.3
- Version bumped to 2.1.0

## [2.0.0] — 2026-05-17

### Added
- **Java Agent support** — Francium-loader can now be used as a `-javaagent` JVM argument. The `FranciumAgent.premain()` hooks into the JVM at startup to inject mod JARs into the system classpath and process Mixin configurations before any Minecraft classes are loaded
- **Mixin 0.8.7 integration** — `org.spongepowered:mixin` is shaded into the JAR. Fabric mods with `mixins.json` configs are automatically detected and their mixin classes registered. Compatible with Fabric mod format (`fabric.mod.json`)
- **MixinConfigProcessor** — scans mod JARs for `mixins.json`, `francium.mixins.json`, and `modid.mixins.json`, registers all `mixins/client/server` entries for runtime class transformation
- **Mixin manifest entries**: `Premain-Class`, `Agent-Class`, `Can-Retransform-Classes`, `Can-Set-Native-Method-Prefix`

### Changed
- Version bumped to 2.0.0 (major — breaking new features for Fabric mod compatibility)
- Shadow JAR now includes SpongePowered Mixin 0.8.7
- Added SpongePowered Maven repository for dependency resolution

## [1.6.0] — 2026-05-16

### Fixed
- **Robustness: Invalid/empty JARs no longer crash the loader** — corrupted JAR files are now gracefully skipped with a warning instead of throwing ZipException to the top-level
- **Duplicate mod registration** when JAR contains both `francium-mod.json` and `fabric.mod.json` — now only the first found manifest format is used per JAR
- **JAR path discovery for validator and AI Bridge** — `discoverPhase()` and `bridgePhase()` now use the actual JAR path from discovery instead of guessing `modId-version.jar` which never existed
- **AI Bridge NaN% displayed** when mod has zero external API calls — compatibility score defaults to 100% for mods that don't need bridging
- Shadow JAR now includes all transitive dependencies (SLF4J, Logback, Gson, ASM, Netty) — was only 88 entries, now bundles 5,492+ entries with full runtime classpath
- Converted hand-written `main()` test in ModGraphTest to proper JUnit 5 `@Test` methods (Gradle 9.x fails on empty test suites)
- Linux CI: changed jpackage from `app-image` (directory) to `deb` installer with menu shortcut and package metadata

### Changed
- Version bumped to 1.6.0
- `FranciumBootstrap.VERSION` now reads from JAR manifest (`Implementation-Version`) instead of hardcoded `"1.2.0"`

## [1.5.0] — 2026-05-16

### Changed
- Updated shadow plugin to `com.gradleup.shadow` 9.4.1 (migrated from unmaintained `com.github.johnrengelman.shadow`)
- Updated Gradle wrapper to 9.5.1 (via Dependabot)
- Updated JUnit Jupiter to 5.11.4 (compatible with Gradle 8.x+ embedded launcher)
- Updated SLF4J to 2.0.18 (consistent across all submodules, via Dependabot)
- Updated Logback to 1.5.32 (consistent across all submodules, via Dependabot)
- Updated ASM to 9.10 (bytecode manipulation, via Dependabot)
- Updated Gson to 2.14.0 (JSON parsing, via Dependabot)
- Updated Netty to 4.2.13.Final (server communication, via Dependabot)
- Updated CI workflow actions: checkout@v6, setup-java@v5, setup-gradle@v6, upload-artifact@v7, download-artifact@v8, action-gh-release@v3
- Version bumped to 1.5.0

### Fixed
- Linux jpackage app-image: removed invalid `--linux-package-name` and `--linux-app-category` flags (only valid for --type deb/rpm)
- Version consistency: build.gradle version now matches release tag

## [1.4.0] — 2026-05-15

### Added
- Modern installer experience with desktop shortcut, start menu, per-user install

### Added
- End-to-end integration test (FranciumE2ETest): SAT resolution, DAG scheduling, parallel loading simulation, profiler metrics, AI bridge — 4 tests covering the full pipeline
- Cycle detection test: verifies CircularDependencyException on mod A→B→C→A
- Conflict detection test: verifies SAT returns `success=false` on unsolvable constraints
- Performance gate test: ensures pipeline completes under 5 seconds
- Full README.md and DEVELOPER.md documentation

### Changed
- JUnit 5 migration completed (all 71 tests now use JUnit 5)

## [1.2.3] — 2026-05-15

### Fixed
- Cross-platform builds: jpackage now produces .exe (Windows) and .dmg (macOS)
- CI matrix: build + test across all platforms before packaging
- macOS icon handling: moved nova_icon_256.png to jpackage input dir
