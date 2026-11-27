package fr.zefut.grammalecte

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

data class CorrectionStatus(
    val spellChecker: SpellCheckerStatus,
    val ime: ImeStatus,
    val keyboardSpellChecker: KeyboardSpellCheckerStatus,
)

enum class SpellCheckerStatus {
    SELECTED,
    FRENCH_SESSION_UNAVAILABLE,
    NOT_SELECTED,
    DISABLED,
    UNAVAILABLE,
}

// This describes the selected IME's declaration, not whether an editor currently has
// an active input session or whether spellchecking is otherwise available.
enum class KeyboardSpellCheckerStatus {
    SUPPRESSES,
    NOT_DECLARED,
    UNKNOWN,
}

enum class ImeStatus {
    SELECTED,
    ENABLED,
    DISABLED,
}

class CorrectionStatusReader(
    private val context: Context,
) {
    fun read(): CorrectionStatus {
        val manager = context.getSystemService(InputMethodManager::class.java)
        val enabledInputMethods = manager.enabledInputMethodList
        val selectedComponent =
            Settings.Secure
                .getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                )?.let(ComponentName::unflattenFromString)
        val selectedInputMethod =
            enabledInputMethods.firstOrNull { it.component == selectedComponent }

        return CorrectionStatus(
            spellChecker = readSpellCheckerStatus(),
            ime = readImeStatus(selectedComponent, enabledInputMethods),
            keyboardSpellChecker = readKeyboardSpellCheckerStatus(selectedInputMethod),
        )
    }

    private fun readKeyboardSpellCheckerStatus(inputMethod: InputMethodInfo?): KeyboardSpellCheckerStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || inputMethod == null) {
            return KeyboardSpellCheckerStatus.UNKNOWN
        }

        // Unlike the live InputMethodManager suppression flag, this public API
        // does not require this activity to have an active editor connection.
        return if (inputMethod.suppressesSpellChecker()) {
            KeyboardSpellCheckerStatus.SUPPRESSES
        } else {
            KeyboardSpellCheckerStatus.NOT_DECLARED
        }
    }

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
                configuredFrenchSessionStatus(manager)
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
                    configuredFrenchSessionStatus(manager)

                else ->
                    SpellCheckerStatus.NOT_SELECTED
            }
        } finally {
            session?.close()
        }
    }

    private fun configuredFrenchSessionStatus(manager: TextServicesManager): SpellCheckerStatus {
        val session =
            runCatching {
                manager.newSpellCheckerSession(
                    null,
                    Locale.FRANCE,
                    EMPTY_SPELL_CHECKER_LISTENER,
                    true,
                )
            }.getOrNull()

        return try {
            if (session?.spellChecker?.packageName == context.packageName) {
                SpellCheckerStatus.SELECTED
            } else {
                SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE
            }
        } finally {
            session?.close()
        }
    }

    private fun readImeStatus(
        selectedComponent: ComponentName?,
        enabledInputMethods: List<InputMethodInfo>,
    ): ImeStatus {
        val component =
            ComponentName(
                context,
                GrammalecteImeService::class.java,
            )

        return when {
            selectedComponent == component ->
                ImeStatus.SELECTED

            enabledInputMethods.any { it.component == component } ->
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
