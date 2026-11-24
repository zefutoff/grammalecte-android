package fr.zefut.grammalecte

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.engine.GrammalecteDictionary
import fr.grammalecteandroid.engine.GrammalecteRuleOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuleSettingsScreenInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComposeUiTestHostActivity>()

    private val sampleRule =
        GrammalecteRuleOption(
            id = "test-rule",
            groupId = "test-group",
            groupLabel = "Grammaire",
            label = "Activer la règle",
            description = "Description de la règle.",
            enabled = false,
            defaultEnabled = true,
        )

    @Test
    fun dictionaryAndRuleActionsAreWired() {
        var dictionaryChosen: GrammalecteDictionary? = null
        var ruleChanged: Pair<String, Boolean>? = null

        composeRule.setContent {
            RuleSettingsScreen(
                dictionary = GrammalecteDictionary.ALL_VARIANTS,
                options = listOf(sampleRule),
                status = "1 règle",
                statusIsError = false,
                loaded = true,
                busy = false,
                onBack = {},
                onDictionary = { dictionaryChosen = it },
                onRule = { id, value -> ruleChanged = id to value },
                onManagePersonal = {},
                onResetRules = {},
                onResetAll = {},
                onExport = {},
                onImport = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithTag("rules-dictionary-all_variants").assertIsSelected()
        composeRule.onNodeWithTag("rules-dictionary-classic").performClick()
        composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
            hasTestTag("rule-option-test-rule"),
        )
        composeRule.onNodeWithTag("rule-option-test-rule").performClick()
        composeRule.runOnIdle {
            assertEquals(GrammalecteDictionary.CLASSIC, dictionaryChosen)
            assertEquals("test-rule" to true, ruleChanged)
        }
    }

    @Test
    fun fullResetRequiresConfirmationAndSupportsCancellation() {
        var resetCount = 0

        composeRule.setContent {
            RuleSettingsScreen(
                dictionary = GrammalecteDictionary.CLASSIC,
                options = listOf(sampleRule),
                status = "1 règle",
                statusIsError = false,
                loaded = true,
                busy = false,
                onBack = {},
                onDictionary = {},
                onRule = { _, _ -> },
                onManagePersonal = {},
                onResetRules = {},
                onResetAll = { resetCount++ },
                onExport = {},
                onImport = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
            hasTestTag("rules-reset-all"),
        )
        composeRule.onNodeWithTag("rules-reset-all").performClick()
        composeRule.onNodeWithText("Cette action est irréversible.", substring = true).assertExists()
        composeRule.onNodeWithText("Annuler").performClick()
        composeRule.onNodeWithText("Réinitialiser tous les réglages ?").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, resetCount) }

        composeRule.onNodeWithTag("rules-reset-all").performClick()
        composeRule.onNodeWithText("Réinitialiser", substring = false).performClick()
        composeRule.runOnIdle { assertEquals(1, resetCount) }
    }

    @Test
    fun busyScreenDisablesMutatingActions() {
        composeRule.setContent {
            RuleSettingsScreen(
                dictionary = GrammalecteDictionary.ALL_VARIANTS,
                options = listOf(sampleRule),
                status = "Importation des réglages…",
                statusIsError = false,
                loaded = true,
                busy = true,
                onBack = {},
                onDictionary = {},
                onRule = { _, _ -> },
                onManagePersonal = {},
                onResetRules = {},
                onResetAll = {},
                onExport = {},
                onImport = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithTag("rules-dictionary-classic").assertIsNotEnabled()
        composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
            hasTestTag("rule-option-test-rule"),
        )
        composeRule.onNodeWithTag("rule-option-test-rule").assertIsNotEnabled()
        composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
            hasTestTag("rules-import"),
        )
        composeRule.onNodeWithTag("rules-import").assertIsNotEnabled()
        composeRule.onNodeWithTag("rule-settings-screen").performScrollToNode(
            hasTestTag("rules-reset-all"),
        )
        composeRule.onNodeWithTag("rules-reset-all").assertIsNotEnabled()
    }
}
