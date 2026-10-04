package fr.grammalecteandroid.engine

import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.IssueKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RealEngineRegressionTest {
    @Test
    fun representativeFrenchCorrectionsRemainAvailable() {
        withEngine { engine ->
            assertCorrection(
                engine = engine,
                text = "Je sui aller au magazin hier.",
                fragment = "sui",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "suis",
            )

            assertCorrection(
                engine = engine,
                text = "Je sui aller au magazin hier.",
                fragment = "magazin",
                kind = IssueKind.SPELLING,
                expectedSuggestion = "magasin",
            )

            assertCorrection(
                engine = engine,
                text = "Les enfant joue dans le jardin.",
                fragment = "enfant",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "enfants",
            )

            assertCorrection(
                engine = engine,
                text = "Il faut que tu fasse attention.",
                fragment = "fasse",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "fasses",
            )

            assertCorrection(
                engine = engine,
                text = "J'ai acheter deux pomme.",
                fragment = "acheter",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "acheté",
            )

            assertCorrection(
                engine = engine,
                text = "Ces une bonne idée.",
                fragment = "Ces",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "C’est",
            )

            assertCorrection(
                engine = engine,
                text = "Je voudrai savoir si vous êtes disponible.",
                fragment = "voudrai",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "voudrais",
            )

            assertCorrection(
                engine = engine,
                text = "Bonjour ,comment allez vous ?",
                fragment = " ,",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = ",",
            )

            assertCorrection(
                engine = engine,
                text = "Bonjour ,comment allez vous ?",
                fragment = "allez vous",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "allez-vous",
            )

            assertCorrection(
                engine = engine,
                text = "Il à oublier son rendez-vous.",
                fragment = "à",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "a",
            )
        }
    }

    @Test
    fun typographicApostropheCorrectionsRemainAvailable() {
        withEngine { engine ->
            assertCorrection(
                engine = engine,
                text = "Elle c'est rendu compte de son erreur.",
                fragment = "c'",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "c’",
            )

            assertCorrection(
                engine = engine,
                text = "J'ai acheter deux pomme.",
                fragment = "J'",
                kind = IssueKind.GRAMMAR,
                expectedSuggestion = "J’",
            )
        }
    }

    @Test
    fun correctSentencesDoNotProduceFalsePositives() {
        withEngine { engine ->
            listOf(
                "Je suis allé au magasin hier.",
                "Les enfants jouent dans le jardin.",
            ).forEach { text ->
                val issues =
                    engine.check(
                        text = text,
                        localeTag = "fr-FR",
                    )

                assertTrue(
                    "Expected no issue for «$text», got: $issues",
                    issues.isEmpty(),
                )
            }
        }
    }

    @Test
    fun allReportedRangesRemainValid() {
        withEngine { engine ->
            val corpus =
                listOf(
                    "Je sui aller au magazin hier.",
                    "Les enfant joue dans le jardin.",
                    "Elle c'est rendu compte de son erreur.",
                    "Il faut que tu fasse attention.",
                    "J'ai acheter deux pomme.",
                    "Ces une bonne idée.",
                    "Je voudrai savoir si vous êtes disponible.",
                    "Bonjour ,comment allez vous ?",
                    "Il à oublier son rendez-vous.",
                    "Nous somme arrivé en retard.",
                )

            corpus.forEach { text ->
                engine
                    .check(
                        text = text,
                        localeTag = "fr-FR",
                    ).forEach { issue ->
                        assertTrue(
                            "Negative start for «$text»: $issue",
                            issue.start >= 0,
                        )

                        assertTrue(
                            "Invalid range for «$text»: $issue",
                            issue.endExclusive > issue.start,
                        )

                        assertTrue(
                            "Range outside text for «$text»: $issue",
                            issue.endExclusive <= text.length,
                        )
                    }
            }
        }
    }

    private fun assertCorrection(
        engine: GrammalecteQuickJsEngine,
        text: String,
        fragment: String,
        kind: IssueKind,
        expectedSuggestion: String,
    ) {
        val issue =
            findIssue(
                engine = engine,
                text = text,
                fragment = fragment,
            ) ?: error(
                "Expected issue on «$fragment» in «$text». " +
                    "Got: ${engine.check(text, "fr-FR")}",
            )

        assertEquals(
            "Unexpected issue kind for «$fragment»",
            kind,
            issue.kind,
        )

        assertTrue(
            "Expected «$expectedSuggestion» for «$fragment», " +
                "got ${issue.suggestions}",
            expectedSuggestion in issue.suggestions,
        )
    }

    private fun findIssue(
        engine: GrammalecteQuickJsEngine,
        text: String,
        fragment: String,
    ): GrammarIssue? =
        engine
            .check(
                text = text,
                localeTag = "fr-FR",
            ).firstOrNull { issue ->
                text.substring(
                    issue.start,
                    issue.endExclusive,
                ) == fragment
            }

    private fun withEngine(
        block: (GrammalecteQuickJsEngine) -> Unit,
    ) {
        val engine =
            GrammalecteQuickJsEngine(
                FileAssetTextLoader(findAssetRoot()),
            )

        try {
            block(engine)
        } finally {
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
}
