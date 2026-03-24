package cz.cuni.gamedev.nail123.mcworldgeneration

import cz.cuni.gamedev.nail123.mcworldgeneration.chunking.IChunk
import org.bukkit.Material
import org.bukkit.util.noise.SimplexOctaveGenerator
import org.bukkit.util.noise.PerlinOctaveGenerator
import java.util.Random
import kotlin.math.*

// ─────────────────
//  World constants
// ─────────────────
private const val WORLD_MIN_Y   = -64
private const val SEA_LEVEL     = 62
private const val BEDROCK_WORLD = -60

/** World Y -> chunk-local array index. */
private fun toLocal(worldY: Int) = worldY - WORLD_MIN_Y

// ───────────────────
//  Biome definitions
// ───────────────────
private enum class Biome { SEA, GRASSLAND, DESERT, MOUNTAINS, FOREST }

/**
 * Per-biome terrain parameters.
 *
 * Surface Y = baseHeight + detailNoise * heightVariance
 */
private data class BiomeParams(
    val baseHeight: Int,
    val heightVariance: Double,
    val noiseFreqMult: Double = 1.0,
    val surfaceMat: Material,
    val subsurfaceMat: Material
)

private val BIOME_PARAMS = mapOf(
    Biome.SEA        to BiomeParams(40,   8.0, 0.8, Material.GRAVEL,      Material.GRAVEL),
    Biome.GRASSLAND  to BiomeParams(75,   7.0, 1.2, Material.GRASS_BLOCK, Material.DIRT),
    Biome.DESERT     to BiomeParams(60,  10.0, 1.2, Material.SAND,        Material.SAND),
    Biome.MOUNTAINS  to BiomeParams(105, 65.0, 1.9, Material.STONE,       Material.STONE),
    Biome.FOREST     to BiomeParams(80,  15.0, 1.4, Material.MOSS_BLOCK,  Material.DIRT)
)

// ─────────────────────────────────────────────────────────────────────────────
//  Whittaker diagram
//
//  Two noise axes, each in -1..1:
//    temperature (t):  negative = cold,  positive = hot
//    humidity    (h):  negative = dry,   positive = wet
//
//               dry           wet
//           ─────────────────────────
//   cold  │  MOUNTAINS       FOREST
//  middle │          GRASSLAND
//   hot   │    DESERT         SEA
// ─────────────────────────────────────────────────────────────────────────────

private fun classifyBiome(t: Double, h: Double) = when {
    abs(t) < 0.22    -> Biome.GRASSLAND   // moderate temperature band
    t < 0.0 && h >= 0.0  -> Biome.FOREST      // cold, wet
    t < 0.0 && h <  0.0  -> Biome.MOUNTAINS   // cold, dry
    t >= 0.0 && h >= 0.0 -> Biome.SEA         // hot, wet
    else                 -> Biome.DESERT       // hot, dry
}

// ─────────────────────────────────────────────────────────────────────────────
//  Ore specs
//
//  Each ore has its own noise generator so blob shapes are fully independent.
//  Ores are checked rarest-first so diamond takes priority over iron
//  when multiple ores qualify at the same block.
// ─────────────────────────────────────────────────────────────────────────────

private data class OreSpec(
    val material:  Material,
    val minY:      Int,
    val maxY:      Int,
    val threshold: Double,
    val scale:     Double
)

private val ORE_SPECS = listOf(
    OreSpec(Material.DIAMOND_ORE, -60, -10, 0.7,  0.10),  // deep only, rare, small blobs
    OreSpec(Material.GOLD_ORE,    -40,   0, 0.66, 0.09),  // deep-mid, uncommon
    OreSpec(Material.IRON_ORE,    -30,  20, 0.62, 0.085), // mid depth, medium density
    OreSpec(Material.COPPER_ORE,  -10,  40, 0.55, 0.08),  // upper-mid depth, fairly common
    OreSpec(Material.COAL_ORE,      0,  80, 0.5,  0.07)   // near surface, common, large veins
)

// ───────────
//  Generator
// ───────────

class CustomChunkGeneratorKotlin(var seed: Long = Random().nextLong()) : IChunkGenerator {

    // Biome axes: low frequency so biomes span hundreds of blocks
    private val tempNoise   = SimplexOctaveGenerator(Random(seed),                  4).apply { setScale(0.005) }
    private val humidNoise  = SimplexOctaveGenerator(Random(seed xor 0xDEADBEEFL),  4).apply { setScale(0.005) }

    // Detail noise: medium frequency, 8 octaves for rich surface variation
    private val detailNoise = SimplexOctaveGenerator(Random(seed + 1L),             8).apply { setScale(0.006) }

    // Cave noise: two independent 3D Perlin fields.
    private val caveNoise1  = PerlinOctaveGenerator(Random(seed + 10L), 3).apply { setScale(0.02) }
    private val caveNoise2  = PerlinOctaveGenerator(Random(seed + 11L), 3).apply { setScale(0.02) }

    // One independent noise generator per ore type
    private val oreNoises = ORE_SPECS.mapIndexed { i, ore ->
        ore to PerlinOctaveGenerator(Random(seed + 20L + i), 2).apply { setScale(ore.scale) }
    }.toMap()

    // ── Biome helpers ──
    private fun temperature(wx: Double, wz: Double) =
        tempNoise.noise(wx, wz, 2.0, 0.5, true)

    private fun humidity(wx: Double, wz: Double) =
        humidNoise.noise(wx, wz, 2.0, 0.5, true)

    /** Raw (unblended) surface height for a single biome at (wx, wz). */
    private fun rawHeight(wx: Double, wz: Double, biome: Biome): Double {
        val p = BIOME_PARAMS.getValue(biome)
        val n = detailNoise.noise(wx * p.noiseFreqMult, wz * p.noiseFreqMult, 2.0, 0.5, true)
        return p.baseHeight + n * p.heightVariance
    }

