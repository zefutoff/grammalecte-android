package fr.grammalecteandroid.engine

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.system.measureNanoTime

class EnginePerformanceCharacterizationTest {
    @Test
    fun characterizeRealEnginePerformance() {
        assumeTrue(
            "Set -PgrammalectePerf=1 to run performance characterization",
            System.getProperty("grammalecte.perf") == "1",
        )

        val assetRoot = findAssetRoot()

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

            println()
            println("GRAMMALECTE PERFORMANCE")
            println("-----------------------")
            println(
                "Cold first check : ${format(coldFirstCheckMillis)} ms",
            )
            println(
                "Sentence median  : ${format(percentile(sentenceSamples, 50))} ms",
            )
            println(
                "Sentence p95     : ${format(percentile(sentenceSamples, 95))} ms",
            )
            println(
                "Paragraph median : ${format(percentile(paragraphSamples, 50))} ms",
            )
            println(
                "Paragraph p95    : ${format(percentile(paragraphSamples, 95))} ms",
            )
            println()
        } finally {
            engine.close()
        }
    }

    private fun measureSamples(
        iterations: Int,
        block: () -> Unit,
    ): List<Double> =
        List(iterations) {
            measureMillis(block)
        }

    private fun measureMillis(block: () -> Unit): Double = measureNanoTime(block) / NANOS_PER_MILLISECOND

    private fun percentile(
        samples: List<Double>,
        percentile: Int,
    ): Double {
        val sorted = samples.sorted()

        val index =
            (
                ceil(
                    percentile / 100.0 * sorted.size,
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
                Path.of("engine-grammalecte/src/main/assets"),
            ).map {
                it.toAbsolutePath().normalize()
            }

        return candidates.firstOrNull {
            Files.isDirectory(it)
        } ?: error(
            "Unable to locate engine assets. Tried: $candidates",
        )
    }

    private class FileAssetTextLoader(
        private val root: Path,
    ) : AssetTextLoader {
        override fun readText(path: String): String {
            val normalizedPath =
                AssetPathNormalizer.normalize(path)

            val file =
                root.resolve(normalizedPath).normalize()

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

    private companion object {
        const val WARMUP_ITERATIONS = 3
        const val SENTENCE_ITERATIONS = 30
        const val PARAGRAPH_ITERATIONS = 20
        const val NANOS_PER_MILLISECOND = 1_000_000.0

        const val SENTENCE =
            "Je suis aller au magazin hier et j'ai acheter deux pomme."

        const val PARAGRAPH =
            "Je suis aller au magazin hier et j'ai acheter deux pomme. " +
                "Les enfant joue dans le jardin pendant que nous somme arrivé en retard. " +
                "Il faut que tu fasse attention car ces une bonne idée de vérifier le texte. " +
                "Je voudrai savoir si vous êtes disponible demain matin."
    }
}
