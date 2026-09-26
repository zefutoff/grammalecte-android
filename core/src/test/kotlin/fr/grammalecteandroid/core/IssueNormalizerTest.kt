package fr.grammalecteandroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueNormalizerTest {
    @Test
    fun `normalization removes invalid ranges and deduplicates suggestions`() {
        val text = "Les serveur sont installer"
        val issues = listOf(
            GrammarIssue(4, 11, listOf("serveurs", " serveurs ", ""), ruleId = "R1"),
            GrammarIssue(4, 11, listOf("serveurs"), ruleId = "R1"),
            GrammarIssue(17, 26, listOf("installés", "installées"), ruleId = "R2"),
            GrammarIssue(999, 1_000, listOf("invalid"), ruleId = "R3"),
        )

        val normalized = IssueNormalizer.normalize(text, issues, suggestionLimit = 1)

        assertEquals(2, normalized.size)
        assertEquals(listOf("serveurs"), normalized[0].suggestions)
        assertEquals(listOf("installés"), normalized[1].suggestions)
    }

    @Test
    fun `UTF-16 offsets remain valid when an emoji precedes the issue`() {
        val text = "🙂 les serveur"
        val start = text.indexOf("serveur")
        val issue = GrammarIssue(start, start + "serveur".length, listOf("serveurs"))

        val normalized = IssueNormalizer.normalize(text, listOf(issue), suggestionLimit = 3)

        assertEquals(1, normalized.size)
        assertEquals("serveur", text.substring(normalized.single().start, normalized.single().endExclusive))
        assertTrue(start > 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative suggestion limit is rejected`() {
        IssueNormalizer.normalize("texte", emptyList(), suggestionLimit = -1)
    }
}
