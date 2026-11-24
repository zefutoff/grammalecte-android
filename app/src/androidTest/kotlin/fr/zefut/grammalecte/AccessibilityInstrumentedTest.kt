package fr.zefut.grammalecte

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
        ActivityScenario.launch(SetupGuideActivity::class.java).use {
            composeRule.onNodeWithText("Configurer Grammalecte").assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            )
            composeRule.onNodeWithTag("setup-progress").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
            composeRule.onNodeWithTag("setup-status").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
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
        ActivityScenario.launch(RuleSettingsActivity::class.java).use {
            composeRule.onNodeWithTag("rules-heading").assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            )
            composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
                hasTestTag("rules-status"),
            )
            composeRule.onNodeWithTag("rules-status").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        }
    }

    @Test
    fun personalDictionaryExposesAccessibilitySemantics() {
        ActivityScenario.launch(PersonalDictionaryActivity::class.java).use {
            composeRule.onNodeWithTag("dictionary-heading").assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            )
            composeRule.onNodeWithTag("dictionary-status").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
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
                putExtra(Intent.EXTRA_PROCESS_TEXT, "Je sui aller au magazin.")
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
            }

        ActivityScenario.launch<ProcessTextActivity>(intent).use {
            composeRule.onNodeWithTag("process-text-heading").assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            )
            composeRule.onNodeWithTag("process-text-status").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
            composeRule.onNodeWithTag("process-text-editor").assertExists()
        }
    }

    private companion object {
        const val SETUP_PREFERENCES_NAME =
            "setup_guide"

        const val SETUP_GUIDE_SHOWN_KEY =
            "setup_guide_shown_v1"
    }
}
