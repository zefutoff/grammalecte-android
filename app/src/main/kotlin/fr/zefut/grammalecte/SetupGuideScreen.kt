package fr.zefut.grammalecte

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SetupGuideScreen(
    step: Int,
    status: CorrectionStatus?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLeave: () -> Unit,
    onSpellCheckerSettings: () -> Unit,
    onKeyboardSettings: () -> Unit,
    onChooseKeyboard: () -> Unit,
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkPalette else lightPalette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Configurer Grammalecte",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Étape ${step + 1}/3",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("setup-progress"),
                )
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (step) {
                            0 -> {
                                GuideTitle("Correcteur Android")
                                Text(
                                    "Recommandé. Dans les réglages, activez la correction puis " +
                                        "choisissez Grammalecte et Français.",
                                )
                                val message =
                                    when (status?.spellChecker) {
                                        SpellCheckerStatus.SELECTED -> "Activé — prêt à corriger."
                                        SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE ->
                                            "À compléter — choisissez Français."
                                        SpellCheckerStatus.NOT_SELECTED ->
                                            "À activer — choisissez Grammalecte."
                                        SpellCheckerStatus.DISABLED ->
                                            "À activer — correction Android désactivée."
                                        SpellCheckerStatus.UNAVAILABLE ->
                                            "Aucun correcteur Android actif détecté."
                                        null -> "Vérification en cours…"
                                    }
                                GuideStatus(message, status?.spellChecker == SpellCheckerStatus.SELECTED)
                                Button(
                                    onClick = onSpellCheckerSettings,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text("Réglages du correcteur") }
                            }
                            1 -> {
                                GuideTitle("Corriger une sélection")
                                Text(
                                    "Sélectionnez du texte, puis choisissez " +
                                        "« Corriger avec Grammalecte » dans le menu.",
                                )
                                GuideStatus("Prêt — aucun réglage nécessaire.", true)
                            }
                            else -> {
                                GuideTitle("Clavier Grammalecte · facultatif")
                                Text(
                                    "Pour corriger une sélection lorsque la correction " +
                                        "native n’est pas disponible.",
                                )
                                val message =
                                    when (status?.ime) {
                                        ImeStatus.SELECTED -> "Activé — clavier Grammalecte sélectionné."
                                        ImeStatus.ENABLED -> "Activé — un autre clavier est utilisé."
                                        ImeStatus.DISABLED -> "Non activé — facultatif."
                                        null -> "Vérification en cours…"
                                    }
                                GuideStatus(message, status?.ime != ImeStatus.DISABLED && status != null)
                                OutlinedButton(
                                    onClick = onKeyboardSettings,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text("Gérer les claviers") }
                                OutlinedButton(
                                    onClick = onChooseKeyboard,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text("Choisir le clavier") }
                            }
                        }
                    }
                }
                if (step == 2) {
                    Text(
                        "Corrections locales, sans connexion Internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (step > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Précédent") }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        when {
                            step == 2 -> "Terminer"
                            step == 0 && status?.spellChecker != SpellCheckerStatus.SELECTED -> "Passer"
                            else -> "Suivant"
                        },
                    )
                }
                if (step < 2) {
                    TextButton(
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Plus tard") }
                }
            }
        }
    }
}

@Composable
private fun GuideTitle(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun GuideStatus(
    message: String,
    ready: Boolean,
) {
    Text(
        message,
        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        modifier =
            Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("setup-status"),
    )
}
