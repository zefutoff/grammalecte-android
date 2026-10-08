package fr.grammalecteandroid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.system.measureNanoTime

class EnginePerformanceRegressionTest {
    @Test
    fun repeatedAnalysesReuseInitializedRuntimeAssets() {
        val loader =
            CountingFileAssetTextLoader(
                findAssetRoot(),
            )

        val engine =
            GrammalecteQuickJsEngine(
                loader,
            )

        try {
            assertTrue(
                engine
                    .check(
                        text = SENTENCE,
                        localeTag = "fr-FR",
                    ).isNotEmpty(),
            )

            assertTrue(
                engine
                    .check(
                        text = PARAGRAPH,
                        localeTag = "fr-FR",
                    ).isNotEmpty(),
            )

            val readsAfterWarmup =
                loader.readCount

            assertTrue(
                "Expected engine initialization to load assets",
                readsAfterWarmup > 0,
            )

            repeat(REUSE_ITERATIONS) {
                engine.check(
                    text = SENTENCE,
                    localeTag = "fr-FR",
                )

                engine.check(
                    text = PARAGRAPH,
                    localeTag = "fr-FR",
                )
            }

            assertEquals(
                "Repeated analyses must reuse the initialized " +
                    "QuickJS runtime without reloading assets",
                readsAfterWarmup,
                loader.readCount,
            )
        } finally {
            engine.close()
        }
    }

    @Test
    fun realEngineStaysWithinRegressionBudgets() {
        assumeTrue(
            "Set -PgrammalectePerf=1 to run timing budgets",
            System.getProperty("grammalecte.perf") == "1",
        )

        val assetRoot =
            findAssetRoot()

        val coldFirstCheckMillis =
            measureMillis {
                val engine =
                    GrammalecteQuickJsEngine(
                        FileAssetTextLoader(assetRoot),
                    )

                try {
                    engine.checkWord(
                        word = "magazin",
                        localeTag = "fr-FR",
                        suggestionLimit = 8,
                    )
                } finally {
                    engine.close()
                }
            }

        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(assetRoot),
            )

