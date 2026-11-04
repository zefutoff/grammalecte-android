package fr.zefut.grammalecte

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupGuideInstrumentedTest {
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

                    scenario.onActivity { activity ->
                        val guideButton =
                            findButton(
                                activity.window.decorView,
                                GUIDE_BUTTON_LABEL,
                            )

                        assertNotNull(
                            "Expected permanent setup guide action",
                            guideButton,
                        )

                        requireNotNull(
                            guideButton,
                        ).performClick()
                    }

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
        ActivityScenario.launch(SetupGuideActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.window.decorView
                assertTrue(
                    "Correcteur Android should be visible first",
                    findButton(root, "Réglages du correcteur")?.isShown == true,
                )
                assertFalse(
                    "Optional IME should not appear on step 1",
                    findButton(root, "Choisir le clavier")?.isShown == true,
                )

                val firstNext =
                    findButton(root, "Passer")
                        ?: findButton(root, "Suivant")
                requireNotNull(firstNext).performClick()

                assertFalse(
                    "Spell checker settings should be hidden on step 2",
                    findButton(root, "Réglages du correcteur")?.isShown == true,
                )
                assertFalse(
                    "IME settings should be hidden on step 2",
                    findButton(root, "Gérer les claviers")?.isShown == true,
                )

                requireNotNull(findButton(root, "Suivant")).performClick()
                assertTrue(
                    "IME activation should appear only on step 3",
                    findButton(root, "Gérer les claviers")?.isShown == true,
                )
                assertTrue(
                    "IME picker should appear on step 3",
                    findButton(root, "Choisir le clavier")?.isShown == true,
                )

                requireNotNull(findButton(root, "Précédent")).performClick()
                assertFalse(
                    "IME settings should be hidden after going back",
                    findButton(root, "Gérer les claviers")?.isShown == true,
                )
            }
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

    private fun findButton(
        view: View,
        label: String,
    ): Button? {
        if (
            view is Button &&
            view.text.toString() == label
        ) {
            return view
        }

        if (view !is ViewGroup) {
            return null
        }

        for (index in 0 until view.childCount) {
            findButton(
                view.getChildAt(index),
                label,
            )?.let {
                return it
            }
        }

        return null
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
