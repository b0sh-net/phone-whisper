package net.b0sh.audiotext.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.b0sh.audiotext.LiveBadge
import net.b0sh.audiotext.R
import net.b0sh.audiotext.liveBadgeFor

/**
 * Indicatore della capacità di trascrizione in tempo reale (Live) mostrato
 * accanto al testo secondario di una riga modello (contratti C2 e C3).
 *
 * L'icona ha la dimensione del carattere secondario (16dp su testo 14sp), prende
 * il colore del testo e non intercetta tocchi; con capacità sconosciuta
 * ([supportsLive] `null`) non mostra nulla.
 */
@Composable
fun LiveBadgeIcon(
    supportsLive: Boolean?,
    modifier: Modifier = Modifier,
) {
    when (liveBadgeFor(supportsLive)) {
        LiveBadge.Mic -> Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = stringResource(R.string.live_badge_supported),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(16.dp),
        )

        LiveBadge.MicOff -> Icon(
            painter = painterResource(R.drawable.ic_mic_off),
            contentDescription = stringResource(R.string.live_badge_unsupported),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(16.dp),
        )

        LiveBadge.None -> Unit
    }
}
