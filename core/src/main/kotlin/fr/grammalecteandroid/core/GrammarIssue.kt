package fr.grammalecteandroid.core

enum class IssueKind {
    SPELLING,
    GRAMMAR,
    TYPOGRAPHY,
    UNKNOWN,
}

data class GrammarIssue(
    val start: Int,
    val endExclusive: Int,
    val suggestions: List<String>,
    val message: String? = null,
    val ruleId: String? = null,
    val kind: IssueKind = IssueKind.UNKNOWN,
) {
    init {
        require(start >= 0) { "start must be >= 0" }
        require(endExclusive >= start) { "endExclusive must be >= start" }
    }

    val length: Int
        get() = endExclusive - start
}
