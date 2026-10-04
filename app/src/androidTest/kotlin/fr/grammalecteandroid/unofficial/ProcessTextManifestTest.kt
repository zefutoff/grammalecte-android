package fr.grammalecteandroid.unofficial

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextManifestTest {
    @Suppress("DEPRECATION")
    @Test
    fun processTextActivityIsDiscoverableAndExported() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val activities =
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_PROCESS_TEXT).apply {
                    type = "text/plain"
                    setPackage(context.packageName)
                },
                PackageManager.MATCH_DEFAULT_ONLY,
            )

        val activity =
            activities.single {
                it.activityInfo.name ==
                    ProcessTextActivity::class.java.name
            }

        assertTrue(
            "PROCESS_TEXT activity must be exported",
            activity.activityInfo.exported,
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun processTextActivityAcceptsPlainText() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val activities =
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_PROCESS_TEXT).apply {
                    type = "text/plain"
                    setPackage(context.packageName)
                },
                PackageManager.MATCH_DEFAULT_ONLY,
            )

        assertTrue(
            "Expected Corriger avec Grammalecte for text/plain PROCESS_TEXT",
            activities.any {
                it.activityInfo.name ==
                    ProcessTextActivity::class.java.name
            },
        )
    }
}