        try {
            engine.checkWord(
                word = "magazin",
                localeTag = "fr-FR",
                suggestionLimit = 8,
            )

            repeat(WARMUP_ITERATIONS) {
                engine.check(
                    text = SENTENCE,
                    localeTag = "fr-FR",
                )
            }

            engine.check(
                text = PARAGRAPH,
                localeTag = "fr-FR",
            )

            val sentenceSamples =
                measureSamples(SENTENCE_ITERATIONS) {
                    engine.check(
                        text = SENTENCE,
                        localeTag = "fr-FR",
                    )
                }

            val paragraphSamples =
                measureSamples(PARAGRAPH_ITERATIONS) {
                    engine.check(
                        text = PARAGRAPH,
                        localeTag = "fr-FR",
                    )
                }

            val sentenceMedian =
                percentile(
                    sentenceSamples,
                    50,
                )

            val sentenceP95 =
                percentile(
                    sentenceSamples,
                    95,
                )

            val paragraphMedian =
                percentile(
                    paragraphSamples,
                    50,
                )

            val paragraphP95 =
                percentile(
                    paragraphSamples,
                    95,
                )

            println()
            println("GRAMMALECTE PERFORMANCE REGRESSION")
            println("----------------------------------")
            println(
                "Cold first check : ${format(coldFirstCheckMillis)} ms",
            )
            println(
                "Sentence median  : ${format(sentenceMedian)} ms",
            )
            println(
                "Sentence p95     : ${format(sentenceP95)} ms",
            )
            println(
                "Paragraph median : ${format(paragraphMedian)} ms",
            )
            println(
                "Paragraph p95    : ${format(paragraphP95)} ms",
            )
            println()

            assertWithinBudget(
                label = "Cold first check",
                measuredMillis = coldFirstCheckMillis,
                budgetMillis = COLD_FIRST_CHECK_BUDGET_MILLIS,
            )

            assertWithinBudget(
                label = "Sentence median",
                measuredMillis = sentenceMedian,
                budgetMillis = SENTENCE_MEDIAN_BUDGET_MILLIS,
            )

            assertWithinBudget(
                label = "Sentence p95",
                measuredMillis = sentenceP95,
                budgetMillis = SENTENCE_P95_BUDGET_MILLIS,
            )

            assertWithinBudget(
                label = "Paragraph median",
                measuredMillis = paragraphMedian,
                budgetMillis = PARAGRAPH_MEDIAN_BUDGET_MILLIS,
            )

            assertWithinBudget(
                label = "Paragraph p95",
                measuredMillis = paragraphP95,
                budgetMillis = PARAGRAPH_P95_BUDGET_MILLIS,
            )
        } finally {
            engine.close()
        }
    }

    private fun assertWithinBudget(
        label: String,
        measuredMillis: Double,
        budgetMillis: Double,
    ) {
        assertTrue(
            "$label regression: ${format(measuredMillis)} ms " +
                "exceeds ${format(budgetMillis)} ms budget",
            measuredMillis <= budgetMillis,
        )
    }

    private fun measureSamples(
        iterations: Int,
        block: () -> Unit,
    ): List<Double> =
        List(iterations) {
            measureMillis(block)
        }

    private fun measureMillis(block: () -> Unit): Double =
        measureNanoTime(block) /
            NANOS_PER_MILLISECOND

    private fun percentile(
        samples: List<Double>,
        percentile: Int,
    ): Double {
        val sorted =
            samples.sorted()

        val index =
            (
                ceil(
                    percentile / 100.0 *
                        sorted.size,
                ).toInt() - 1
            ).coerceIn(
                0,
                sorted.lastIndex,
            )

        return sorted[index]
    }

    private fun format(value: Double): String =
        "%.2f".format(
            java.util.Locale.ROOT,
            value,
        )

    private fun findAssetRoot(): Path {
        val candidates =
            listOf(
                Path.of("src/main/assets"),
                Path.of(
                    "engine-grammalecte/src/main/assets",
                ),
            ).map {
                it.toAbsolutePath().normalize()
            }

        return candidates.firstOrNull {
            Files.isDirectory(it)
        } ?: error(
            "Unable to locate engine assets. Tried: $candidates",
        )
    }

    private open class FileAssetTextLoader(
        private val root: Path,
    ) : AssetTextLoader {
        override fun readText(path: String): String {
            val normalizedPath =
                AssetPathNormalizer.normalize(path)

            val file =
                root
                    .resolve(normalizedPath)
                    .normalize()

            check(file.startsWith(root)) {
                "Asset path escapes test root: $path"
            }

            return Files
                .newBufferedReader(
                    file,
                    StandardCharsets.UTF_8,
                ).use {
                    it.readText()
                }
        }
    }

    private class CountingFileAssetTextLoader(
        root: Path,
    ) : FileAssetTextLoader(root) {
        var readCount: Int = 0
            private set

        override fun readText(path: String): String {
            readCount += 1

            return super.readText(path)
        }
    }

    private companion object {
        const val REUSE_ITERATIONS = 10
        const val WARMUP_ITERATIONS = 3
        const val SENTENCE_ITERATIONS = 20
        const val PARAGRAPH_ITERATIONS = 12

        const val NANOS_PER_MILLISECOND =
            1_000_000.0

        const val COLD_FIRST_CHECK_BUDGET_MILLIS =
            3_000.0

        const val SENTENCE_MEDIAN_BUDGET_MILLIS =
            75.0

        const val SENTENCE_P95_BUDGET_MILLIS =
            200.0

        const val PARAGRAPH_MEDIAN_BUDGET_MILLIS =
            150.0

        const val PARAGRAPH_P95_BUDGET_MILLIS =
            400.0

        const val SENTENCE =
            "Je suis aller au magazin hier et " +
                "j'ai acheter deux pomme."

        const val PARAGRAPH =
            "Je suis aller au magazin hier et " +
                "j'ai acheter deux pomme. " +
                "Les enfant joue dans le jardin pendant que " +
                "nous somme arrivé en retard. " +
                "Il faut que tu fasse attention car ces une " +
                "bonne idée de vérifier le texte. " +
                "Je voudrai savoir si vous êtes disponible " +
                "demain matin."
    }
}
