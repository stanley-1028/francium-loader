# Changelog

All notable changes to francium-loader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.3.0] — 2026-05-15

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
