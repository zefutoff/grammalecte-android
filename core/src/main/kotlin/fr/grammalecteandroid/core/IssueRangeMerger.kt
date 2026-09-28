package fr.grammalecteandroid.core

object IssueRangeMerger {
    fun merge(
        issues: Iterable<GrammarIssue>,
        suggestionLimit: Int,
    ): List<GrammarIssue> {
        require(suggestionLimit >= 0) { "suggestionLimit must be >= 0" }

        return issues
            .groupBy { issue -> issue.start to issue.endExclusive }
            .values
            .map { sameRange ->
                val primary = sameRange.maxBy { issue -> issue.kind.priority() }
                val ordered = listOf(primary) + sameRange.filterNot { issue -> issue === primary }
                primary.copy(
                    suggestions =
                        ordered
                            .asSequence()
                            .flatMap { issue -> issue.suggestions.asSequence() }
                            .map(String::trim)
                            .filter(String::isNotEmpty)
                            .distinct()
                            .take(suggestionLimit)
                            .toList(),
                    message = sameRange.firstNotNullOfOrNull { issue -> issue.message },
                )
            }.sortedWith(compareBy<GrammarIssue> { it.start }.thenBy { it.endExclusive })
    }

    private fun IssueKind.priority(): Int =
        when (this) {
            IssueKind.GRAMMAR -> 4
            IssueKind.TYPOGRAPHY -> 3
            IssueKind.SPELLING -> 2
            IssueKind.UNKNOWN -> 1
        }
}
