package fr.zefut.grammalecte

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SetupGuideActivity : ComponentActivity() {
    private var stepIndex by mutableIntStateOf(0)
    private var correctionStatus by mutableStateOf<CorrectionStatus?>(null)
    private var awaitingSpellCheckerSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stepIndex = (savedInstanceState?.getInt(STATE_STEP_INDEX) ?: 0).coerceIn(0, STEP_COUNT - 1)
        awaitingSpellCheckerSettings =
            savedInstanceState?.getBoolean(STATE_WAITING_FOR_SETTINGS) ?: false

        setContent {
            SetupGuideScreen(
                step = stepIndex,
                status = correctionStatus,
                onPrevious = { if (stepIndex > 0) stepIndex-- },
                onNext = {
                    if (stepIndex == STEP_COUNT - 1) {
                        finish()
                    } else {
                        stepIndex++
                    }
                },
                onLeave = { finish() },
                onSpellCheckerSettings = { openSpellCheckerSettings() },
                onKeyboardSettings = { openInputMethodSettings() },
                onChooseKeyboard = { showInputMethodPicker() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (awaitingSpellCheckerSettings) {
            awaitingSpellCheckerSettings = false
            if (stepIndex == 0 && correctionStatus?.spellChecker == SpellCheckerStatus.SELECTED) {
                stepIndex = 1
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_STEP_INDEX, stepIndex)
        outState.putBoolean(STATE_WAITING_FOR_SETTINGS, awaitingSpellCheckerSettings)
        super.onSaveInstanceState(outState)
    }

    private fun refreshStatus() {
        correctionStatus = CorrectionStatusReader(this).read()
    }

    private fun openSpellCheckerSettings() {
        awaitingSpellCheckerSettings = true
        val directIntent =
            Intent(Intent.ACTION_MAIN).apply {
                component =
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$SpellCheckersSettingsActivity",
                    )
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        val intent =
            if (directIntent.resolveActivity(packageManager) != null) {
                directIntent
            } else {
                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            }
        runCatching {
            startActivity(intent)
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openInputMethodSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showInputMethodPicker() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private companion object {
        const val STEP_COUNT = 3
        const val STATE_STEP_INDEX = "onboarding_step"
        const val STATE_WAITING_FOR_SETTINGS = "onboarding_waiting_settings"
    }
}
