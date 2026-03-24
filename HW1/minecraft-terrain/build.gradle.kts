// ExecOperations is the Gradle 9+ API for running external processes from tasks.
// Project.javaexec() / Project.exec() were removed in Gradle 9, so tasks that need
// to launch a JVM process must inject ExecOperations instead.
import org.gradle.process.ExecOperations
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import javax.inject.Inject

group = "cz.cuni.gamedev.nail123.mcworldgeneration"
version = "1.0-SNAPSHOT"

// Apply shared configuration to every subproject (MCWorldGenerationPlugin + MCWorldGenerationVisualizer).
subprojects {
    version = "1.0"

    repositories {
        mavenCentral()

        // Spigot snapshots — required for spigot-api artifacts that haven't been promoted to a stable repo.
        maven {
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
        }
        // OSS Sonatype snapshots — Spigot's transitive dependencies (e.g. Bukkit) live here.
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}

plugins {
    // Provides the Download task used to fetch BuildTools.jar from the Spigot CI server.
    id("de.undercouch.download") version "5.7.0"
    // java-base registers JavaToolchainService on the root project without adding
    // any compilation tasks — required to resolve a toolchain launcher here.
    `java-base`
}

// ======================================
//  Use generator to output Map into PNG
// ======================================

// Runs the Visualizer's main class, which renders the terrain into a colour PNG.
tasks.register<JavaExec>("generateMap") {
    group = "map generation"
    dependsOn(":MCWorldGenerationVisualizer:jar")

    classpath = files("MCWorldGenerationVisualizer/build/libs/MCWorldGenerationVisualizer.jar")
}

// Same as generateMap but passes --grayscale so the output is a single-channel heightmap.
tasks.register<JavaExec>("generateMapGrayscale") {
    group = "map generation"
    dependsOn(":MCWorldGenerationVisualizer:jar")

    classpath = files("MCWorldGenerationVisualizer/build/libs/MCWorldGenerationVisualizer.jar")
    args = listOf("--grayscale")
}

// ================================================
//  Run a full Minecraft server with custom plugin
// ================================================

// The Spigot server version to build and run. Changing this also changes the BuildTools target.
val spigotVersion = "1.21.11"
// Filename of the compiled Spigot server JAR produced by BuildTools.
val serverRunnable = "spigot-$spigotVersion.jar"

// A toolchain-resolved Java 25 launcher, used wherever we need to spawn a JVM process.
// This lets Gradle locate the JDK through its toolchain infrastructure (auto-detection,
// gradle.properties paths, etc.) rather than a hardcoded filesystem path.
val javaToolchains = extensions.getByType<JavaToolchainService>()
val serverJavaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
}

// Downloads BuildTools.jar from the Spigot Jenkins CI if it isn't already present.
// BuildTools is the official tool that compiles Spigot from Minecraft's obfuscated sources.
tasks.register<de.undercouch.gradle.tasks.download.Download>("getBuildTools") {
    group = "server"
    src("https://hub.spigotmc.org/jenkins/job/BuildTools/lastStableBuild/artifact/target/BuildTools.jar")
    dest("server/BuildTools.jar")
    overwrite(false)   // Don't re-download if already present.

    outputs.file(dest)
}

// Custom task class for building the Spigot server JAR via BuildTools.
// Declared as an abstract class so Gradle can inject services (ExecOperations) automatically.
abstract class BuildServerTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask() {
    @get:Input abstract val targetVersion: Property<String>
    @get:Input abstract val targetJar: Property<String>
    // JavaLauncher carries the toolchain-resolved JVM — Gradle wires it up automatically.
    @get:Nested abstract val launcher: Property<JavaLauncher>

    @TaskAction
    fun run() {
        val f = File(project.projectDir, "server/${targetJar.get()}")
        // Skip the lengthy BuildTools compilation if the server JAR already exists.
        if (!f.exists()) {
            execOps.javaexec {
                // Use the toolchain launcher's executable rather than whatever java is on PATH.
                executable = launcher.get().executablePath.asFile.absolutePath
                workingDir = File(project.projectDir, "server")
                classpath = project.files("server/BuildTools.jar")
                // --rev tells BuildTools which Minecraft version to compile Spigot for.
                args = listOf("--rev", targetVersion.get())
            }
        }
    }
}

