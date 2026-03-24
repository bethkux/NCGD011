package cz.cuni.gamedev.nail123.mcworldgeneration

import cz.cuni.gamedev.nail123.mcworldgeneration.chunking.IChunk

interface IChunkGenerator {
    fun generateChunk(chunkX: Int, chunkZ: Int, chunk: IChunk)
}