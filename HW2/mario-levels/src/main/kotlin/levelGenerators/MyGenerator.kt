package levelGenerators

import engine.core.MarioLevelGenerator
import engine.core.MarioLevelModel
import engine.core.MarioTimer
import kotlin.math.abs
import kotlin.random.Random

class MyGenerator : MarioLevelGenerator {

    //private val seed = System.currentTimeMillis()
    private val rng = Random.Default
    private val SAFE_ZONE_END = 10

    override fun getGeneratedLevel(model: MarioLevelModel, timer: MarioTimer): String {
        model.clearMap()
        //println(seed)

        val groundY = model.height - 1

        for (x in 0 until SAFE_ZONE_END) model.setBlock(x, groundY, MarioLevelModel.GROUND)
        model.setBlock(1, groundY - 1, MarioLevelModel.MARIO_START)

        val endStart = model.width - 5
        for (x in endStart until model.width) model.setBlock(x, groundY, MarioLevelModel.GROUND)
        model.setBlock(model.width - 2, groundY - 1, MarioLevelModel.MARIO_EXIT)

        var x = SAFE_ZONE_END
        var lastSegment = "flat"
        var sinceLastGap = 0
        var backtrackPlaced = false

        // Place the backtrack obstacle at a random position in the first 2/3 of the level
        val backtrackAt = SAFE_ZONE_END + rng.nextInt(8, (endStart - SAFE_ZONE_END) * 2 / 3)

        while (x < endStart) {
            val remaining = endStart - x
            if (remaining <= 5) {
                for (fillX in x until endStart) model.setBlock(fillX, groundY, MarioLevelModel.GROUND)
                break
            }

            val isGapSegment = lastSegment in listOf("gap", "bridgeGap", "tubeGap", "snakeGap", "backtrack")

            val choice = when {
                x < 20 -> "flat"
                !backtrackPlaced && x >= backtrackAt -> "backtrack"
                !backtrackPlaced && remaining < 35  -> "backtrack" // safety net
                isGapSegment -> { sinceLastGap = 0; listOf("hill", "flat", "tube").random(rng) }
                remaining < 15 -> listOf("flat", "hill", "tube").random(rng)
                sinceLastGap >= 16 -> weightedGapChoice()
                else -> weightedChoice()
            }

            val used = when (choice) {
                "flat"      -> flat(model, x, groundY, endStart)
                "hill"      -> hill(model, x, groundY, endStart)
                "gap"       -> gap(model, x, groundY, endStart)
                "bridgeGap" -> bridgeGap(model, x, groundY, endStart)
                "snakeGap"  -> snakeGap(model, x, groundY, endStart)
                "backtrack"  -> backtrackObstacle(model, x, groundY, endStart).also { backtrackPlaced = true }
                "tube"      -> tube(model, x, groundY, endStart)
                "tubeGap"   -> tubeGap(model, x, groundY, endStart)
                "coinArc"   -> coinArc(model, x, groundY, endStart)
                else        -> flat(model, x, groundY, endStart)
            }

            if (!isGapSegment) sinceLastGap += used
            lastSegment = choice
            x += used
        }

        return model.map
    }

