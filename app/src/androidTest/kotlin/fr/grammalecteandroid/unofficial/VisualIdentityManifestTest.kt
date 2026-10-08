package fr.grammalecteandroid.unofficial

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualIdentityManifestTest {
    @Suppress("DEPRECATION")
    @Test
    fun visualIdentityResourcesAreWiredToAndroidComponents() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val packageManager =
            context.packageManager

        val application =
            packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )

        assertEquals(
            R.mipmap.ic_launcher,
            application.icon,
        )

        assertNotNull(
            application.loadIcon(
                packageManager,
            ),
        )

        assertEquals(
            "Grammalecte Android (non officiel)",
            application.loadLabel(packageManager).toString(),
        )

        val mainActivity =
            packageManager.getActivityInfo(
                ComponentName(
                    context,
                    MainActivity::class.java,
                ),
                PackageManager.GET_META_DATA,
            )

        assertEquals(
            "Grammalecte",
            mainActivity.loadLabel(packageManager).toString(),
        )

        val tileService =
            packageManager.getServiceInfo(
                ComponentName(
                    context,
                    GrammalecteTileService::class.java,
                ),
                PackageManager.GET_META_DATA,
            )

        assertEquals(
            R.drawable.ic_qs_grammalecte,
            tileService.icon,
        )
    }
}
