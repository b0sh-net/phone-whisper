package net.b0sh.audiotext

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.*
import java.util.concurrent.TimeUnit

data class ModelFile(
    val remotePath: String,
    val localName: String,
)

data class Model(
    val id: String,
    val name: String,
    val source: String,
    val sizeMb: Int,
    val qualityRes: Int,
    val recommended: Boolean = false,
    val dirName: String = "",
    val archive: String? = null,
    val files: List<ModelFile> = emptyList(),
)

const val SHERPA_MODELS_SOURCE =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"

val MODEL_CATALOG = listOf(
    Model(
        id = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8",
        name = "Parakeet 110M - English",
        source = SHERPA_MODELS_SOURCE,
        sizeMb = 100,
        qualityRes = R.string.quality_two_stars,
        dirName = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8",
        archive = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8",
    ),
    Model(
        id = "sherpa-onnx-whisper-base.en",
        name = "Whisper Base - English",
        source = SHERPA_MODELS_SOURCE,
        sizeMb = 199,
        qualityRes = R.string.quality_two_stars,
        dirName = "sherpa-onnx-whisper-base.en",
        archive = "sherpa-onnx-whisper-base.en",
    ),
    Model(
        id = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8",
        name = "Parakeet 0.6B - Multilanguage",
        source = SHERPA_MODELS_SOURCE,
        sizeMb = 465,
        qualityRes = R.string.quality_best_quality,
        recommended = true,
        dirName = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8",
        archive = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8",
    ),
    Model(
        id = "sherpa-onnx-moonshine-tiny-en-int8",
        name = "Moonshine Tiny - English",
        source = SHERPA_MODELS_SOURCE,
        sizeMb = 103,
        qualityRes = R.string.quality_two_stars,
        dirName = "sherpa-onnx-moonshine-tiny-en-int8",
        archive = "sherpa-onnx-moonshine-tiny-en-int8",
    ),
    Model(
        id = "kroko-128l-it",
        name = "Kroko – Italiano",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-it",
        files = krokoFiles("it"),
    ),
    Model(
        id = "kroko-128l-de",
        name = "Kroko – Deutsch",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-de",
        files = krokoFiles("de"),
    ),
    Model(
        id = "kroko-128l-en",
        name = "Kroko – English",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-en",
        files = krokoFiles("en"),
    ),
    Model(
        id = "kroko-128l-es",
        name = "Kroko – Español",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-es",
        files = krokoFiles("es"),
    ),
    Model(
        id = "kroko-128l-fr",
        name = "Kroko – Français",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-fr",
        files = krokoFiles("fr"),
    ),
    Model(
        id = "kroko-128l-pt",
        name = "Kroko – Português",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-pt",
        files = krokoFiles("pt"),
    ),
    Model(
        id = "kroko-128l-tr",
        name = "Kroko – Türkçe",
        source = "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main",
        sizeMb = 154,
        qualityRes = R.string.quality_best_lang,
        dirName = "kroko-128l-tr",
        files = krokoFiles("tr"),
    ),
)

/** The four Kroko model files, hosted under `<lang>/kroko_128l/`. */
private fun krokoFiles(lang: String) = listOf(
    ModelFile("$lang/kroko_128l/decoder.int8.onnx?download=true", "decoder.int8.onnx"),
    ModelFile("$lang/kroko_128l/encoder.int8.onnx?download=true", "encoder.int8.onnx"),
    ModelFile("$lang/kroko_128l/joiner.int8.onnx?download=true", "joiner.int8.onnx"),
    ModelFile("$lang/kroko_128l/tokens.txt?download=true", "tokens.txt"),
)

sealed class DownloadState {
    data class Downloading(val progress: Float, val currentFile: String? = null) : DownloadState()
    data class Extracting(val filesDone: Int, val currentFile: String) : DownloadState()
    object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object ModelDownloader {
    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS).build()

    fun modelDir(ctx: Context, model: Model) =
        File(ctx.filesDir, "models/${model.dirName}")

    /** Full URL for an archive download, or for a single file of an uncompressed model. */
    fun downloadUrl(model: Model, remotePath: String? = null): String {
        val base = model.source.trimEnd('/')
        if (remotePath != null) return "$base/$remotePath"
        val archive = model.archive ?: throw IOException("Model ${model.id} has no archive")
        return "$base/$archive.tar.bz2"
    }

    /** Local file name for a remote path: drop any query string and folder prefix. */
    fun localNameFor(remotePath: String): String =
        remotePath.substringBefore('?').substringAfterLast('/')

    /** True when dir contains all requiredFiles and satisfies the common rule
     *  (at least one model file and one tokens file). */
    fun isModelDirInstalled(dir: File, requiredFiles: List<String> = emptyList()): Boolean {
        if (!dir.exists()) return false
        val files = dir.listFiles() ?: return false
        val hasModel = files.any { it.name.endsWith(".onnx") || it.name.endsWith(".ort") }
        val hasTokens = files.any { it.name.contains("tokens.txt") }
        if (!hasModel || !hasTokens) return false
        return requiredFiles.all { required -> files.any { it.name == required } }
    }

    fun isInstalled(ctx: Context, model: Model): Boolean =
        isModelDirInstalled(modelDir(ctx, model), model.files.map { it.localName })

    /** Download a model: by archive (tar.bz2, extracted) or as a list of
     *  uncompressed files. Callbacks fire on a background thread. */
    fun download(ctx: Context, model: Model, onState: (DownloadState) -> Unit) {
        if (model.files.isNotEmpty()) {
            downloadFiles(ctx, model, onState)
        } else {
            downloadArchive(ctx, model, onState)
        }
    }

