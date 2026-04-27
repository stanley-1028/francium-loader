# Contributing to Francium Mod Loader

Thank you for your interest in contributing! Francium is a next-generation Minecraft mod loader
that aims to solve the structural problems that have plagued the modding ecosystem for years.
We welcome contributions of all kinds.

## Getting Started

### Prerequisites
- **Java 21+** — Francium uses modern Java features (records, pattern matching, virtual threads)
- **Gradle 8.8+** — Build system (the wrapper is included: `./gradlew`)

### Setup

```bash
# Clone the repository
git clone https://github.com/francium-loader/francium.git
cd francium

# Generate the Gradle wrapper jar (first time only)
gradle wrapper

# Build the project
./gradlew build

# Run tests
./gradlew test

# Or use the lightweight build script (core modules only, no external deps)
./build.sh        # Linux/macOS
build.bat         # Windows
```

### Project Structure

```
francium-loader/
├── francium-core/          # Core loader: DAG, ClassLoader, lifecycle
├── francium-ai-bridge/     # AI version bridge (core innovation)
├── francium-resolver/      # SAT dependency resolver
├── francium-manager/       # Package manager (npm-like)
├── francium-profiler/      # Memory profiler & leak detection
├── francium-server/        # Server sync protocol
├── docs/                   # Architecture & configuration docs
├── TestRunner.java         # Lightweight test runner (no JUnit needed)
├── build.gradle            # Root build configuration
└── settings.gradle         # Subproject includes
```

## How to Contribute

### Reporting Bugs

Open an issue with:
- Your Minecraft version and Francium version
- Steps to reproduce
- Expected vs actual behavior
- Any relevant logs or stack traces

### Suggesting Features

We love ambitious ideas! Open an issue with the `enhancement` tag. Describe:
- The problem you're trying to solve
- Your proposed solution
- Any alternatives you've considered

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Write your code, following the existing style
4. Add tests for new functionality
5. Ensure all tests pass: `./gradlew test`
6. Commit with descriptive messages
7. Push and open a PR against `main`

### Code Style

- **Java 21** features are encouraged (records, sealed classes, pattern matching)
- Zero-cost abstractions preferred over heavy dependencies
- Each module should minimize external dependencies
- Pure Java implementations are preferred where feasible
- ASCII art in comments is not just tolerated, it's celebrated

### Areas That Need Help

| Area | Priority | Description |
|------|----------|-------------|
| **AI Bridge** | 🔴 High | TensorFlow/ONNX integration for ML-based mapping prediction |
| **Package Registry** | 🔴 High | Web-based registry server with CI/CD integration |
| **Forge API Adapter** | 🟡 Medium | Full Forge/NeoForge mod compatibility layer |
| **Sandbox Mode** | 🟡 Medium | Per-mod SecurityManager for behavior analysis |
| **IDE Plugin** | 🟢 Nice-to-have | IntelliJ/VSCode plugin for compatibility checking |
| **Documentation** | 🟢 Always | Translations, tutorials, API docs |

## Design Principles

1. **Every mod is an independent unit** — isolated ClassLoader, independent lifecycle, individually unloadable
2. **Dependencies are a graph, the graph is the schedule** — DAG drives parallel loading directly
3. **AI bridges version gaps** — don't ask developers to rewrite; understand and adapt automatically
4. **Security is built-in** — SHA256 verification, digital signatures, optional sandbox analysis

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
