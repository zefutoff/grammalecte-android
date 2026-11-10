package fr.zefut.grammalecte

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test
    fun accessibilityHelpersApplyExpectedSemantics() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val view =
            TextView(context)

        view.enablePoliteAccessibilityUpdates()

        assertEquals(
            View.ACCESSIBILITY_LIVE_REGION_POLITE,
            view.accessibilityLiveRegion,
        )

        view.markAsAccessibilityHeading()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(
                view.isAccessibilityHeading,
            )
        }
    }

    @Test
    fun mainScreenExposesAccessibilitySemantics() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val preferences =
            context.getSharedPreferences(
                SETUP_PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )

        preferences
            .edit()
            .putBoolean(
                SETUP_GUIDE_SHOWN_KEY,
                true,
            ).commit()

        try {
            ActivityScenario
                .launch(MainActivity::class.java)
                .use {
                    composeRule.onNodeWithTag("main-heading").assert(
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
                    )
                    composeRule.onNodeWithTag("main-status").assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.LiveRegion,
                            LiveRegionMode.Polite,
                        ),
                    )
                    composeRule.onNodeWithTag("keyboard-status").assertExists()
                }
        } finally {
            preferences
                .edit()
                .clear()
                .commit()
        }
    }

    @Test
    fun setupGuideExposesAccessibilitySemantics() {
        ActivityScenario
            .launch(SetupGuideActivity::class.java)
            .use { scenario ->
                scenario.onActivity { activity ->
                    assertScreenHeading(
                        activity,
                        "Configurer Grammalecte",
                    )

                    assertTrue(
                        "Expected setup status live regions",
                        countPoliteLiveRegions(
                            activity.window.decorView,
                        ) >= 2,
                    )
                }
            }
    }

    @Test
    fun correctionStatusExposesAccessibilitySemantics() {
        ActivityScenario.launch(CorrectionStatusActivity::class.java).use {
            composeRule.onNodeWithTag("status-heading").assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            )
            composeRule.onNodeWithTag("status-spell-checker").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
            composeRule.onNodeWithTag("status-keyboard").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        }
    }

    @Test
    fun ruleSettingsExposesAccessibilitySemantics() {
        ActivityScenario
            .launch(RuleSettingsActivity::class.java)
            .use { scenario ->
                scenario.onActivity { activity ->
                    assertScreenHeading(
                        activity,
                        "Réglages de correction",
                    )

                    assertTrue(
                        "Expected rule settings live region",
                        countPoliteLiveRegions(
                            activity.window.decorView,
                        ) >= 1,
                    )
                }
            }
    }

    @Test
    fun personalDictionaryExposesAccessibilitySemantics() {
        ActivityScenario
            .launch(PersonalDictionaryActivity::class.java)
            .use { scenario ->
                scenario.onActivity { activity ->
                    assertScreenHeading(
                        activity,
                        "Dictionnaire personnel",
                    )

                    assertTrue(
                        "Expected personal dictionary live region",
                        countPoliteLiveRegions(
                            activity.window.decorView,
                        ) >= 1,
                    )
                }
            }
    }

    @Test
    fun processTextExposesAccessibilitySemantics() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val intent =
            Intent(
                context,
                ProcessTextActivity::class.java,
            ).apply {
                action = Intent.ACTION_PROCESS_TEXT
                type = "text/plain"

                putExtra(
                    Intent.EXTRA_PROCESS_TEXT,
                    "Je sui aller au magazin.",
                )

                putExtra(
                    Intent.EXTRA_PROCESS_TEXT_READONLY,
                    false,
                )
            }

        ActivityScenario
            .launch<ProcessTextActivity>(
                intent,
            ).use { scenario ->
                scenario.onActivity { activity ->
                    assertScreenHeading(
                        activity,
                        activity.getString(R.string.process_text_label),
                    )

                    assertTrue(
                        "Expected process text live region",
                        countPoliteLiveRegions(
                            activity.window.decorView,
                        ) >= 1,
                    )
                }
            }
    }

    private fun assertScreenHeading(
        activity: Activity,
        text: String,
    ) {
        val heading =
            findTextView(
                activity.window.decorView,
                text,
            )

        assertNotNull(
            "Expected heading: $text",
            heading,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(
                "Expected accessibility heading: $text",
                requireNotNull(heading)
                    .isAccessibilityHeading,
            )
        }
    }

    private fun findTextView(
        view: View,
        text: String,
    ): TextView? {
        if (
            view is TextView &&
            view.text.toString() == text
        ) {
            return view
        }

        if (view !is ViewGroup) {
            return null
        }

        for (index in 0 until view.childCount) {
            findTextView(
                view.getChildAt(index),
                text,
            )?.let {
                return it
            }
        }

        return null
    }

    private fun countPoliteLiveRegions(view: View): Int {
        var count =
            if (
                view is TextView &&
                view.accessibilityLiveRegion ==
                View.ACCESSIBILITY_LIVE_REGION_POLITE
            ) {
                1
            } else {
                0
            }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                count +=
                    countPoliteLiveRegions(
                        view.getChildAt(index),
                    )
            }
        }

        return count
    }

    private companion object {
        const val SETUP_PREFERENCES_NAME =
            "setup_guide"

        const val SETUP_GUIDE_SHOWN_KEY =
            "setup_guide_shown_v1"
    }
}
