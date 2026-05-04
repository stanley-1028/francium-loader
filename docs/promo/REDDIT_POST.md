# Francium Mod Loader — DAG parallel loading + SAT dependency resolution + AI cross-version bridging

**I built a new Minecraft mod loader from scratch. Here's why and what it does.**

## The Problem

Forge and Fabric are mature, but their architecture is showing its age. Three pain points every modded player knows:

1. **"MC updated, my 80 mods are dead"** — Every Minecraft version re-obfuscates method names. Mods break. Authors scramble. Players wait weeks or months.

2. **"Why won't this work"** — Dependency resolution is a guessing game. Install Sodium, get `NoClassDefFoundError`. Google for 20 minutes. Install missing dep. Another error. Repeat.

3. **"200 mods = 2-3 minute startup"** — Sequential loading wastes multi-core CPUs. Even a Threadripper waits for mod #187 to finish before starting mod #188.

## Francium's Approach

Instead of patching existing loaders, I redesigned from scratch with three core innovations:

### 1. AI Cross-Version Bridging (`francium-ai-bridge`)

Mojang renames every method on every release (`block.getBlockState()` → `m_12345_()`), but the **semantic structure is invariant**. Francium:

- Analyzes your mod's bytecode with ASM
- Queries a multi-version Mapping database (Mojang/Yarn/SRG)
- Scores candidates using feature-weighted similarity (descriptor edit distance, structural patterns, call context)
- Auto-generates bytecode adapters injected into your mod's ClassLoader

A mod compiled for 1.20.4 has 85-90% of its API calls auto-bridged on 1.21. The mod runs untouched.

### 2. SAT Dependency Resolution (`francium-resolver`)

Model dependency resolution as a Constraint Satisfaction Problem. DPLL-style backtracking + MRV variable ordering + forward checking constraint propagation:

```
sodium  → needs fabric-api >=0.90
iris    → needs fabric-api >=0.85
Result: selected fabric-api 0.92, satisfies both. 12ms, 89 nodes explored.
```

When no solution exists, the solver tells you exactly which mods conflict and why. No more staring at stack traces guessing.

### 3. DAG Parallel Loading (`francium-core`)

Mod dependencies form a Directed Acyclic Graph. Topological layering (Kahn's algorithm) identifies independent mods that can load simultaneously via ForkJoinPool:

```
Layer 0: [A, B]      ← no dependencies, parallel
Layer 1: [C, D, E]   ← depend on Layer 0, parallel
Layer 2: [F]         ← depends on Layer 1
```

100 mod benchmark: **4.2x speedup** vs sequential. Failed mods don't block their peer layer.

## Other Features

| Feature | What it does |
|---|---|
| `francium install sodium` | CLI package manager with SAT-powered transitive dependency resolution |
| `francium sync` | Server protocol auto-compares your mods/ with server manifest (SHA256 verified) |
| Per-mod ClassLoader isolation | Prevents class collision; enables per-mod memory tracking |
| Memory leak detection | WeakReference tracking identifies mods that fail to release classloaders |
| Docker deployment | `docker compose up -d` for dedicated mod sync server |

## Compatibility

- **Fabric mods**: ✅ Full (reads `fabric.mod.json`, supports Mixin)
- **Forge mods**: ⚠️ Partial (reads `mods.toml`, some API adaptation needed)
- **Native Francium mods**: ✅ Full

## Status

**v1.0.0-alpha**. Core test suite: 70/70 passed. Actively seeking early testers and mod developers.

| Module | Tests | Result |
|---|---|---|
| SAT resolver | 7 | ✅ |
| DAG loader | 21 | ✅ |
| AI bridge | 10 | ✅ |
| Version constraints | 32 | ✅ |

## Write a Francium Mod in 5 Minutes

```bash
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-mod
cd my-mod
# Edit ExampleMod.java → write your onInitialize()
./gradlew build
```

No Gradle plugin wars. No Loom. No ForgeGradle. One `compileOnly` dependency. Standard Java 21 project.

## Who Built This

I'm a 15-year-old developer from Macau. I built Francium because I was tired of waiting for mods to update after every Minecraft release, tired of the 3-minute startup for 200-mod packs, tired of "well, it works on my machine" dependency debugging.

SAT solving, DAG topology, ASM bytecode analysis — this stuff usually shows up in grad school. The fact that a high schooler built it solo says something about how much technical debt the existing loader ecosystem has accumulated.

This isn't a flex. It's an argument that we can do better.

## Links

- **GitHub**: [github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader)
- **License**: MIT (do whatever you want, owe nothing)
- **Issues**: Bugs, suggestions, PRs — all welcome

---

**"Stop waiting for mod updates. Let AI bridge the future for you."**

*— stanley1028, May 2026*
