package net.b0sh.audiotext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Riga di una card del catalogo (modello ancora da installare). */
data class CatalogModelRow(
    val id: String,
    val name: String,
    val sizeText: String,
)

/** Fase di download di una singola card. */
sealed class DownloadPhase {
    object Idle : DownloadPhase()
    data class Downloading(val progress: Float, val currentFile: String?) : DownloadPhase()
    data class Extracting(val currentFile: String) : DownloadPhase()
}

/** Stato UI del catalogo, derivato in MainActivity. */
data class CatalogScreenState(
    val rows: List<CatalogModelRow>,
    val downloadPhases: Map<String, DownloadPhase>,
)

/**
 * Catalogo modelli Compose: una card per modello ancora da installare, con
 * download e barra di avanzamento. La pagina scorre nel suo insieme; quando
 * tutto è installato mostra lo stato vuoto.
 */
@Composable
fun ModelCatalogScreen(
    state: CatalogScreenState,
    onDownload: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Title
        Text(
            text = stringResource(R.string.section_downloadable_models),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )

        // Hint
        Text(
            text = stringResource(R.string.models_catalog_hint),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp),
        )

        if (state.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.models_catalog_empty),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                state.rows.forEach { row ->
                    ModelCard(
                        row = row,
                        phase = state.downloadPhases[row.id] ?: DownloadPhase.Idle,
                        onDownload = { onDownload(row.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    row: CatalogModelRow,
    phase: DownloadPhase,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.name, fontSize = 18.sp)
                    Text(
                        phaseText(row.sizeText, phase),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    enabled = phase == DownloadPhase.Idle,
                    onClick = onDownload,
                    colors = ButtonDefaults.textButtonColors(),
                    content = {
                        Icon(
                            painterResource(R.drawable.arrow_downward_24),
                            contentDescription = stringResource(R.string.action_download),
                        )
                    },
                )
            }
            if (phase != DownloadPhase.Idle) {
                LinearProgressIndicator(
                    progress = { (phase as? DownloadPhase.Downloading)?.progress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun phaseText(sizeText: String, phase: DownloadPhase): String = when (phase) {
    DownloadPhase.Idle -> sizeText
    is DownloadPhase.Downloading -> {
        val percent = (phase.progress * 100).toInt().coerceIn(0, 100)
        if (phase.currentFile.isNullOrBlank())
            stringResource(R.string.subtitle_downloading, percent)
        else
            stringResource(R.string.subtitle_downloading_file, percent, phase.currentFile)
    }
    is DownloadPhase.Extracting ->
        if (phase.currentFile.isBlank())
            stringResource(R.string.subtitle_installing)
        else
            stringResource(R.string.subtitle_installing_file, phase.currentFile)
}