package cz.cuni.gamedev.nail123.mcworldgeneration.visualizer

import java.util.*

fun main(args: Array<String>) {
    // If you want to generate deterministically, set seed here
//    val seed = 42L
    val seed = Random().nextLong()

    val visualizer = CustomVisualizer(1024, 768, seed, grayscale = "--grayscale" in args)

    val outPath = args.firstOrNull { it != "--grayscale" && it != "" } ?: "build/render/map.png"
    visualizer.generate()
    visualizer.save(outPath)
}