    private fun downloadArchive(ctx: Context, model: Model, onState: (DownloadState) -> Unit) {
        val url = downloadUrl(model)
        val tmpArchive = File(ctx.cacheDir, "${model.dirName}.tar.bz2")
        val finalDir = modelDir(ctx, model)

        Thread {
            try {
                // Fallback: the HEAD probe may fail on servers that answer 3xx
                // (e.g. GitHub release assets) — the GET below reports the final
                // content length, which is preferable for accurate progress.
                val headTotal = headContentLength(url)
                downloadFile(url, tmpArchive) { written, total ->
                    var effective = -1L
                    if (total > 0) effective = total
                    else if (headTotal > 0) effective = headTotal
                    if (effective > 0) {
                        val p = (if (written > effective) effective else written).toFloat() / effective
                        onState(DownloadState.Downloading(p))
                    }
                }
                onState(DownloadState.Extracting(0, ""))

                // Extract to a temporary directory first to ensure atomicity
                val tmpExtractDir = File(ctx.cacheDir, "extract_${model.dirName}")
                tmpExtractDir.deleteRecursively()
                tmpExtractDir.mkdirs()

                extractTarBz2(tmpArchive, tmpExtractDir) { filesDone, current ->
                    onState(DownloadState.Extracting(filesDone, current))
                }

                // The archive usually contains a top-level directory.
                // We need to find the actual model content.
                val extractedContent = tmpExtractDir.listFiles()?.firstOrNull { it.isDirectory }
                    ?: tmpExtractDir

                // Move to final destination
                finalDir.deleteRecursively()
                if (!extractedContent.renameTo(finalDir)) {
                    // Fallback to copy if rename fails across filesystems
                    extractedContent.copyRecursively(finalDir, overwrite = true)
                }

                tmpExtractDir.deleteRecursively()
                onState(DownloadState.Done)
            } catch (e: Exception) {
                finalDir.deleteRecursively()
                onState(DownloadState.Error(e.message ?: "Unknown error"))
            } finally {
                tmpArchive.delete()
            }
        }.start()
    }

    /** Download every file of an uncompressed model into its directory. On any
     *  failure the model directory is removed so no partial model remains. */
    private fun downloadFiles(ctx: Context, model: Model, onState: (DownloadState) -> Unit) {
        val urls = model.files.map { downloadUrl(model, it.remotePath) }
        val finalDir = modelDir(ctx, model)

        Thread {
            try {
                // Probe lengths once, before touching the target dir, so the overall
                // progress can be byte-weighed. Files without a length count as "1 step".
                val lengths = urls.map { headContentLength(it) }
                var totalUnits = 0.0
                for (len in lengths) totalUnits += if (len > 0) len.toDouble() else 1.0

                finalDir.deleteRecursively()
                finalDir.mkdirs()
                var doneUnits = 0.0
                var index = 0
                for (file in model.files) {
                    val fileUnits = if (lengths[index] > 0) lengths[index].toDouble() else 1.0
                    val dest = File(finalDir, file.localName)
                    downloadFile(urls[index], dest) { written, _ ->
                        var writtenUnits = written.toDouble()
                        if (writtenUnits > fileUnits) writtenUnits = fileUnits
                        val p = ((doneUnits + writtenUnits) / totalUnits).toFloat()
                        onState(DownloadState.Downloading(p, file.localName))
                    }
                    doneUnits += fileUnits
                    index++
                }
                onState(DownloadState.Done)
            } catch (e: Exception) {
                finalDir.deleteRecursively()
                onState(DownloadState.Error(e.message ?: "Unknown error"))
            }
        }.start()
    }

    fun delete(ctx: Context, model: Model) =
        modelDir(ctx, model).deleteRecursively()

    private fun headContentLength(url: String): Long =
        try {
            val response = client.newCall(Request.Builder().url(url).head().build()).execute()
            if (response.isSuccessful) response.body?.contentLength() ?: -1L else -1L
        } catch (e: Exception) { -1L }

    /** Download url into dest, reporting the written byte count and the final
     *  content length (from the GET response, after redirects) via onProgress. */
    private fun downloadFile(
        url: String, dest: File, onProgress: (written: Long, total: Long) -> Unit = { _, _ -> }
    ) {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        val body = response.body ?: throw IOException("Empty response")
        val total = body.contentLength()
        var downloaded = 0L

        body.byteStream().use { src ->
            FileOutputStream(dest).use { dst ->
                val buf = ByteArray(16384)
                var n: Int
                while (src.read(buf).also { n = it } != -1) {
                    dst.write(buf, 0, n)
                    downloaded += n
                    onProgress(downloaded, total)
                }
            }
        }
    }

    /** Extract tar.bz2 to outDir. Validates paths to prevent traversal.
     *  onProgress fires after each extracted file with (filesDone, entryName). */
    fun extractTarBz2(
        archive: File,
        outDir: File,
        onProgress: (filesDone: Int, currentFile: String) -> Unit = { _, _ -> }
    ) {
        outDir.mkdirs()
        val bzIn = BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        TarArchiveInputStream(bzIn).use { tar ->
            var filesDone = 0
            generateSequence { tar.nextEntry }.forEach { entry ->
                val dest = File(outDir, entry.name)
                require(dest.canonicalPath.startsWith(outDir.canonicalPath)) {
                    "Path traversal: ${entry.name}"
                }
                if (entry.isDirectory) dest.mkdirs()
                else {
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { tar.copyTo(it) }
                    filesDone++
                    onProgress(filesDone, entry.name.substringAfterLast('/'))
                }
            }
        }
    }
}