package fr.grammalecteandroid.unofficial

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpellCheckerManifestTest {
    @Suppress("DEPRECATION")
    @Test
    fun spellCheckerServiceIsDiscoverableAndProtected() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val services = context.packageManager.queryIntentServices(
            Intent("android.service.textservice.SpellCheckerService").setPackage(context.packageName),
            PackageManager.GET_META_DATA,
        )

        assertTrue("Expected a SpellCheckerService in the application package", services.isNotEmpty())
        assertEquals("android.permission.BIND_TEXT_SERVICE", services.single().serviceInfo.permission)
    }
}
