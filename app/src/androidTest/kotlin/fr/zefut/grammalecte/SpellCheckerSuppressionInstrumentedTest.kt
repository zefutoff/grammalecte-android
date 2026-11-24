package fr.zefut.grammalecte

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpellCheckerSuppressionInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun diagnosticsIdentifyActiveSuppressingKeyboard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val component =
            ComponentName(
                instrumentation.context,
                SpellCheckerSuppressingTestImeService::class.java,
            )
        val componentId = component.flattenToString()
        val originalIme = shell("settings get secure default_input_method")
        val originalShowIme = shell("settings get secure show_ime_with_hard_keyboard")
        val wasEnabled =
            shell("ime list -s")
                .lineSequence()
                .mapNotNull(ComponentName::unflattenFromString)
                .any { it == component }
        val preferences = context.getSharedPreferences("setup_guide", Context.MODE_PRIVATE)
        val guideWasShown = preferences.getBoolean("setup_guide_shown_v1", false)
        val hadGuidePreference = preferences.contains("setup_guide_shown_v1")

        try {
            preferences.edit().putBoolean("setup_guide_shown_v1", true).commit()
            shell("settings put secure show_ime_with_hard_keyboard 1")
            shell("ime enable $componentId")
            shell("ime set $componentId")
            assertTrue(
                "Test IME was not selected",
                ComponentName.unflattenFromString(shell("settings get secure default_input_method")) == component,
            )

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                composeRule.onNodeWithText("Outils et diagnostics").performScrollTo().performClick()
                composeRule.onNodeWithText("Tester dans un champ Android").performScrollTo().performClick()

                composeRule.waitUntil(timeoutMillis = 15_000L) {
                    var suppressed = false
                    scenario.onActivity { activity ->
                        val manager = activity.getSystemService(InputMethodManager::class.java)
                        suppressed =
                            manager.isAcceptingText &&
                            manager.isInputMethodSuppressingSpellChecker
                    }
                    suppressed
                }

                val message = context.getString(R.string.spell_checker_suppressed_by_keyboard)
                composeRule.onNodeWithText("Inspecter les corrections Android").performScrollTo().performClick()
                onView(withText(message)).check(matches(withText(message)))

                composeRule.onNodeWithText("Tester Grammalecte").performScrollTo().performClick()
                composeRule.onNodeWithTag("spell-check-test-error").assertTextEquals(message)

                // The framework can also report an empty array after a session
                // was already created. Keep the same actionable diagnosis.
                scenario.onActivity { activity ->
                    activity.onGetSentenceSuggestions(emptyArray())
                }
                composeRule.onNodeWithTag("spell-check-test-error").assertTextEquals(message)
            }
        } finally {
            try {
                if (originalIme.isNotBlank() && originalIme != "null") {
                    shell("ime set $originalIme")
                }
            } finally {
                if (!wasEnabled) {
                    shell("ime disable $componentId")
                }
                if (originalShowIme == "null" || originalShowIme.isBlank()) {
                    shell("settings delete secure show_ime_with_hard_keyboard")
                } else {
                    shell("settings put secure show_ime_with_hard_keyboard $originalShowIme")
                }
                preferences
                    .edit()
                    .apply {
                        if (hadGuidePreference) {
                            putBoolean("setup_guide_shown_v1", guideWasShown)
                        } else {
                            remove("setup_guide_shown_v1")
                        }
                    }.commit()
            }
        }
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor
            .AutoCloseInputStream(
                InstrumentationRegistry
                    .getInstrumentation()
                    .uiAutomation
                    .executeShellCommand(command),
            ).bufferedReader()
            .use { it.readText().trim() }
}
