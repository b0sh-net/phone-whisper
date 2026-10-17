package net.b0sh.audiotext

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import net.b0sh.audiotext.ui.AboutScreen
import net.b0sh.audiotext.ui.CatalogModelRow
import net.b0sh.audiotext.ui.CatalogScreenState
import net.b0sh.audiotext.ui.DownloadPhase
import net.b0sh.audiotext.ui.InstalledModelRow
import net.b0sh.audiotext.ui.LiveArchiveScreen
import net.b0sh.audiotext.ui.LiveScreen
import net.b0sh.audiotext.ui.MainScreen
import net.b0sh.audiotext.ui.MainScreenUiState
import net.b0sh.audiotext.ui.ModelCatalogScreen
import net.b0sh.audiotext.ui.statusDrawable
import net.b0sh.audiotext.ui.theme.AudioToTextTheme
import kotlin.concurrent.thread
import kotlinx.coroutines.launch

/** Destinazioni della barra di navigazione in basso. */
private enum class Tab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    Home(R.string.bottom_nav_home, R.drawable.ic_home),
    Catalog(R.string.bottom_nav_catalog, R.drawable.ic_catalog),
    Live(R.string.bottom_nav_live, R.drawable.ic_mic),
    About(R.string.bottom_nav_about, R.drawable.ic_info),
}

/**
 * Activity host unica: `Scaffold` con una NavigationBar fissa (Home, Catalogo,
 * Maggiori informazioni) e le tre schermate come destinazioni. Il download dei
 * modelli è gestito da [ModelDownloadService] (foreground service).
 */
class MainActivity : AppCompatActivity() {

    private var currentTab by mutableStateOf(Tab.Home)

    // Stato schermata Home
    private var statusText by mutableStateOf("")
    private var statusIconRes by mutableStateOf(R.drawable.status_ready)
    private var infoText by mutableStateOf("")
    private var installedModels by mutableStateOf<List<InstalledModelRow>>(emptyList())

    // Id del modello per cui è in corso il cambio (selezione). Mentre è non-null
    // le ulteriori richieste di cambio vengono ignorate, così un cambio non
    // viene interrotto da un altro.
    private var switchingModelId by mutableStateOf<String?>(null)

    // Abilitazione della scheda Live: dipende dal modello selezionato (streaming).
    private var liveEnabled by mutableStateOf(false)

    // Sotto-schermata Live: trascrizione in tempo reale oppure archivio.
    private var showArchive by mutableStateOf(false)
    private var archiveItems by mutableStateOf<List<ArchivedTranscript>>(emptyList())

    // Stato catalogo
    private var rows by mutableStateOf<List<CatalogModelRow>>(emptyList())

