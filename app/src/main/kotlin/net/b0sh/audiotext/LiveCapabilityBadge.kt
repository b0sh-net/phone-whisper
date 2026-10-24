package net.b0sh.audiotext

/**
 * Indicatore della capacità di trascrizione in tempo reale (Live) di un modello,
 * mostrato accanto al testo secondario della riga modello nel Catalogo e nella
 * Home (contratto C1 di `specs/008-live-capability-badge/contracts/model-live-badge.md`).
 */
enum class LiveBadge {
    /** Il modello supporta la Live: icona microfono. */
    Mic,

    /** Il modello non supporta la Live: icona microfono barrato. */
    MicOff,

    /** Capacità sconosciuta: nessun indicatore. */
    None,
}

/**
 * Mappa la capacità Live di un modello sull'indicatore da mostrare (FR-003, FR-009).
 *
 * L'unica sorgente della capacità è il flag `streaming` di [MODEL_CATALOG], la stessa
 * informazione usata dal gate della scheda Live ([canUseLive]): indicatore e
 * comportamento della Live non possono quindi divergere (FR-011).
 *
 * @param supportsLive `true` se il modello supporta la Live, `false` se non la
 *   supporta, `null` se la capacità non è nota.
 */
fun liveBadgeFor(supportsLive: Boolean?): LiveBadge = when (supportsLive) {
    true -> LiveBadge.Mic
    false -> LiveBadge.MicOff
    null -> LiveBadge.None
}
