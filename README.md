\# 📦 LibraryLoader Gradle Plugin



<p align="center">

&nbsp; <img src="https://img.shields.io/badge/Gradle-Plugin-blue?style=for-the-badge\&logo=gradle" />

&nbsp; <img src="https://img.shields.io/badge/Kotlin-JVM-purple?style=for-the-badge\&logo=kotlin" />

&nbsp; <img src="https://img.shields.io/badge/Java-9+-orange?style=for-the-badge\&logo=java" />

&nbsp; <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />

</p>



> Gradle плагин + runtime либа для загрузки зависимостей \*\*во время запуска\*\* приложения, а не во время сборки. Держи свой `.jar` лёгким — пусть зависимости скачиваются сами.



---



\## ✨ Как это работает



```

┌─────────────────────────────────────────────────────┐

│  БИЛД (Gradle плагин)                               │

│  - читает объявленные зависимости                   │

│  - генерирует libraries.toml внутрь jar'а           │

└─────────────────────────────────────────────────────┘

&nbsp;                       ↓

┌─────────────────────────────────────────────────────┐

│  РАНТАЙМ (LibraryLoader)                            │

│  - читает libraries.toml из ресурсов jar'а          │

│  - скачивает jar'ы в указанную папку                │

│  - резолвит транзитивные зависимости через pom.xml  │

│  - загружает всё в ClassLoader                      │

└─────────────────────────────────────────────────────┘

```



Повторный запуск — \*\*скачивания не будет\*\*, всё берётся из кэша.



---



\## 🚀 Быстрый старт



\### 1. Подключи плагин



```kotlin

// settings.gradle.kts

pluginManagement {

&nbsp;   repositories {

&nbsp;       gradlePluginPortal()

&nbsp;   }

}

```



```kotlin

// build.gradle.kts

plugins {

&nbsp;   id("ru.lewis.plugin.libraryloader") version "1.0.0"

}

```



\### 2. Объяви зависимости



```kotlin

libraryLoader {

&nbsp;   // кастомный репозиторий (опционально)

&nbsp;   repository("panda", "https://repo.panda-lang.org/releases")



&nbsp;   // зависимости — скачаются при первом запуске

&nbsp;   library("dev.rollczi:litecommands-bukkit:3.10.9")

&nbsp;   library("com.google.guava:guava:33.0.0-jre")

&nbsp;   library("org.reflections:reflections:0.10.2")

}

```



> \*\*Maven Central\*\* добавляется автоматически — не нужно объявлять вручную.



\### 3. Используй LibraryLoader в коде



```java

// Main.java — точка входа, никаких импортов загружаемых либ!

public class Main {

&nbsp;   public static void main(String\[] args) throws Exception {

&nbsp;       ClassLoader loader = new LibraryLoader(

&nbsp;           new File("libraries"),          // папка для кэша

&nbsp;           Main.class.getClassLoader(),    // classloader

&nbsp;           Logger.getAnonymousLogger()     // логгер (null если не нужен)

&nbsp;       ).load();



&nbsp;       // запускаем основной класс через новый classloader

&nbsp;       loader.loadClass("com.example.App")

&nbsp;             .getMethod("main", String\[].class)

&nbsp;             .invoke(null, (Object) args);

&nbsp;   }

}

```



```java

// App.java — здесь уже можно импортировать загруженные либы

public class App {

&nbsp;   public static void main(String\[] args) {

&nbsp;       // зависимости уже загружены и доступны

&nbsp;       Reflections reflections = new Reflections("com.example");

&nbsp;       // ...

&nbsp;   }

}

```



---



\## 📦 Runtime либа



Подключи `LibraryLoader` в свой проект:



```kotlin

dependencies {

&nbsp;   implementation("ru.lewis:LibraryLoader:1.0-SNAPSHOT")

}

```



```kotlin

repositories {

&nbsp;   mavenCentral()

&nbsp;   maven("https://maven.pkg.github.com/zyr1x/library-loader")

}

```



---



\## ⚙️ Конфигурация



\### Gradle плагин



| Метод | Описание |

|---|---|

| `repository(name, url)` | Добавить Maven репозиторий |

| `library(notation)` | Добавить зависимость в формате `group:artifact:version` |



\### LibraryLoader (runtime)



| Параметр | Тип | Описание |

|---|---|---|

| `libDir` | `File` | Папка для скачивания и кэширования jar'ов |

| `classLoader` | `ClassLoader` | ClassLoader в который инжектятся зависимости |

| `logger` | `Logger?` | Логгер (передай `null` если не нужен) |



---



\## 📁 Структура после первого запуска



```

libraries/

├── dev/rollczi/litecommands-bukkit/3.10.9/

│   └── litecommands-bukkit-3.10.9.jar

├── com/google/guava/guava/33.0.0-jre/

│   └── guava-33.0.0-jre.jar

└── org/reflections/reflections/0.10.2/

&nbsp;   └── reflections-0.10.2.jar

```



---



\## 🔄 Транзитивные зависимости



LibraryLoader \*\*автоматически резолвит транзитивные зависимости\*\* через парсинг `pom.xml`.



```kotlin

libraryLoader {

&nbsp;   // укажи только прямую зависимость

&nbsp;   library("org.reflections:reflections:0.10.2")

&nbsp;   // slf4j, javassist и другие подтянутся автоматически ↑

}

```



Скопы `test`, `provided`, `system` и `optional` зависимости \*\*игнорируются\*\*.



---



\## 🎯 Для Minecraft плагинов



```kotlin

class MyPlugin : JavaPlugin() {

&nbsp;   override fun onEnable() {

&nbsp;       val loader = LibraryLoader(

&nbsp;           libDir = File(dataFolder, "libraries"),

&nbsp;           classLoader = javaClass.classLoader,

&nbsp;           logger = logger

&nbsp;       ).load()



&nbsp;       // либы загружены, можно использовать

&nbsp;   }

}

```



---



\## ❗ Важно



> \*\*Не импортируй\*\* классы из загружаемых либ в точке входа (`Main`). JVM резолвит импорты при загрузке класса — до того как `LibraryLoader` успеет что-либо скачать. Выноси логику в отдельный класс (`App`) и загружай его через рефлексию.



---



\## 🛠️ Сборка из исходников



```bash

git clone https://github.com/zyr1x/library-loader

cd library-loader



\# публикация плагина локально

./gradlew :lib:publishToMavenLocal



\# публикация runtime либы локально

./gradlew :LibraryLoader:publishToMavenLocal

```



---



\## 📄 Лицензия



MIT — делай что хочешь.

