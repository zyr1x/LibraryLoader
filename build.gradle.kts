plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
}

group = "ru.lewis"
version = "1.0.5-SNAPSHOT"

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
    implementation("org.apache.maven:maven-model:3.9.6")
}