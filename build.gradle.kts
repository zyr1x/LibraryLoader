plugins {
    java
    `maven-publish`
}

group = "ru.lewis"
version = "1.2.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
}