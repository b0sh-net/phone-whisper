package net.b0sh.audiotext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.ArchivedTranscript
import net.b0sh.audiotext.R
import net.b0sh.audiotext.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Archive of saved live transcriptions (contract C7): a pure, state-driven
 * composable (items + callbacks) so it can be exercised by Compose UI tests.
 *
 * "Review" shows the full text in a dialog; "Share" and "Delete" are delegated to
 * the caller through [onShare]/[onDelete], keeping I/O orchestration out of the UI.
 */
@Composable
fun LiveArchiveScreen(
    items: List<ArchivedTranscript>,
    onBack: () -> Unit,
    onShare: (ArchivedTranscript) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var reviewItem by remember { mutableStateOf<ArchivedTranscript?>(null) }
    var pendingDelete by remember { mutableStateOf<ArchivedTranscript?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back_button))
        }

        Text(
            text = stringResource(R.string.live_archive_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.live_archive_empty),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 24.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEach { item ->
                    ArchiveRow(
                        item = item,
                        dateTime = dateFormat.format(Date(item.createdAtMillis)),
                        onReview = { reviewItem = item },
                        onShare = { onShare(item) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    val reviewing = reviewItem
    if (reviewing != null) {
        AlertDialog(
            onDismissRequest = { reviewItem = null },
            title = { Text(stringResource(R.string.live_archive_title)) },
            text = {
                Text(
                    text = reviewing.text,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { reviewItem = null }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }

    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.live_delete_title)) },
            text = { Text(stringResource(R.string.live_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(deleting.id)
                }) {
                    Text(stringResource(R.string.live_action_delete))
                }
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
private fun ArchiveRow(
    item: ArchivedTranscript,
    dateTime: String,
    onReview: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dateTime, fontSize = 16.sp)
            Text(
                text = formatDuration(item.durationMillis),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onReview) {
            Icon(
                painter = painterResource(R.drawable.ic_description),
                contentDescription = stringResource(R.string.live_action_review),
            )
        }
        TextButton(onClick = onShare) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.live_action_share),
            )
        }
        TextButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.live_action_delete),
            )
        }
    }
}
