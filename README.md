# 📦 LibraryLoader Gradle Plugin

<p align="center">
  <img src="https://img.shields.io/badge/Gradle-Plugin-blue?style=for-the-badge&logo=gradle" />
  <img src="https://img.shields.io/badge/Kotlin-JVM-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Java-9+-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

> A Gradle plugin + runtime library for downloading dependencies **at application startup** instead of bundling them at build time. Keep your `.jar` lightweight — let dependencies download themselves.

---

## ✨ How it works
```
┌─────────────────────────────────────────────────────┐
│  BUILD (Gradle Plugin)                              │
│  - reads declared dependencies                      │
│  - generates libraries.toml inside the jar          │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  RUNTIME (LibraryInjector)                          │
│  - reads libraries.toml from jar resources          │
│  - downloads jars into a specified folder           │
│  - resolves transitive dependencies via pom.xml     │
│  - creates isolated ClassLoader                     │
│  - injects classes with dependency resolution       │
└─────────────────────────────────────────────────────┘
```

On subsequent runs — **no downloading**, everything is taken from cache.

---

## 🚀 Quick Start

### 1. Apply the plugin
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```
```kotlin
// build.gradle.kts
plugins {
    id("io.github.zyr1x.libraryloader") version "1.0.1"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.zyr1x:LibraryLoader:1.3.0")
}
```

### 2. Declare dependencies
```kotlin
libraryLoader {
    // custom repository (optional)
    repository("panda", "https://repo.panda-lang.org/releases")

    // dependencies — will be downloaded on first run
    library("dev.rollczi:litecommands-bukkit:3.10.9")
    library("com.google.guava:guava:33.0.0-jre")
    library("org.reflections:reflections:0.10.2")
}
```

> **Maven Central** is added automatically — no need to declare it manually.

### 3. Use LibraryInjector in your code
```java
// Main.java — entry point
public class Main {
    public static void main(String[] args) throws Exception {
        LibraryInjector injector = new LibraryLoaderInjector(
            new File("libraries"),          // cache folder
            Main.class.getClassLoader(),    // parent classloader
            Logger.getAnonymousLogger(),    // logger (or null)
            Collections.emptyList()         // extra jars (optional)
        );

        // create instance of your main class with isolated dependencies
        App app = injector.inject(App.class);
        app.run(args);
    }
}
```
```java
// App.java — here you can freely import downloaded libs
import org.reflections.Reflections;
import com.google.common.collect.Lists;

public class App {
    public void run(String[] args) {
        // dependencies are already loaded and available
        Reflections reflections = new Reflections("com.example");
        List<String> list = Lists.newArrayList("a", "b", "c");
        // ...
    }
}
```

---

## 🎯 Injection Methods

### Simple injection (no constructor arguments)
```java
LibraryInjector injector = new LibraryLoaderInjector(new File("libraries"));

// create instance with default constructor
MyPlugin plugin = injector.inject(MyPlugin.class);
```

### Injection with constructor arguments
```java
// create instance with dependencies
Config config = new Config();
Repository repo = new Repository();

MyService service = injector.inject(MyService.class, config, repo);
```

### Injection by class name
```java
// load class by name (useful for plugins/dynamic loading)
Object instance = injector.inject(
    "com.example.DynamicPlugin",
    Object.class  // expected type for casting
);
```

### Access to ClassLoader
```java
// get isolated ClassLoader for manual operations
ClassLoader loader = injector.getClassLoader();

