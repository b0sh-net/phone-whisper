package net.b0sh.audiotext.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Schermata principale Compose. La pagina è fissa: solo il box dei modelli
 * installati scorre internamente (weight), così i due bottoni restano sempre
 * sopra la barra di navigazione (riproduce il vecchio `computeCaps`).
 */
@Composable
fun MainScreen(
    state: MainScreenUiState,
    onSelectModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<InstalledModelRow?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
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

        // Internally-scrolling list of installed models (page stays fixed)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(state.installedModels, key = { it.id }) { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModel(row.id) }
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
                        content = { Text("🗑") },
                    )
                    RadioButton(
                        selected = row.isActive,
                        onClick = { onSelectModel(row.id) },
                    )
                }
            }
        }

        // Bottom buttons, pinned above the navigation bar
        Button(
            onClick = onOpenCatalog,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            content = { Text(stringResource(R.string.models_download_button)) },
        )
        OutlinedButton(
            onClick = onOpenAbout,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            content = { Text(stringResource(R.string.about_button)) },
        )
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