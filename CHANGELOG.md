# Changelog

All notable changes to francium-loader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.6.0] — 2026-05-16

### Fixed
- Shadow JAR now includes all transitive dependencies (SLF4J, Logback, Gson, ASM, Netty) — was only 88 entries, now bundles 5,492+ entries with full runtime classpath
- Converted hand-written `main()` test in ModGraphTest to proper JUnit 5 `@Test` methods (Gradle 9.x fails on empty test suites)
- Added `failOnNoDiscoveredTests = false` for AIBridgeDemo.java (demo class with `main()`, not a JUnit test)
- Linux CI: changed jpackage from `app-image` (directory) to `deb` installer with menu shortcut and package metadata

### Changed
- Version bumped to 1.6.0

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
