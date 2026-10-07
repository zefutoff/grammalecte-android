package fr.grammalecteandroid.unofficial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.engine.GrammalecteDictionary
import fr.grammalecteandroid.engine.GrammalecteDictionaryPreferences
import fr.grammalecteandroid.engine.GrammalectePersonalDictionaryPreferences
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import fr.grammalecteandroid.engine.GrammalecteRulePreferences
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

    @Test
    fun existingEngineRefreshesStoredRuleOptions() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val preferences =
            GrammalecteRulePreferences(context)

        preferences.reset()

        val engine =
            GrammalecteQuickJsEngine(context)

        val text =
            "Elle c'est rendu compte de son erreur."

        fun hasApostropheIssue(): Boolean =
            engine
                .check(
                    text = text,
                    localeTag = "fr-FR",
                ).any { issue ->
                    text.substring(
                        issue.start,
                        issue.endExclusive,
                    ) == "c'" &&
                        "c’" in issue.suggestions
                }

        try {
            assertTrue(
                "Expected apostrophe correction by default",
                hasApostropheIssue(),
            )

            preferences.setRuleOption(
                id = "apos",
                enabled = false,
            )

            assertFalse(
                "Expected existing engine to pick up disabled apos option",
                hasApostropheIssue(),
            )

            preferences.reset()

            assertTrue(
                "Expected reset to restore apostrophe correction",
                hasApostropheIssue(),
            )
        } finally {
            preferences.reset()
            engine.close()
        }
    }

    @Test
    fun existingEngineRefreshesStoredDictionarySelection() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val preferences =
            GrammalecteDictionaryPreferences(context)

        preferences.reset()

        val engine =
            GrammalecteQuickJsEngine(context)

        fun isValid(word: String): Boolean =
            engine
                .checkWord(
                    word = word,
                    localeTag = "fr-FR",
                    suggestionLimit = 5,
                ).valid

        try {
            assertTrue(
                "Expected classic spelling to be valid by default",
                isValid("coût"),
            )

            assertTrue(
                "Expected reformed spelling to be valid by default",
                isValid("cout"),
            )

            preferences.select(
                GrammalecteDictionary.CLASSIC,
            )

            assertTrue(
                "Expected classic spelling after selecting classic dictionary",
                isValid("coût"),
            )

            assertFalse(
                "Expected reformed spelling to be rejected by classic dictionary",
                isValid("cout"),
            )

            preferences.select(
                GrammalecteDictionary.REFORM_1990,
            )

            assertFalse(
                "Expected classic spelling to be rejected by reformed dictionary",
                isValid("coût"),
            )

            assertTrue(
                "Expected reformed spelling after selecting reform dictionary",
                isValid("cout"),
            )

            preferences.reset()

            assertTrue(
                "Expected classic spelling after restoring all variants",
                isValid("coût"),
            )

            assertTrue(
                "Expected reformed spelling after restoring all variants",
                isValid("cout"),
            )
        } finally {
            preferences.reset()
            engine.close()
        }
    }

    @Test
    fun existingEngineRefreshesStoredPersonalDictionary() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val preferences =
            GrammalectePersonalDictionaryPreferences(context)

        val personalWord =
            "grammalecteandroidique"

        preferences.reset()

        val engine =
            GrammalecteQuickJsEngine(context)

        fun isValid(): Boolean =
            engine
                .checkWord(
                    word = personalWord,
                    localeTag = "fr-FR",
                    suggestionLimit = 5,
                ).valid

        try {
            assertFalse(
                "Expected personal word to be unknown initially",
                isValid(),
            )

            preferences.replace(
                listOf(personalWord),
            )

            assertTrue(
                "Expected existing engine to load stored personal word",
                isValid(),
            )

            preferences.reset()

            assertFalse(
                "Expected existing engine to remove cleared personal word",
                isValid(),
            )
        } finally {
            preferences.reset()
            engine.close()
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
