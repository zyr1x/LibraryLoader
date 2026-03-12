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
│  - provides fluent builder API for injection        │
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

        // create instance using fluent builder API
        App app = injector.prepare(App.class)
            .build();
        
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

## 🎯 Fluent Builder API

### Simple injection (no constructor arguments)
```java
LibraryInjector injector = new LibraryLoaderInjector(new File("libraries"));

// create instance with default constructor
MyPlugin plugin = injector.prepare(MyPlugin.class)
    .build();
```

### Injection with constructor arguments
```java
Config config = new Config();
Repository repo = new Repository();
Logger logger = Logger.getLogger("app");

// fluent API for adding constructor arguments
MyService service = injector.prepare(MyService.class)
    .with(config)
    .with(repo)
    .with(logger)
    .build();
```

### Injection with explicit parameter types

Useful when you have overloaded constructors or need to pass primitives:
```java
// explicit type specification
MyService service = injector.prepare(MyService.class)
    .withTypes(Config.class, Repository.class, int.class)
    .with(config)
    .with(repo)
    .with(42)
    .build();
```

### Injection by class name
```java
// load class by name (useful for plugins/dynamic loading)
Object instance = injector.prepare("com.example.DynamicPlugin", Object.class)
    .build();

// with constructor arguments
Plugin plugin = injector.prepare("com.example.MyPlugin", Plugin.class)
    .with(config)
    .with(logger)
    .build();
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

| Method | Returns | Description |
|---|---|---|
| `prepare(Class<T>)` | `InjectionBuilder<T>` | Start building an instance of the given class |
| `prepare(String, Class<T>)` | `InjectionBuilder<T>` | Start building an instance by class name |
| `getClassLoader()` | `ClassLoader` | Get the isolated ClassLoader |

### InjectionBuilder API

| Method | Returns | Description |
|---|---|---|
| `with(Object)` | `InjectionBuilder<T>` | Add a constructor argument |
| `withTypes(Class<?>...)` | `InjectionBuilder<T>` | Specify explicit parameter types |
| `build()` | `T` | Create the instance |

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
            
            // inject plugin core with dependencies using builder API
            core = injector.prepare(PluginCore.class)
                .with(this)
                .build();
            
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

## 🔧 Advanced Usage Examples

### Complex constructor with multiple dependencies
```java
DatabaseConfig dbConfig = new DatabaseConfig("localhost", 5432);
CacheConfig cacheConfig = new CacheConfig(1000);
MetricsCollector metrics = new MetricsCollector();

// chain multiple dependencies
Application app = injector.prepare(Application.class)
    .with(dbConfig)
    .with(cacheConfig)
    .with(metrics)
    .with(true)  // enable debug mode
    .build();
```

### Working with primitives and wrappers
```java
// explicit types to handle primitives correctly
MyService service = injector.prepare(MyService.class)
    .withTypes(String.class, int.class, boolean.class)
    .with("myService")
    .with(8080)
    .with(true)
    .build();
```

### Plugin system with dynamic loading
```java
// load multiple plugins dynamically
List<String> pluginClasses = Arrays.asList(
    "com.example.plugins.DatabasePlugin",
    "com.example.plugins.CachePlugin",
    "com.example.plugins.ApiPlugin"
);

List<Plugin> plugins = new ArrayList<>();
for (String className : pluginClasses) {
    Plugin plugin = injector.prepare(className, Plugin.class)
        .with(config)
        .with(logger)
        .build();
    
    plugins.add(plugin);
    plugin.initialize();
}
```

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
    extraJars
);

// extra jars are now available in the isolated ClassLoader
MyPlugin plugin = injector.prepare(MyPlugin.class)
    .build();
```

### Multiple isolated environments
```java
// create separate isolated environments with different dependencies
LibraryInjector injector1 = new LibraryLoaderInjector(new File("libs1"));
LibraryInjector injector2 = new LibraryLoaderInjector(new File("libs2"));

// each has its own dependency versions
Plugin1 plugin1 = injector1.prepare(Plugin1.class)
    .build();

Plugin2 plugin2 = injector2.prepare(Plugin2.class)
    .build();
```

### Reusing the builder pattern
```java
// you can reuse the builder for similar instances
var builder = injector.prepare(Worker.class)
    .with(config)
    .with(logger);

Worker worker1 = builder.build();
Worker worker2 = builder.build();  // creates new instance with same args
```

---

## 💡 Builder Pattern Benefits

### Why use the builder API?

✅ **Fluent and readable** — method chaining makes code self-documenting  
✅ **Type-safe** — compile-time checks for most errors  
✅ **Flexible** — easy to add/remove arguments without changing method signatures  
✅ **Explicit types** — handle primitives and overloaded constructors correctly  
✅ **IDE-friendly** — autocomplete works perfectly with the fluent API

### Comparison: Direct vs Builder
```java
// Without builder (varargs approach)
MyService service = injector.inject(MyService.class, config, repo, logger, 42);

// With builder (fluent API)
MyService service = injector.prepare(MyService.class)
    .with(config)
    .with(repo)
    .with(logger)
    .with(42)
    .build();
```

The builder approach is more verbose but significantly clearer, especially with many parameters.

---

## ❗ Important

> **ClassLoader Isolation**: Each `LibraryInjector` creates a `ChildFirstClassLoader` that prioritizes loaded libraries over parent classloader. This prevents conflicts but means classes from different injectors cannot directly interact.

> **Constructor Resolution**: When using `with()` without `withTypes()`, the builder infers parameter types from argument classes. Use `withTypes()` for explicit control over constructor selection, especially with primitives or overloaded constructors.

> **Builder Reusability**: Each call to `build()` creates a new instance. The builder itself can be reused to create multiple instances with the same constructor arguments.

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