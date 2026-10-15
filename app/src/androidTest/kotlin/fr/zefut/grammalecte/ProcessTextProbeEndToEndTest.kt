package fr.zefut.grammalecte

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextProbeEndToEndTest {
    @Test
    fun independentProviderIsListedForProcessTextInDebugBuild() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent =
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                addCategory(Intent.CATEGORY_DEFAULT)
            }

        @Suppress("DEPRECATION")
        val handlers =
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY,
            )

        assertTrue(
            "Expected the debug-only PROCESS_TEXT probe to be discoverable",
            handlers.any {
                it.activityInfo.packageName == context.packageName &&
                    it.activityInfo.name == ProcessTextProbeActivity::class.java.name
            },
        )
    }

    @Test
    fun independentProviderReturnsFixedCorrectionForSyntheticSelection() {
        assertCorrection(
            initial = "Je vais au magazin.",
            expected = "Je vais au magasin.",
        )
    }

    @Test
    fun independentProviderSupportsLowercaseSyntheticSelection() {
        assertCorrection(
            initial = "je vais au magazin.",
            expected = "je vais au magasin.",
        )
    }

    @Test
    fun readOnlySelectionCannotReturnReplacement() {
        assertDoesNotReturnReplacement(
            initial = "Je vais au magazin.",
            readOnly = true,
        )
    }

    @Test
    fun unrecognizedSelectionCannotReturnReplacement() {
        assertDoesNotReturnReplacement(
            initial = "Ceci est un autre exemple fictif.",
            readOnly = false,
        )
    }

    @Test
    fun cancellingRecognizedSelectionReturnsNoReplacement() {
        launchProbe(
            initial = "Je vais au magazin.",
            readOnly = false,
        ).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    findButton(
                        activity,
                        R.string.process_text_probe_cancel,
                    ).performClick(),
                )
            }

            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertNull(scenario.result.resultData)
        }
    }

    private fun assertCorrection(
        initial: String,
        expected: String,
    ) {
        launchProbe(
            initial = initial,
            readOnly = false,
        ).use { scenario ->
            scenario.onActivity { activity ->
                val button =
                    findButton(
                        activity,
                        R.string.process_text_probe_return,
                    )

                assertTrue(button.isEnabled)
                assertTrue(button.performClick())
            }

            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals(
                expected,
                scenario.result.resultData
                    ?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.toString(),
            )
        }
    }

    private fun assertDoesNotReturnReplacement(
        initial: String,
        readOnly: Boolean,
    ) {
        launchProbe(
            initial = initial,
            readOnly = readOnly,
        ).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(
                    findButton(
                        activity,
                        R.string.process_text_probe_return,
                    ).isEnabled,
                )
                assertTrue(
                    findButton(
                        activity,
                        R.string.process_text_probe_cancel,
                    ).performClick(),
                )
            }

            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertNull(scenario.result.resultData)
        }
    }

    private fun launchProbe(
        initial: String,
        readOnly: Boolean,
    ): ActivityScenario<ProcessTextProbeActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent =
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                setClass(context, ProcessTextProbeActivity::class.java)
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, initial)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readOnly)
            }

        return ActivityScenario.launchActivityForResult(intent)
    }

    private fun findButton(
        activity: ProcessTextProbeActivity,
        labelId: Int,
    ): Button {
        val label = activity.getString(labelId)
        val views = ArrayList<View>()
        activity.window.decorView.findViewsWithText(
            views,
            label,
            View.FIND_VIEWS_WITH_TEXT,
        )

        return requireNotNull(
            views
                .filterIsInstance<Button>()
                .firstOrNull { it.text.toString() == label },
        ) {
            "Expected diagnostic button resource: $labelId"
        }
    }
}
