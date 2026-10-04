package fr.grammalecteandroid.engine

import fr.grammalecteandroid.core.IssueKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RealEngineSmokeTest {
    @Test
    fun realEmbeddedEngineProducesSpellingAndGrammarCorrections() {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        try {
            val spelling =
                engine.checkWord(
                    word = "magazin",
                    localeTag = "fr-FR",
                    suggestionLimit = 8,
                )

            assertFalse(
                "Expected « magazin » to be reported as invalid",
                spelling.valid,
            )

            assertTrue(
                "Expected « magasin » among spelling suggestions: ${spelling.suggestions}",
                "magasin" in spelling.suggestions,
            )

            val text = "Je suis aller au magasin."
            val issues = engine.check(text, "fr-FR")

            val grammarIssue =
                issues.firstOrNull { issue ->
                    text.substring(issue.start, issue.endExclusive) == "aller"
                } ?: error(
                    "Expected a grammar issue on « aller », got: $issues",
                )

            assertEquals(
                IssueKind.GRAMMAR,
                grammarIssue.kind,
            )

            assertTrue(
                "Expected « allé » or « allée » among grammar suggestions: ${grammarIssue.suggestions}",
                grammarIssue.suggestions.any {
                    it == "allé" || it == "allée"
                },
            )
        } finally {
            engine.close()
        }
    }

    @Test
    fun concurrentCallsShareRuntimeSafely() {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        val executor = Executors.newFixedThreadPool(4)
        val startGate = CountDownLatch(1)

        try {
            val futures =
                List(4) { index ->
                    executor.submit<Boolean> {
                        startGate.await()

                        if (index % 2 == 0) {
                            val text = "Je suis aller au magasin."

                            engine
                                .check(
                                    text = text,
                                    localeTag = "fr-FR",
                                ).any { issue ->
                                    text.substring(
                                        issue.start,
                                        issue.endExclusive,
                                    ) == "aller"
                                }
                        } else {
                            val check =
                                engine.checkWord(
                                    word = "magazin",
                                    localeTag = "fr-FR",
                                    suggestionLimit = 8,
                                )

                            !check.valid &&
                                "magasin" in check.suggestions
                        }
                    }
                }

            startGate.countDown()

            futures.forEachIndexed { index, future ->
                assertTrue(
                    "Concurrent engine call $index failed",
                    future.get(
                        60,
                        TimeUnit.SECONDS,
                    ),
                )
            }
        } finally {
            startGate.countDown()
            executor.shutdown()

            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }

            engine.close()
        }
    }

    private fun findAssetRoot(): Path {
        val candidates =
            listOf(
                Path.of("src/main/assets"),
                Path.of("engine-grammalecte/src/main/assets"),
            ).map {
                it.toAbsolutePath().normalize()
            }

        return candidates.firstOrNull { Files.isDirectory(it) }
            ?: error(
                "Unable to locate engine-grammalecte/src/main/assets. Tried: $candidates",
            )
    }

    private class FileAssetTextLoader(
        private val root: Path,
    ) : AssetTextLoader {
        override fun readText(path: String): String {
            val normalizedPath = AssetPathNormalizer.normalize(path)
            val file = root.resolve(normalizedPath).normalize()

            check(file.startsWith(root)) {
                "Asset path escapes test root: $path"
            }

            return Files
                .newBufferedReader(
                    file,
                    StandardCharsets.UTF_8,
                ).use { it.readText() }
        }
    }
}