    /**
     * A challenge where the player must move slightly backward (left)
     * before progressing right. It creates a deliberate puzzle jump.
     * A dumb always-right agent walks off the ground and falls.
     * A smart agent sees it can't cross from ground level, backtracks
     * left and hops through the platforms.
     */
    private fun backtrackObstacle(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val total = 30
        if (startX + total > endStart) return flat(model, startX, groundY, endStart)

        // --- Stable ground before obstacle ---
        for (gx in startX until startX + 4) {
            model.setBlock(gx, groundY, MarioLevelModel.GROUND)
        }

        // Small horizontal variation to avoid identical layouts
        val shiftA = rng.nextInt(-1, 2)
        val shiftB = rng.nextInt(-1, 2)
        val shiftC = rng.nextInt(-1, 2)
        val shiftD = rng.nextInt(-1, 2)

        // First platform: encourages player to jump back onto it
        val aX = (startX + 1 + shiftA).coerceIn(startX, startX + 3)
        model.setBlock(aX,     groundY - 6, MarioLevelModel.PLATFORM)
        model.setBlock(aX - 1, groundY - 6, MarioLevelModel.PLATFORM)

        // Step forward platform
        model.setBlock(startX + 3, groundY - 3, MarioLevelModel.PLATFORM)

        // Elevated platform over the gap start
        val bX = (startX + 5 + shiftB).coerceIn(startX + 4, startX + 7)
        model.setBlock(bX - 1,     groundY - 10, MarioLevelModel.PLATFORM)
        model.setBlock(bX,     groundY - 10, MarioLevelModel.PLATFORM)
        model.setBlock(bX, groundY - 7, MarioLevelModel.PLATFORM)
        model.setBlock(bX + 1, groundY - 7, MarioLevelModel.PLATFORM)
        model.setBlock(bX + 2, groundY - 7, MarioLevelModel.PLATFORM)


        // Midair stepping stone
        val cX = (startX + 10 + shiftC).coerceIn(startX + 11, startX + 15)
        model.setBlock(cX - 1, groundY - 7, MarioLevelModel.PLATFORM)
        model.setBlock(cX, groundY - 7, MarioLevelModel.PLATFORM)
        model.setBlock(cX + 1, groundY - 7, MarioLevelModel.PLATFORM)


        // Final approach platform before landing
        model.setBlock(startX + 16, groundY - 4, MarioLevelModel.PLATFORM)
        model.setBlock(startX + 17, groundY - 4, MarioLevelModel.PLATFORM)

        val dX = (startX + 17 + shiftD).coerceIn(startX + 16, startX + 19)
        model.setBlock(dX - 1 + shiftB, groundY - 5 + shiftA, MarioLevelModel.PLATFORM)
        model.setBlock(dX, groundY - 4, MarioLevelModel.PLATFORM)
        model.setBlock(dX + 1, groundY - 4, MarioLevelModel.PLATFORM)

        if (rng.nextFloat() < 0.6f)
            model.setBlock(bX, groundY - 9, MarioLevelModel.COIN)

        if (rng.nextFloat() < 0.5f)
            model.setBlock(cX, groundY - 8, MarioLevelModel.COIN)

        model.setBlock(dX, groundY - 6, MarioLevelModel.COIN)

        // Safe landing area after obstacle
        for (gx in startX + 24 until startX + total) {
            if (gx < endStart) model.setBlock(gx, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.3f) placeEnemy(model, gx, groundY - 1)
        }

        return total
    }

    private fun weightedChoice(): String = when (rng.nextInt(20)) {
        0, 1, 2    -> "gap"
        3, 4, 5    -> "bridgeGap"
        6, 7, 8    -> "snakeGap"
        9, 10      -> "tubeGap"
        11, 12     -> "tube"
        13, 14, 15 -> "hill"
        16, 17     -> "flat"
        18         -> "coinArc"
        else       -> "hill"
    }

    private fun weightedGapChoice(): String = when (rng.nextInt(10)) {
        0, 1    -> "gap"
        2, 3    -> "bridgeGap"
        4, 5, 6 -> "snakeGap"
        else    -> "tubeGap"
    }

