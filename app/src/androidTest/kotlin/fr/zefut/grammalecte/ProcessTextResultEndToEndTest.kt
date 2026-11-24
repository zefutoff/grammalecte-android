package fr.zefut.grammalecte

import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextResultEndToEndTest {
    @get:Rule val composeRule = createEmptyComposeRule()

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
                waitForButton("magasin"),
            )

            clickButton("magasin")
            clickButton("Remplacer le texte sélectionné")

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
                waitForButton("magasin"),
            )

            clickButton("magasin")
            clickButton("Copier et revenir")

            assertEquals(
                android.app.Activity.RESULT_CANCELED,
                scenario.result.resultCode,
            )

            assertClipboardContains("Je vais au magasin.")
        }
    }

    @Test
    fun editedTextCannotBeOverwrittenByStaleSuggestionsAtSameOffsets() {
        val originalText = "Je vais au magazin."
        val editedText = "Je vais au cabinet."
        assertEquals(originalText.length, editedText.length)

        ActivityScenario.launch<ProcessTextActivity>(
            processTextIntent(text = originalText, readOnly = false),
        ).use {
            assertTrue("Expected initial suggestion", waitForButton("magasin"))

            composeRule.onNodeWithTag("process-text-editor").performTextReplacement(editedText)
            composeRule.onNodeWithText("magasin").assertDoesNotExist()
            composeRule.onNodeWithTag("process-text-editor").assert(hasText(editedText))

            // Restoring the original string must not restore an earlier analysis revision.
            composeRule.onNodeWithTag("process-text-editor").performTextReplacement(originalText)
            composeRule.onNodeWithText("magasin").assertDoesNotExist()
            composeRule.onNodeWithTag("process-text-editor").assert(hasText(originalText))
        }
    }

    @Test
    fun editingTextImmediatelyRemovesVisibleSuggestions() {
        ActivityScenario.launch<ProcessTextActivity>(
            processTextIntent(text = "Je vais au magazin.", readOnly = false),
        ).use {
            assertTrue("Expected initial suggestion", waitForButton("magasin"))

            composeRule.onNodeWithTag("process-text-editor").performTextReplacement("Je vais au cabinet.")

            composeRule.onNodeWithTag("process-text-editor").assert(hasText("Je vais au cabinet."))
            composeRule.onNodeWithText("magasin").assertDoesNotExist()
        }
    }

    @Test
    fun pendingAnalysisDoesNotRestoreSuggestionsAfterEditing() {
        val editedText = "Je vais au cabinet."

        ActivityScenario.launch<ProcessTextActivity>(
            processTextIntent(text = "Je vais au magazin.", readOnly = false),
        ).use {
            assertTrue("Expected initial suggestion", waitForButton("magasin"))

            composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
                hasTestTag("process-text-analyze"),
            )
            composeRule.onNodeWithTag("process-text-analyze").performClick()
            composeRule.onNodeWithTag("process-text-editor").performTextReplacement(editedText)

            composeRule.onNodeWithText("magasin").assertDoesNotExist()

            SystemClock.sleep(3000L)

            composeRule.onNodeWithTag("process-text-editor").assert(hasText(editedText))
            composeRule.onNodeWithText("magasin").assertDoesNotExist()
        }
    }

    @Test
    fun readOnlySelectionCopiesTextInsteadOfClaimingToReplaceIt() {
        val text = "Je vais au magasin."
        val scenario =
            ActivityScenario.launchActivityForResult<ProcessTextActivity>(
                processTextIntent(text = text, readOnly = true),
            )

        scenario.use {
            composeRule.onNodeWithText("Remplacer le texte sélectionné").assertDoesNotExist()
            clickButton("Copier et revenir")

            assertEquals(android.app.Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertClipboardContains(text)
        }
    }

    @Test
    fun manualEditSurvivesActivityRecreation() {
        val editedText = "Un texte saisi par le lecteur."

        ActivityScenario.launch<ProcessTextActivity>(
            processTextIntent(text = "Je vais au magazin.", readOnly = false),
        ).use { scenario ->
            composeRule.onNodeWithTag("process-text-editor").performTextReplacement(editedText)
            scenario.recreate()
            composeRule.onNodeWithTag("process-text-editor").assert(hasText(editedText))
        }
    }

    private fun assertClipboardContains(expected: String) {
        // Clipboard reads can be restricted after PROCESS_TEXT returns: open our test editor.
        ActivityScenario.launch(ImeIntegrationTestActivity::class.java).use { foreground ->
            foreground.onActivity { activity ->
                val clipboard = activity.getSystemService(ClipboardManager::class.java)
                val clip =
                    requireNotNull(clipboard.primaryClip) {
                        "Expected corrected text on clipboard when host is in foreground"
                    }
                assertEquals(expected, clip.getItemAt(0).text?.toString())
            }
        }
    }

    private fun processTextIntent(
        text: String,
        readOnly: Boolean,
    ): Intent =
        Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setClass(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ProcessTextActivity::class.java,
            )
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readOnly)
        }

    private fun waitForButton(label: String): Boolean {
        repeat(80) {
            try {
                composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
                    hasText(label),
                )
                return true
            } catch (_: AssertionError) {
                SystemClock.sleep(250)
            }
        }
        return false
    }

    private fun clickButton(label: String) {
        composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
            hasText(label),
        )
        composeRule.onNodeWithText(label).performClick()
    }
}
