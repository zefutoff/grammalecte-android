package fr.grammalecteandroid.engine

import android.content.Context

class GrammalecteRulePreferences(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun overrides(): Map<String, Boolean> =
        preferences.all
            .mapNotNull { (id, value) ->
                (value as? Boolean)?.let { enabled ->
                    id to enabled
                }
            }.toMap()

    fun setRuleOption(
        id: String,
        enabled: Boolean,
    ) {
        require(id.isNotBlank()) {
            "Grammalecte rule option id must not be blank"
        }

        preferences
            .edit()
            .putBoolean(id, enabled)
            .apply()
    }

    fun reset() {
        preferences
            .edit()
            .clear()
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME =
            "grammalecte_rule_options"
    }
}
