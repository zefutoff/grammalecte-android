package fr.zefut.grammalecte

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextHostEndToEndTest {
    @Test
    fun replaceModeUpdatesOriginalSelectionExactlyOnce() {
        assertHostResult(
            mode = ProcessTextHostTestActivity.HostMode.REPLACE,
            expectedEditorText = CORRECTED,
        )
    }

    @Test
    fun appendModeReproducesDuplicateTextWithoutDuplicatedProcessTextResult() {
        assertHostResult(
            mode = ProcessTextHostTestActivity.HostMode.APPEND,
            expectedEditorText = ORIGINAL + CORRECTED,
        )
    }

    @Test
    fun ignoredResultPreservesUncorrectedText() {
        assertHostResult(
            mode = ProcessTextHostTestActivity.HostMode.IGNORE,
            expectedEditorText = ORIGINAL,
        )
    }

    private fun assertHostResult(
        mode: ProcessTextHostTestActivity.HostMode,
        expectedEditorText: String,
    ) {
        val scenario =
            ActivityScenario.launch(ProcessTextHostTestActivity::class.java)

        scenario.use {
            scenario.onActivity { activity ->
                activity.mode = mode
                activity.editor.setText(ORIGINAL)
                activity.editor.setSelection(0, ORIGINAL.length)
                activity.startCorrection()
            }

            assertTrue(
                "Expected a Grammalecte spelling suggestion",
                waitForSuggestion(),
            )

            clickCorrectionButton("magasin")
            clickCorrectionButton("Remplacer le texte sélectionné")

            assertTrue(
                "Timed out waiting for the PROCESS_TEXT host callback",
                waitForCallback(scenario),
            )

            scenario.onActivity { activity ->
                assertEquals(Activity.RESULT_OK, activity.lastResultCode)
                assertEquals(CORRECTED, activity.returnedText)
                assertEquals(expectedEditorText, activity.editor.text.toString())
            }
        }
    }

    private fun waitForSuggestion(): Boolean {
        repeat(80) {
            if (findButton("magasin") != null) {
                return true
            }

            SystemClock.sleep(250)
        }

        return false
    }

    private fun clickCorrectionButton(label: String) {
        val button =
            requireNotNull(findButton(label)) {
                "Expected PROCESS_TEXT button: $label"
            }

        assertTrue(
            "Failed to click PROCESS_TEXT button: $label",
            button.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK),
        )
    }

    private fun findButton(label: String): android.view.accessibility.AccessibilityNodeInfo? {
        val automation =
            InstrumentationRegistry.getInstrumentation().uiAutomation

        return automation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(label)
            ?.firstOrNull {
                it.text?.toString() == label && it.isClickable
            }
    }

    private fun waitForCallback(
        scenario: ActivityScenario<ProcessTextHostTestActivity>,
    ): Boolean {
        repeat(80) {
            var finished = false

            scenario.onActivity { activity ->
                finished = activity.lastResultCode != null
            }

            if (finished) {
                return true
            }

            SystemClock.sleep(250)
        }

        return false
    }

    private companion object {
        const val ORIGINAL = "Je vais au magazin."
        const val CORRECTED = "Je vais au magasin."
    }
}
