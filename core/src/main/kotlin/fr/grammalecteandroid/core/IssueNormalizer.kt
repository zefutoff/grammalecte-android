package fr.grammalecteandroid.core

object IssueNormalizer {
    fun normalize(
        text: String,
        issues: Iterable<GrammarIssue>,
        suggestionLimit: Int,
    ): List<GrammarIssue> {
        require(suggestionLimit >= 0) { "suggestionLimit must be >= 0" }

        return issues
            .asSequence()
            .filter { issue -> issue.start < issue.endExclusive }
            .filter { issue -> issue.endExclusive <= text.length }
            .map { issue ->
                issue.copy(
                    suggestions = issue.suggestions
                        .asSequence()
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .distinct()
                        .take(suggestionLimit)
                        .toList(),
                )
            }
            .distinctBy { issue ->
                Triple(issue.start, issue.endExclusive, issue.ruleId ?: issue.kind.name)
            }
            .sortedWith(
                compareBy<GrammarIssue> { issue -> issue.start }
                    .thenBy { issue -> issue.endExclusive },
            )
            .toList()
    }
}
