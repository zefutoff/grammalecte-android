package fr.zefut.grammalecte

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupGuideInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test
    fun firstRunGuideIsShownOnceAndRemainsManuallyAvailable() {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val context =
            instrumentation.targetContext

        val preferences =
            context.getSharedPreferences(
                SETUP_PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )

        preferences
            .edit()
            .clear()
            .commit()

        try {
            ActivityScenario
                .launch(MainActivity::class.java)
                .use {
                    assertTrue(
                        "Expected setup guide on first launch",
                        waitForResumedActivity(
                            SetupGuideActivity::class.java,
                        ),
                    )

                    finishSetupGuide()

                    assertTrue(
                        "Expected MainActivity after closing setup guide",
                        waitForResumedActivity(
                            MainActivity::class.java,
                        ),
                    )
                }

            ActivityScenario
                .launch(MainActivity::class.java)
                .use { scenario ->
                    SystemClock.sleep(
                        ACTIVITY_SETTLE_DELAY_MILLIS,
                    )

                    assertFalse(
                        "Setup guide must not reopen automatically",
                        isActivityResumed(
                            SetupGuideActivity::class.java,
                        ),
                    )

                    assertTrue(
                        "Expected MainActivity on later launches",
                        isActivityResumed(
                            MainActivity::class.java,
                        ),
                    )

                    composeRule.onNodeWithText(GUIDE_BUTTON_LABEL).performClick()

                    assertTrue(
                        "Expected manual setup guide opening",
                        waitForResumedActivity(
                            SetupGuideActivity::class.java,
                        ),
                    )

                    finishSetupGuide()

                    assertTrue(
                        "Expected return to MainActivity after manual guide",
                        waitForResumedActivity(
                            MainActivity::class.java,
                        ),
                    )
                }
        } finally {
            preferences
                .edit()
                .clear()
                .commit()
        }
    }

    @Test
    fun setupGuideShowsOneStepAtATimeAndAllowsBackNavigation() {
        ActivityScenario.launch(SetupGuideActivity::class.java).use {
            composeRule.onNodeWithText("Réglages du correcteur").assertExists()
            composeRule.onNodeWithText("Choisir le clavier").assertDoesNotExist()

            val nextLabel =
                if (composeRule.onAllNodesWithText("Passer").fetchSemanticsNodes().isNotEmpty()) {
                    "Passer"
                } else {
                    "Suivant"
                }
            composeRule.onNodeWithText(nextLabel).performClick()
            composeRule.onNodeWithText("Réglages du correcteur").assertDoesNotExist()
            composeRule.onNodeWithText("Gérer les claviers").assertDoesNotExist()

            composeRule.onNodeWithText("Suivant").performClick()
            composeRule.onNodeWithText("Gérer les claviers").assertExists()
            composeRule.onNodeWithText("Choisir le clavier").assertExists()

            composeRule.onNodeWithText("Précédent").performClick()
            composeRule.onNodeWithText("Gérer les claviers").assertDoesNotExist()
        }
    }

    private fun waitForResumedActivity(expected: Class<out Activity>): Boolean {
        repeat(WAIT_ATTEMPTS) {
            if (isActivityResumed(expected)) {
                return true
            }

            SystemClock.sleep(
                WAIT_DELAY_MILLIS,
            )
        }

        return false
    }

    private fun isActivityResumed(expected: Class<out Activity>): Boolean {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        var found = false

        instrumentation.runOnMainSync {
            found =
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(
                        Stage.RESUMED,
                    ).any { activity ->
                        expected.isInstance(activity)
                    }
        }

        return found
    }

    private fun finishSetupGuide() {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(
                    Stage.RESUMED,
                ).filterIsInstance<SetupGuideActivity>()
                .firstOrNull()
                ?.finish()
        }
    }

    private companion object {
        const val SETUP_PREFERENCES_NAME =
            "setup_guide"

        const val GUIDE_BUTTON_LABEL =
            "Guide de démarrage"

        const val WAIT_ATTEMPTS = 30
        const val WAIT_DELAY_MILLIS = 100L
        const val ACTIVITY_SETTLE_DELAY_MILLIS = 500L
    }
}
