package net.b0sh.audiotext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Stato UI della schermata di trascrizione, derivato in TranscribeActivity. */
data class TranscriptUiState(
    val statusText: String,
    val resultText: String,
    val showProgress: Boolean,
)

/** Schermata di trascrizione Compose (share-target): stato, box risultato con
 *  progresso, copia negli appunti e chiusura. */
@Composable
fun TranscribeScreen(
    state: TranscriptUiState,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.title_transcribing),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = state.statusText,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                Text(
                    text = state.resultText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Button(
                    onClick = onCopy,
                    content = { Text(stringResource(R.string.action_copy_to_clipboard)) },
                )
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            content = { Text(stringResource(R.string.action_close)) },
        )
    }
}