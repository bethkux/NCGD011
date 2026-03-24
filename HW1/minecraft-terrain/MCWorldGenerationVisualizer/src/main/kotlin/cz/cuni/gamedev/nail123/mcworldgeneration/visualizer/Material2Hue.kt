package cz.cuni.gamedev.nail123.mcworldgeneration.visualizer

import org.bukkit.Material

// Map of materials to colors used by the Visualizer
val material2Hue = mapOf(
    Material.GRASS_BLOCK  to 0x3a9e3a,
    Material.DIRT         to 0x7a4f2e,
    Material.SAND         to 0xdec97b,
    Material.GRAVEL       to 0x888888,
    Material.STONE        to 0xb4b4b4,
    Material.SNOW_BLOCK   to 0xf0f8ff,
    Material.SNOW         to 0xffffff,
    // Water / sea
    Material.WATER        to 0x1a6eb5,
    Material.AIR          to 0x000000,   // caves – shown as black
    // Ores
    Material.COAL_ORE     to 0x2e2e2e,
    Material.IRON_ORE     to 0xc07830,
    Material.GOLD_ORE     to 0xffd700,
    Material.DIAMOND_ORE  to 0x00e5ff,
    Material.COPPER_ORE   to 0xb86c3b,
    Material.LAPIS_ORE    to 0x1a4fdb,
    // Indestructible base
    Material.BEDROCK      to 0x111111
)