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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.grammalecteandroid.engine.GrammalecteDictionary
import fr.grammalecteandroid.engine.GrammalecteRuleOption

@Composable
internal fun RuleSettingsScreen(
    dictionary: GrammalecteDictionary,
    options: List<GrammalecteRuleOption>,
    status: String,
    statusIsError: Boolean,
    loaded: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onDictionary: (GrammalecteDictionary) -> Unit,
    onRule: (String, Boolean) -> Unit,
    onManagePersonal: () -> Unit,
    onResetRules: () -> Unit,
    onResetAll: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
) {
    var confirmFullReset by rememberSaveable { mutableStateOf(false) }
    val enabled = loaded && !busy

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkPalette else lightPalette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .testTag("rule-settings-screen"),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                        }
                        Text(
                            "Réglages de correction",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .semantics { heading() }
                                    .testTag("rules-heading"),
                        )
                    }
                }
                item {
                    Text(
                        "Choisissez le dictionnaire et les règles Grammalecte à appliquer. " +
                            "Les réglages sont partagés par le correcteur Android, " +
                            "la correction d’une sélection et le clavier Grammalecte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    RulesSectionTitle("Dictionnaire")
                    Text(
                        "Choisissez les graphies françaises reconnues par le correcteur orthographique.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().selectableGroup().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DictionaryChoice(
                                label = "Toutes variantes",
                                description = "Accepte les graphies classiques et celles issues des rectifications de 1990.",
                                value = GrammalecteDictionary.ALL_VARIANTS,
                                selected = dictionary,
                                enabled = enabled,
                                onDictionary = onDictionary,
                            )
                            DictionaryChoice(
                                label = "Orthographe classique",
                                description = "Utilise les graphies traditionnelles.",
                                value = GrammalecteDictionary.CLASSIC,
                                selected = dictionary,
                                enabled = enabled,
                                onDictionary = onDictionary,
                            )
                            DictionaryChoice(
                                label = "Rectifications de 1990",
                                description = "Utilise les graphies issues des rectifications orthographiques de 1990.",
                                value = GrammalecteDictionary.REFORM_1990,
                                selected = dictionary,
                                enabled = enabled,
                                onDictionary = onDictionary,
                            )
                        }
                    }
                }
                item {
                    RulesSectionTitle("Dictionnaire personnel")
                    Text(
                        "Ajoutez vos noms propres, termes techniques ou mots absents du dictionnaire Grammalecte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onManagePersonal,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Gérer le dictionnaire personnel") }
                }
                item {
                    RulesSectionTitle("Règles de correction")
                    if (busy) {
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
                                        .testTag("rules-status"),
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
                                    .testTag("rules-status"),
                        )
                    }
                }
                if (!loaded && !busy) {
                    item {
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                            Text("Réessayer")
                        }
                    }
                }
                if (loaded) {
                    item {
                        OutlinedButton(
                            onClick = onResetRules,
                            enabled = enabled && options.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rules-reset-rules"),
                        ) { Text("Réinitialiser les règles") }
                    }
                }
                options.groupBy { it.groupId }.forEach { (groupId, group) ->
                    item(key = "group-$groupId") {
                        RulesSectionTitle(group.first().groupLabel)
                    }
                    items(group, key = { "rule-${it.id}" }) { option ->
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = option.enabled,
                                            enabled = enabled,
                                            role = Role.Switch,
                                            onValueChange = { onRule(option.id, it) },
                                        ).testTag("rule-option-${option.id}")
                                        .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        buildString {
                                            if (option.description.isNotBlank()) {
                                                append(option.description)
                                                append("\n")
                                            }
                                            append("Par défaut : ")
                                            append(if (option.defaultEnabled) "activé" else "désactivé")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = option.enabled,
                                    onCheckedChange = null,
                                    enabled = enabled,
                                )
                            }
                        }
                    }
                }
                item {
                    RulesSectionTitle("Importation et exportation")
                    Text(
                        "Sauvegarde le dictionnaire principal sélectionné et les réglages des règles Grammalecte. " +
                            "Le dictionnaire personnel n’est jamais inclus.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onExport,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rules-export"),
                    ) { Text("Exporter les réglages") }
                }
                item {
                    OutlinedButton(
                        onClick = onImport,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rules-import"),
                    ) { Text("Importer des réglages") }
                }
                item {
                    RulesSectionTitle("Réinitialisation")
                    Text(
                        "Restaure le dictionnaire principal, les règles Grammalecte et vide le dictionnaire personnel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { confirmFullReset = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rules-reset-all"),
                    ) { Text("Réinitialiser tous les réglages") }
                }
            }
        }

        if (confirmFullReset) {
            AlertDialog(
                onDismissRequest = { confirmFullReset = false },
                title = { Text("Réinitialiser tous les réglages ?") },
                text = {
                    Text(
                        "Le dictionnaire principal reviendra à « Toutes variantes », " +
                            "les règles Grammalecte retrouveront leurs valeurs par défaut " +
                            "et tous les mots du dictionnaire personnel seront supprimés.\n\n" +
                            "Cette action est irréversible.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmFullReset = false
                            onResetAll()
                        },
                    ) {
                        Text("Réinitialiser", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmFullReset = false }) {
                        Text("Annuler")
                    }
                },
            )
        }
    }
}

@Composable
private fun RulesSectionTitle(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun DictionaryChoice(
    label: String,
    description: String,
    value: GrammalecteDictionary,
    selected: GrammalecteDictionary,
    enabled: Boolean,
    onDictionary: (GrammalecteDictionary) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected == value,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { if (value != selected) onDictionary(value) },
                ).testTag("rules-dictionary-${value.preferenceValue}")
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected == value,
            onClick = null,
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
