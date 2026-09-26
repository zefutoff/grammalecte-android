package fr.grammalecteandroid.spellchecker

import android.os.Build
import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.core.IssueRangeMerger
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine

class GrammalecteSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session {
        Log.i(TAG, "createSession()")
        return GrammalecteSession()
    }

    private inner class GrammalecteSession : Session() {
        private val engineDelegate = lazy(LazyThreadSafetyMode.NONE) {
            GrammalecteQuickJsEngine(applicationContext)
        }
        private val engine by engineDelegate
        private var sessionLocale = DEFAULT_LOCALE

        override fun onCreate() {
            sessionLocale = locale.orEmpty().replace('_', '-').ifBlank { DEFAULT_LOCALE }
            Log.i(TAG, "session onCreate locale=$sessionLocale")
        }

        override fun onGetSuggestions(
            textInfo: TextInfo,
            suggestionsLimit: Int,
        ): SuggestionsInfo {
            val safeLimit = suggestionsLimit.coerceAtLeast(0)
            Log.i(
                TAG,
                "onGetSuggestions length=${textInfo.text.length} limit=$safeLimit locale=$sessionLocale",
            )

            val check = runCatching {
                engine.checkWord(
                    word = textInfo.text,
                    localeTag = sessionLocale,
                    suggestionLimit = safeLimit,
                )
            }.getOrElse { error ->
                logEngineFailure(error)
                return SuggestionsInfo(0, emptyArray(), textInfo.cookie, textInfo.sequence)
            }

            Log.i(
                TAG,
                "word result valid=${check.valid} suggestions=${check.suggestions.size}",
            )

            if (check.valid) {
                return SuggestionsInfo(
                    SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY,
                    emptyArray(),
                    textInfo.cookie,
                    textInfo.sequence,
                )
            }

            return SuggestionsInfo(
                typoFlags(check.suggestions.isNotEmpty()),
                check.suggestions.toTypedArray(),
                textInfo.cookie,
                textInfo.sequence,
            )
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            val safeLimit = suggestionsLimit.coerceAtLeast(0)

            Log.i(
                TAG,
                "onGetSentenceSuggestionsMultiple count=${textInfos.size} limit=$safeLimit locale=$sessionLocale",
            )

            return Array(textInfos.size) { index ->
                val text = textInfos[index].text

                Log.i(TAG, "sentence[$index] length=${text.length}")

                val issues = runCatching {
                    engine.check(text = text, localeTag = sessionLocale)
                }.getOrElse { error ->
                    logEngineFailure(error)
                    emptyList()
                }

                Log.i(TAG, "sentence[$index] issues=${issues.size}")

                issues.toSentenceSuggestions(
                    textInfo = textInfos[index],
                    suggestionsLimit = safeLimit,
                )
            }
        }

        override fun onCancel() = Unit

        override fun onClose() {
            Log.i(TAG, "session onClose()")
            if (engineDelegate.isInitialized()) {
                engine.close()
            }
        }

        override fun getSupportedAttributes(): Int =
            SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY or
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
                SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
                } else {
                    0
                }
    }

    private fun List<GrammarIssue>.toSentenceSuggestions(
        textInfo: TextInfo,
        suggestionsLimit: Int,
    ): SentenceSuggestionsInfo {
        forEach { issue ->
            Log.i(
                TAG,
                "raw issue start=${issue.start} end=${issue.endExclusive} " +
                    "length=${issue.length} kind=${issue.kind}",
            )
        }

        val retained = IssueRangeMerger.merge(
            issues = filter { issue -> issue.length > 0 },
            suggestionLimit = suggestionsLimit,
        )
        val infos = Array(retained.size) { index ->
            val issue = retained[index]
            val suggestions = issue.suggestions.take(suggestionsLimit)

            Log.i(
                TAG,
                "return issue start=${issue.start} length=${issue.length} " +
                    "kind=${issue.kind} suggestions=${suggestions.joinToString("|")}",
            )

            SuggestionsInfo(
                flagsFor(issue, suggestions.isNotEmpty()),
                suggestions.toTypedArray(),
                textInfo.cookie,
                textInfo.sequence,
            )
        }
        val offsets = IntArray(retained.size) { index -> retained[index].start }
        val lengths = IntArray(retained.size) { index -> retained[index].length }

        return SentenceSuggestionsInfo(infos, offsets, lengths)
    }

    private fun flagsFor(
        issue: GrammarIssue,
        hasSuggestions: Boolean,
    ): Int {
        val issueFlag = when (issue.kind) {
            IssueKind.GRAMMAR, IssueKind.TYPOGRAPHY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
                } else {
                    SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                }
            }

            IssueKind.SPELLING, IssueKind.UNKNOWN -> SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
        }

        return issueFlag or
            if (hasSuggestions) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0
    }

    private fun typoFlags(hasSuggestions: Boolean): Int =
        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
            if (hasSuggestions) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0

    private fun logEngineFailure(error: Throwable) {
        Log.e(TAG, "Grammalecte analysis failed", error)
    }

    private companion object {
        const val TAG = "GrammalecteService"
        const val DEFAULT_LOCALE = "fr-FR"
    }
}