Class<?> clazz = loader.loadClass("com.example.SomeClass");
```

---

## ⚙️ Configuration

### Gradle Plugin

| Method | Description |
|---|---|
| `repository(name, url)` | Add a Maven repository |
| `library(notation)` | Add a dependency in `group:artifact:version` format |

### LibraryLoaderInjector (runtime)

#### Constructor parameters:

| Parameter | Type | Description |
|---|---|---|
| `libDir` | `File` | Folder for downloading and caching jars |
| `parent` | `ClassLoader` | Parent ClassLoader (default: `ClassLoader.getSystemClassLoader()`) |
| `logger` | `Logger` | Logger instance (pass `null` if not needed) |
| `extraJars` | `List<File>` | Additional jar files to include (optional) |

#### Methods:

| Method | Description |
|---|---|
| `inject(Class<T>, Object...)` | Create instance with constructor arguments |
| `inject(String, Class<T>, Object...)` | Create instance by class name |
| `getClassLoader()` | Get isolated ClassLoader |

---

## 📁 Folder structure after first run
```
libraries/
├── dev/rollczi/litecommands-bukkit/3.10.9/
│   └── litecommands-bukkit-3.10.9.jar
├── com/google/guava/guava/33.0.0-jre/
│   └── guava-33.0.0-jre.jar
└── org/reflections/reflections/0.10.2/
    └── reflections-0.10.2.jar
```

---

## 🔄 Transitive Dependencies

LibraryInjector **automatically resolves transitive dependencies** by parsing `pom.xml`.
```kotlin
libraryLoader {
    // just declare the direct dependency
    library("org.reflections:reflections:0.10.2")
    // slf4j, javassist and others will be pulled in automatically ↑
}
```

Dependencies with scopes `test`, `provided`, `system` and `optional` are **ignored**.

---

## 🎮 Minecraft Plugin Example
```java
public class MyPlugin extends JavaPlugin {
    
    private LibraryInjector injector;
    private PluginCore core;
    
    @Override
    public void onEnable() {
        try {
            // initialize injector
            injector = new LibraryLoaderInjector(
                new File(getDataFolder(), "libraries"),
                getClass().getClassLoader(),
                getLogger(),
                Collections.emptyList()
            );
            
            // inject plugin core with dependencies
            core = injector.inject(PluginCore.class, this);
            core.enable();
            
        } catch (Exception e) {
            getLogger().severe("Failed to load libraries: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (core != null) {
            core.disable();
        }
    }
}
```
```java
// PluginCore.java — with downloaded dependencies
import dev.rollczi.litecommands.LiteCommands;
import org.reflections.Reflections;

public class PluginCore {
    private final JavaPlugin plugin;
    private LiteCommands commands;
    
    public PluginCore(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void enable() {
        // use downloaded libraries
        Reflections reflections = new Reflections("com.example.commands");
        // initialize LiteCommands, etc.
    }
    
    public void disable() {
        if (commands != null) {
            commands.unregister();
        }
    }
}
```

---

## 🔧 Advanced Usage

### With extra jar files
```java
List<File> extraJars = Arrays.asList(
    new File("plugins/MyPlugin/custom-lib.jar"),
    new File("plugins/MyPlugin/another-lib.jar")
);

LibraryInjector injector = new LibraryLoaderInjector(
    new File("libraries"),
    ClassLoader.getSystemClassLoader(),
    null,
    extraJars  // will be added to isolated ClassLoader
);
```

### Multiple instances
```java
// create multiple isolated instances with different dependencies
LibraryInjector injector1 = new LibraryLoaderInjector(new File("libs1"));
LibraryInjector injector2 = new LibraryLoaderInjector(new File("libs2"));

Plugin1 plugin1 = injector1.inject(Plugin1.class);
Plugin2 plugin2 = injector2.inject(Plugin2.class);
```

---

## ❗ Important

> **ClassLoader Isolation**: Each `LibraryInjector` creates a `ChildFirstClassLoader` that prioritizes loaded libraries over parent classloader. This prevents conflicts but means classes from different injectors cannot directly interact.

> **Constructor Arguments**: When passing arguments to `inject()`, the method will try to find a matching constructor. If you have overloaded constructors, it uses the first compatible match based on argument types.

---

## 🛠️ Building from source
```bash
git clone https://github.com/zyr1x/library-loader
cd library-loader

# publish plugin locally
./gradlew :lib:publishToMavenLocal

# publish runtime library locally
./gradlew :LibraryLoader:publishToMavenLocal
```

---

## 📄 License

MIT — do whatever you want.