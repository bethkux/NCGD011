package metrics

import adapters.UniversalMarioGame
import engine.helper.EventType

class MyMetric : AbstractMetric() {
    override val name = "challenge"

    override fun getValue(level: String): String {
        val game = UniversalMarioGame()
        val result = game.runGame(
            mff.agents.astar.Agent(),
            level,
            200,
            0
        )

        val jumps = result.gameEvents.count { it.eventType == EventType.JUMP.value }

        val killEvents = setOf(
            EventType.STOMP_KILL.value,
            EventType.FIRE_KILL.value,
            EventType.SHELL_KILL.value,
            EventType.FALL_KILL.value
        )
        val kills = result.gameEvents.count { it.eventType in killEvents }

        val coins = result.gameEvents.count { it.eventType == EventType.COLLECT.value }

        val completion = result.completionPercentage

        val effortScore = jumps * 1.0
        val combatScore = kills * 1.5
        val rewardScore = coins * 0.3
        val completionPenalty = (1 - completion) * 30.0

        val score = effortScore + combatScore + rewardScore - completionPenalty

        return score.toInt().toString()
    }
}
