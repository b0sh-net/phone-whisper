package net.b0sh.audiotext

import java.util.Locale

/**
 * Risolve i nomi degli asset immagine dell'introduzione in base alla lingua.
 *
 * Convenzione (contratto C2): `intro_<locale>_<n>` dove <n> è la posizione
 * della schermata (1 = scaricare il modello, 2 = attendere, 3 = trascrivere).
 */
object IntroAssets {

    /** Locale di fallback per le lingue non supportate (contratto C3). */
    private const val FALLBACK_LOCALE = "en"

    private val SUPPORTED_LOCALES = setOf("it", "en")

    /**
     * Restituisce il prefisso del set di immagini per la lingua attiva.
     * Le lingue non supportate ricadono sul set inglese.
     */
    fun resolve(locale: Locale): String =
        if (locale.language in SUPPORTED_LOCALES) locale.language else FALLBACK_LOCALE

    /**
     * Nome dell'asset per la schermata [page] (1-based). Es. `intro_it_1`.
     */
    fun name(locale: Locale, page: Int): String =
        "intro_${resolve(locale)}_$page"
}