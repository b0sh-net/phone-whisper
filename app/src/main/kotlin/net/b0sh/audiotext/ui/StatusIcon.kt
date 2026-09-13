package net.b0sh.audiotext.ui

import androidx.annotation.DrawableRes
import net.b0sh.audiotext.R

/** Mappa pura stato → icona del pannello di stato (funzione testabile senza
 *  Activity). Default: icona "ready". */
@DrawableRes
fun statusDrawable(res: Int): Int = when (res) {
    R.string.status_ready -> R.drawable.status_ready
    R.string.status_initializing_model -> R.drawable.status_initializing_model
    R.string.status_local_model_ready -> R.drawable.status_local_model_ready
    R.string.status_no_local_model -> R.drawable.status_no_local_model
    R.string.status_active_model -> R.drawable.status_active_model
    R.string.status_installing_model -> R.drawable.status_installing_model
    R.string.status_model_installed -> R.drawable.status_model_installed
    R.string.status_model_ready -> R.drawable.status_model_ready
    R.string.status_model_load_failed -> R.drawable.status_model_load_failed
    R.string.status_download_failed -> R.drawable.status_download_failed
    R.string.status_removing_model -> R.drawable.status_removing_model
    R.string.status_model_removed -> R.drawable.status_model_removed
    else -> R.drawable.status_ready
}