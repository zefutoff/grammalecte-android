package fr.grammalecteandroid.unofficial

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeManifestTest {
    @Suppress("DEPRECATION")
    @Test
    fun imeServiceIsDiscoverableAndProtected() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val services =
            context.packageManager.queryIntentServices(
                Intent(INPUT_METHOD_ACTION)
                    .setPackage(context.packageName),
                PackageManager.GET_META_DATA,
            )

        val service =
            services.single {
                it.serviceInfo.name ==
                    GrammalecteImeService::class.java.name
            }

        assertEquals(
            Manifest.permission.BIND_INPUT_METHOD,
            service.serviceInfo.permission,
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun imeServiceDeclaresExpectedMetadata() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val services =
            context.packageManager.queryIntentServices(
                Intent(INPUT_METHOD_ACTION)
                    .setPackage(context.packageName),
                PackageManager.GET_META_DATA,
            )

        val service =
            services.single {
                it.serviceInfo.name ==
                    GrammalecteImeService::class.java.name
            }

        val metadata = service.serviceInfo.metaData

        assertNotNull(
            "Expected android.view.im metadata on GrammalecteImeService",
            metadata,
        )

        assertEquals(
            R.xml.grammalecte_input_method,
            metadata.getInt(INPUT_METHOD_METADATA),
        )
    }

    private companion object {
        const val INPUT_METHOD_ACTION = "android.view.InputMethod"
        const val INPUT_METHOD_METADATA = "android.view.im"
    }
}
