package cz.cuni.gamedev.nail123.mcworldgeneration.visualizer

import cz.cuni.gamedev.nail123.mcworldgeneration.CustomChunkGeneratorKotlin
import cz.cuni.gamedev.nail123.mcworldgeneration.getGeneratorBySelectedLanguage
import cz.cuni.gamedev.nail123.mcworldgeneration.visualizer.chunking.SimpleChunk
import org.bukkit.Material
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


class CustomVisualizer(val width: Int = 640, val height: Int = 640, seed: Long = Random().nextLong(), var grayscale: Boolean = false) {
    val rng = Random()
    val pixels = IntArray(width * height)
    var generator = getGeneratorBySelectedLanguage(seed)

    fun generate() {
        val heightMap = IntArray2D(width, height) { 0 }
        val materialsMap = List2D(width, height) { Material.AIR }

        for (chunkZ in 0 until height / 16) {
            for (chunkX in 0 until width / 16) {
                val chunk = SimpleChunk()
                generator.generateChunk(chunkX, chunkZ, chunk)
                for (z in 0..15) {
                    for (x in 0..15) {
                        // Generate one pixel of image
                        for (y in 255 downTo 0) {
                            if (chunk[x, y, z] != Material.AIR) {
                                val globX = chunkX * 16 + x
                                val globY = chunkZ * 16 + z
                                heightMap[globX, globY] = y
                                materialsMap[globX, globY] = chunk[x, y, z]

                                break
                            }
                        }
                    }
                }
            }
        }

        val minHeight = heightMap.data.minOrNull()!!
        val maxHeight = heightMap.data.maxOrNull()!!

        val hsb = FloatArray(3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentHeight = heightMap[x, y]
                var heightPercentage = (currentHeight - minHeight).toFloat() / (maxHeight - minHeight)
                // Remap to 0.2 - 0.8
                heightPercentage = heightPercentage * 0.7f + 0.15f
                val material = materialsMap[x, y]

                val colorHex = if (grayscale) 0xffffff else material2Hue[material] ?: continue
                val color = Color(colorHex)

                Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
                // Modify brightness based on height
                hsb[2] *= heightPercentage
                pixels[y * width + x] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])
            }
        }
        // the image is indexed top to bottom, left to right
    }

    fun save(filename: String) {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                img.setRGB(x, y, pixels[y * width + x])
            }
        }

        val file = File(filename)
        file.parentFile?.mkdirs()
        ImageIO.write(img, "png", file)
    }
}
