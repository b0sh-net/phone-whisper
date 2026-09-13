package net.b0sh.audiotext

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import net.b0sh.audiotext.ui.CatalogModelRow
import net.b0sh.audiotext.ui.CatalogScreenState
import net.b0sh.audiotext.ui.DownloadPhase
import net.b0sh.audiotext.ui.ModelCatalogScreen
import net.b0sh.audiotext.ui.theme.AudioToTextTheme
import kotlinx.coroutines.launch

/** Catalogo modelli scaricabili. Il download è gestito da [ModelDownloadService]
 *  (foreground service) così continua anche chiudendo il catalogo. Una card per
 *  modello ancora da installare; i modelli installati escono dall'elenco. */
class ModelCatalogActivity : AppCompatActivity() {

    private var rows by mutableStateOf<List<CatalogModelRow>>(emptyList())

    private var pendingModel: Model? = null
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            pendingModel?.let { ModelDownloadService.start(this, it) }
            pendingModel = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refresh()

        setContent {
            AudioToTextTheme {
                ModelCatalogScreen(
                    state = CatalogScreenState(
                        rows = rows,
                        downloadPhases = ModelDownloadRepository.downloads,
                    ),
                    onBack = { finish() },
                    onDownload = ::onDownload,
                )
            }
        }

        // Quando un download termina (modello installato) mentre siamo ancora
        // sul catalogo, aggiorniamo l'elenco per far sparire la card.
        lifecycleScope.launch {
            var previous = ModelDownloadRepository.downloads.toMap()
            snapshotFlow { ModelDownloadRepository.downloads.toMap() }.collect { current ->
                val completed = current.entries.any { (id, phase) ->
                    phase == DownloadPhase.Idle && previous[id] !in setOf(null, DownloadPhase.Idle)
                }
                previous = current
                if (completed) refresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Al ritorno (anche da un download concluso in background) rimuovi le
        // card dei modelli che si sono installati nel frattempo.
        refresh()
    }

    private fun onDownload(id: String) {
        val model = MODEL_CATALOG.find { it.id == id } ?: return
        ModelDownloadRepository.mark(id, DownloadPhase.Downloading(0f, null))
        if (needsNotificationPermission()) {
            pendingModel = model
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ModelDownloadService.start(this, model)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        rows = MODEL_CATALOG
            .filterNot { ModelDownloader.isInstalled(this, it) }
            .map {
                CatalogModelRow(
                    id = it.id,
                    name = it.name,
                    sizeText = string(R.string.model_size_mb, string(it.qualityRes), it.sizeMb),
                )
            }
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)
}