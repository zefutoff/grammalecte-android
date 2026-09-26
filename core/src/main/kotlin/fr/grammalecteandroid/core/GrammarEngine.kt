package fr.grammalecteandroid.core

data class WordCheck(
    val valid: Boolean,
    val suggestions: List<String> = emptyList(),
)

interface GrammarEngine : AutoCloseable {
    fun check(
        text: String,
        localeTag: String = "fr-FR",
    ): List<GrammarIssue>

    fun checkWord(
        word: String,
        localeTag: String = "fr-FR",
        suggestionLimit: Int = 8,
    ): WordCheck

    override fun close() = Unit
}
