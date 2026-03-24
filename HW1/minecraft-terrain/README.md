# README

A task for NAIL123 Procedural Generation in Computer Games - generating levels
in Minecraft.

The repo contains a full Spigot Minecraft server that is used for map generation.

## Requirements

- Java 17 or newer
- IntelliJ IDEA (recommended)

## How to run

Everything can be managed by Gradle, which should be run using the `./gradlew` wrapper.

To start, just run `./gradlew generateMap`.
The first run will take a few minutes, but the subsequent will be faster.
This task will fail on a minecraft EULA agreement, which you can confirm by editing `server/eula.txt` and changing false to true.
Then, try running it again.

Two tasks are used to run the solution (by `./gradlew <Task>`):
 - `generateMap` - runs the Minecraft server for a short while to generate map, then saves it to `build/render/big.png`.
 - `runServer` - runs the Minecraft server indefinitely (on `localhost:25565`), so you can join as a player and explore!

## How to edit

The task is implementing a world generator for Minecraft. You can use either Java or Kotlin, select which you are using in
`cz/cuni/gamedev/nail123/mcworldgeneration/PluginLanguage.kt`.

If you choose Kotlin, edit only `src/main/kotlin/cz/cuni/gamedev/nail123/mcworldgeneration/CustomChunkGeneratorKotlin`.

If you choose Java, edit only     `src/main/java/cz/cuni/gamedev/nail123/mcworldgeneration/CustomChunkGeneratorJava`.

These currently contain a very simple generator using noise. 
Edit the contents of this file (use inner classes if you need them) and generate the world your own way! 
