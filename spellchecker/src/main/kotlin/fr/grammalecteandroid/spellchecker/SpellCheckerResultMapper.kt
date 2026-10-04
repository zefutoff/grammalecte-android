package fr.grammalecteandroid.spellchecker

import android.os.Build
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.core.IssueRangeMerger
import fr.grammalecteandroid.core.WordCheck

internal object SpellCheckerResultMapper {
    fun word(
        textInfo: TextInfo,
        check: WordCheck,
        suggestionsLimit: Int,
    ): SuggestionsInfo {
        val safeLimit = suggestionsLimit.coerceAtLeast(0)
        val suggestions = check.suggestions.take(safeLimit)

        if (check.valid) {
            return SuggestionsInfo(
                SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY,
                emptyArray(),
                textInfo.cookie,
                textInfo.sequence,
            )
        }

        return SuggestionsInfo(
            typoFlags(suggestions.isNotEmpty()),
            suggestions.toTypedArray(),
            textInfo.cookie,
            textInfo.sequence,
        )
    }

    fun sentence(
        textInfo: TextInfo,
        issues: Iterable<GrammarIssue>,
        suggestionsLimit: Int,
    ): SentenceSuggestionsInfo {
        val safeLimit = suggestionsLimit.coerceAtLeast(0)

        val retained =
            IssueRangeMerger.merge(
                issues = issues.filter { issue -> issue.length > 0 },
                suggestionLimit = safeLimit,
            )

        val infos =
            Array(retained.size) { index ->
                val issue = retained[index]
                val suggestions =
                    issue.suggestions.take(safeLimit)

                SuggestionsInfo(
                    flagsFor(
                        issue = issue,
                        hasSuggestions = suggestions.isNotEmpty(),
                    ),
                    suggestions.toTypedArray(),
                    textInfo.cookie,
                    textInfo.sequence,
                )
            }

        val offsets =
            IntArray(retained.size) { index ->
                retained[index].start
            }

        val lengths =
            IntArray(retained.size) { index ->
                retained[index].length
            }

        return SentenceSuggestionsInfo(
            infos,
            offsets,
            lengths,
        )
    }

    fun supportedAttributes(): Int =
        SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY or
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
            SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            } else {
                0
            }

    private fun flagsFor(
        issue: GrammarIssue,
        hasSuggestions: Boolean,
    ): Int {
        val issueFlag =
            when (issue.kind) {
                IssueKind.GRAMMAR,
                IssueKind.TYPOGRAPHY,
                -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
                    } else {
                        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                    }
                }

                IssueKind.SPELLING,
                IssueKind.UNKNOWN,
                -> SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }

        return issueFlag or
            if (hasSuggestions) {
                SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
            } else {
                0
            }
    }

    private fun typoFlags(
        hasSuggestions: Boolean,
    ): Int =
        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
            if (hasSuggestions) {
                SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
            } else {
                0
            }
}
