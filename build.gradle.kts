plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
}

group = "ru.lewis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

publishing {
    publications {
        register<MavenPublication>("maven") {  // ← добавь это
            from(components["java"])
        }
    }
}

dependencies {
}