package cz.cuni.gamedev.nail123.mcworldgeneration

import cz.cuni.gamedev.nail123.mcworldgeneration.chunking.ChunkWrapper
import org.bukkit.World
import org.bukkit.generator.ChunkGenerator
import java.util.Random

class WorldGenerationAdapter: ChunkGenerator() {
    val chunkGenerator = getGeneratorBySelectedLanguage()

    override fun generateChunkData(world: World, random: Random, chunkX: Int, chunkZ: Int, biome: BiomeGrid): ChunkData {
        val chunk = createChunkData(world)

        chunkGenerator.generateChunk(chunkX, chunkZ, ChunkWrapper(chunk))

        return chunk
    }
}