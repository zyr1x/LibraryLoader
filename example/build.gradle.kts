plugins {
    id("java")
    id("io.github.zyr1x.libraryloader") version "1.0.1"
    id("com.gradleup.shadow") version "8.3.3"
}

group = "ru.lewis.testplugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    maven("https://jitpack.io") { name = "Jitpack" }
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation("com.github.zyr1x:LibraryLoader:1.2.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

tasks.shadowJar {
    archiveBaseName.set("TestPlugin")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("")
}

libraryLoader {
    library("com.google.code.gson:gson:2.13.2")
}