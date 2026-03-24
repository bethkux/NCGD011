plugins {
    kotlin("jvm") version "2.3.10"
}

group = "cz.cuni.gamedev.nail123.mcworldgeneration"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // Spigot snapshots — required to resolve spigot-api.
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    }
    // OSS Sonatype snapshots — hosts Spigot's transitive dependencies.
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(project(":MCWorldGenerationPlugin"))
    implementation("org.spigotmc:spigot-api:1.21.11-R0.2-SNAPSHOT")
    // org.bukkit:bukkit is bundled inside spigot-api — no separate dependency needed.
}

kotlin {
    // Match the plugin's JVM target so both modules stay consistent.
    jvmToolchain(25)
}

tasks {

    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        from(configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file else zipTree(file)
        })

        archiveFileName.set("MCWorldGenerationVisualizer.jar")
        manifest {
            attributes["Main-Class"] = "cz.cuni.gamedev.nail123.mcworldgeneration.visualizer.MainKt"
        }
    }
}
