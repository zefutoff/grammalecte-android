package fr.zefut.grammalecte

import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextResultEndToEndTest {
    @Test
    fun editableSelectionReturnsCorrectedTextToCallingActivity() {
        val scenario =
            ActivityScenario.launchActivityForResult<ProcessTextActivity>(
                processTextIntent(
                    text = "Je vais au magazin.",
                    readOnly = false,
                ),
            )

        scenario.use {
            assertTrue(
                "Expected real Grammalecte suggestion for magazin",
                waitForButton(scenario, "magasin"),
            )

            clickButton(scenario, "magasin")
            clickButton(scenario, "Remplacer le texte sélectionné")

            val result = scenario.result

            assertEquals(android.app.Activity.RESULT_OK, result.resultCode)
            assertEquals(
                "Je vais au magasin.",
                result.resultData?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString(),
            )
        }
    }

    @Test
    fun editableSelectionCanCopyCorrectionWithoutReturningReplacement() {
        val scenario =
            ActivityScenario.launchActivityForResult<ProcessTextActivity>(
                processTextIntent(
                    text = "Je vais au magazin.",
                    readOnly = false,
                ),
            )

        scenario.use {
            assertTrue(
                "Expected real Grammalecte suggestion for magazin",
                waitForButton(scenario, "magasin"),
            )

            clickButton(scenario, "magasin")
            clickButton(scenario, "Copier et revenir")

            assertEquals(
                android.app.Activity.RESULT_CANCELED,
                scenario.result.resultCode,
            )

            assertClipboardContains("Je vais au magasin.")
        }
    }

    @Test
    fun staleSuggestionDoesNotOverwriteEditedTextAtSameOffsets() {
        val originalText = "Je vais au magazin."
        val editedText = "Je vais au cabinet."

        assertEquals(originalText.length, editedText.length)

        val scenario =
            ActivityScenario.launch<ProcessTextActivity>(
                processTextIntent(
                    text = originalText,
                    readOnly = false,
                ),
            )

        scenario.use {
            assertTrue(
                "Expected initial Grammalecte suggestion",
                waitForButton(scenario, "magasin"),
            )

            scenario.onActivity { activity ->
                val root = activity.window.decorView
                val staleButton =
                    requireNotNull(findButton(root, "magasin"))
                val editor =
                    requireNotNull(findEditor(root))

                editor.setText(editedText)

                assertTrue(staleButton.performClick())

                assertEquals(
                    "Stale suggestion must not overwrite user edits",
                    editedText,
                    editor.text.toString(),
                )

                // Even restoring the original text must not reactivate
                // a suggestion from the previous analysis revision.
                editor.setText(originalText)

                assertTrue(staleButton.performClick())

                assertEquals(
                    "Restoring text must not reactivate a stale suggestion",
                    originalText,
                    editor.text.toString(),
                )
            }
        }
    }

    @Test
    fun editingTextImmediatelyRemovesVisibleSuggestions() {
        val scenario =
            ActivityScenario.launch<ProcessTextActivity>(
                processTextIntent(
                    text = "Je vais au magazin.",
                    readOnly = false,
                ),
            )

        scenario.use {
            assertTrue(
                "Expected the initial spelling suggestion",
                waitForButton(scenario, "magasin"),
            )

            scenario.onActivity { activity ->
                val root = activity.window.decorView
                val editor = requireNotNull(findEditor(root))

                editor.setText("Je vais au cabinet.")

                assertEquals(
                    "Je vais au cabinet.",
                    editor.text.toString(),
                )
                assertTrue(
                    "Old suggestion remained visible after editing",
                    findButton(root, "magasin") == null,
                )
            }
        }
    }

    @Test
    fun pendingAnalysisDoesNotRestoreSuggestionsAfterEditing() {
        val editedText = "Je vais au cabinet."

        val scenario =
            ActivityScenario.launch<ProcessTextActivity>(
                processTextIntent(
                    text = "Je vais au magazin.",
                    readOnly = false,
                ),
            )

        scenario.use {
            assertTrue(
                "Expected the initial spelling suggestion",
                waitForButton(scenario, "magasin"),
            )

            scenario.onActivity { activity ->
                val root = activity.window.decorView
                val editor = requireNotNull(findEditor(root))
                val analyzeButton =
                    requireNotNull(findButton(root, "Analyser"))

                // Start analysis and edit before the main thread can
                // process its asynchronous completion callback.
                assertTrue(analyzeButton.performClick())
                editor.setText(editedText)

                assertTrue(
                    findButton(root, "magasin") == null,
                )
            }

            // Allow the background analysis time to complete.
            SystemClock.sleep(3000L)

            scenario.onActivity { activity ->
                val root = activity.window.decorView
                val editor = requireNotNull(findEditor(root))

                assertEquals(
                    editedText,
                    editor.text.toString(),
                )
                assertTrue(
                    "Obsolete analysis restored an old suggestion",
                    findButton(root, "magasin") == null,
                )
            }
        }
    }

    @Test
    fun readOnlySelectionCopiesTextInsteadOfClaimingToReplaceIt() {
        val text = "Je vais au magasin."
        val scenario =
            ActivityScenario.launchActivityForResult<ProcessTextActivity>(
                processTextIntent(
                    text = text,
                    readOnly = true,
                ),
            )

        scenario.use {
            clickButton(scenario, "Copier et revenir")

            val result = scenario.result

            assertEquals(android.app.Activity.RESULT_CANCELED, result.resultCode)

            assertClipboardContains(text)
        }
    }

    private fun findEditor(root: View): EditText? {
        if (root is EditText) {
            return root
        }

        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findEditor(root.getChildAt(index))?.let {
                    return it
                }
            }
        }

        return null
    }

    private fun assertClipboardContains(expected: String) {
        // Android restricts clipboard reads when the app is not in the
        // foreground. Reopen our own test editor after PROCESS_TEXT returns.
        ActivityScenario.launch(ImeIntegrationTestActivity::class.java).use { foreground ->
            foreground.onActivity { activity ->
                val clipboard =
                    activity.getSystemService(ClipboardManager::class.java)

                val clip =
                    requireNotNull(clipboard.primaryClip) {
                        "Expected corrected text on clipboard when host is in foreground"
                    }

                assertEquals(
                    expected,
                    clip.getItemAt(0).text?.toString(),
                )
            }
        }
    }

    private fun processTextIntent(
        text: String,
        readOnly: Boolean,
    ): Intent =
        Intent(
            Intent.ACTION_PROCESS_TEXT,
        ).apply {
            setClass(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ProcessTextActivity::class.java,
            )
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readOnly)
        }

    private fun waitForButton(
        scenario: ActivityScenario<ProcessTextActivity>,
        label: String,
    ): Boolean {
        repeat(60) {
            var found = false

            scenario.onActivity { activity ->
                found = findButton(activity.window.decorView, label) != null
            }

            if (found) {
                return true
            }

            SystemClock.sleep(250)
        }

        return false
    }

    private fun clickButton(
        scenario: ActivityScenario<ProcessTextActivity>,
        label: String,
    ) {
        scenario.onActivity { activity ->
            val button =
                requireNotNull(
                    findButton(activity.window.decorView, label),
                ) {
                    "Expected PROCESS_TEXT button: $label"
                }

            assertTrue(
                "Unable to click PROCESS_TEXT button: $label",
                button.performClick(),
            )
        }
    }

    private fun findButton(
        root: View,
        label: String,
    ): Button? {
        val views = ArrayList<View>()
        root.findViewsWithText(
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
}
