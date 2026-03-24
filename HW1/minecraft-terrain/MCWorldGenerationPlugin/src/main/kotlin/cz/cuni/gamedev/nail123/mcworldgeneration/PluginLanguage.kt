package cz.cuni.gamedev.nail123.mcworldgeneration

import kotlin.random.Random

enum class PluginLanguage {
    KOTLIN, JAVA
}

// Change this to use the plugin implementation in a particular language
val selectedLanguage = PluginLanguage.KOTLIN

fun getGeneratorBySelectedLanguage(seed: Long = Random.nextLong()): IChunkGenerator = when (selectedLanguage) {
    PluginLanguage.KOTLIN -> CustomChunkGeneratorKotlin(seed)
    PluginLanguage.JAVA -> CustomChunkGeneratorJava(seed)
}
