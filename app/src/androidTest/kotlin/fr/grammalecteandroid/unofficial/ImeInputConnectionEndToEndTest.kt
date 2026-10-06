package fr.grammalecteandroid.unofficial

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeInputConnectionEndToEndTest {
    @Test
    fun imeRefreshesLateSelectionAndReplacesItThroughRealInputConnection() {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val context =
            instrumentation.targetContext

        val originalIme =
            shell("settings get secure default_input_method")

        val componentName =
            ComponentName(
                context,
                GrammalecteImeService::class.java,
            )

        val component =
            componentName.flattenToShortString()

        val grammalecteWasEnabled =
            imeListContains(
                output = shell("ime list -s"),
                expected = componentName,
            )

        try {
            assertTrue(
                "Grammalecte IME was not registered by Android",
                waitForImeListed(
                    command = "ime list -a -s",
                    expected = componentName,
                ),
            )

            shell("ime enable $component")

            assertTrue(
                "Grammalecte IME was not enabled",
                waitForImeListed(
                    command = "ime list -s",
                    expected = componentName,
                ),
            )

            ActivityScenario
                .launch(ImeIntegrationTestActivity::class.java)
                .use { scenario ->
                    scenario.onActivity { activity ->
                        activity.editor.setText(INITIAL_TEXT)

                        activity.editor.requestFocus()

                        activity.editor.setSelection(
                            INITIAL_TEXT.length,
                        )

                        activity.editor.post {
                            activity
                                .getSystemService(
                                    InputMethodManager::class.java,
                                ).showSoftInput(
                                    activity.editor,
                                    InputMethodManager.SHOW_IMPLICIT,
                                )
                        }
                    }

                    SystemClock.sleep(EDITOR_READY_DELAY_MILLIS)

                    shell("ime set $component")

                    assertTrue(
                        "Android did not select Grammalecte as the default IME",
                        waitForDefaultIme(componentName),
                    )

                    scenario.onActivity { activity ->
                        val inputMethodManager =
                            activity.getSystemService(
                                InputMethodManager::class.java,
                            )

                        inputMethodManager.restartInput(
                            activity.editor,
                        )

                        inputMethodManager.hideSoftInputFromWindow(
                            activity.editor.windowToken,
                            0,
                        )

                        activity.editor.postDelayed(
                            {
                                inputMethodManager.showSoftInput(
                                    activity.editor,
                                    InputMethodManager.SHOW_IMPLICIT,
                                )
                            },
                            IME_RECONNECT_DELAY_MILLIS,
                        )
                    }

                    SystemClock.sleep(IME_SWITCH_DELAY_MILLIS)

                    requireNotNull(
                        waitForNode(
                            IME_TITLE_LABEL,
                        ),
                    ) {
                        "Expected Grammalecte IME to be visible before late selection"
                    }

                    scenario.onActivity { activity ->
                        assertEquals(
                            INITIAL_TEXT.length,
                            activity.editor.selectionStart,
                        )

                        assertEquals(
                            INITIAL_TEXT.length,
                            activity.editor.selectionEnd,
                        )

                        activity.editor.setSelection(
                            0,
                            INITIAL_TEXT.length,
                        )
                    }

                    val button =
                        requireNotNull(
                            waitForNode(
                                CORRECT_AND_RETURN_LABEL,
                            ),
                        ) {
                            "Expected Grammalecte IME to refresh the late selection"
                        }

                    assertTrue(
                        "Unable to click Grammalecte correction action",
                        button.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                        ),
                    )

                    assertTrue(
                        "Timed out waiting for corrected EditText content",
                        waitForText(
                            scenario = scenario,
                            expected = EXPECTED_TEXT,
                        ),
                    )

                    scenario.onActivity { activity ->
                        assertEquals(
                            EXPECTED_TEXT,
                            activity.editor.text.toString(),
                        )
                    }
                }
        } finally {
            restoreInputMethod(
                originalIme = originalIme,
                component = component,
                grammalecteWasEnabled = grammalecteWasEnabled,
            )
        }
    }

    private fun waitForNode(expectedText: String): AccessibilityNodeInfo? {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val automation =
            instrumentation.uiAutomation

        val serviceInfo =
            automation.serviceInfo

        serviceInfo.flags =
            serviceInfo.flags or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        automation.serviceInfo =
            serviceInfo

        repeat(WAIT_ATTEMPTS) {
            automation.windows.forEach { window ->
                val root =
                    window.root
                        ?: return@forEach

                val node =
                    root
                        .findAccessibilityNodeInfosByText(
                            expectedText,
                        ).firstOrNull {
                            it.text?.toString() ==
                                expectedText &&
                                it.isVisibleToUser
                        }

                if (node != null) {
                    return node
                }
            }

            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }

        return null
    }

    private fun waitForText(
        scenario: ActivityScenario<ImeIntegrationTestActivity>,
        expected: String,
    ): Boolean {
        repeat(WAIT_ATTEMPTS) {
            var currentText = ""

            scenario.onActivity { activity ->
                currentText =
                    activity.editor.text.toString()
            }

            if (currentText == expected) {
                return true
            }

            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }

        return false
    }

    private fun waitForImeListed(
        command: String,
        expected: ComponentName,
    ): Boolean {
        repeat(WAIT_ATTEMPTS) {
            if (
                imeListContains(
                    output = shell(command),
                    expected = expected,
                )
            ) {
                return true
            }

            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }

        return false
    }

    private fun imeListContains(
        output: String,
        expected: ComponentName,
    ): Boolean =
        output
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull {
                ComponentName.unflattenFromString(it)
            }.any {
                it == expected
            }

    private fun waitForDefaultIme(expected: ComponentName): Boolean {
        repeat(WAIT_ATTEMPTS) {
            val current =
                ComponentName.unflattenFromString(
                    shell(
                        "settings get secure default_input_method",
                    ),
                )

            if (current == expected) {
                return true
            }

            SystemClock.sleep(WAIT_DELAY_MILLIS)
        }

        return false
    }

    private fun restoreInputMethod(
        originalIme: String,
        component: String,
        grammalecteWasEnabled: Boolean,
    ) {
        if (
            originalIme.isNotBlank() &&
            originalIme != "null"
        ) {
            shell(
                "ime set $originalIme",
            )
        }

        if (!grammalecteWasEnabled) {
            shell(
                "ime disable $component",
            )
        }
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
            .use {
                it.readText().trim()
            }
    }

    private companion object {
        const val INITIAL_TEXT =
            "Je suis aller au magasin hier."

        const val EXPECTED_TEXT =
            "Je suis allé au magasin hier."

        const val IME_TITLE_LABEL =
            "Grammalecte"

        const val CORRECT_AND_RETURN_LABEL =
            "Corriger tout et revenir"

        const val WAIT_ATTEMPTS = 60
        const val WAIT_DELAY_MILLIS = 250L
        const val EDITOR_READY_DELAY_MILLIS = 750L
        const val IME_RECONNECT_DELAY_MILLIS = 250L
        const val IME_SWITCH_DELAY_MILLIS = 1_500L
    }
}
