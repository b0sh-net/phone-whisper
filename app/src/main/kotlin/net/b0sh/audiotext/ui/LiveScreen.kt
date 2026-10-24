package net.b0sh.audiotext.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.LivePhase
import net.b0sh.audiotext.R

/**
 * Live transcription screen (contract C7): a pure, state-driven composable.
 *
 * It takes all data and callbacks as parameters (no direct coupling to
 * [net.b0sh.audiotext.LiveTranscriptionController]) so it can be exercised
 * deterministically by Compose UI tests.
 *
 * When [interruptionNoticeRes] is not null the screen shows a non-blocking notice
 * explaining that the session ended because the app was no longer visible: it never
 * hides the text, never blocks the start button and can be dismissed (FR-007).
 */
@Composable
fun LiveScreen(
    phase: LivePhase,
    currentText: String,
    archiveAvailable: Boolean,
    @StringRes errorMessageRes: Int?,
    @StringRes interruptionNoticeRes: Int?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismissInterruption: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    val recording = phase == LivePhase.Recording

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Title (same style as Catalog / Info)
        Text(
            text = stringResource(R.string.live_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
        )

        // Start / Stop button
        Button(
            onClick = if (recording) onStop else onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (recording) R.drawable.ic_stop_circle else R.drawable.ic_speech_to_text,
                ),
                contentDescription = stringResource(
                    if (recording) R.string.live_stop else R.string.live_start,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (recording) R.string.live_stop else R.string.live_start),
            )
        }

        // Error message (localized), when present
        if (phase == LivePhase.Error && errorMessageRes != null) {
            Text(
                text = stringResource(errorMessageRes),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp),
            )
        }

        // Interruption notice (localized, dismissal only — FR-007). It is not an
        // error: informational tone, and the start button stays usable.
        if (interruptionNoticeRes != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(interruptionNoticeRes),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissInterruption) {
                    Text(text = stringResource(R.string.live_interrupted_dismiss))
                }
            }
        }

        // Scrollable transcript box
        val scrollState = rememberScrollState()
        LaunchedEffect(currentText) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            ) {
                Text(
                    text = currentText.ifEmpty {
                        if (recording) stringResource(R.string.live_recording) else ""
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Archive button (blocked while recording — FR-010)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onOpenArchive,
                enabled = archiveAvailable,
                colors = ButtonDefaults.textButtonColors(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_archive),
                    contentDescription = stringResource(R.string.live_archive),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.live_archive))
            }
        }
    }
}
