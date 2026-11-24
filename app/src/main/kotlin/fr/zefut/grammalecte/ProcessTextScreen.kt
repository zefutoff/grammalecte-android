package fr.zefut.grammalecte

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fr.grammalecteandroid.core.GrammarIssue

/**
 * Carries the original analysis snapshot with each suggestion. A delayed click can never
 * apply a suggestion to text from a different revision.
 */
internal data class ProcessTextIssuePresentation(
    val analyzedText: String,
    val analyzedRevision: Long,
    val issue: GrammarIssue,
    val fragment: String,
)

@Composable
internal fun ProcessTextScreen(
    text: TextFieldValue,
    readOnlySelection: Boolean,
    status: String,
    statusIsError: Boolean,
    analyzing: Boolean,
    issues: List<ProcessTextIssuePresentation>,
    onBack: () -> Unit,
    onTextChange: (TextFieldValue) -> Unit,
    onAnalyze: () -> Unit,
    onSuggestion: (ProcessTextIssuePresentation, String) -> Unit,
    onCopy: () -> Unit,
    onReplace: () -> Unit,
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkPalette else lightPalette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .testTag("process-text-screen"),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Retour",
                            )
                        }
                        Text(
                            "Corriger avec Grammalecte",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .semantics { heading() }
                                    .testTag("process-text-heading"),
                        )
                    }
                }
                item {
                    Text(
                        if (readOnlySelection) {
                            "Cette application ne permet pas de remplacer la sélection. " +
                                "Vous pouvez corriger le texte puis le copier."
                        } else {
                            "Modifiez le texte ou appliquez une proposition, puis " +
                                "renvoyez la correction à l’application d’origine."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        label = { Text("Texte sélectionné") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("process-text-editor"),
                    )
                }
                item {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("process-text-analyze"),
                    ) { Text("Analyser") }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (analyzing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (statusIsError) {
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    status,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .semantics { liveRegion = LiveRegionMode.Polite }
                                            .testTag("process-text-status"),
                                )
                            }
                        } else {
                            Text(
                                status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier
                                        .semantics { liveRegion = LiveRegionMode.Polite }
                                        .testTag("process-text-status"),
                            )
                        }
                    }
                }
                if (issues.isNotEmpty()) {
                    item {
                        Text(
                            "Propositions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                }
                itemsIndexed(issues, key = { index, _ -> "issue-$index" }) { index, entry ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("process-text-issue-$index"),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "« ${entry.fragment} »",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            entry.issue.message?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (entry.issue.suggestions.isEmpty()) {
                                Text(
                                    "Aucune proposition.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                entry.issue.suggestions.take(5).forEach { suggestion ->
                                    OutlinedButton(
                                        onClick = { onSuggestion(entry, suggestion) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    ) { Text(suggestion) }
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = if (readOnlySelection) onCopy else onReplace,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("process-text-primary-action"),
                    ) {
                        Text(if (readOnlySelection) "Copier et revenir" else "Remplacer le texte sélectionné")
                    }
                }
                if (!readOnlySelection) {
                    item {
                        OutlinedButton(
                            onClick = onCopy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("process-text-copy"),
                        ) { Text("Copier et revenir") }
                    }
                }
            }
        }
    }
}
