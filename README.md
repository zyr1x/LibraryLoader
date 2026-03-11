# 📦 LibraryLoader Gradle Plugin

<p align="center">
  <img src="https://img.shields.io/badge/Gradle-Plugin-blue?style=for-the-badge&logo=gradle" />
  <img src="https://img.shields.io/badge/Kotlin-JVM-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Java-9+-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

> Gradle плагин + runtime либа для загрузки зависимостей **во время запуска** приложения, а не во время сборки. Держи свой `.jar` лёгким — пусть зависимости скачиваются сами.

---

## ✨ Как это работает

```
┌─────────────────────────────────────────────────────┐
│  БИЛД (Gradle плагин)                               │
│  - читает объявленные зависимости                   │
│  - генерирует libraries.toml внутрь jar'а           │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  РАНТАЙМ (LibraryLoader)                            │
│  - читает libraries.toml из ресурсов jar'а          │
│  - скачивает jar'ы в указанную папку                │
│  - резолвит транзитивные зависимости через pom.xml  │
│  - загружает всё в ClassLoader                      │
└─────────────────────────────────────────────────────┘
```

Повторный запуск — **скачивания не будет**, всё берётся из кэша.

---

## 🚀 Быстрый старт

### 1. Подключи плагин

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
    id("ru.lewis.plugin.libraryloader") version "1.0.0"
}

repositories {
	maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.zyr1x:LibraryLoader:1.0.0")
}
```

### 2. Объяви зависимости

```kotlin
libraryLoader {
    // кастомный репозиторий (опционально)
    repository("panda", "https://repo.panda-lang.org/releases")

    // зависимости — скачаются при первом запуске
    library("dev.rollczi:litecommands-bukkit:3.10.9")
    library("com.google.guava:guava:33.0.0-jre")
    library("org.reflections:reflections:0.10.2")
}
```

> **Maven Central** добавляется автоматически — не нужно объявлять вручную.

### 3. Используй LibraryLoader в коде

```java
// Main.java — точка входа, никаких импортов загружаемых либ!
public class Main {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = new LibraryLoader(
            new File("libraries"),          // папка для кэша
            Main.class.getClassLoader(),    // classloader
            Logger.getAnonymousLogger()     // логгер (null если не нужен)
        ).load();

        // запускаем основной класс через новый classloader
        loader.loadClass("com.example.App")
              .getMethod("main", String[].class)
              .invoke(null, (Object) args);
    }
}
```

```java
// App.java — здесь уже можно импортировать загруженные либы
public class App {
    public static void main(String[] args) {
        // зависимости уже загружены и доступны
        Reflections reflections = new Reflections("com.example");
        // ...
    }
}
```

---

## ⚙️ Конфигурация

### Gradle плагин

| Метод | Описание |
|---|---|
| `repository(name, url)` | Добавить Maven репозиторий |
| `library(notation)` | Добавить зависимость в формате `group:artifact:version` |

### LibraryLoader (runtime)

| Параметр | Тип | Описание |
|---|---|---|
| `libDir` | `File` | Папка для скачивания и кэширования jar'ов |
| `classLoader` | `ClassLoader` | ClassLoader в который инжектятся зависимости |
| `logger` | `Logger?` | Логгер (передай `null` если не нужен) |

---

## 📁 Структура после первого запуска

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

## 🔄 Транзитивные зависимости

LibraryLoader **автоматически резолвит транзитивные зависимости** через парсинг `pom.xml`.

```kotlin
libraryLoader {
    // укажи только прямую зависимость
    library("org.reflections:reflections:0.10.2")
    // slf4j, javassist и другие подтянутся автоматически ↑
}
```

Скопы `test`, `provided`, `system` и `optional` зависимости **игнорируются**.

---

## ❗ Важно

> **Не импортируй** классы из загружаемых либ в точке входа (`Main`). JVM резолвит импорты при загрузке класса — до того как `LibraryLoader` успеет что-либо скачать. Выноси логику в отдельный класс (`App`) и загружай его через рефлексию.

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/zyr1x/library-loader
cd library-loader

# публикация плагина локально
./gradlew :lib:publishToMavenLocal

# публикация runtime либы локально
./gradlew :LibraryLoader:publishToMavenLocal
```

---

## 📄 Лицензия

MIT — делай что хочешь.