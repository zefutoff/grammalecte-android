package fr.grammalecteandroid.unofficial

import android.content.Context

class SetupGuidePreferences(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun shouldShowAutomatically(): Boolean =
        !preferences.getBoolean(
            KEY_GUIDE_SHOWN_V1,
            false,
        )

    fun markShown() {
        preferences
            .edit()
            .putBoolean(
                KEY_GUIDE_SHOWN_V1,
                true,
            ).apply()
    }

    private companion object {
        const val PREFERENCES_NAME =
            "setup_guide"

        const val KEY_GUIDE_SHOWN_V1 =
            "setup_guide_shown_v1"
    }
}
