plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "xyz.handshot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("http://nexus.okkero.com/repository/maven-releases/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.okkero.skedule:skedule:1.2.6")
    implementation("org.koin:koin-core:2.2.2")
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {

    }
}