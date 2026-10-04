package fr.grammalecteandroid.spellchecker

import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine

class GrammalecteSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session = GrammalecteSession()

    private inner class GrammalecteSession : Session() {
        private val engineDelegate =
            lazy(LazyThreadSafetyMode.NONE) {
                GrammalecteQuickJsEngine(applicationContext)
            }
        private val engine by engineDelegate
        private var sessionLocale = DEFAULT_LOCALE

        override fun onCreate() {
            sessionLocale = locale.orEmpty().replace('_', '-').ifBlank { DEFAULT_LOCALE }
        }

        override fun onGetSuggestions(
            textInfo: TextInfo,
            suggestionsLimit: Int,
        ): SuggestionsInfo {
            val safeLimit = suggestionsLimit.coerceAtLeast(0)

            val check =
                runCatching {
                    engine.checkWord(
                        word = textInfo.text,
                        localeTag = sessionLocale,
                        suggestionLimit = safeLimit,
                    )
                }.getOrElse { error ->
                    logEngineFailure(error)
                    return SuggestionsInfo(0, emptyArray(), textInfo.cookie, textInfo.sequence)
                }

            return SpellCheckerResultMapper.word(
                textInfo = textInfo,
                check = check,
                suggestionsLimit = safeLimit,
            )
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            val safeLimit = suggestionsLimit.coerceAtLeast(0)

            return Array(textInfos.size) { index ->
                val text = textInfos[index].text

                val issues =
                    runCatching {
                        engine.check(text = text, localeTag = sessionLocale)
                    }.getOrElse { error ->
                        logEngineFailure(error)
                        emptyList()
                    }

                SpellCheckerResultMapper.sentence(
                    textInfo = textInfos[index],
                    issues = issues,
                    suggestionsLimit = safeLimit,
                )
            }
        }

        override fun onCancel() = Unit

        override fun onClose() {
            if (engineDelegate.isInitialized()) {
                engine.close()
            }
        }

        override fun getSupportedAttributes(): Int =
            SpellCheckerResultMapper.supportedAttributes()
    }

    private fun logEngineFailure(error: Throwable) {
        Log.e(TAG, "Grammalecte analysis failed", error)
    }

    private companion object {
        const val TAG = "GrammalecteService"
        const val DEFAULT_LOCALE = "fr-FR"
    }
}