// Compiles a Spigot server JAR for the configured Minecraft version using BuildTools.
// This only runs BuildTools when the JAR is actually missing, making it idempotent.
tasks.register<BuildServerTask>("buildServer") {
    dependsOn("getBuildTools")
    group = "server"
    targetVersion.set(spigotVersion)
    targetJar.set(serverRunnable)
    launcher.set(serverJavaLauncher)
    outputs.file("server/$serverRunnable")
}

// Deletes all generated server files except BuildTools.jar, eula.txt, and the plugins folder
// (preserving StartCommands.jar/dir inside plugins as well). Useful for a clean server reset.
tasks.register<Delete>("cleanServer") {
    group = "server"
    (
        fileTree("server") { exclude("BuildTools.jar", "eula.txt", "plugins") } +
        fileTree("server/plugins") { exclude("StartCommands.jar", "StartCommands") }
    ).visit {
        delete(this.file)
    }
}

// Deletes the generated world folder so the next server run produces a fresh map.
tasks.register<Delete>("deleteMap") {
    group = "server"
    delete("server/world")
}

// Custom task class for one-time server configuration.
// Injects ExecOperations to be able to start the server process (Gradle 9+ requirement).
abstract class ConfigureServerTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask() {
    @get:Input abstract val targetJar: Property<String>
    // JavaLauncher carries the toolchain-resolved JVM — Gradle wires it up automatically.
    @get:Nested abstract val launcher: Property<JavaLauncher>

    @TaskAction
    fun run() {
        val f = File(project.projectDir, "server/bukkit.yml")
        if (!f.exists()) {
            // bukkit.yml is only generated on the first server start, so we boot the server
            // and immediately send "stop" to stdin to shut it down right after initialisation.
            execOps.javaexec {
                // Use the toolchain launcher's executable rather than whatever java is on PATH.
                executable = launcher.get().executablePath.asFile.absolutePath
                workingDir = File(project.projectDir, "server")
                classpath = project.files("server/${targetJar.get()}")
                standardInput = "stop\n".byteInputStream()
            }
        }
        val lines = f.readLines()
        // Append the custom world generator setting if it hasn't been added yet.
        // This tells Bukkit to use our MCWorldGenerationPlugin for the "world" level.
        if (lines.none { "worlds:" in it }) {
            f.appendText("""
                worlds:
                  world:
                    generator: MCWorldGenerationPlugin
            """.trimIndent())
            // Delete the world folder so it gets regenerated with our custom generator
            // on the next server start (the old world was produced without it).
            project.delete("server/world")
        }
    }
}

// Ensures bukkit.yml exists and contains the custom world generator entry.
// Depends on buildServer so the Spigot JAR is available before we try to start it.
tasks.register<ConfigureServerTask>("configureServerForPlugin") {
    group = "server"
    dependsOn("buildServer")
    targetJar.set(serverRunnable)
    launcher.set(serverJavaLauncher)
}

// Copies the plugin JAR built by MCWorldGenerationPlugin into the server's plugins folder.
tasks.register<Copy>("copyPluginToServer") {
    group = "server"
    dependsOn(":MCWorldGenerationPlugin:jar")

    from("MCWorldGenerationPlugin/build/libs")
    into("server/plugins")
}

// Starts the Spigot server with the custom terrain-generation plugin loaded.
// Forwards stdin so you can type server commands interactively in the terminal.
// javaLauncher is the JavaExec-native way to pin a task to a specific toolchain JVM.
tasks.register<JavaExec>("runServer") {
    group = "server"
    dependsOn("configureServerForPlugin", "copyPluginToServer")

    javaLauncher.set(serverJavaLauncher)
    workingDir = File("server")
    classpath = files("server/$serverRunnable")
    standardInput = System.`in`
}
