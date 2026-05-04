# Francium Developer Guide (English)

**Build mods that survive Minecraft version updates. Yes, you read that right.**

---

## Table of Contents

1. [Five-Minute Quickstart](#five-minute-quickstart)
2. [Project Structure](#project-structure)
3. [The Mod Manifest (francium-mod.json)](#the-mod-manifest)
4. [Lifecycle & Entry Points](#lifecycle--entry-points)
5. [Dependency Management: Let SAT Do the Work](#dependency-management)
6. [AI Cross-Version Bridging: Francium's Killer Feature](#ai-cross-version-bridging)
7. [Mixin Support](#mixin-support)
8. [API Reference](#api-reference)
9. [Testing Without Minecraft](#testing-without-minecraft)
10. [Migrating from Fabric/Forge](#migrating-from-fabricforge)
11. [Publishing Your Mod](#publishing-your-mod)
12. [Troubleshooting](#troubleshooting)

---

## Five-Minute Quickstart

```bash
# 1. Clone the template
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-mod
cd my-mod

# 2. Edit gradle.properties
#    Fill in your modId, modName, mcVersion, mainClass

# 3. Open ExampleMod.java, replace with your code

# 4. Build
./gradlew build

# 5. Drop build/libs/my-mod-1.0.0.jar into Minecraft's mods/ folder
#    Launch the game. Francium handles everything else.
```

No Gradle plugin wars. No Loom. No ForgeGradle. Just a standard Java project with one `compileOnly` dependency. You can even test without Minecraft installed — we'll get to that.

> *"The first 5 minutes of modding shouldn't be spent fighting your build system."*

---

## Project Structure

```
my-mod/
├── build.gradle              # Standard Java + Francium dependency
├── settings.gradle           # rootProject.name
├── gradle.properties         # ← Put your metadata here
├── src/
│   ├── main/java/            # Your code
│   ├── main/resources/
│   │   └── francium-mod.json # Auto-generated from gradle.properties
│   └── test/java/            # JUnit tests (no Minecraft needed!)
└── .gitignore
```

**Things you no longer need (compared to Fabric/Forge dev experience):**

| You don't need this anymore | Why you used to need it |
|---|---|
| Access Widener files | Because Mojang renames obfuscated methods every version |
| Mixin config files | Unless you actually *need* Mixin |
| Seven layers of Gradle plugin nesting | Because Loom/ForgeGradle dependency hell |
| Manual mapping table management | Francium handles it for you |
| Asking "why won't it compile" on Discord | SAT solver tells you exactly which mod conflicts |

Bottom line: **everything that used to take half an hour of setup before you could write code? One command now. The rest of your time goes into writing the mod you actually want to build.**

---

## The Mod Manifest

Francium reads three manifest formats, seamlessly:

| Format | File | Compatibility |
|---|---|---|
| Francium native | `francium-mod.json` | ✅ Full |
| Fabric | `fabric.mod.json` | ✅ Full (including Mixin) |
| Forge/NeoForge | `META-INF/mods.toml` | ⚠️ Partial |

### francium-mod.json (recommended)

```json
{
  "schemaVersion": 1,
  "id": "my-mod",
  "name": "My Awesome Mod",
  "version": "1.0.0",
  "description": "One sentence that tells people what your mod does. Don't write 'A Minecraft mod' — this isn't a filler-words competition.",
  "authors": ["Your Name"],
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
|---|---|---|
| `id` | ✅ | Unique identifier. Lowercase, hyphen-separated. Be restrained like `sodium`, not unhinged like `SuperAwesomeEpicModFinal_v2_real`. |
| `name` | ✅ | Human-readable name |
| `version` | ✅ | Semantic version (1.0.0) |
| `mcVersion` | ✅ | Target Minecraft version |
| `mainClass` | ✅ | Entry class, must have `onInitialize()` method |
| `dependencies` | — | Hard dependencies. Missing = load failure |
| `optionalDependencies` | — | Soft dependencies. Missing = warning, not crash |
| `entrypoints` | ✅ | Lifecycle hooks (see below) |
| `aiBridgeEnabled` | — | `true` = Francium auto-adapts to new MC versions. You don't touch your code; it runs on 1.22. |
| `mixins` | — | Mixin config files (Fabric-style) |

---

## Lifecycle & Entry Points

Francium has a five-phase lifecycle. Your code runs during LOADING:

```
INIT → DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
                                                     ↑
                                               Your code runs here
```

### Entry Methods

Your main class implements these:

```java
public class YourMod {
    // Called during LOADING, before the game renders
    public void onInitialize() {
        // Register blocks, items, entities
        Registry.register(Blocks.MY_BLOCK);
        System.out.println("My mod is locked and loaded!");
    }

    // Called once client rendering is ready
    public void onInitializeClient() {
        // Register renderers, keybinds, HUD elements
    }

    // Optional: server-side init
    public void onInitializeServer() {
        // Register commands, server tick handlers
    }
}
```

**No `@Mod` annotation required.** Francium finds your class through `francium-mod.json` → `mainClass` → reflection. You write the code, it finds the way.

### Extension Points (Advanced)

```java
FranciumLoader loader = FranciumLoader.getInstance();

loader.onPreLaunch(() -> {
    // All mods loaded, game about to start
});

loader.onModLoaded("sodium", () -> {
    // Callback when a specific mod finishes loading
    // If sodium isn't installed or crashes, this never fires
});
```

---

## Dependency Management

Declare dependencies in `francium-mod.json`:

```json
"dependencies": {
  "sodium": ">=0.5.0 <1.0.0",   // Range: 0.5.0 and above, below 1.0
  "iris": "^1.7.0",               // Caret: any 1.x.x
  "fabric-api": ">=0.90.0"        // Minimum version
}
```

Francium uses a **SAT solver** instead of a "try it and see what crashes" strategy. When dependencies conflict:

| Loader | What happens |
|---|---|
| Forge/Fabric | `NoClassDefFoundError`, then you stare at the log for 10 minutes |
| Francium | *"sodium requires fabric-api >=0.90, iris requires fabric-api >=0.85. Selected fabric-api 0.92, satisfying both."* |

If there's truly no solution, Francium tells you **exactly which mods are fighting, why, and what your options are** — instead of dumping a cryptic stack trace on you.

---

## AI Cross-Version Bridging

**This is Francium's killer feature.** When `aiBridgeEnabled: true`:

1. Francium analyzes your mod's bytecode with ASM
2. Extracts all Minecraft API calls
3. Compares against the target MC version's mappings
4. If method names changed, Francium **auto-generates a bridge adapter**
5. Your mod runs on the new MC version, untouched

**Example:**

```
Your mod calls:  Block.m_12345_()       (1.20.4 obfuscated name)
Target version:  Block.getBlockState()   (1.21 mapped name)
Francium:        "I got this." → Generates adapter → Your mod just works
```

> Think of Francium's AI bridge like a **translator who knows every Minecraft version**. Your mod says "I want to call the get-block-state method", the translator checks the current version and says "In 1.21 it's called `getBlockState`, let me route that for you."

**Confidence levels:**
- ≥ 0.95: Exact mapping, 100% reliable
- 0.85–0.95: Structural match, high confidence
- 0.60–0.85: Structural search, may need verification
- < 0.60: Flagged for manual review

Set `aiConfidenceThreshold` in `config/francium/loader.toml` to control the trigger threshold.

---

## Mixin Support

Francium supports Fabric-style Mixin. Add to `francium-mod.json`:

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

Francium loads Mixins during the BRIDGING phase, after AI analysis but before class loading. This means AI bridging and Mixin can coexist.

---

## API Reference

### FranciumLoader (Core)

```java
// Get loader instance
FranciumLoader loader = FranciumLoader.getInstance();

// Check current state
LoaderState state = loader.state();

// Get loaded mod count
int count = loader.getLoadedModCount();

// Get per-phase timing
Map<String, Long> timings = loader.phaseTimings();
// Output: {discovery: 45ms, resolution: 12ms, bridging: 230ms, loading: 340ms}

// Get memory snapshot
MemorySnapshot mem = loader.getMemorySnapshot();
System.out.println("Heap usage: " + mem.heapUsed() / 1024 / 1024 + "MB");
```

### ModGraph (DAG)

```java
ModGraph graph = loader.modGraph();
List<Set<String>> layers = graph.getLayers();
double speedup = graph.getSpeedupRatio();  // e.g. 4.2x
```

### PackageManager (CLI)

```java
PackageManager pm = new PackageManager(modsDir, cacheDir);

// Install a mod
InstallReport report = pm.install("sodium", "^0.5.0");

// Search for mods
List<RegistryMod> results = pm.search("shader");

// Check for updates
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

Francium's design philosophy: **mod logic should be testable independent of the game engine.**

```java
class ExampleModTest {
    @Test
    void testModInitialization() {
        ExampleMod mod = new ExampleMod();
        mod.onInitialize();
        // Assert your registration logic
    }

    @Test
    void testYourGameLogic() {
        // Pure logic test, zero Minecraft dependencies
        assertEquals(64, new ItemStack(Items.DIRT, 64).getCount());
    }
}
```

Run: `./gradlew test`

> Being able to run mod tests in CI instead of "well, it works on my machine" is genuinely revolutionary in this space.

---

## Migrating from Fabric/Forge

### From Fabric

1. Keep your `fabric.mod.json` (Francium reads it natively)
2. Replace `fabric-loader` dependency with `francium-loader`
3. Remove Fabric Loom from `build.gradle`
4. Set `aiBridgeEnabled: true` for cross-version support
5. `./gradlew build`

Your Mixin configs, entry points, and annotations all stay intact.

### From Forge

1. Create a `francium-mod.json` alongside `mods.toml`
2. Replace Forge event bus calls with Francium entry points
3. Use `mainClass` instead of `@Mod` annotation
4. Port Forge-specific APIs to Mojang mappings (or enable AI bridging)
5. `./gradlew build`

### Compatibility at a Glance

| Feature | Fabric → Francium | Forge → Francium |
|---|---|---|
| Entry points | ✅ Automatic | ⚠️ Manual port |
| Mixin | ✅ Full support | ❌ Use ASM transformers instead |
| Registry system | ✅ Standard | ⚠️ Replace with vanilla Registry |
| Networking | ✅ Standard | ⚠️ Replace with vanilla packets |
| Events | ✅ Fabric API events | ⚠️ Downgrade to vanilla events |
| Capabilities | ❌ N/A | ❌ Not supported |

---

## Publishing Your Mod

### Option 1: GitHub Releases (Recommended)

```bash
./gradlew build
# Upload build/libs/my-mod-1.0.0.jar to GitHub Releases
```

### Option 2: JitPack

Add to your `build.gradle`:

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

Then tag on GitHub, JitPack will build and publish automatically.

### Option 3: Francium Registry (Coming Soon)

Soon you'll be able to `francium publish` to push to the official registry.

---

## Troubleshooting

### "NoClassDefFoundError: com.francium.loader.FranciumLoader"

Make sure francium-loader is `compileOnly`, not `implementation`. Francium provides these classes at runtime.

### "My Fabric mod doesn't work on Francium"

Francium reads `fabric.mod.json` but doesn't implement the full Fabric API. Include Fabric API as a dependency, or stick to vanilla API calls.

### "AI bridge says my mod is only 40% compatible"

Means 40% of your API calls have exact mappings, the remaining 60% need structural search or manual review. Enable `aiBridgeReportOnly: true` in loader.toml to see the detailed report.

### "SAT solver is taking forever"

Rare. If your mod pack is exceptionally complex (200+ mods with heavy constraints), increase `layerTimeoutSeconds` in loader.toml.

### "Mod loaded but nothing happened"

Check that `entrypoints.main` in `francium-mod.json` points to `com.your.Class::onInitialize` — the method name after the double colon must be exact.

### "I don't want AI bridging, I'll handle compatibility myself"

Completely fine. Set `aiBridgeEnabled: false` and Francium won't touch your bytecode. Turn it off when you don't need it, turn it on when you do. Francium respects your choices.

---

*Got more questions? Open an issue at [github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader). The Francium community welcomes you — no "did you search first?" gatekeeping.*

*Remember — "Stop manually wrangling mods. Let Francium do the work." Because your time should be spent building cool mods, not wrestling Gradle plugins.*
