plugins {
    java
    `maven-publish`
    kotlin("jvm")
}

group = "ru.lewis"
version = "1.3.0-SNAPSHOT"

java {
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation("org.apache.maven:maven-model:3.9.6")
    implementation(kotlin("stdlib-jdk8"))
}
kotlin {
    jvmToolchain(16)
}