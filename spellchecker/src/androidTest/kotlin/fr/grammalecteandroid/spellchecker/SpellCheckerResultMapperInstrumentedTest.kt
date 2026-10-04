package fr.grammalecteandroid.spellchecker

import android.os.Build
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.core.WordCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpellCheckerResultMapperInstrumentedTest {
    @Test
    fun spellingSuggestionSurvivesAndroidMapping() {
        val text = "Je sui aller au magazin hier."
        val start = text.indexOf("magazin")

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = start,
                            endExclusive = start + "magazin".length,
                            suggestions = listOf("magasin"),
                            kind = IssueKind.SPELLING,
                        ),
                    ),
            )

        val info =
            findIssue(
                sentence = sentence,
                text = text,
                fragment = "magazin",
            )

        assertSuggestion(info, "magasin")

        assertFlag(
            info,
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO,
        )

        assertFlag(
            info,
            SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS,
        )
    }

    @Test
    fun grammarSuggestionSurvivesAndroidMapping() {
        val text = "Je suis aller au magasin."
        val start = text.indexOf("aller")

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = start,
                            endExclusive = start + "aller".length,
                            suggestions = listOf("allé", "allée"),
                            kind = IssueKind.GRAMMAR,
                        ),
                    ),
            )

        val info =
            findIssue(
                sentence = sentence,
                text = text,
                fragment = "aller",
            )

        assertSuggestion(info, "allé")

        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            } else {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }

        assertFlag(info, flag)
    }

    @Test
    fun sentenceOffsetsRemainCorrectWithEmojiBeforeIssue() {
        val text = "🙂 Je suis aller au magasin."
        val start = text.indexOf("aller")

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = start,
                            endExclusive = start + "aller".length,
                            suggestions = listOf("allé"),
                            kind = IssueKind.GRAMMAR,
                        ),
                    ),
            )

        assertEquals(1, sentence.suggestionsCount)
        assertEquals(start, sentence.getOffsetAt(0))
        assertEquals("aller".length, sentence.getLengthAt(0))

        assertSuggestion(
            sentence.getSuggestionsInfoAt(0),
            "allé",
        )
    }

    @Test
    fun apostropheRangeRemainsCorrect() {
        val text = "J'ai acheter deux pomme."
        val start = text.indexOf("J'")

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = start,
                            endExclusive = start + 2,
                            suggestions = listOf("J’"),
                            kind = IssueKind.GRAMMAR,
                        ),
                    ),
            )

        val info =
            findIssue(
                sentence = sentence,
                text = text,
                fragment = "J'",
            )

        assertSuggestion(info, "J’")
    }

    @Test
    fun multipleIssuesKeepTheirOwnRanges() {
        val text = "Je sui aller au magazin hier."
        val sui = text.indexOf("sui")
        val magazin = text.indexOf("magazin")

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = sui,
                            endExclusive = sui + 3,
                            suggestions = listOf("suis"),
                            kind = IssueKind.GRAMMAR,
                        ),
                        GrammarIssue(
                            start = magazin,
                            endExclusive = magazin + 7,
                            suggestions = listOf("magasin"),
                            kind = IssueKind.SPELLING,
                        ),
                    ),
            )

        assertEquals(2, sentence.suggestionsCount)

        findIssue(sentence, text, "sui")
        findIssue(sentence, text, "magazin")
    }

    @Test
    fun identicalRangesAreMergedWithGrammarPriority() {
        val text = "erreur"

        val sentence =
            mapSentence(
                text = text,
                issues =
                    listOf(
                        GrammarIssue(
                            start = 0,
                            endExclusive = text.length,
                            suggestions = listOf("orthographe"),
                            kind = IssueKind.SPELLING,
                        ),
                        GrammarIssue(
                            start = 0,
                            endExclusive = text.length,
                            suggestions = listOf("grammaire"),
                            kind = IssueKind.GRAMMAR,
                        ),
                    ),
            )

        assertEquals(1, sentence.suggestionsCount)

        val info = sentence.getSuggestionsInfoAt(0)

        assertSuggestion(info, "grammaire")
        assertSuggestion(info, "orthographe")

        val grammarFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            } else {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }

        assertFlag(info, grammarFlag)
    }

    @Test
    fun wordSuggestionLimitIsRespected() {
        val mapped =
            SpellCheckerResultMapper.word(
                textInfo = TextInfo("magazin"),
                check =
                    WordCheck(
                        valid = false,
                        suggestions =
                            listOf(
                                "magasin",
                                "magasine",
                                "magasins",
                            ),
                    ),
                suggestionsLimit = 2,
            )

        assertEquals(2, mapped.suggestionsCount)
        assertEquals("magasin", mapped.getSuggestionAt(0))
        assertEquals("magasine", mapped.getSuggestionAt(1))
    }

    @Test
    fun validWordIsReportedAsDictionaryWord() {
        val mapped =
            SpellCheckerResultMapper.word(
                textInfo = TextInfo("magasin"),
                check = WordCheck(valid = true),
                suggestionsLimit = 8,
            )

        assertFlag(
            mapped,
            SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY,
        )

        assertEquals(0, mapped.suggestionsCount)
    }

    private fun mapSentence(
        text: String,
        issues: List<GrammarIssue>,
    ): SentenceSuggestionsInfo =
        SpellCheckerResultMapper.sentence(
            textInfo = TextInfo(text),
            issues = issues,
            suggestionsLimit = 8,
        )

    private fun findIssue(
        sentence: SentenceSuggestionsInfo,
        text: String,
        fragment: String,
    ): SuggestionsInfo {
        val expectedOffset = text.indexOf(fragment)

        require(expectedOffset >= 0)

        for (index in 0 until sentence.suggestionsCount) {
            if (
                sentence.getOffsetAt(index) == expectedOffset &&
                sentence.getLengthAt(index) == fragment.length
            ) {
                return sentence.getSuggestionsInfoAt(index)
            }
        }

        error(
            "No Android suggestion for «$fragment» " +
                "at $expectedOffset in «$text»",
        )
    }

    private fun assertSuggestion(
        info: SuggestionsInfo,
        expected: String,
    ) {
        val suggestions =
            buildList {
                for (index in 0 until info.suggestionsCount) {
                    add(info.getSuggestionAt(index))
                }
            }

        assertTrue(
            "Expected «$expected» in $suggestions",
            expected in suggestions,
        )
    }

    private fun assertFlag(
        info: SuggestionsInfo,
        flag: Int,
    ) {
        assertTrue(
            "Expected flag $flag in ${info.suggestionsAttributes}",
            info.suggestionsAttributes and flag != 0,
        )
    }
}
