package cz.cuni.gamedev.nail123.mcworldgeneration.chunking

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator

class ChunkWrapper(val chunkData: ChunkGenerator.ChunkData): IChunk {
    override operator fun set(x: Int, y: Int, z: Int, material: Material) {
        chunkData.setBlock(x, y, z, material)
    }

    override fun get(x: Int, y: Int, z: Int): Material {
        return chunkData.getBlockData(x, y, z).material
    }

    override fun setRegion(xMin: Int, yMin: Int, zMin: Int, xMax: Int, yMax: Int, zMax: Int, material: Material) {
        chunkData.setRegion(xMin, yMin, zMin, xMax, yMax, zMax, material)
    }
}