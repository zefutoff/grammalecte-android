package fr.grammalecteandroid.unofficial

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

data class CorrectionStatus(
    val spellChecker: SpellCheckerStatus,
    val ime: ImeStatus,
)

enum class SpellCheckerStatus {
    SELECTED,
    NOT_SELECTED,
    DISABLED,
    UNAVAILABLE,
}

enum class ImeStatus {
    SELECTED,
    ENABLED,
    DISABLED,
}

class CorrectionStatusReader(
    private val context: Context,
) {
    fun read(): CorrectionStatus =
        CorrectionStatus(
            spellChecker = readSpellCheckerStatus(),
            ime = readImeStatus(),
        )

    private fun readSpellCheckerStatus(): SpellCheckerStatus {
        val manager =
            context.getSystemService(
                TextServicesManager::class.java,
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!manager.isSpellCheckerEnabled) {
                return SpellCheckerStatus.DISABLED
            }

            return if (
                manager.currentSpellCheckerInfo
                    ?.packageName == context.packageName
            ) {
                SpellCheckerStatus.SELECTED
            } else {
                SpellCheckerStatus.NOT_SELECTED
            }
        }

        val session =
            manager.newSpellCheckerSession(
                null,
                Locale.FRANCE,
                EMPTY_SPELL_CHECKER_LISTENER,
                false,
            )

        return try {
            when {
                session == null ->
                    SpellCheckerStatus.UNAVAILABLE

                session.spellChecker.packageName ==
                    context.packageName ->
                    SpellCheckerStatus.SELECTED

                else ->
                    SpellCheckerStatus.NOT_SELECTED
            }
        } finally {
            session?.close()
        }
    }

    private fun readImeStatus(): ImeStatus {
        val manager =
            context.getSystemService(
                InputMethodManager::class.java,
            )

        val component =
            ComponentName(
                context,
                GrammalecteImeService::class.java,
            )

        val enabled =
            manager.enabledInputMethodList
                .any { inputMethod ->
                    ComponentName(
                        inputMethod.packageName,
                        inputMethod.serviceName,
                    ) == component
                }

        val selectedComponent =
            Settings.Secure
                .getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                )?.let(
                    ComponentName::unflattenFromString,
                )

        return when {
            selectedComponent == component ->
                ImeStatus.SELECTED

            enabled ->
                ImeStatus.ENABLED

            else ->
                ImeStatus.DISABLED
        }
    }

    private companion object {
        val EMPTY_SPELL_CHECKER_LISTENER =
            object :
                SpellCheckerSession.SpellCheckerSessionListener {
                override fun onGetSuggestions(results: Array<SuggestionsInfo>?) = Unit

                override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) = Unit
            }
    }
}
