package fr.zefut.grammalecte

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp

@Composable
internal fun CorrectionStatusScreen(
    status: CorrectionStatus?,
    onBack: () -> Unit,
    onConfigureSpellChecker: () -> Unit,
    onManageKeyboards: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                    Text(
                        "État de la correction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).semantics { heading() }.testTag("status-heading"),
                    )
                }
                Text(
                    "État détecté par Android, actualisé au retour des paramètres système.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val spellChecker = status?.spellChecker
                val spellMessage =
                    when (spellChecker) {
                        SpellCheckerStatus.SELECTED -> "Activé et sélectionné"
                        SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE ->
                            "Grammalecte est sélectionné, mais la session française est indisponible. " +
                                "Dans les réglages Android, choisissez Français plutôt qu’Automatique."
                        SpellCheckerStatus.NOT_SELECTED -> "Disponible, mais non sélectionné"
                        SpellCheckerStatus.DISABLED -> "Correction Android désactivée"
                        SpellCheckerStatus.UNAVAILABLE -> "Aucun correcteur actif détecté"
                        null -> "Vérification en cours…"
                    }

                StatusSection(
                    title = "Correcteur Android",
                    description = "Correction native dans les applications compatibles.",
                    message = spellMessage,
                    active = spellChecker == SpellCheckerStatus.SELECTED,
                    tag = "status-spell-checker",
                ) {
                    Button(
                        onClick = onConfigureSpellChecker,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Configurer le correcteur Android")
                    }
                }

                val ime = status?.ime
                val imeMessage =
                    when (ime) {
                        ImeStatus.SELECTED -> "Activé et actuellement sélectionné"
                        ImeStatus.ENABLED -> "Activé, mais un autre clavier est sélectionné"
                        ImeStatus.DISABLED -> "Non activé dans les méthodes de saisie"
                        null -> "Vérification en cours…"
                    }

                StatusSection(
                    title = "Clavier Grammalecte",
                    description = "Correction d’une sélection quand le correcteur Android n’est pas utilisé.",
                    message = imeMessage,
                    active = ime == ImeStatus.SELECTED,
                    tag = "status-keyboard",
                    keyboard = true,
                ) {
                    OutlinedButton(
                        onClick = onManageKeyboards,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Gérer les méthodes de saisie")
                    }
                    OutlinedButton(
                        onClick = onChooseKeyboard,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Choisir le clavier")
                    }
                }

                Text(
                    "Ces deux modes sont indépendants. Vous pouvez activer l’un, l’autre ou les deux.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusSection(
    title: String,
    description: String,
    message: String,
    active: Boolean,
    tag: String,
    keyboard: Boolean = false,
    actions: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector =
                        if (keyboard) {
                            Icons.Outlined.Keyboard
                        } else if (active) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.Info
                        },
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag(tag),
            )
            actions()
        }
    }
}
