package fr.grammalecteandroid.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EngineRuntimeStressTest {
    @Test
    fun longLivedRuntimeMemoryRemainsBounded() {
        requireStressEnabled()

        val engine =
            newEngine()

        try {
            repeat(WARMUP_ROUNDS) {
                CORPUS.forEach { text ->
                    assertTrue(
                        engine
                            .check(
                                text = text,
                                localeTag = "fr-FR",
                            ).isNotEmpty(),
                    )
                }

                assertSpellingCorrection(engine)
            }

            val before =
                engine.collectRuntimeGarbageAndSnapshot()

            repeat(LONG_LIVED_ITERATIONS) { index ->
                val text =
                    CORPUS[
                        index %
                            CORPUS.size,
                    ]

                assertTrue(
                    "Expected issues during stress iteration $index",
                    engine
                        .check(
                            text = text,
                            localeTag = "fr-FR",
                        ).isNotEmpty(),
                )

                if (
                    index %
                    WORD_CHECK_INTERVAL ==
                    0
                ) {
                    assertSpellingCorrection(engine)
                }
            }

            val after =
                engine.collectRuntimeGarbageAndSnapshot()

            val growth =
                (
                    after.memoryUsedBytes -
                        before.memoryUsedBytes
                ).coerceAtLeast(0)

            println()
            println("GRAMMALECTE RUNTIME MEMORY")
            println("--------------------------")
            println(
                "Before      : ${formatBytes(before.memoryUsedBytes)}",
            )
            println(
                "After       : ${formatBytes(after.memoryUsedBytes)}",
            )
            println(
                "Growth      : ${formatBytes(growth)}",
            )
            println(
                "Malloc      : ${formatBytes(after.mallocBytes)}",
            )
            println(
                "Objects     : ${after.objectCount}",
            )
            println(
                "Strings     : ${after.stringCount}",
            )
            println()

            assertTrue(
                "QuickJS retained-memory growth " +
                    "${formatBytes(growth)} exceeds " +
                    "${formatBytes(MAX_RETAINED_MEMORY_GROWTH_BYTES)}",
                growth <=
                    MAX_RETAINED_MEMORY_GROWTH_BYTES,
            )
        } finally {
            engine.close()
        }
    }

    @Test
    fun concurrentBurstRemainsCorrect() {
        requireStressEnabled()

        val engine =
            newEngine()

        val executor =
            Executors.newFixedThreadPool(
                CONCURRENT_THREADS,
            )

        val startGate =
            CountDownLatch(1)

        try {
            assertSpellingCorrection(engine)

            val futures =
                List(CONCURRENT_THREADS) { worker ->
                    executor.submit<Boolean> {
                        startGate.await()

                        repeat(
                            CONCURRENT_ITERATIONS_PER_THREAD,
                        ) { iteration ->
                            if (
                                (
                                    worker +
                                        iteration
                                ) % 2 ==
                                0
                            ) {
                                val text =
                                    CORPUS[
                                        (
                                            worker +
                                                iteration
                                        ) %
                                            CORPUS.size,
                                    ]

                                if (
                                    engine
                                        .check(
                                            text = text,
                                            localeTag = "fr-FR",
                                        ).isEmpty()
                                ) {
                                    return@submit false
                                }
                            } else {
                                val check =
                                    engine.checkWord(
                                        word = "magazin",
                                        localeTag = "fr-FR",
                                        suggestionLimit = 8,
                                    )

                                if (
                                    check.valid ||
                                    "magasin" !in
                                    check.suggestions
                                ) {
                                    return@submit false
                                }
                            }
                        }

                        true
                    }
                }

            startGate.countDown()

            futures.forEachIndexed { index, future ->
                assertTrue(
                    "Concurrent stress worker $index failed",
                    future.get(
                        CONCURRENT_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    ),
                )
            }
        } finally {
            startGate.countDown()

            executor.shutdown()

            if (
                !executor.awaitTermination(
                    CONCURRENT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            ) {
                executor.shutdownNow()
            }

            engine.close()
        }
    }

    @Test
    fun repeatedRuntimeCreateUseCloseCyclesRemainStable() {
        requireStressEnabled()

        repeat(RUNTIME_LIFECYCLE_CYCLES) { cycle ->
            val engine =
                newEngine()

            try {
                assertSpellingCorrection(engine)

                val issues =
                    engine.check(
                        text =
                            CORPUS[
                                cycle %
                                    CORPUS.size,
                            ],
                        localeTag = "fr-FR",
                    )

                assertTrue(
                    "Runtime lifecycle cycle $cycle returned no issues",
                    issues.isNotEmpty(),
                )
            } finally {
                engine.close()
            }
        }
    }

    private fun assertSpellingCorrection(engine: GrammalecteQuickJsEngine) {
        val check =
            engine.checkWord(
                word = "magazin",
                localeTag = "fr-FR",
                suggestionLimit = 8,
            )

        assertFalse(
            "Expected « magazin » to remain invalid",
            check.valid,
        )

        assertTrue(
            "Expected « magasin » among suggestions",
            "magasin" in check.suggestions,
        )
    }

    private fun newEngine(): GrammalecteQuickJsEngine =
        GrammalecteQuickJsEngine(
            FileAssetTextLoader(
                findAssetRoot(),
            ),
        )

    private fun requireStressEnabled() {
        assumeTrue(
            "Set -PgrammalecteStress=1 to run runtime stress tests",
            System.getProperty("grammalecte.stress") ==
                "1",
        )
    }

    private fun formatBytes(bytes: Long): String =
        "%.2f MiB".format(
            java.util.Locale.ROOT,
            bytes.toDouble() /
                BYTES_PER_MEBIBYTE,
        )

    private fun findAssetRoot(): Path {
        val candidates =
            listOf(
                Path.of("src/main/assets"),
                Path.of(
                    "engine-grammalecte/src/main/assets",
                ),
            ).map {
                it
                    .toAbsolutePath()
                    .normalize()
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

    private companion object {
        const val WARMUP_ROUNDS = 3
        const val LONG_LIVED_ITERATIONS = 400
        const val WORD_CHECK_INTERVAL = 10

        const val CONCURRENT_THREADS = 8
        const val CONCURRENT_ITERATIONS_PER_THREAD = 20
        const val CONCURRENT_TIMEOUT_SECONDS = 120L

        const val RUNTIME_LIFECYCLE_CYCLES = 10

        const val MAX_RETAINED_MEMORY_GROWTH_BYTES =
            8L * 1024L * 1024L

        const val BYTES_PER_MEBIBYTE =
            1024.0 * 1024.0

        val CORPUS =
            listOf(
                "Je suis aller au magazin hier et " +
                    "j'ai acheter deux pomme.",
                "Les enfant joue dans le jardin " +
                    "pendant que nous somme arrivé en retard.",
                "Elle c'est rendu compte de son erreur.",
                "Il faut que tu fasse attention car " +
                    "ces une bonne idée.",
            )
    }
}