    /**
     * Gaussian-weighted blend of all five biome heights at (wx, wz).
     *
     * Each biome has an ideal center in (temperature, humidity) space.
     * Weight = e^(-distance^2 / falloff^2). Points near a center get mostly
     * that biome's height, points between centres get a smooth interpolation.
     */
    private fun blendedHeight(wx: Double, wz: Double): Int {
        val t = temperature(wx, wz)
        val h = humidity(wx, wz)

        data class Centre(val biome: Biome, val tc: Double, val hc: Double)
        val centres = listOf(
            Centre(Biome.MOUNTAINS, -0.6, -0.5),
            Centre(Biome.FOREST,    -0.6,  0.5),
            Centre(Biome.GRASSLAND,  0.0,  0.0),
            Centre(Biome.DESERT,     0.6, -0.5),
            Centre(Biome.SEA,        0.6,  0.5)
        )

        val falloff = 0.5
        var wSum = 0.0
        var hSum = 0.0
        for (c in centres) {
            val dt = (t - c.tc) / falloff
            val dh = (h - c.hc) / falloff
            val w  = exp(-(dt * dt + dh * dh))
            wSum += w
            hSum += w * rawHeight(wx, wz, c.biome)
        }
        return (hSum / wSum).roundToInt()
    }

    /** Returns the dominant biome at (wx, wz) for surface material selection. */
    private fun dominantBiome(wx: Double, wz: Double) =
        classifyBiome(temperature(wx, wz), humidity(wx, wz))

    /**
     * Perlin worm cave check.
     */
    private fun isCave(wx: Double, wy: Double, wz: Double): Boolean {
        val n1 = caveNoise1.noise(wx, wy, wz, 1.0, 0.5, false)
        val n2 = caveNoise2.noise(wx, wy, wz, 1.0, 0.5, false)
        val narrow = abs(n1) < 0.05 && abs(n2) < 0.05
        val wide   = abs(n1) < 0.11 && abs(n2) < 0.11
        return narrow || (wide && n1 * n2 < 0)
    }

    /**
     * Returns the ore to place at (wx, wy, wz), or null for plain stone.
     */
    private fun oreAt(wx: Double, wy: Double, wz: Double): Material? {
        for (ore in ORE_SPECS) {
            if (wy.toInt() !in ore.minY..ore.maxY) continue
            val n = (oreNoises[ore] ?: return null).noise(wx, wy, wz, 1.0, 0.5, false)
            if (n > ore.threshold) return ore.material
        }
        return null
    }

    // ── Chunk generation ──
    override fun generateChunk(chunkX: Int, chunkZ: Int, chunk: IChunk) {
        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = (chunkX * 16 + x).toDouble()
                val worldZ = (chunkZ * 16 + z).toDouble()

                val surfaceY = blendedHeight(worldX, worldZ).coerceIn(BEDROCK_WORLD + 5, 310)
                val biome    = dominantBiome(worldX, worldZ)
                val params   = BIOME_PARAMS.getValue(biome)

                // ── Surface block ──
                // Mountains override their surface material by height
                // All other biomes use their BiomeParams surface material directly.
                val topMat = when {
                    biome == Biome.FOREST    && surfaceY > 104 -> Material.SNOW_BLOCK
                    biome == Biome.MOUNTAINS && surfaceY > 102 -> Material.SNOW_BLOCK
                    biome == Biome.MOUNTAINS && surfaceY > 88  -> Material.STONE
                    else -> params.surfaceMat
                }
                chunk[x, toLocal(surfaceY), z] = topMat

                // ── Sub-surface ──
                for (i in 1..3) {
                    val worldY = surfaceY - i
                    if (worldY > BEDROCK_WORLD)
                        chunk[x, toLocal(worldY), z] = params.subsurfaceMat
                }

                // ── Water fill ──
                // Fills from just above the terrain up to sea level.
                // Only has effect when surfaceY < SEA_LEVEL (sea biome columns).
                for (worldY in (surfaceY + 1)..SEA_LEVEL) {
                    chunk[x, toLocal(worldY), z] = Material.WATER
                }

                // ── Underground: stone and ores ──
                // Fill from just below the sub-surface layers down to just above
                // bedrock with stone, replacing with ore where noise qualifies.
                // This runs before cave carving so ores end up in cave walls.
                for (worldY in (surfaceY - 4) downTo (BEDROCK_WORLD + 1)) {
                    val ly = toLocal(worldY)
                    val wy = worldY.toDouble()
                    chunk[x, ly, z] = oreAt(worldX, wy, worldZ) ?: Material.STONE
                }

                // ── Cave carving ──
                // Carves from near the surface down to just above bedrock.
                // Starting near surfaceY allows cave openings to appear at surface.
                // Skipped on underwater columns so ocean floors stay solid.
                // Uses a per-column hash to vary how close to the surface carving star,
                // giving irregular entrance shapes.
                val isUnderwater = surfaceY < SEA_LEVEL
                val caveHash  = (worldX.toLong() * 1664525L + worldZ.toLong() * 1013904223L + seed) ushr 32
                val caveExtra = (caveHash % 3).toInt()

                for (worldY in (surfaceY - 2 + caveExtra) downTo (BEDROCK_WORLD + 1)) {
                    val ly = toLocal(worldY)
                    val wy = worldY.toDouble()
                    if (!isUnderwater && isCave(worldX, wy, worldZ)) {
                        chunk[x, ly, z] = Material.AIR
                    }
                }

                // ── Bedrock ──
                chunk[x, toLocal(BEDROCK_WORLD), z] = Material.BEDROCK
            }
        }
    }
}