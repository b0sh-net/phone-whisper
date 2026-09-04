package net.b0sh.audiotext

import android.content.SharedPreferences

/**
 * Flag persistente che stabilisce se l'introduzione è già stata mostrata
 * automaticamente almeno una volta.
 *
 * È indipendente dalla presenza/assenza di un modello: la sola presenza di un
 * modello non distingue un "nuovo installato" e una sua rimozione non deve
 * riproporre la guida (FR-001, FR-008, FR-012).
 */
object IntroFlag {

    const val PREFS_NAME = "audiotext"

    /** SharedPreferences chiave che memorizza lo stato di prima visualizzazione. */
    const val KEY = "intro_shown"

    /**
     * L'introduzione va proposta solo se non è mai stata mostrata.
     * Versione pura, testabile su JVM.
     */
    fun shouldShow(hasBeenShown: Boolean): Boolean = !hasBeenShown

    /** Legge lo stato dallo storage dell'app. */
    fun shouldShow(prefs: SharedPreferences): Boolean = shouldShow(prefs.getBoolean(KEY, false))

    /** Segna l'introduzione come già mostrata. */
    fun markShown(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY, true).apply()
    }
}