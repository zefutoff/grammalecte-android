package fr.zefut.grammalecte

import android.app.Activity
import android.os.SystemClock
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextHostEndToEndTest {
    @get:Rule val composeRule = createEmptyComposeRule()

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
    fun selectionEndInsertionReproducesSamsungMessagesPartialSelectionSymptom() {
        assertHostResult(
            mode = ProcessTextHostTestActivity.HostMode.INSERT_AT_SELECTION_END,
            expectedEditorText = "Avant : Je vais au magazin.Je vais au magasin. Après.",
            initialEditorText = "Avant : Je vais au magazin. Après.",
            selectedText = ORIGINAL,
        )
    }

    @Test
    fun replacementPreservesSurroundingTextAndDoesNotDuplicateSelection() {
        assertHostResult(
            mode = ProcessTextHostTestActivity.HostMode.REPLACE,
            expectedEditorText = "Avant : Je vais au magasin. Après.",
            initialEditorText = "Avant : Je vais au magazin. Après.",
            selectedText = ORIGINAL,
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
        initialEditorText: String = ORIGINAL,
        selectedText: String = ORIGINAL,
    ) {
        val scenario =
            ActivityScenario.launch(ProcessTextHostTestActivity::class.java)

        scenario.use {
            scenario.onActivity { activity ->
                activity.mode = mode
                activity.editor.setText(initialEditorText)

                val selectedStart = initialEditorText.indexOf(selectedText)
                require(selectedStart >= 0) {
                    "Expected synthetic selected text inside the host fixture"
                }

                activity.editor.setSelection(
                    selectedStart,
                    selectedStart + selectedText.length,
                )
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
            try {
                composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
                    hasText("magasin"),
                )
                return true
            } catch (_: AssertionError) {
                SystemClock.sleep(250)
            }
        }

        return false
    }

    private fun clickCorrectionButton(label: String) {
        composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
            hasText(label),
        )
        composeRule.onNodeWithText(label).performClick()
    }

    private fun waitForCallback(scenario: ActivityScenario<ProcessTextHostTestActivity>): Boolean {
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
