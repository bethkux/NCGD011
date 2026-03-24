plugins {
    kotlin("jvm") version "2.3.10"
}

group = "cz.cuni.gamedev.nail123.mcworldgeneration"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.2-SNAPSHOT")
    // org.bukkit:bukkit is bundled inside spigot-api — no separate dependency needed.
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

kotlin {
    // Compile to Java 25 bytecode. The server must also be launched with Java 25
    // (configured in runServer) so Spigot can load class file version 69.
    jvmToolchain(25)
}

tasks {

    jar {
        archiveFileName.set("MCWorldGenerationPlugin.jar")
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE

        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        from(configurations.runtimeClasspath.get().map { file -> if (file.isDirectory) file else zipTree(file) })
    }
}