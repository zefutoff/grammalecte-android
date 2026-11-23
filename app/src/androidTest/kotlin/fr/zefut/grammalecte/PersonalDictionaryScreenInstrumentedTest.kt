package fr.zefut.grammalecte

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalDictionaryScreenInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun addAndRemoveActionsAreWired() {
        var entered = ""
        var added = 0
        var removed = ""

        composeRule.setContent {
            PersonalDictionaryScreen(
                input = entered,
                words = listOf("grammalecte"),
                status = "1 mot",
                busy = false,
                onInputChange = { entered = it },
                onAdd = { added++ },
                onRemove = { removed = it },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Ajouter").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Supprimer").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(1, added)
            assertEquals("grammalecte", removed)
        }
    }

    @Test
    fun busyStateDisablesEditingAndActions() {
        composeRule.setContent {
            PersonalDictionaryScreen(
                input = "",
                words = listOf("grammalecte"),
                status = "Chargement…",
                busy = true,
                onInputChange = {},
                onAdd = {},
                onRemove = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("dictionary-input").assertIsNotEnabled()
        composeRule.onNodeWithText("Ajouter").assertIsNotEnabled()
        composeRule.onNodeWithText("Supprimer").assertIsNotEnabled()
        composeRule.onNodeWithTag("dictionary-status").assertTextContains("Chargement")
    }
}
