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
│  RUNTIME (LibraryLoader)                            │
│  - reads libraries.toml from jar resources          │
│  - downloads jars into a specified folder           │
│  - resolves transitive dependencies via pom.xml     │
│  - injects everything into the ClassLoader          │
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
    id("io.github.zyr1x.libraryloader") version "1.0.0"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.zyr1x:LibraryLoader:1.0.0")
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

### 3. Use LibraryLoader in your code

```java
// Main.java — entry point, no imports from downloaded libs here!
public class Main {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = new LibraryLoader(
            new File("libraries"),          // cache folder
            Main.class.getClassLoader(),    // classloader
            Logger.getAnonymousLogger()     // logger (or null)
        ).load();

        // launch the main class via the new classloader
        loader.loadClass("com.example.App")
              .getMethod("main", String[].class)
              .invoke(null, (Object) args);
    }
}
```

```java
// App.java — here you can freely import downloaded libs
public class App {
    public static void main(String[] args) {
        // dependencies are already loaded and available
        Reflections reflections = new Reflections("com.example");
        // ...
    }
}
```

---

## ⚙️ Configuration

### Gradle Plugin

| Method | Description |
|---|---|
| `repository(name, url)` | Add a Maven repository |
| `library(notation)` | Add a dependency in `group:artifact:version` format |

### LibraryLoader (runtime)

| Parameter | Type | Description |
|---|---|---|
| `libDir` | `File` | Folder for downloading and caching jars |
| `classLoader` | `ClassLoader` | ClassLoader to inject dependencies into |
| `logger` | `Logger?` | Logger instance (pass `null` if not needed) |

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

LibraryLoader **automatically resolves transitive dependencies** by parsing `pom.xml`.

```kotlin
libraryLoader {
    // just declare the direct dependency
    library("org.reflections:reflections:0.10.2")
    // slf4j, javassist and others will be pulled in automatically ↑
}
```

Dependencies with scopes `test`, `provided`, `system` and `optional` are **ignored**.

---

```kotlin
class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        val loader = LibraryLoader(
            libDir = File(dataFolder, "libraries"),
            classLoader = javaClass.classLoader,
            logger = logger
        ).load()
    }
}
```

---

## ❗ Important

> **Do not import** classes from downloaded libraries in your entry point (`Main`). The JVM resolves imports when the class is loaded — before `LibraryLoader` has a chance to download anything. Move your logic to a separate class (`App`) and load it via reflection.

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