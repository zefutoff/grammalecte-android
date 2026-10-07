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
    fun ruleOptionsExposeUpstreamMetadataAndCanBeReset() {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        try {
            val options = engine.ruleOptions()

            assertTrue(
                "Expected Grammalecte rule options",
                options.isNotEmpty(),
            )

            assertTrue(
                "Debug options must not be exposed",
                options.none { option ->
                    option.groupId == "debug"
                },
            )

            val infinitive =
                options.firstOrNull { option ->
                    option.id == "infi"
                } ?: error(
                    "Expected the upstream infi option",
                )

            assertEquals(
                "verbs",
                infinitive.groupId,
            )

            assertEquals(
                "Verbes",
                infinitive.groupLabel,
            )

            assertEquals(
                "Infinitif",
                infinitive.label,
            )

            assertTrue(
                "Expected infi to be enabled by default",
                infinitive.defaultEnabled,
            )

            val apostrophe =
                options.firstOrNull { option ->
                    option.id == "apos"
                } ?: error(
                    "Expected the upstream apos option",
                )

            assertTrue(
                "Expected apos to be enabled by default",
                apostrophe.defaultEnabled,
            )

            val apostropheText =
                "Elle c'est rendu compte de son erreur."

            assertTrue(
                "Expected an apostrophe issue before disabling apos",
                engine
                    .check(
                        text = apostropheText,
                        localeTag = "fr-FR",
                    ).any { issue ->
                        apostropheText.substring(
                            issue.start,
                            issue.endExclusive,
                        ) == "c'" &&
                            "c’" in issue.suggestions
                    },
            )

            engine.setRuleOption(
                id = "apos",
                enabled = false,
            )

            assertFalse(
                "Expected apos to be disabled",
                engine
                    .ruleOptions()
                    .first { option ->
                        option.id == "apos"
                    }.enabled,
            )

            assertTrue(
                "Expected disabling apos to suppress the apostrophe issue",
                engine
                    .check(
                        text = apostropheText,
                        localeTag = "fr-FR",
                    ).none { issue ->
                        apostropheText.substring(
                            issue.start,
                            issue.endExclusive,
                        ) == "c'" &&
                            "c’" in issue.suggestions
                    },
            )

            engine.resetRuleOptions()

            assertTrue(
                "Expected reset to restore apos",
                engine
                    .ruleOptions()
                    .first { option ->
                        option.id == "apos"
                    }.enabled,
            )

            assertTrue(
                "Expected reset to restore the apostrophe issue",
                engine
                    .check(
                        text = apostropheText,
                        localeTag = "fr-FR",
                    ).any { issue ->
                        apostropheText.substring(
                            issue.start,
                            issue.endExclusive,
                        ) == "c'" &&
                            "c’" in issue.suggestions
                    },
            )
        } finally {
            engine.close()
        }
    }

    @Test
    fun bundledDictionaryVariantsCanBeSelected() {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        try {
            GrammalecteDictionary.entries
                .forEach { dictionary ->
                    engine.setDictionary(dictionary)

                    assertEquals(
                        dictionary,
                        engine.selectedDictionary(),
                    )

                    assertFalse(
                        "Expected magazin to remain invalid with $dictionary",
                        engine
                            .checkWord(
                                word = "magazin",
                                localeTag = "fr-FR",
                                suggestionLimit = 8,
                            ).valid,
                    )
                }
        } finally {
            engine.close()
        }
    }

    @Test
    fun dictionaryVariantsApplyExpectedOrthographies() {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        fun isValid(word: String): Boolean =
            engine
                .checkWord(
                    word = word,
                    localeTag = "fr-FR",
                    suggestionLimit = 5,
                ).valid

        try {
            engine.setDictionary(
                GrammalecteDictionary.ALL_VARIANTS,
            )

            assertTrue(isValid("coût"))
            assertTrue(isValid("cout"))
            assertTrue(isValid("week-end"))
            assertTrue(isValid("weekend"))

            engine.setDictionary(
                GrammalecteDictionary.CLASSIC,
            )

            assertTrue(isValid("coût"))
            assertFalse(isValid("cout"))
            assertTrue(isValid("week-end"))
            assertFalse(isValid("weekend"))

            engine.setDictionary(
                GrammalecteDictionary.REFORM_1990,
            )

            assertFalse(isValid("coût"))
            assertTrue(isValid("cout"))
            assertFalse(isValid("week-end"))
            assertTrue(isValid("weekend"))

            engine.resetDictionary()

            assertEquals(
                GrammalecteDictionary.ALL_VARIANTS,
                engine.selectedDictionary(),
            )

            assertTrue(isValid("coût"))
            assertTrue(isValid("cout"))
            assertTrue(isValid("week-end"))
            assertTrue(isValid("weekend"))
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
