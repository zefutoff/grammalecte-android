package fr.grammalecteandroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IssueRangeMergerTest {
    @Test
    fun `same range is merged and global suggestion limit is respected`() {
        val merged = IssueRangeMerger.merge(
            issues = listOf(
                GrammarIssue(4, 11, listOf("serveurs", "service"), kind = IssueKind.SPELLING),
                GrammarIssue(4, 11, listOf("serveurs", "serveur"), kind = IssueKind.GRAMMAR),
            ),
            suggestionLimit = 2,
        )

        assertEquals(1, merged.size)
        assertEquals(IssueKind.GRAMMAR, merged.single().kind)
        assertEquals(listOf("serveurs", "serveur"), merged.single().suggestions)
    }
}
