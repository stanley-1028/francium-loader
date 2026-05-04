# Francium Developer Guide

**Build mods that survive Minecraft updates. Yes, really.**

---

## Table of Contents

1. [Five-Minute Quick Start](#five-minute-quick-start)
2. [Project Anatomy](#project-anatomy)
3. [Mod Manifest (`francium-mod.json`)](#mod-manifest)
4. [Lifecycle & Entry Points](#lifecycle--entry-points)
5. [Dependency Management That Doesn't Suck](#dependency-management)
6. [AI Cross-Version Bridge](#ai-cross-version-bridge)
7. [Mixin Support](#mixin-support)
8. [API Reference](#api-reference)
9. [Testing Without Minecraft](#testing-without-minecraft)
10. [Migrating from Fabric/Forge](#migrating-from-fabricforge)
11. [Publishing Your Mod](#publishing-your-mod)
12. [Troubleshooting](#troubleshooting)

---

## Five-Minute Quick Start

```bash
# 1. Clone the template
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-mod
cd my-mod

# 2. Edit gradle.properties
#    Set modId, modName, mcVersion, mainClass

# 3. Open ExampleMod.java, replace with your content

# 4. Build
./gradlew build

# 5. Drop build/libs/my-mod-1.0.0.jar into Minecraft's mods/ folder
```

No Gradle plugin fights. No Loom. No ForgeGradle. Just a standard Java project with one `compileOnly` dependency.

---

## Project Anatomy

```
my-mod/
├── build.gradle              # Standard Java + Francium dependency
├── settings.gradle           # rootProject.name
├── gradle.properties         # ← YOUR metadata goes here
├── src/
│   ├── main/java/            # Your code
│   ├── main/resources/
│   │   └── francium-mod.json # Auto-populated from gradle.properties
│   └── test/java/            # JUnit tests (no Minecraft needed!)
└── .gitignore
```

**What you DON'T need:**
- Access widener files
- Mixin configs (unless you want them)
- Gradle plugin configurations
- Minecraft obfuscation mappings (Francium handles this)

---

## Mod Manifest

Francium reads **three formats** interchangeably:

| Format | File | Native Support |
|--------|------|----------------|
| Francium native | `francium-mod.json` | ✅ Full |
| Fabric | `fabric.mod.json` | ✅ Full (including Mixin) |
| Forge/NeoForge | `META-INF/mods.toml` | ⚠️ Partial |

### francium-mod.json (Recommended)

```json
{
  "schemaVersion": 1,
  "id": "my-mod",
  "name": "My Mod",
  "version": "1.0.0",
  "description": "What your mod does, in one sentence.",
  "authors": ["You"],
  "license": "MIT",
  "mcVersion": "1.21",
  "mainClass": "com.yourname.YourMod",
  "dependencies": {
    "fabric-api": ">=0.100.0"
  },
  "optionalDependencies": {
    "sodium": ">=0.5.0"
  },
  "entrypoints": {
    "main": "com.yourname.YourMod::onInitialize",
    "client": "com.yourname.YourMod::onInitializeClient"
  },
  "aiBridgeEnabled": true,
  "mixins": ["my-mod.mixins.json"]
}
```

**Field Reference:**

| Field | Required | Description |
|-------|----------|-------------|
| `id` | ✅ | Unique identifier. Lowercase, hyphens only. Think `sodium`, not `Sodium`. |
| `name` | ✅ | Human-readable name. |
| `version` | ✅ | Semantic version (1.0.0). |
| `mcVersion` | ✅ | Target Minecraft version. |
| `mainClass` | ✅ | Entry class with `onInitialize()` method. |
| `dependencies` | - | Hard dependencies. Missing = load failure. |
| `optionalDependencies` | - | Soft dependencies. Missing = warning only. |
| `entrypoints` | ✅ | Lifecycle hooks (see below). |
| `aiBridgeEnabled` | - | `true` = Francium auto-adapts your mod to newer MC versions. |
| `mixins` | - | Mixin config files (Fabric-style). |

---

## Lifecycle & Entry Points

Francium has a five-phase lifecycle:

```
INIT → DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
                                                         ↑
                                                   your code runs here
```

### Entry Points

Your main class can implement these methods:

```java
public class YourMod {
    // Called during LOADING phase, before the game renders
    public void onInitialize() {
        // Register blocks, items, entities, etc.
        Registry.register(Blocks.MY_BLOCK);
    }

    // Called client-side when rendering is ready
    public void onInitializeClient() {
        // Register renderers, keybinds, HUD elements
    }

    // Optional: server-side initialization
    public void onInitializeServer() {
        // Register commands, server tick handlers
    }
}
```

**No `@Mod` annotation needed.** Francium finds your class via `francium-mod.json` → `mainClass` → reflection.

### Extension Points (Advanced)

If you need to hook into Francium itself:

```java
FranciumLoader loader = FranciumLoader.getInstance();

loader.onPreLaunch(() -> {
    // All mods loaded, game about to start
});

loader.onModLoaded("sodium", () -> {
    // Specific mod finished loading
});
```

---

## Dependency Management

Declaration in `francium-mod.json`:

```json
"dependencies": {
  "sodium": ">=0.5.0 <1.0.0",   // Range: 0.5.0 through 0.x.x
  "iris": "^1.7.0",               // Caret: compatible with 1.x.x
  "fabric-api": ">=0.90.0"        // Minimum version
}
```

Francium uses a **SAT solver** (not the "try it and crash" strategy). When dependencies conflict:

- **Forge/Fabric**: `NoClassDefFoundError`. Good luck.
- **Francium**: "`sodium` requires `fabric-api >=0.90` but `iris` requires `fabric-api >=0.85`. Selected `fabric-api 0.92` which satisfies both."

If no solution exists, Francium tells you **exactly which mods conflict and why**, not a cryptic stack trace.

---

## AI Cross-Version Bridge

The killer feature. When `aiBridgeEnabled: true`:

1. Francium analyzes your mod's bytecode (ASM)
2. Extracts all Minecraft API calls
3. Checks them against the target MC version's mappings
4. If a method name changed, Francium generates a **bridge adapter** automatically
5. Your mod runs on the new MC version without recompilation

**Example:**

```
Your mod calls:  Block.m_12345_()      (1.20.4 obfuscated)
Target is:       Block.getBlockState()  (1.21 mapped)
Francium:        "I got this." → generates adapter → your mod runs fine
```

**Confidence levels:**
- ≥ 0.95: Direct mapping found, 100% reliable
- 0.85–0.95: Structural match, high confidence
- 0.60–0.85: Structural search, might need verification
- < 0.60: Flagged for manual review

Set `aiConfidenceThreshold` in `config/francium/loader.toml` to control when bridge logic fires.

---

## Mixin Support

Francium supports Fabric-style Mixins. Add to your `francium-mod.json`:

```json
{
  "mixins": ["my-mod.mixins.json"]
}
```

Your `my-mod.mixins.json`:

```json
{
  "required": true,
  "package": "com.yourname.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": ["MixinLivingEntity"],
  "client": ["MixinTitleScreen"]
}
```

Francium loads Mixins during the BRIDGING phase, after AI analysis but before class loading.

---

## API Reference

### FranciumLoader (Core)

```java
// Get the loader instance
FranciumLoader loader = FranciumLoader.getInstance();

// Check state
LoaderState state = loader.state();  // INIT, DISCOVERING, RESOLVING, BRIDGING, LOADING, READY

// Get loaded mods
int count = loader.getLoadedModCount();

// Get timing info
Map<String, Long> timings = loader.phaseTimings();
// {"discovery": 45, "resolution": 12, "bridging": 230, "loading": 340}

// Get memory snapshot
MemorySnapshot mem = loader.getMemorySnapshot();
System.out.println("Heap: " + mem.heapUsed() / 1024 / 1024 + "MB");
```

### ModGraph (DAG)

```java
ModGraph graph = loader.modGraph();
List<Set<String>> layers = graph.getLayers();
double speedup = graph.getSpeedupRatio();  // e.g., 4.2x
```

### PackageManager (CLI)

```java
PackageManager pm = new PackageManager(modsDir, cacheDir);

// Install
InstallReport report = pm.install("sodium", "^0.5.0");

// Search
List<RegistryMod> results = pm.search("shader");

// Check updates
List<UpdateInfo> updates = pm.checkUpdates();
```

### VersionBridge (AI)

```java
VersionBridge bridge = new VersionBridge("1.20.4", "1.21");
BridgeReport report = bridge.analyze(Path.of("mods/my-mod.jar"));
System.out.println("Compatibility: " + (report.compatibilityScore * 100) + "%");
```

---

## Testing Without Minecraft

Francium was designed so you can test mod logic **without launching Minecraft**.

```java
class ExampleModTest {
    @Test
    void testModInitialization() {
        ExampleMod mod = new ExampleMod();
        mod.onInitialize();
        // Assert your registrations happened
    }

    @Test
    void testYourGameLogic() {
        // Pure logic tests - no Minecraft dependency
        assertEquals(64, new ItemStack(Items.DIRT, 64).getCount());
    }
}
```

Run: `./gradlew test`

---

## Migrating from Fabric/Forge

### From Fabric

1. Keep your `fabric.mod.json` (Francium reads it natively)
2. Replace `fabric-loader` dependency with `francium-loader`
3. Remove Fabric Loom from `build.gradle`
4. Set `aiBridgeEnabled: true` if you want cross-version support
5. `./gradlew build`

Your Mixin configs, entry points, and annotations all work as-is.

### From Forge

1. Create a `francium-mod.json` alongside `mods.toml`
2. Replace Forge event bus calls with Francium entry points
3. Replace `@Mod` annotation with the `mainClass` field in francium-mod.json
4. Port Forge-specific APIs to vanilla/Mojang mappings (or use AI bridge)
5. `./gradlew build`

### Compatibility Table

| Feature | Fabric → Francium | Forge → Francium |
|---------|-------------------|-------------------|
| Entry points | ✅ Automatic | ⚠️ Manual port |
| Mixins | ✅ Full support | ❌ ASM transformers preferred |
| Registry | ✅ Standard | ⚠️ Replace with vanilla Registry |
| Network | ✅ Standard | ⚠️ Replace with vanilla packets |
| Events | ✅ Fabric API events | ⚠️ Fallback to vanilla events |
| Capabilities | ❌ N/A | ❌ Not supported |

---

## Publishing Your Mod

### Option 1: GitHub Releases (Recommended for new mods)

```bash
./gradlew build
# Upload build/libs/*.jar to GitHub Releases
```

### Option 2: JitPack

Add to your own build.gradle:

```groovy
publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'com.yourname'
            artifactId = 'my-mod'
            version = project.version
            from components.java
        }
    }
}
```

Then tag a release on GitHub. JitPack picks it up automatically.

### Option 3: Francium Registry

Coming soon: `francium publish` will push to the official Francium mod registry.

---

## Troubleshooting

### "I get NoClassDefFoundError for com.francium.loader.FranciumLoader"

Make sure francium-loader is `compileOnly`, not `implementation`. Francium provides it at runtime.

### "My Fabric mod doesn't work on Francium"

Francium reads `fabric.mod.json` but doesn't implement the full Fabric API. Use Fabric API as a dependency or stick to vanilla API calls.

### "AI bridge says my mod is 40% compatible"

That means 40% of your API calls have direct mappings. The rest need structural search or manual review. Enable `aiBridgeReportOnly: true` in loader.toml to see a detailed report without actually patching.

### "SAT resolver took too long"

Rare. If you have 200+ mods with complex constraints, increase `layerTimeoutSeconds` in loader.toml.

### "My mod loads but does nothing"

Check that `entrypoints.main` in francium-mod.json points to `com.your.Class::onInitialize` (the method name is required after `::`).

---

*Still stuck? File an issue at [github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader). Francium has your back.*