    private var pendingModel: Model? = null
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            pendingModel?.let { ModelDownloadService.start(this, it) }
            pendingModel = null
        }

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                LiveTranscriptionController.start(this)
            } else {
                LiveTranscriptionController.setError(R.string.live_error_permission_denied)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ripristino dopo un cambio di configurazione (rotazione): la scheda
        // attiva e la sotto-schermata archivio non devono andare perse. Alla
        // prima apertura, senza un modello installato si parte dal catalogo.
        currentTab = savedInstanceState?.getString(STATE_TAB)
            ?.let { name -> Tab.entries.firstOrNull { it.name == name } }
            ?: if (MODEL_CATALOG.any { ModelDownloader.isInstalled(this, it) })
                Tab.Home else Tab.Catalog
        showArchive = savedInstanceState?.getBoolean(STATE_ARCHIVE, false) ?: false

        setContent {
            AudioToTextTheme {
                MainScaffold()
            }
        }

        // Onboarding: proponi l'introduzione solo alla prima apertura.
        if (savedInstanceState == null && IntroFlag.shouldShow(prefs())) {
            startActivity(
                Intent(this, IntroActivity::class.java)
                    .putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_FIRST_OPEN)
            )
            IntroFlag.markShown(prefs())
        }

        thread { initLocalModel() }
        refreshCatalog()
        refreshHome()
        if (showArchive) archiveItems = LiveTranscriptionController.transcripts(this)

        // Un download concluso in background (service): aggiorna lista dei
        // modelli installati (Home) e catalogo, e inizializza il modello.
        lifecycleScope.launch {
            var previous = ModelDownloadRepository.downloads.toMap()
            snapshotFlow { ModelDownloadRepository.downloads.toMap() }.collect { current ->
                val completed = current.entries.any { (id, phase) ->
                    phase == DownloadPhase.Idle &&
                        previous[id] !in setOf(null, DownloadPhase.Idle)
                }
                previous = current
                if (completed) {
                    refreshCatalog()
                    refreshHome()
                    thread { initLocalModel() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCatalog()
        refreshHome()
        thread { initLocalModel() }
    }

    override fun onStop() {
        super.onStop()
        // End the live session on a real background transition, but not on a
        // configuration change (rotation), which the process singleton survives.
        if (!isChangingConfigurations) {
            LiveTranscriptionController.onAppBackgrounded(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Keep the active tab (and the archive sub-screen) across rotations.
        outState.putString(STATE_TAB, currentTab.name)
        outState.putBoolean(STATE_ARCHIVE, showArchive)
    }

    /** Start the live session, requesting the microphone permission when needed. */
    private fun onLiveStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            LiveTranscriptionController.start(this)
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /** Open the archive sub-screen, loading the saved transcriptions. */
    private fun openArchive() {
        archiveItems = LiveTranscriptionController.transcripts(this)
        showArchive = true
    }

    /** Delete a saved transcription and refresh the list (FR-012/FR-018). */
    private fun deleteTranscript(id: Long) {
        LiveTranscriptionController.deleteTranscript(this, id)
        archiveItems = LiveTranscriptionController.transcripts(this)
    }

    /** Share a saved transcription as plain text (FR-014). */
    private fun shareTranscript(item: ArchivedTranscript) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, item.text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.live_action_share)))
    }

    @Composable
    private fun MainScaffold() {
        // Il tasto Back dall'archivio torna alla Live; dalle altre schede torna alla Home.
        BackHandler(enabled = currentTab != Tab.Home) {
            if (currentTab == Tab.Live && showArchive) {
                showArchive = false
            } else {
                currentTab = Tab.Home
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        val liveLocked = tab == Tab.Live && !liveEnabled
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (liveLocked) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.live_requires_streaming_model),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } else {
                                    currentTab = tab
                                }
                            },
                            icon = {
                                Icon(
                                    painterResource(tab.iconRes),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = if (liveLocked) {
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                    selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                )
                            } else {
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            },
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                when (currentTab) {
                    Tab.Home -> MainScreen(
                        state = MainScreenUiState(
                            infoText = infoText,
                            statusText = statusText,
                            statusIconRes = statusIconRes,
                            installedModels = installedModels,
                        ),
                        switchingModelId = switchingModelId,
                        onSelectModel = ::selectModel,
                        onDeleteModel = ::deleteModel,
                    )
                    Tab.Catalog -> ModelCatalogScreen(
                        state = CatalogScreenState(
                            rows = rows,
                            downloadPhases = ModelDownloadRepository.downloads,
                        ),
                        onDownload = ::onDownload,
                    )
                    Tab.Live -> if (showArchive) {
                        LiveArchiveScreen(
                            items = archiveItems,
                            onBack = { showArchive = false },
                            onShare = ::shareTranscript,
                            onDelete = ::deleteTranscript,
                        )
                    } else {
                        LiveScreen(
                            phase = LiveTranscriptionController.phase,
                            currentText = LiveTranscriptionController.currentText,
                            archiveAvailable = LiveTranscriptionController.archiveAvailable,
                            errorMessageRes = LiveTranscriptionController.errorMessageRes,
                            onStart = ::onLiveStart,
                            onStop = { LiveTranscriptionController.stop(this@MainActivity) },
                            onOpenArchive = ::openArchive,
                        )
                    }
                    Tab.About -> AboutScreen(
                        paragraphs = aboutParagraphs(),
                        versionName = installedVersionName(),
                        onReviewIntro = {
                            startActivity(
                                Intent(this@MainActivity, IntroActivity::class.java)
                                    .putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_MANUAL)
                            )
                        },
                    )
                }
            }
        }
    }

    private fun aboutParagraphs(): List<String> = listOf(
        string(R.string.about_intro),
        string(R.string.about_source_code),
        string(R.string.about_issues),
        string(R.string.about_support),
    )

    /** Versione installata dell'app, letta dal PackageManager (coincide con
     *  `versionName` in build.gradle.kts). */
    private fun installedVersionName(): String =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            ""
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
        // Ignora ulteriori richieste di cambio mentre una selezione è già in corso.
        val switching = switchingModelId
        if (switching != null) return

        val model = MODEL_CATALOG.find { it.id == id } ?: return
        switchingModelId = id
        LiveTranscriptionController.stop(this)
        prefs().edit().putString("model_name", model.id).apply()
        TranscriberManager.reset()
        thread {
            try {
                val success = initLocalModel()
                if (success) setStatus(R.string.status_active_model, model.name)
                refreshHome()
            } finally {
                switchingModelId = null
            }
        }
    }

    /** Avvia il download del modello (come foreground service). */
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

    private fun refreshHome() {
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
        liveEnabled = LiveTranscriptionController.canUseLive(this)
    }

    private fun refreshCatalog() {
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

    private fun deleteModel(id: String) {
        val model = MODEL_CATALOG.find { it.id == id } ?: return
        LiveTranscriptionController.stop(this)
        val wasActive = prefs().getString("model_name", "") == model.id
        if (wasActive) {
            prefs().edit().remove("model_name").apply()
            TranscriberManager.reset()
        }
        setStatus(R.string.status_removing_model, model.name)
        thread {
            ModelDownloader.delete(this, model)
            if (wasActive) {
                // Il modello rimosso era quello in uso: riseleziona
                // automaticamente il primo modello installato rimasto.
                val replacement = MODEL_CATALOG
                    .filter { ModelDownloader.isInstalled(this, it) }
                    .firstOrNull()
                if (replacement != null) {
                    prefs().edit().putString("model_name", replacement.id).apply()
                    TranscriberManager.reset()
                    val success = initLocalModel()
                    setStatus(
                        if (success) R.string.status_active_model else R.string.status_no_local_model,
                        if (success) replacement.name else "",
                    )
                } else {
                    setStatus(R.string.status_no_local_model)
                }
            } else {
                setStatus(R.string.status_model_removed, model.name)
            }
            refreshHome()
            refreshCatalog()
        }
    }

    private fun setStatus(@StringRes res: Int, vararg args: Any) {
        statusText = string(res, *args)
        statusIconRes = statusDrawable(res)
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)

    private fun prefs() = getSharedPreferences("audiotext", MODE_PRIVATE)

    companion object {
        private const val STATE_TAB = "live_state_tab"
        private const val STATE_ARCHIVE = "live_state_archive"
    }
}