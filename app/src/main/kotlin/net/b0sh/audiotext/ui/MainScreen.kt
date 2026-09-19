package net.b0sh.audiotext.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Riga di un modello installato mostrato nel box della schermata principale. */
data class InstalledModelRow(
    val id: String,
    val name: String,
    val sizeText: String,
    val sizeMb: Int,
    val isActive: Boolean,
)

/** Stato UI della schermata principale, derivato direttamente in MainActivity. */
data class MainScreenUiState(
    val infoText: String,
    val statusText: String,
    @DrawableRes val statusIconRes: Int,
    val installedModels: List<InstalledModelRow>,
)

/**
 * Schermata principale Compose. Titolo ("Audio To Text") e testo informativo
 * restano fissi; il blocco "Stato" + "modelli installati" scorre verticalmente,
 * così la schermata funziona anche in orizzontale (spazio verticale ridotto).
 */
@Composable
fun MainScreen(
    state: MainScreenUiState,
    switchingModelId: String?,
    onSelectModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<InstalledModelRow?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        // Header
        Text(
            text = "Audio To Text",
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )

        // Informational section
        Text(
            text = state.infoText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Area scrollabile: Stato + modelli installati. Titolo e testo
        // informativo restano fissi anche in orizzontale.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // Status row: icon + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                statusIcon(state.statusIconRes)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(stringResource(R.string.status_label), fontSize = 18.sp)
                    Text(
                        state.statusText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Installed models header
            Text(
                text = stringResource(R.string.section_local_models_available),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            // Installed models: scorrono insieme allo stato
            state.installedModels.forEach { row ->
                // Durante un cambio di modello tutti i selettori mostrano
                // l'ora (hourglass) e i tocchi sul cambio vengono ignorati.
                val switching = switchingModelId != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !switching) {
                            onSelectModel(row.id)
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, fontSize = 18.sp)
                        Text(
                            row.sizeText,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { pendingDelete = row },
                        content = {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        },
                    )
                    selectIcon(
                        switching = switching,
                        active = row.isActive,
                        onClick = { onSelectModel(row.id) },
                    )
                }
            }
        }
    }

    // Deletion confirmation dialog
    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_model_title)) },
            text = { Text(stringResource(R.string.delete_model_message, deleting.name, deleting.sizeMb)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeleteModel(deleting.id)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun statusIcon(@DrawableRes iconRes: Int) {
    val painter: Painter? = painterResource(iconRes)
    if (painter != null) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
        )
    }
}

/**
 * Selettore del modello attivo nella schermata Home. Normalmente mostra il
 * RadioButton (pallino) che evidenzia il modello selezionato; mentre un cambio
 * di modello è in corso ([switching] == true) lo sostituisce con l'icona
 * "hourglass_pause" e disabilita il tocco, finché l'operazione non termina.
 */
@Composable
private fun selectIcon(
    switching: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    if (switching) {
        Icon(
            painter = painterResource(R.drawable.ic_hourglass_pause),
            contentDescription = stringResource(R.string.action_switching_model),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    } else {
        RadioButton(
            selected = active,
            onClick = onClick,
        )
    }
}
