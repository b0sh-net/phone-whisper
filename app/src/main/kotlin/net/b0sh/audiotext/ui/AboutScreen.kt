package net.b0sh.audiotext.ui

import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Pagina "Maggiori informazioni" Compose: paragrafi (con eventuali link) e
 *  pulsanti per rivedere l'introduzione e chiudere. */
@Composable
fun AboutScreen(
    paragraphs: List<String>,
    versionName: String,
    onReviewIntro: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Text(
            text = stringResource(R.string.about_installed_version, versionName),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        OutlinedButton(
            onClick = onReviewIntro,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            content = { Text(stringResource(R.string.intro_review_button)) },
        )
    }
}

@Composable
private fun aboutParagraph(html: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val parsed = remember(html, linkColor) { parseHtmlLinks(html, linkColor) }

    // Risultato del layout del paragrafo per mappare il tocco a un carattere.
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = parsed.annotated,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .padding(bottom = 16.dp)
            .pointerInput(parsed) {
                detectTapGestures { offset ->
                    val result = layoutResult ?: return@detectTapGestures
                    val charOffset = result.getOffsetForPosition(offset)
                    parsed.links.firstOrNull { charOffset in it.start until it.end }?.let {
                        uriHandler.openUri(it.url)
                    }
                }
            },
    )
}

/** Link estratto da un paragrafo: intervallo di caratteri (start..end escluso)
 *  nel testo e URL di destinazione. */
private data class ParagraphLink(val start: Int, val end: Int, val url: String)

private data class ParsedParagraph(
    val annotated: AnnotatedString,
    val links: List<ParagraphLink>,
)

/** Converte un frammento HTML (con link `<a href="...">testo</a>` ed entità
 *  `&lt;`/`&gt;`) in un `AnnotatedString`, colorando e sottolineando i link e
 *  registrandone gli intervalli per il tap. Usa `Html.fromHtml`, la stessa
 *  sorgente della precedente UI a View: decodifica le entità e riconosce i tag. */
private fun parseHtmlLinks(html: String, linkColor: Color): ParsedParagraph {
    val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT) as Spanned
    val links = mutableListOf<ParagraphLink>()
    val annotated = buildAnnotatedString {
        append(spanned.toString())
        for (urlSpan in spanned.getSpans(0, spanned.length, URLSpan::class.java)) {
            val start = spanned.getSpanStart(urlSpan)
            val end = spanned.getSpanEnd(urlSpan)
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                start,
                end,
            )
            links += ParagraphLink(start, end, urlSpan.url)
        }
    }
    return ParsedParagraph(annotated, links)
}