    // Flat ground with scattered enemies and floating reward blocks
    private fun flat(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val length = rng.nextInt(3, 6).coerceAtMost(endStart - startX)
        for (x in startX until startX + length) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.12f) placeEnemy(model, x, groundY - 1)
            if (rng.nextFloat() < 0.15f) addReward(model, x, groundY)
        }
        return length
    }

    // Raised ground column with enemies on top
    private fun hill(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val length = rng.nextInt(3, 7).coerceAtMost(endStart - startX)
        val height = rng.nextInt(2, 5)
        for (x in startX until startX + length) {
            for (h in 0..height) model.setBlock(x, groundY - h, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.08f) placeEnemy(model, x, groundY - height - 1)
            if (rng.nextFloat() < 0.18f) addReward(model, x, groundY - height)
        }
        return length
    }

    // Small plain gap (2–4 tiles)
    private fun gap(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val available = endStart - startX

        // Not enough space -> fallback to safe flat terrain
        if (available < 10) return flat(model, startX, groundY, endStart)

        // Short run-up before the gap so the player can build speed
        val runUp = rng.nextInt(2, 4)
        for (x in startX until startX + runUp) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.12f) placeEnemy(model, x, groundY - 1)
        }

        // Gap width constrained to stay jumpable and leave room for landing
        val gapSize = rng.nextInt(2, 5)
            .coerceAtMost(available - runUp - 4)
            .coerceAtLeast(2)

        val gapEnd = startX + runUp + gapSize

        // Safe landing zone after the gap
        val landingLength = rng.nextInt(3, 5)
            .coerceAtMost(endStart - gapEnd)

        for (x in gapEnd until gapEnd + landingLength) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.12f) placeEnemy(model, x, groundY - 1)
        }

        return runUp + gapSize + landingLength
    }

    // Wide gap (5–8 tiles) with 1–2 floating platforms as the only path across
    private fun bridgeGap(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val available = endStart - startX

        // Not enough space -> fallback to simpler gap
        if (available < 14) return gap(model, startX, groundY, endStart)

        // Run-up so player can prepare for a longer, more precise jump
        val runUp = rng.nextInt(2, 4)
        for (x in startX until startX + runUp) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.12f) placeEnemy(model, x, groundY - 1)
        }

        // Larger gap that cannot be cleared in one jump and requires platforms
        val gapSize = rng.nextInt(5, 9)
            .coerceAtMost(available - runUp - 4)
            .coerceAtLeast(5)

        val gapEnd = startX + runUp + gapSize

        // Landing zone after crossing the platforms
        val landingLength = rng.nextInt(3, 5)
            .coerceAtMost(endStart - gapEnd)

        for (x in gapEnd until gapEnd + landingLength) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.08f) placeEnemy(model, x, groundY - 1)
        }

        // Place midair platforms that act as stepping stones
        placeBridgePlatforms(model, startX + runUp, gapSize, groundY)

        return runUp + gapSize + landingLength
    }

    // Very wide gap crossed via 3–4 narrow staggered platforms at varying heights.
    private fun snakeGap(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val available = endStart - startX
        if (available < 18) return bridgeGap(model, startX, groundY, endStart)

        val runUp = 3
        for (x in startX until startX + runUp) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.06f) placeEnemy(model, x, groundY - 1)
        }

        val numPlatforms = rng.nextInt(3, 5)

        // Fix gap size first, then place platforms evenly inside it
        val gapSize = rng.nextInt(12, 18).coerceAtMost(available - runUp - 5)
        val gapEnd = startX + runUp + gapSize

        // Divide gap into equal slots, one platform per slot
        val slotWidth = gapSize / (numPlatforms + 1)

        // Start at a reachable height and shift +-1 per platform
        var lastY = groundY - rng.nextInt(2, 4)

        for (i in 1..numPlatforms) {
            val platX = startX + runUp + i * slotWidth
            val platW = rng.nextInt(2, 4)

            // Keep platforms between groundY-3 and groundY-5 — always jumpable from ground
            lastY = (lastY + rng.nextInt(-1, 2)).coerceIn(groundY - 5, groundY - 3)

            for (tx in platX until (platX + platW).coerceAtMost(gapEnd)) {
                model.setBlock(tx, lastY, MarioLevelModel.PLATFORM)
            }
        }

        val landingLength = rng.nextInt(3, 5).coerceAtMost(endStart - gapEnd)
        for (x in gapEnd until gapEnd + landingLength) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.10f) placeEnemy(model, x, groundY - 1)
        }

        return runUp + gapSize + landingLength
    }


    // Pipe sitting on a tiny island with a gap on each side.
    private fun tubeGap(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        // Structure: gap -> small island with pipe -> gap -> landing
        val leftGap = rng.nextInt(2, 4)
        val islandWidth = 3
        val rightGap = rng.nextInt(2, 4)
        val landing = rng.nextInt(3, 6)

        val total = leftGap + islandWidth + rightGap + landing

        // Not enough space -> fallback to simple gap
        if (startX + total > endStart) return gap(model, startX, groundY, endStart)

        // Small run-up before first gap
        for (x in startX until startX + 2) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)

            // Moderate pressure before entering the obstacle
            if (rng.nextFloat() < 0.10f) placeEnemy(model, x, groundY - 1)
        }

        // Create the small island between gaps
        val islandX = startX + leftGap
        for (x in islandX until islandX + islandWidth) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
        }

        // Place vertical pipe on the island
        val tubeHeight = rng.nextInt(2, 4)
        for (h in 0 until tubeHeight) {
            val tile =
                if (h == tubeHeight - 1) MarioLevelModel.PIPE_FLOWER
                else MarioLevelModel.PIPE

            model.setBlock(islandX + 1, groundY - 1 - h, tile)
        }

        // Final landing area after second gap
        val landingX = islandX + islandWidth + rightGap
        for (x in landingX until landingX + landing) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)

            // Slightly lower enemy chance to avoid punishing double-gap too hard
            if (rng.nextFloat() < 0.08f) placeEnemy(model, x, groundY - 1)
        }

        return total
    }

    // Pipe on flat ground. Variable height, optional flower on top,
    // sometimes guarded by an enemy on one side, bonus block hovering above.
    private fun tube(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val padWidth = rng.nextInt(5, 8).coerceAtMost(endStart - startX) // wider pad: was nextInt(4, 7)
        if (startX + padWidth >= endStart) return flat(model, startX, groundY, endStart)

        for (x in startX until startX + padWidth) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.08f) placeEnemy(model, x, groundY - 1)
        }

        val tubeHeight = rng.nextInt(2, 5)
        val pipeX = startX + rng.nextInt(1, (padWidth - 1).coerceAtLeast(2))
        for (h in 0 until tubeHeight) {
            val hasFlower = h == tubeHeight - 1 && rng.nextFloat() < 0.40f
            model.setBlock(pipeX, groundY - 1 - h, if (hasFlower) MarioLevelModel.PIPE_FLOWER else MarioLevelModel.PIPE)
        }

        if (rng.nextFloat() < 0.35f) { // less frequent guard: was 0.60f
            val guardX = if (rng.nextBoolean()) pipeX - 1 else pipeX + 1
            if (guardX in startX until startX + padWidth) placeEnemy(model, guardX, groundY - 1)
        }
        if (rng.nextFloat() < 0.45f) {
            model.setBlock(pipeX, groundY - tubeHeight - 3, MarioLevelModel.COIN_QUESTION_BLOCK)
        }

        return padWidth
    }

    // Arc of coins over flat ground: visual reward and movement guide
    // Randomly picks either a V-shape or an A-shape
    private fun coinArc(model: MarioLevelModel, startX: Int, groundY: Int, endStart: Int): Int {
        val length = 7
        val baseY = groundY - 4
        val useVShape = rng.nextBoolean()
        for (i in 0 until length) {
            val x = startX + i
            val y = if (useVShape)
                baseY - abs(3 - i)          // V: middle coin is lowest
            else
                baseY + abs(3 - i) - 3      // A: middle coin is highest
            if (x < endStart && y >= 0) model.setBlock(x, y, MarioLevelModel.COIN)
        }
        for (x in startX until (startX + length).coerceAtMost(endStart)) {
            model.setBlock(x, groundY, MarioLevelModel.GROUND)
            if (rng.nextFloat() < 0.08f) placeEnemy(model, x, groundY - 1)
        }
        return length
    }

    // Places 1 or 2 platforms inside a gap as stepping stones.
    // Wider gaps get two platforms at slightly different heights
    private fun placeBridgePlatforms(model: MarioLevelModel, gapStartX: Int, gapSize: Int, groundY: Int) {
        val platformY = groundY - rng.nextInt(3, 5)
        if (gapSize <= 6) {
            val cx = gapStartX + gapSize / 2 - 1
            val len = rng.nextInt(3, 5) // wider platform: was nextInt(2, 4)
            for (x in cx until cx + len) {
                if (x < model.width) {
                    model.setBlock(x, platformY, MarioLevelModel.PLATFORM)
                    if (rng.nextFloat() < 0.55f) model.setBlock(x, platformY - 1, MarioLevelModel.COIN)
                }
            }
            if (rng.nextFloat() < 0.25f) placeEnemy(model, cx + 1, platformY - 1)
        } else {
            val leftX  = gapStartX + 1
            val rightX = gapStartX + gapSize - 3
            val rightY = (platformY + rng.nextInt(-1, 2)).coerceIn(groundY - 6, groundY - 3)
            for (x in leftX until leftX + rng.nextInt(3, 5)) { // wider platform: was nextInt(2, 4)
                if (x < model.width) {
                    model.setBlock(x, platformY, MarioLevelModel.PLATFORM)
                    if (rng.nextFloat() < 0.55f) model.setBlock(x, platformY - 1, MarioLevelModel.COIN)
                }
            }
            if (rng.nextFloat() < 0.3f) placeEnemy(model, leftX + 1, platformY - 1)
            for (x in rightX until rightX + rng.nextInt(3, 5)) { // wider platform: was nextInt(2, 4)
                if (x < model.width) {
                    model.setBlock(x, rightY, MarioLevelModel.PLATFORM)
                    if (rng.nextFloat() < 0.55f) model.setBlock(x, rightY - 1, MarioLevelModel.COIN)
                }
            }
            if (rng.nextFloat() < 0.3f) placeEnemy(model, rightX + 1, rightY - 1)
        }
    }

    // Randomly picks a non-winged enemy, 15% chance it gets wings
    private fun placeEnemy(model: MarioLevelModel, x: Int, y: Int) {
        if (x < SAFE_ZONE_END || x >= model.width || y < 0) return
        val enemies = MarioLevelModel.getEnemyCharacters(false)
        val winged = rng.nextFloat() < 0.15f
        model.setBlock(x, y, MarioLevelModel.getWingedEnemyVersion(enemies.random(), winged))
    }

    // Floating reward block (coin, special, or coin brick) with an optional extra coin beside it
    private fun addReward(model: MarioLevelModel, x: Int, groundY: Int) {
        val y = groundY - rng.nextInt(3, 6)
        if (y < 0 || x >= model.width) return
        val blockType = when (rng.nextInt(3)) {
            0    -> MarioLevelModel.COIN_QUESTION_BLOCK
            1    -> MarioLevelModel.SPECIAL_QUESTION_BLOCK
            else -> MarioLevelModel.COIN_BRICK
        }
        model.setBlock(x, y, blockType)
        if (rng.nextBoolean() && x + 1 < model.width) model.setBlock(x + 1, y - 1, MarioLevelModel.COIN)
    }

    override fun getGeneratorName(): String = "KulichovaGenerator"
}