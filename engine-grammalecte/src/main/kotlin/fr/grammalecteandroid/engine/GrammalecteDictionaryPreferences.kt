package fr.grammalecteandroid.engine

import android.content.Context

class GrammalecteDictionaryPreferences(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun selected(): GrammalecteDictionary =
        GrammalecteDictionary.fromPreferenceValue(
            preferences.getString(
                KEY_DICTIONARY,
                null,
            ),
        )

    fun select(dictionary: GrammalecteDictionary) {
        preferences
            .edit()
            .putString(
                KEY_DICTIONARY,
                dictionary.preferenceValue,
            ).apply()
    }

    fun reset() {
        preferences
            .edit()
            .remove(KEY_DICTIONARY)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME =
            "grammalecte_dictionary"

        const val KEY_DICTIONARY =
            "selected_dictionary"
    }
}
