package cz.cuni.gamedev.nail123.mcworldgeneration.chunking

import org.bukkit.Material

interface IChunk {
    operator fun set(x: Int, y: Int, z: Int, material: Material)
    operator fun get(x: Int, y: Int, z: Int): Material
    fun setRegion(
        xMin: Int,
        yMin: Int,
        zMin: Int,
        xMax: Int,
        yMax: Int,
        zMax: Int,
        material: Material
    ) {
        for (x in xMin .. xMax) {
            for (y in yMin .. yMax) {
                for (z in zMin .. zMax) {
                    this[x, y, z] = material
                }
            }
        }
    }
}