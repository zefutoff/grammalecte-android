package fr.grammalecteandroid.unofficial

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuleSettingsManifestTest {
    @Suppress("DEPRECATION")
    @Test
    fun ruleSettingsActivityIsInternal() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val activity =
            context.packageManager.getActivityInfo(
                ComponentName(
                    context,
                    RuleSettingsActivity::class.java,
                ),
                PackageManager.GET_META_DATA,
            )

        assertFalse(
            "Rule settings activity must not be exported",
            activity.exported,
        )
    }
}
