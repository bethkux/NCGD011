package cz.cuni.gamedev.nail123.mcworldgeneration.visualizer.chunking

import cz.cuni.gamedev.nail123.mcworldgeneration.chunking.IChunk
import org.bukkit.Material

class SimpleChunk: IChunk {
    val data = Array(16 * 16 * 256) { Material.AIR }

    override operator fun set(x: Int, y: Int, z: Int, material: Material) {
        data[x * 256 * 16 + z * 256 + y] = material
    }

    override fun get(x: Int, y: Int, z: Int): Material {
        return data[x * 256 * 16 + z * 256 + y]
    }
}