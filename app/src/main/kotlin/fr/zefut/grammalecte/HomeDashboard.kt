package fr.zefut.grammalecte

import android.widget.EditText
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

internal data class HomeCorrectionIssue(
    val word: String,
    val suggestions: List<String>,
)

private val lightPalette =
    lightColorScheme(
        primary = Color(0xFF5342B6),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE7E1FF),
        onPrimaryContainer = Color(0xFF251758),
        background = Color(0xFFFCFBFF),
        onBackground = Color(0xFF191923),
        surface = Color(0xFFFCFBFF),
        surfaceVariant = Color(0xFFF0EEF7),
        onSurface = Color(0xFF191923),
        onSurfaceVariant = Color(0xFF555461),
        outlineVariant = Color(0xFFDAD8E5),
    )

private val darkPalette =
    darkColorScheme(
        primary = Color(0xFFC9BCFF),
        onPrimary = Color(0xFF32216C),
        primaryContainer = Color(0xFF45338E),
        onPrimaryContainer = Color(0xFFF1ECFF),
        background = Color(0xFF111218),
        onBackground = Color(0xFFF2F0F8),
        surface = Color(0xFF111218),
        surfaceVariant = Color(0xFF262730),
        onSurface = Color(0xFFF2F0F8),
        onSurfaceVariant = Color(0xFFC7C4D1),
        outlineVariant = Color(0xFF41414C),
    )

@Composable
internal fun GrammalecteDashboard(
    status: CorrectionStatus?,
    showTestResult: Boolean,
    testPending: Boolean,
    testError: String?,
    testIssues: List<HomeCorrectionIssue>,
    diagnosticsVisible: Boolean,
    engineReady: Boolean,
    systemEditText: EditText,
    resultView: TextView,
    onTest: () -> Unit,
    onDismissTest: () -> Unit,
    onConfigure: () -> Unit,
    onGuide: () -> Unit,
    onChooseKeyboard: () -> Unit,
    onManageKeyboards: () -> Unit,
    onDictionary: () -> Unit,
    onRules: () -> Unit,
    onStatus: () -> Unit,
    onDiagnostics: () -> Unit,
    onTestInField: () -> Unit,
    onInspect: () -> Unit,
    onQuickTile: () -> Unit,
    aboutDescription: String,
    onProjectWebsite: () -> Unit,
) {
    val colors = if (isSystemInDarkTheme()) darkPalette else lightPalette
    var aboutVisible by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grammalecte",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() }.testTag("main-heading"),
                        )
                        Text(
                            text = "Votre correcteur français, hors ligne",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(onClick = onRules, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Réglages de correction",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                CorrectionOverview(status = status)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onTest,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Tester")
                    }
                    OutlinedButton(
                        onClick = onConfigure,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Configurer")
                    }
                }

                AnimatedVisibility(visible = showTestResult) {
                    TestResultCard(
                        pending = testPending,
                        error = testError,
                        issues = testIssues,
                        onDismiss = onDismissTest,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Accès rapides",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val compact =
                        LocalConfiguration.current.fontScale >= 1.5f ||
                            LocalConfiguration.current.screenWidthDp < 360
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Shortcut("Guide de démarrage", Icons.AutoMirrored.Outlined.MenuBook, onGuide)
                            Shortcut("Clavier", Icons.Outlined.Keyboard, onChooseKeyboard)
                            Shortcut("Dictionnaire", Icons.Outlined.Book, onDictionary)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                Shortcut("Guide de démarrage", Icons.AutoMirrored.Outlined.MenuBook, onGuide)
                            }
                            Box(Modifier.weight(1f)) {
                                Shortcut("Clavier", Icons.Outlined.Keyboard, onChooseKeyboard)
                            }
                            Box(Modifier.weight(1f)) {
                                Shortcut("Dictionnaire", Icons.Outlined.Book, onDictionary)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paramètres et outils",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    ) {
                        DashboardRow("Préférences de correction", Icons.Outlined.Tune, onRules)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DashboardRow("État des services", Icons.Outlined.CheckCircle, onStatus)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DashboardRow("Outils et diagnostics", Icons.Outlined.BugReport, onDiagnostics)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DashboardRow("À propos", Icons.Outlined.Info) { aboutVisible = true }
                    }
                }

                AnimatedVisibility(visible = diagnosticsVisible) {
                    DiagnosticsPanel(
                        engineReady = engineReady,
                        editText = systemEditText,
                        resultView = resultView,
                        onTest = onTest,
                        onManageKeyboards = onManageKeyboards,
                        onTestInField = onTestInField,
                        onInspect = onInspect,
                        onQuickTile = onQuickTile,
                    )
                }

                Text(
                    "Aucun texte n’est transmis sur Internet.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (aboutVisible) {
            AlertDialog(
                onDismissRequest = { aboutVisible = false },
                title = { Text("À propos de Grammalecte Android") },
                text = { Text(aboutDescription) },
                confirmButton = {
                    TextButton(onClick = {
                        aboutVisible = false
                        onProjectWebsite()
                    }) {
                        Text("Site du projet")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { aboutVisible = false }) { Text("Fermer") }
                },
            )
        }
    }
}

