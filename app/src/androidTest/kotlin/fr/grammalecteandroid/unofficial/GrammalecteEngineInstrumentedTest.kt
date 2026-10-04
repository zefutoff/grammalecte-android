package fr.grammalecteandroid.unofficial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrammalecteEngineInstrumentedTest {
    @Test
    fun grammalecteDictionaryIsPackagedInApplication() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        context.assets
            .open(
                "grammalecte/graphspell/" +
                    "_dictionaries/fr-allvars.json",
            ).use { stream ->
                assertTrue(
                    "Expected packaged Grammalecte dictionary",
                    stream.read() >= 0,
                )
            }
    }

    @Test
    fun realPackagedEngineChecksSpellingAndGrammar() {
        val text = "🙂 Je suis aller au magasin."

        withEngine { engine ->
            val wordResult =
                engine.checkWord(
                    word = "magazin",
                    localeTag = "fr-FR",
                    suggestionLimit = 8,
                )

            assertFalse(wordResult.valid)

            assertTrue(
                "Expected «magasin» in ${wordResult.suggestions}",
                "magasin" in wordResult.suggestions,
            )

            val issues =
                engine.check(
                    text = text,
                    localeTag = "fr-FR",
                )

            val issue =
                issues.firstOrNull { candidate ->
                    candidate.start >= 0 &&
                        candidate.endExclusive <= text.length &&
                        text.substring(
                            candidate.start,
                            candidate.endExclusive,
                        ) == "aller"
                } ?: error(
                    "Expected issue on «aller», got $issues",
                )

            assertEquals(
                IssueKind.GRAMMAR,
                issue.kind,
            )

            assertEquals(
                text.indexOf("aller"),
                issue.start,
            )

            assertEquals(
                text.indexOf("aller") + "aller".length,
                issue.endExclusive,
            )

            assertTrue(
                "Expected «allé» in ${issue.suggestions}",
                "allé" in issue.suggestions,
            )
        }
    }

    private fun withEngine(block: (GrammalecteQuickJsEngine) -> Unit) {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val engine =
            GrammalecteQuickJsEngine(context)

        try {
            block(engine)
        } finally {
            engine.close()
        }
    }
}
