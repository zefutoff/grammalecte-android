package fr.zefut.grammalecte

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** Refresh diagnostics after a keyboard switch, even if the activity keeps window focus. */
internal class KeyboardSelectionObserver(
    context: Context,
    private val onChanged: () -> Unit,
) : DefaultLifecycleObserver {
    private val resolver = context.applicationContext.contentResolver
    private var registered = false
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (registered) {
                    onChanged()
                }
            }
        }

    override fun onStart(owner: LifecycleOwner) {
        registered =
            runCatching {
                resolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
                    false,
                    observer,
                )
            }.isSuccess
    }

    override fun onStop(owner: LifecycleOwner) {
        if (registered) {
            registered = false
            resolver.unregisterContentObserver(observer)
        }
    }
}
