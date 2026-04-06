import levelGenerators.sample.GapLevelGenerator
import levelGenerators.MyGenerator
import metrics.AbstractMetric
import metrics.JumpMetric
import metrics.LinearityMetric
import metrics.MetricsEvaluator
import metrics.MyMetric

// Runs an evaluation of metrics on a given generator
fun main() {
    val levelGenerator = MyGenerator()
    //val levelGenerator = levelGenerators.notch.LevelGenerator()
    //val levelGenerator = levelGenerators.benWeber.LevelGenerator()

    val metrics = listOf<AbstractMetric>(
        LinearityMetric(),
        MyMetric()
    )

    val metricsEvaluator = MetricsEvaluator(levelGenerator, metrics)

    metricsEvaluator.generateCSV("src/main/python/data/metrics.csv", 100)
}