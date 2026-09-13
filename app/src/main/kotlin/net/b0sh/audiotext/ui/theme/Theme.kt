package net.b0sh.audiotext.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
)

private val DarkColors = darkColorScheme(
    primary = OnPrimaryContainer,
    onPrimary = PrimaryContainer,
    primaryContainer = Primary,
    onPrimaryContainer = PrimaryContainer,
)

/** Tema Material3 dell'app: seleziona la palette chiara/scura in base al
 *  sistema, riusando i colori accent del brand. */
@Composable
fun AudioToTextTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}