package net.b0sh.audiotext

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import net.b0sh.audiotext.ui.DownloadPhase
import net.b0sh.audiotext.ui.InstalledModelRow
import net.b0sh.audiotext.ui.MainScreen
import net.b0sh.audiotext.ui.MainScreenUiState
import net.b0sh.audiotext.ui.statusDrawable
import net.b0sh.audiotext.ui.theme.AudioToTextTheme
import kotlin.concurrent.thread
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var statusText by mutableStateOf("")
    private var statusIconRes by mutableStateOf(R.drawable.status_ready)
    private var infoText by mutableStateOf("")
    private var installedModels by mutableStateOf<List<InstalledModelRow>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AudioToTextTheme {
                MainScreen(
                    state = MainScreenUiState(
                        infoText = infoText,
                        statusText = statusText,
                        statusIconRes = statusIconRes,
                        installedModels = installedModels,
                    ),
                    onSelectModel = ::selectModel,
                    onDeleteModel = ::deleteModel,
                    onOpenCatalog = { startActivity(Intent(this@MainActivity, ModelCatalogActivity::class.java)) },
                    onOpenAbout = { startActivity(Intent(this@MainActivity, AboutActivity::class.java)) },
                )
            }
        }

        // Onboarding: proponi l'introduzione solo alla prima apertura.
        if (IntroFlag.shouldShow(prefs())) {
            startActivity(Intent(this, IntroActivity::class.java).putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_FIRST_OPEN))
            IntroFlag.markShown(prefs())
        }

        // No model installed yet: bring the user straight to the catalog.
        if (MODEL_CATALOG.none { ModelDownloader.isInstalled(this, it) }) {
            startActivity(Intent(this, ModelCatalogActivity::class.java))
        }

        // Load model in background if needed
        thread { initLocalModel() }

        refresh()

        // Un download concluso in background (service) mentre siamo sulla
        // schermata principale: aggiorna la lista e inizializza il modello.
        lifecycleScope.launch {
            var previous = ModelDownloadRepository.downloads.toMap()
            snapshotFlow { ModelDownloadRepository.downloads.toMap() }.collect { current ->
                val completed = current.entries.any { (id, phase) ->
                    phase == DownloadPhase.Idle &&
                        previous[id] !in setOf(null, DownloadPhase.Idle)
                }
                previous = current
                if (completed) {
                    refresh()
                    thread { initLocalModel() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the catalog page: pick up installed/removed models.
        refresh()
        thread { initLocalModel() }
    }

    private fun initLocalModel(): Boolean {
        val modelName = prefs().getString("model_name", "")
        setStatus(R.string.status_initializing_model)

        val t = TranscriberManager.getOrCreateTranscriber(this)
        if (t != null) {
            val name = MODEL_CATALOG.find { it.id == modelName }?.name ?: modelName ?: "Unknown"
            setStatus(R.string.status_local_model_ready, name)
            return true
        }

        setStatus(R.string.status_no_local_model)
        return false
    }

    /** Seleziona il modello installato da usare per la trascrizione. */
    private fun selectModel(id: String) {
        val model = MODEL_CATALOG.find { it.id == id } ?: return
        prefs().edit().putString("model_name", model.id).apply()
        TranscriberManager.reset()
        thread {
            val success = initLocalModel()
            if (success) setStatus(R.string.status_active_model, model.name)
            refresh()
        }
    }

    private fun refresh() {
        val activeModel = prefs().getString("model_name", "")
        installedModels = MODEL_CATALOG
            .filter { ModelDownloader.isInstalled(this, it) }
            .sortedBy { it.name }
            .map {
                InstalledModelRow(
                    id = it.id,
                    name = it.name,
                    sizeText = string(R.string.model_size_mb, string(it.qualityRes), it.sizeMb),
                    sizeMb = it.sizeMb,
                    isActive = it.id == activeModel,
                )
            }
        infoText = if (MODEL_CATALOG.any { ModelDownloader.isInstalled(this, it) })
            string(R.string.info_model_ready)
        else string(R.string.info_no_model)
    }

    private fun deleteModel(id: String) {
        val model = MODEL_CATALOG.find { it.id == id } ?: return
        val wasActive = prefs().getString("model_name", "") == model.id
        if (wasActive) {
            prefs().edit().remove("model_name").apply()
            TranscriberManager.reset()
        }
        setStatus(R.string.status_removing_model, model.name)
        thread {
            ModelDownloader.delete(this, model)
            setStatus(R.string.status_model_removed, model.name)
            refresh()
        }
    }

    private fun setStatus(@StringRes res: Int, vararg args: Any) {
        statusText = string(res, *args)
        statusIconRes = statusDrawable(res)
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)

    private fun prefs() = getSharedPreferences("audiotext", MODE_PRIVATE)
}