@Composable
private fun CorrectionOverview(status: CorrectionStatus?) {
    val ready = status?.spellChecker == SpellCheckerStatus.SELECTED
    val indicator =
        if (ready) {
            if (isSystemInDarkTheme()) Color(0xFFA7F3C1) else Color(0xFF176A37)
        } else {
            MaterialTheme.colorScheme.primary
        }
    val title =
        when (status?.spellChecker) {
            SpellCheckerStatus.SELECTED -> "Votre correcteur est prêt"
            SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE -> "Choisir la langue française"
            SpellCheckerStatus.DISABLED -> "Activer la correction Android"
            SpellCheckerStatus.NOT_SELECTED -> "Sélectionner Grammalecte"
            SpellCheckerStatus.UNAVAILABLE -> "Correcteur non disponible"
            null -> "Vérification du correcteur…"
        }
    val details =
        when (status?.ime) {
            ImeStatus.SELECTED -> "Clavier Grammalecte sélectionné"
            ImeStatus.ENABLED -> "Clavier facultatif disponible"
            ImeStatus.DISABLED, null -> "Clavier Grammalecte facultatif"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "CORRECTEUR ANDROID",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("main-status"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = indicator,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("keyboard-status"),
            )
        }
    }
}

@Composable
private fun Shortcut(
    label: String,
    image: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashboardRow(
    label: String,
    image: ImageVector,
    action: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = action)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun TestResultCard(
    pending: Boolean,
    error: String?,
    issues: List<HomeCorrectionIssue>,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Résultat du test",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("Fermer") }
            }
            Text(
                "« Je sui aller au magazin hier. »",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                pending -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Analyse en cours…")
                    }
                }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                issues.isEmpty() -> Text("Aucune correction proposée par Android.")
                else -> {
                    issues.forEach { issue ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("« ${issue.word} »", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (issue.suggestions.isEmpty()) {
                                    "Aucune suggestion"
                                } else {
                                    "Suggestions : ${issue.suggestions.take(4).joinToString(", ")}"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsPanel(
    engineReady: Boolean,
    editText: EditText,
    resultView: TextView,
    onTest: () -> Unit,
    onManageKeyboards: () -> Unit,
    onTestInField: () -> Unit,
    onInspect: () -> Unit,
    onQuickTile: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Diagnostics avancés", style = MaterialTheme.typography.titleMedium)
            Text(
                if (engineReady) {
                    "Moteur Grammalecte embarqué : prêt."
                } else {
                    "Moteur Grammalecte non embarqué."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                Text("Tester Grammalecte")
            }
            OutlinedButton(onClick = onManageKeyboards, modifier = Modifier.fillMaxWidth()) {
                Text("Gérer les claviers")
            }
            OutlinedButton(onClick = onQuickTile, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter la tuile")
            }
            AndroidView(
                factory = { editText },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            OutlinedButton(onClick = onTestInField, modifier = Modifier.fillMaxWidth()) {
                Text("Tester dans un champ Android")
            }
            OutlinedButton(onClick = onInspect, modifier = Modifier.fillMaxWidth()) {
                Text("Inspecter les corrections Android")
            }
            AndroidView(
                factory = { resultView },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
