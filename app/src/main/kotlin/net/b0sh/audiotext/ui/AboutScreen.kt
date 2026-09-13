package net.b0sh.audiotext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Pagina "Maggiori informazioni" Compose: paragrafi (con eventuali link) e
 *  pulsanti per rivedere l'introduzione e chiudere. */
@Composable
fun AboutScreen(
    paragraphs: List<String>,
    onReviewIntro: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.about_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        paragraphs.forEach { aboutParagraph(it) }

        OutlinedButton(
            onClick = onReviewIntro,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            content = { Text(stringResource(R.string.intro_review_button)) },
        )
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            content = { Text(stringResource(R.string.action_close)) },
        )
    }
}

@Composable
private fun aboutParagraph(html: String) {
    Text(
        text = htmlToAnnotated(html),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/** Converte un frammento HTML con link `<a href="...">testo</a>` in un
 *  `AnnotatedString` con i link cliccabili in colore primary. Il testo senza
 *  link viene restituito così com'è (senza tag HTML). */
@Composable
private fun htmlToAnnotated(html: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        val pattern = Regex("""<a href="([^"]+)">([^<]+)</a>""")
        var last = 0
        for (match in pattern.findAll(html)) {
            append(html.substring(last, match.range.first))
            val url = match.groupValues[1]
            val label = match.groupValues[2]
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                ),
            ) { append(label) }
            last = match.range.last + 1
        }
        append(html.substring(last))
    }
}