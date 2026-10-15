package fr.zefut.grammalecte

import android.content.ClipboardManager
import android.content.Intent
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

            // Android restricts clipboard reads when the app is not in the
            // foreground. After PROCESS_TEXT finishes, launch a test editor
            // in our own app before verifying the copied content.
            ActivityScenario.launch(ImeIntegrationTestActivity::class.java).use { foreground ->
                foreground.onActivity { activity ->
                    val clipboard =
                        activity.getSystemService(ClipboardManager::class.java)

                    val clip =
                        requireNotNull(clipboard.primaryClip) {
                            "Expected corrected text on clipboard when host is in foreground"
                        }

                    assertEquals(
                        text,
                        clip.getItemAt(0).text?.toString(),
                    )
                }
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
