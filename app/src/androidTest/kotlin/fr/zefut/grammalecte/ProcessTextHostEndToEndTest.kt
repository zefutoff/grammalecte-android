package fr.zefut.grammalecte

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
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
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        repeat(80) {
            var found = false

            instrumentation.runOnMainSync {
                found = findCorrectionButtonOnMainThread("magasin") != null
            }

            if (found) {
                return true
            }

            SystemClock.sleep(250)
        }

        return false
    }

    private fun clickCorrectionButton(label: String) {
        var clicked = false

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val button =
                requireNotNull(findCorrectionButtonOnMainThread(label)) {
                    "Expected PROCESS_TEXT button: $label"
                }

            clicked = button.performClick()
        }

        assertTrue(
            "Failed to click PROCESS_TEXT button: $label",
            clicked,
        )
    }

    private fun findCorrectionButtonOnMainThread(label: String): Button? {
        // Android accessibility queries can return disconnected nodes while
        // switching activities. Both activities belong to this test process,
        // so inspect the resumed correction activity directly instead.
        val activities =
            ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(Stage.RESUMED)

        val activity =
            activities
                .filterIsInstance<ProcessTextActivity>()
                .firstOrNull()
                ?: return null

        val views = ArrayList<View>()
        activity.window.decorView.findViewsWithText(
            views,
            label,
            View.FIND_VIEWS_WITH_TEXT,
        )

        return views
            .filterIsInstance<Button>()
            .firstOrNull {
                it.text.toString() == label
            }
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
