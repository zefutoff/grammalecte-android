package fr.grammalecteandroid.engine

import android.content.Context

class GrammalectePersonalDictionaryPreferences(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun words(): List<String> =
        preferences
            .getStringSet(
                KEY_WORDS,
                emptySet(),
            ).orEmpty()
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filter { word ->
                word.length <= MAX_WORD_LENGTH &&
                    word.none(Char::isWhitespace)
            }.distinct()
            .sorted()
            .toList()

    fun replace(words: Collection<String>) {
        preferences
            .edit()
            .putStringSet(
                KEY_WORDS,
                words.toSet(),
            ).apply()
    }

    fun reset() {
        preferences
            .edit()
            .remove(KEY_WORDS)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME =
            "grammalecte_personal_dictionary"

        const val KEY_WORDS =
            "words"

        const val MAX_WORD_LENGTH =
            64
    }
}
