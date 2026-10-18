package fr.zefut.grammalecte

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowInsets
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
        runImeScenario(staleChangeBeforeApply = false)
    }

    @Test
    fun imeRejectsChangedTextAtUnchangedSelectionCoordinates() {
        runImeScenario(staleChangeBeforeApply = true)
    }

    @Test
    fun imeDoesNotReadOrModifyPasswordFields() {
        val sensitiveTypes =
            listOf(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            )

        sensitiveTypes.forEach { type ->
            runImeScenario(
                staleChangeBeforeApply = false,
                sensitiveInputType = type,
            )
        }
    }

    @Test
    fun imeDisablesCorrectionAfterSwitchingToPasswordEditor() {
        runImeScenario(
            staleChangeBeforeApply = false,
            switchToSensitiveEditor = true,
        )
    }

    private fun runImeScenario(
        staleChangeBeforeApply: Boolean,
        sensitiveInputType: Int? = null,
        switchToSensitiveEditor: Boolean = false,
    ) {
        val numericPasswordType =
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD

        val sampleText =
            if (sensitiveInputType == numericPasswordType) "123456" else INITIAL_TEXT

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val context =
            instrumentation.targetContext

        val originalIme =
            shell("settings get secure default_input_method")

        val originalShowImeWithHardKeyboard =
            shell("settings get secure show_ime_with_hard_keyboard")

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
            // Android can hide the software IME when a hardware keyboard is detected.
            shell("settings put secure show_ime_with_hard_keyboard 1")

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
                        if (sensitiveInputType != null) {
                            activity.editor.inputType = sensitiveInputType
                        }

                        activity.editor.setText(sampleText)

                        activity.editor.requestFocus()

                        activity.editor.setSelection(
                            sampleText.length,
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

                        activity.editor.requestFocus()

                        if (sensitiveInputType != null) {
                            activity.editor.resetImeCounters()
                        }

                        inputMethodManager.restartInput(
                            activity.editor,
                        )

                        activity.editor.postDelayed(
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    activity.editor.windowInsetsController?.show(
                                        WindowInsets.Type.ime(),
                                    )
                                }

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
                        "Expected Grammalecte IME to be visible before late selection.\n" +
                            imeVisibilityDiagnostics(scenario)
                    }

                    scenario.onActivity { activity ->
                        assertEquals(
                            sampleText.length,
                            activity.editor.selectionStart,
                        )

                        assertEquals(
                            sampleText.length,
                            activity.editor.selectionEnd,
                        )

                        activity.editor.setSelection(
                            0,
                            sampleText.length,
                        )
                    }

                    if (sensitiveInputType != null) {
                        val blockedMessageVisible =
                            waitForNode(
                                "Correction désactivée dans les champs sensibles.",
                            ) != null

                        SystemClock.sleep(400L)

                        scenario.onActivity { activity ->
                            assertEquals(sampleText, activity.editor.text.toString())
                            assertEquals(
                                "IME read selected text from a sensitive field",
                                0,
                                activity.editor.selectedTextReadCount,
                            )
                            assertEquals(
                                "IME committed text into a sensitive field",
                                0,
                                activity.editor.commitTextCount,
                            )
                        }

                        assertTrue(
                            "The IME did not disable correction for a sensitive field",
                            blockedMessageVisible,
                        )

                        return@use
                    }

                    val button =
                        requireNotNull(
                            waitForNode(
                                CORRECT_AND_RETURN_LABEL,
                            ),
                        ) {
                            "Expected Grammalecte IME to refresh the late selection"
                        }

                    if (switchToSensitiveEditor) {
                        var switchedEditor: ObservedEditText? = null

                        scenario.onActivity { activity ->
                            val passwordEditor =
                                ObservedEditText(activity).apply {
                                    inputType =
                                        InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    setText(SYNTHETIC_PASSWORD)
                                    setSelection(0, SYNTHETIC_PASSWORD.length)
                                }

                            activity.addContentView(
                                passwordEditor,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                ),
                            )

                            assertTrue(
                                "Password editor did not receive focus",
                                passwordEditor.requestFocus(),
                            )

                            val inputMethodManager =
                                activity.getSystemService(
                                    InputMethodManager::class.java,
                                )

                            inputMethodManager.restartInput(passwordEditor)
                            inputMethodManager.showSoftInput(
                                passwordEditor,
                                InputMethodManager.SHOW_IMPLICIT,
                            )

                            switchedEditor = passwordEditor
                        }

                        assertTrue(
                            "IME did not block correction after editor switch",
                            waitForNode(
                                "Correction désactivée dans les champs sensibles.",
                            ) != null,
                        )

                        SystemClock.sleep(400L)

                        scenario.onActivity { activity ->
                            val passwordEditor = requireNotNull(switchedEditor)

                            assertTrue(passwordEditor.hasFocus())
                            assertEquals(
                                SYNTHETIC_PASSWORD,
                                passwordEditor.text.toString(),
                            )
                            assertEquals(
                                "Sensitive editor was read after focus change",
                                0,
                                passwordEditor.selectedTextReadCount,
                            )
                            assertEquals(
                                "Sensitive editor was modified after focus change",
                                0,
                                passwordEditor.commitTextCount,
                            )
                        }

                        return@use
                    }

                    if (staleChangeBeforeApply) {
                        scenario.onActivity { activity ->
                            assertEquals(INITIAL_TEXT.length, STALE_TEXT.length)
                            assertEquals(0, activity.editor.selectionStart)
                            assertEquals(INITIAL_TEXT.length, activity.editor.selectionEnd)

                            activity.editor.text.replace(
                                0,
                                INITIAL_TEXT.length,
                                STALE_TEXT,
                            )

                            assertEquals(0, activity.editor.selectionStart)
                            assertEquals(STALE_TEXT.length, activity.editor.selectionEnd)
                            assertEquals(STALE_TEXT, activity.editor.text.toString())
                        }
                    }

                    assertTrue(
                        "Unable to click Grammalecte correction action",
                        button.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                        ),
                    )

                    if (staleChangeBeforeApply) {
                        assertTrue(
                            "IME did not reject the changed selected text",
                            waitForNode(
                                "Le texte a changé. Sélectionnez-le de nouveau.",
                            ) != null,
                        )

                        scenario.onActivity { activity ->
                            assertEquals(
                                "Stale correction overwrote modified editor content",
                                STALE_TEXT,
                                activity.editor.text.toString(),
                            )
                        }
                    } else {
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
                }
        } finally {
            try {
                restoreInputMethod(
                    originalIme = originalIme,
                    component = component,
                    grammalecteWasEnabled = grammalecteWasEnabled,
                )
            } finally {
                restoreSecureSetting(
                    key = "show_ime_with_hard_keyboard",
                    originalValue = originalShowImeWithHardKeyboard,
                )
            }
        }
    }

    private fun imeVisibilityDiagnostics(scenario: ActivityScenario<ImeIntegrationTestActivity>): String {
        var editorState = "unavailable"

        scenario.onActivity { activity ->
            val editor = activity.editor
            val inputMethodManager =
                activity.getSystemService(InputMethodManager::class.java)

            val imeVisible =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    editor.rootWindowInsets?.isVisible(
                        WindowInsets.Type.ime(),
                    )
                } else {
                    null
                }

            editorState =
                "editorFocused=${editor.hasFocus()}, " +
                "windowFocused=${editor.hasWindowFocus()}, " +
                "inputActive=${inputMethodManager.isActive(editor)}, " +
                "imeInsetsVisible=$imeVisible, " +
                "keyboard=${activity.resources.configuration.keyboard}, " +
                "hardKeyboardHidden=${activity.resources.configuration.hardKeyboardHidden}"
        }

        val inputMethodState =
            shell("dumpsys input_method")
                .lineSequence()
                .filter { line ->
                    listOf(
                        "mCurId",
                        "mCurFocusedWindow",
                        "mCurClient",
                        "mCurMethod",
                        "mShowRequested",
                        "mInputShown",
                        "mImeWindowVis",
                        "mEditorInfo",
                        "mInputViewShown",
                    ).any { key ->
                        line.contains(key)
                    }
                }.take(30)
                .joinToString("\n")

        val windowFocus =
            shell("dumpsys window")
                .lineSequence()
                .filter { line ->
                    line.contains("mCurrentFocus") ||
                        line.contains("mFocusedApp") ||
                        line.contains("mInputMethodWindow")
                }.take(10)
                .joinToString("\n")

        val accessibilityWindows =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .windows
                .joinToString { window ->
                    "${window.type}:${window.title}"
                }

        return buildString {
            appendLine(editorState)
            appendLine("defaultIme=${shell("settings get secure default_input_method")}")
            appendLine("showWithHardKeyboard=${shell("settings get secure show_ime_with_hard_keyboard")}")
            appendLine("accessibilityWindows=$accessibilityWindows")
            appendLine("inputMethodState:\n$inputMethodState")
            appendLine("windowFocus:\n$windowFocus")
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

    private fun restoreSecureSetting(
        key: String,
        originalValue: String,
    ) {
        if (originalValue.isBlank() || originalValue == "null") {
            shell("settings delete secure $key")
        } else {
            shell("settings put secure $key $originalValue")
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

        const val STALE_TEXT =
            "Je suis passe au magasin hier."

        const val SYNTHETIC_PASSWORD =
            "FAKE_test_12345"

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
