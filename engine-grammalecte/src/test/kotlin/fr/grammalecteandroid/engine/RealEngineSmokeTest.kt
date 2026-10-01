package fr.grammalecteandroid.engine

import fr.grammalecteandroid.core.IssueKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

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
