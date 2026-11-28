package fr.zefut.grammalecte

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.core.GrammarIssue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextScreenInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComposeUiTestHostActivity>()

    private val example =
        ProcessTextIssuePresentation(
            analyzedText = "Je vais au magazin.",
            analyzedRevision = 7L,
            issue =
                GrammarIssue(
                    start = 11,
                    endExclusive = 18,
                    suggestions = listOf("magasin", "magasins"),
                    message = "Vérifiez l'orthographe.",
                ),
            fragment = "magazin",
        )

    @Test
    fun suggestionActionUsesOriginalSnapshot() {
        var chosen: Pair<ProcessTextIssuePresentation, String>? = null

        composeRule.setContent {
            ProcessTextScreen(
                text = TextFieldValue(example.analyzedText),
                readOnlySelection = false,
                status = "1 erreur détectée.",
                statusIsError = false,
                analyzing = false,
                issues = listOf(example),
                onBack = {},
                onTextChange = {},
                onAnalyze = {},
                onSuggestion = { entry, suggestion -> chosen = entry to suggestion },
                onCopy = {},
                onReplace = {},
            )
        }

        composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
            hasText("magasin"),
        )
        composeRule.onNodeWithText("magasin").performClick()

        composeRule.runOnIdle {
            assertEquals(example to "magasin", chosen)
        }
    }

    @Test
    fun readOnlyHostOffersCopyButNoReplacement() {
        var copied = 0
        var replaced = 0

        composeRule.setContent {
            ProcessTextScreen(
                text = TextFieldValue("Texte en lecture seule"),
                readOnlySelection = true,
                status = "Aucune erreur détectée.",
                statusIsError = false,
                analyzing = false,
                issues = emptyList(),
                onBack = {},
                onTextChange = {},
                onAnalyze = {},
                onSuggestion = { _, _ -> },
                onCopy = { copied++ },
                onReplace = { replaced++ },
            )
        }

        composeRule.onNodeWithTag("process-text-screen").performScrollToNode(
            hasTestTag("process-text-primary-action"),
        )
        composeRule.onNodeWithTag("process-text-primary-action").performClick()
        composeRule.onNodeWithText("Remplacer le texte sélectionné").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, copied)
            assertEquals(0, replaced)
        }
    }

    @Test
    fun errorStatusHasAccessibleLiveAnnouncement() {
        composeRule.setContent {
            ProcessTextScreen(
                text = TextFieldValue(""),
                readOnlySelection = false,
                status = "Erreur pendant l’analyse : impossible de vérifier.",
                statusIsError = true,
                analyzing = false,
                issues = emptyList(),
                onBack = {},
                onTextChange = {},
                onAnalyze = {},
                onSuggestion = { _, _ -> },
                onCopy = {},
                onReplace = {},
            )
        }

        composeRule.onNodeWithTag("process-text-status").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.LiveRegion,
                androidx.compose.ui.semantics.LiveRegionMode.Polite,
            ),
        )
        composeRule.onNodeWithTag("process-text-status").assert(
            hasText("Erreur pendant l’analyse : impossible de vérifier."),
        )
    }
}
