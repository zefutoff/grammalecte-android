package fr.zefut.grammalecte

import android.content.ComponentName
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardSpellCheckerStatusInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test
    @SdkSuppress(maxSdkVersion = 30)
    fun olderAndroidReportsUnknownSuppression() {
        ActivityScenario.launch(CorrectionStatusActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    KeyboardSpellCheckerStatus.UNKNOWN,
                    CorrectionStatusReader(activity).read().keyboardSpellChecker,
                )
            }
            assertStatusMessageEventually(UNKNOWN_MESSAGE)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun statusScreenRefreshesWithoutAnActiveEditor() {
        withTestKeyboards { normal, suppressing ->
            selectKeyboard(normal)
            ActivityScenario.launch(CorrectionStatusActivity::class.java).use { scenario ->
                assertStatusMessageEventually(NOT_DECLARED_MESSAGE)

                selectKeyboard(suppressing)
                assertStatusMessageEventually(SUPPRESSES_MESSAGE)
                scenario.onActivity { activity ->
                    assertEquals(
                        KeyboardSpellCheckerStatus.SUPPRESSES,
                        CorrectionStatusReader(activity).read().keyboardSpellChecker,
                    )
                }

                selectKeyboard(normal)
                assertStatusMessageEventually(NOT_DECLARED_MESSAGE)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun activeEditorReportsActualFrameworkSuppression() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        withTestKeyboards { normal, suppressing ->
            selectKeyboard(suppressing)
            ActivityScenario.launch(ImeIntegrationTestActivity::class.java).use { scenario ->
                focusEditor(scenario)
                assertActiveSuppressionEventually(scenario, expected = true)

                selectKeyboard(normal)
                focusEditor(scenario)
                assertActiveSuppressionEventually(scenario, expected = false)
            }
        }
    }

    private fun assertStatusMessageEventually(message: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_ATTEMPTS * WAIT_DELAY_MILLIS) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("status-keyboard-spellchecker").assertTextEquals(message)
    }

    private fun focusEditor(scenario: ActivityScenario<ImeIntegrationTestActivity>) {
        scenario.onActivity { activity ->
            val editor = activity.editor
            val manager = activity.getSystemService(InputMethodManager::class.java)
            editor.requestFocus()
            manager.restartInput(editor)
            editor.post { manager.showSoftInput(editor, 0) }
        }
    }

    private fun assertActiveSuppressionEventually(
        scenario: ActivityScenario<ImeIntegrationTestActivity>,
        expected: Boolean,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        var actual: Boolean? = null
        repeat(WAIT_ATTEMPTS) {
            scenario.onActivity { activity ->
                val manager = activity.getSystemService(InputMethodManager::class.java)
                actual =
                    if (manager.isActive(activity.editor) && manager.isAcceptingText) {
                        manager.isInputMethodSuppressingSpellChecker
                    } else {
                        null
                    }
            }
            if (actual == expected) {
                return
            }
            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }
        assertEquals("Unexpected suppression in the active editor", expected, actual)
    }

    private fun withTestKeyboards(block: (ComponentName, ComponentName) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val normal = ComponentName(context, GrammalecteImeService::class.java)
        val suppressing = ComponentName(context, SuppressingTestImeService::class.java)
        val originalIme =
            requireNotNull(ComponentName.unflattenFromString(shell("settings get secure default_input_method"))) {
                "A default keyboard is required for safe restoration"
            }
        val originalSubtype = shell("settings get secure selected_input_method_subtype")
        val enabledBefore = listedKeyboards("ime list -s")
        val registered = listedKeyboards("ime list -a -s")
        assertTrue("Missing test keyboards", normal in registered && suppressing in registered)

        try {
            for (component in listOf(normal, suppressing)) {
                shell("ime enable ${component.flattenToShortString()}")
                assertTrue("Keyboard was not enabled: $component", component in listedKeyboards("ime list -s"))
            }
            block(normal, suppressing)
        } finally {
            selectKeyboard(originalIme)
            if (originalSubtype.isBlank() || originalSubtype == "null") {
                shell("settings delete secure selected_input_method_subtype")
            } else {
                shell("settings put secure selected_input_method_subtype $originalSubtype")
            }
            for (component in listOf(normal, suppressing)) {
                if (component !in enabledBefore) {
                    shell("ime disable ${component.flattenToShortString()}")
                }
            }
        }
    }

    private fun listedKeyboards(command: String): Set<ComponentName> =
        shell(command)
            .lineSequence()
            .map(String::trim)
            .mapNotNull(ComponentName::unflattenFromString)
            .toSet()

    private fun selectKeyboard(component: ComponentName) {
        shell("ime set ${component.flattenToShortString()}")
        repeat(WAIT_ATTEMPTS) {
            val current = ComponentName.unflattenFromString(shell("settings get secure default_input_method"))
            if (component == current) {
                return
            }
            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }
        error("Android did not select the expected keyboard: $component")
    }

    private fun shell(command: String): String {
        val descriptor =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)

        return ParcelFileDescriptor
            .AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText().trim() }
    }

    private companion object {
        const val WAIT_ATTEMPTS = 60
        const val WAIT_DELAY_MILLIS = 250L
        const val UNKNOWN_MESSAGE = "Impossible de déterminer si le clavier désactive le correcteur Android."
        const val NOT_DECLARED_MESSAGE = "Le clavier sélectionné ne déclare pas de restriction du correcteur Android."
        const val SUPPRESSES_MESSAGE =
            "Le clavier sélectionné désactive le correcteur Android pendant la saisie. " +
                "Choisissez un autre clavier pour utiliser les suggestions natives."
    }
}
