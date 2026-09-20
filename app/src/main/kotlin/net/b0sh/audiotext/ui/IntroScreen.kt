package net.b0sh.audiotext.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.b0sh.audiotext.R

/** Introduzione Compose: tre schermate sfogliabili orizzontalmente con snap e
 *  indicatori a punti. Le immagini sono risolte dall'Activity (IntroAssets).
 *
 *  Le immagini sfruttano tutto lo spazio verticale disponibile: la loro
 *  larghezza deriva dall'altezza (mantenendo le proporzioni). Se così facendo
 *  l'immagine risulta più larga dello schermo (tipico in verticale, dato che
 *  le introduzioni sono in orizzontale), viene mostrata dentro una zona
 *  scorrevole in orizzontale, così si può scorrere a destra per vedere la
 *  parte che non entra nello schermo. I pallini sotto l'immagine permettono
 *  comunque di cambiare schermata (lo scroll orizzontale dell'immagine
 *  coesiste con lo swipe del pager). */
@Composable
fun IntroScreen(
    @DrawableRes imageRes: List<Int>,
    onClose: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { imageRes.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.intro_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // L'area delle immagini occupa tutto lo spazio rimasto in altezza:
        // il pager (e con lui ogni immagine) si espande con weight(1f), così
        // in verticale l'illustrazione non è più limitata dalla larghezza
        // dello schermo ma usa l'intera altezza disponibile.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            introImage(painterResource(imageRes[page]))
        }

        // Page indicator dots (tappabili: vedi nota in testa alla classe)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            repeat(imageRes.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .clickable { pagerState.requestScrollToPage(index, 0f) }
                        .padding(horizontal = 4.dp)
                        .size(12.dp)
                        .background(
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                )
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            content = { Text(stringResource(R.string.action_close)) },
        )
    }
}

/** Mostra un'immagine dell'introduzione usando tutta l'altezza disponibile.
 *
 *  L'immagine riempie l'intera altezza della pagina ([fillMaxHeight]) e la
 *  larghezza è ricavata dal rapporto d'aspetto naturale ([aspectRatio]),
 *  mantenendo le proporzioni. Se così facendo l'immagine risulta più larga
 *  dello schermo (tipico in verticale, dato che le introduzioni sono in
 *  orizzontale), è avvolta in una zona scorrevole in orizzontale per poter
 *  vedere la parte che non entra.
 */
@Composable
private fun introImage(painter: Painter) {
    // Proporzioni naturali dell'immagine (larghezza / altezza).
    val intrinsic = painter.intrinsicSize
    val aspect = intrinsic.width / intrinsic.height

    val imageModifier = Modifier
        .fillMaxHeight()
        .aspectRatio(aspect)

    // La pagina del pager riempie l'area disponibile; se l'immagine
    // (a tutta altezza) è più larga dello schermo, lo scroll orizzontale
    // interno permette di vedere la parte che non ci sta. I pallini sotto
    // restano il modo di cambiare schermata.
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState()),
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = imageModifier,
        )
    }
}