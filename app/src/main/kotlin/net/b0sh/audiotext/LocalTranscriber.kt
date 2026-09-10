package net.b0sh.audiotext

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File

/**
 * Local on-device transcription via sherpa-onnx.
 * Models are loaded from the app's external files dir.
 *
 * Supports two model families:
 * - Offline models (Whisper, Moonshine, NeMo non-streaming): decoded in one pass.
 * - Streaming models (Kroko Zipformer2): decoded incrementally with tail padding
 *   and an is_ready loop, following the sherpa-onnx online ASR pattern.
 */
class LocalTranscriber private constructor(
    private val recognizer: OfflineRecognizer?,
    private val streamingRecognizer: OnlineRecognizer?
) {

    /** True when this instance wraps a streaming (online) recognizer. */
    val isStreaming: Boolean get() = streamingRecognizer != null

    /**
     * Transcribe raw PCM float samples. Blocking — call from background thread.
     * For streaming models, follows the sherpa-onnx pattern: feed the whole audio,
     * add 0.5s of tail padding, signal end of input, then decode until ready.
     */
    fun transcribe(samples: FloatArray, sampleRate: Int = 16000): String {
        if (streamingRecognizer != null) {
            return transcribeStreaming(streamingRecognizer, samples, sampleRate)
        }
        return transcribeOffline(recognizer!!, samples, sampleRate)
    }

    private fun transcribeOffline(recognizer: OfflineRecognizer, samples: FloatArray, sampleRate: Int): String {
        val stream = recognizer.createStream()
        stream.acceptWaveform(samples, sampleRate)
        recognizer.decode(stream)
        val result = recognizer.getResult(stream)
        stream.release()
        return result.text.trim()
    }

    private fun transcribeStreaming(recognizer: OnlineRecognizer, samples: FloatArray, sampleRate: Int): String {
        val stream = recognizer.createStream()

        // Feed the entire audio, then add tail padding and signal end of input
        stream.acceptWaveform(samples, sampleRate)
        val tail = FloatArray(sampleRate / 2) { 0f }  // 0.5s of silence
        stream.acceptWaveform(tail, sampleRate)
        stream.inputFinished()

        // Decode until the stream has consumed everything
        while (recognizer.isReady(stream)) {
            recognizer.decode(stream)
        }

        val result = recognizer.getResult(stream)
        stream.release()
        return result.text.trim()
    }

    companion object {
        private const val TAG = "LocalTranscriber"

        /** Find available model dirs under the app's files/models/ dir */
        fun availableModels(ctx: Context): List<String> {
            val modelsDir = File(ctx.filesDir, "models")
            if (!modelsDir.exists()) return emptyList()
            return modelsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        }

        /** Create a LocalTranscriber for the given model directory name. Returns null on failure. */
        fun create(ctx: Context, modelName: String): LocalTranscriber? {
            val modelDir = File(ctx.filesDir, "models/$modelName")
            if (!modelDir.exists()) {
                Log.e(TAG, "Model dir not found: $modelDir")
                return null
            }

            Log.e(TAG, "create() called with modelName=$modelName")
            logDir(modelDir)
            logMemory()
            Log.e(TAG, "Available classes: " + com.k2fsa.sherpa.onnx.OfflineRecognizer::class.java.name)

            // Detect streaming (online) models BEFORE building any config, since creating
            // an OfflineRecognizer with a streaming graph crashes natively (SIGABRT).
            val isStreaming = hasCachedInputs(File(modelDir, "encoder.int8.onnx"))
                || hasCachedInputs(File(modelDir, "encoder.onnx"))
            if (isStreaming) {
                Log.i(TAG, "Detected streaming (online) model — using OnlineRecognizer")
                return createStreaming(ctx, modelDir)
            }

            val config = detectModelConfig(modelDir) ?: run {
                Log.e(TAG, "Could not detect model type in $modelDir")
                return null
            }
            logModelConfig(config)

            return try {
                Log.e(TAG, "About to call OfflineRecognizer constructor")
                val recognizer = OfflineRecognizer(null, config)
                Log.i(TAG, "Loaded model: $modelName")
                LocalTranscriber(recognizer, null)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "UnsatisfiedLinkError: ${e.message}")
                Log.e(TAG, "Available .so files in APK: " + ctx.assets.list("libs")?.joinToString(", ") { "" })
                Log.e(TAG, "JNI lib check: " + try {
                    val libsDir = ctx.applicationInfo.nativeLibraryDir
                    File(libsDir).listFiles()?.map { it.name }?.joinToString(", ")
                } catch (e2: Exception) { "N/A: ${e2.message}" })
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: NoSuchMethodError) {
                Log.e(TAG, "NoSuchMethodError: ${e.message}")
                Log.e(TAG, "Available OfflineRecognizer constructors:")
                for (ctor in com.k2fsa.sherpa.onnx.OfflineRecognizer::class.java.constructors) {
                    Log.e(TAG, "  " + ctor.parameterTypes.joinToString(", ") { it.simpleName })
                }
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OutOfMemoryError: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: ${e.javaClass.name}: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error loading model: ${e.javaClass.name}: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            }
        }

        /** Build a streaming (online) recognizer for the given model dir containing a transducer. */
        private fun createStreaming(ctx: Context, modelDir: File): LocalTranscriber? {
            val p = modelDir.absolutePath
            val tokens = findTokensFile(p) ?: run {
                Log.e(TAG, "No tokens file found in $p")
                return null
            }
            val encoder = findFileContaining(p, "-encoder")
                ?: findFileStarting(p, "encoder") ?: run {
                    Log.e(TAG, "No encoder file found in $p")
                    return null
                }
            val decoder = findFileContaining(p, "-decoder")
                ?: findFileStarting(p, "decoder") ?: run {
                    Log.e(TAG, "No decoder file found in $p")
                    return null
                }
            val joiner = findFileContaining(p, "-joiner")
                ?: findFileStarting(p, "joiner") ?: run {
                    Log.e(TAG, "No joiner file found in $p")
                    return null
                }

            val transducerConfig = OnlineTransducerModelConfig(
                encoder = encoder,
                decoder = decoder,
                joiner = joiner
            )
            val modelConfig = OnlineModelConfig(
                transducer = transducerConfig,
                tokens = tokens,
                numThreads = 2,
                modelType = "nemo_transducer"
            )

            Log.e(TAG, "Streaming transducer: encoder=$encoder")
            Log.e(TAG, "Streaming transducer: decoder=$decoder")
            Log.e(TAG, "Streaming transducer: joiner=$joiner")

            val config = OnlineRecognizerConfig(
                modelConfig = modelConfig,
                decodingMethod = "greedy_search"
            )

            return try {
                Log.e(TAG, "About to call OnlineRecognizer constructor")
                val recognizer = OnlineRecognizer(null, config)
                Log.i(TAG, "Loaded streaming model")
                LocalTranscriber(null, recognizer)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OutOfMemoryError: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load streaming model: ${e.javaClass.name}: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error loading streaming model: ${e.javaClass.name}: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)
                null
            }
        }

        /**
         * Heuristic: streaming (online) transducer encoders expose recurrent cache
         * states (cached_key/cached_val/cached_conv/embed_states/processed_lens).
         * Offline model graphs never contain these node names.
         * Non-destructive: scans the file in small blocks without loading it all.
         */
        private fun hasCachedInputs(file: File): Boolean {
            if (!file.exists()) return false
            return try {
                val stream = file.inputStream()
                try {
                    containsMarker(stream, "cached_key".toByteArray())
                } finally {
                    stream.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not scan ${file.name} for streaming markers: ${e.message}")
                false
            }
        }

        /** Scan a stream for a byte marker, reading in 64 KiB blocks with overlap. */
        private fun containsMarker(stream: java.io.InputStream, marker: ByteArray): Boolean {
            val blockSize = 65536
            val buf = ByteArray(blockSize)
            var overlap = ByteArray(0)
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                if (n == 0) continue
                val window = ByteArray(overlap.size + n) { if (it < overlap.size) overlap[it] else buf[it - overlap.size] }
                if (indexOf(window, marker) >= 0) return true
                // Keep the last (marker.size - 1) bytes for the next overlap
                val keep = if (marker.size - 1 < window.size) marker.size - 1 else window.size
                overlap = ByteArray(keep) { window[window.size - keep + it] }
            }
            return false
        }

        /** Byte index of a marker in a byte array, or -1. */
        private fun indexOf(bytes: ByteArray, marker: ByteArray): Int {
            if (marker.size == 0) return 0
            if (marker.size > bytes.size) return -1
            for (i in 0..(bytes.size - marker.size)) {
                var found = true
                for (j in 0 until marker.size) {
                    if (bytes[i + j] != marker[j]) {
                        found = false
                        break
                    }
                }
                if (found) return i
            }
            return -1
        }

        /** Log the contents of a model directory (sizes included). */
        private fun logDir(dir: File) {
            val files = dir.listFiles()
            Log.e(TAG, "Model dir: $dir")
            if (files == null) {
                Log.e(TAG, "  (cannot list)")
                return
            }
            for (f in files) {
                if (f.isFile) Log.e(TAG, "  ${f.name}  ${f.length()} bytes")
                else Log.e(TAG, "  ${f.name}/ (dir)")
            }
        }

        /** Log the available memory, from /proc/meminfo when present, else the JVM heap. */
        private fun logMemory() {
            try {
                val meminfo = File("/proc/meminfo")
                if (meminfo.exists()) {
                    val text = meminfo.readText()
                    Log.e(TAG, "System memory: total=${meminfoValue(text, "MemTotal")} kB, " +
                        "available=${meminfoValue(text, "MemAvailable")} kB")
                    return
                }
            } catch (e: Exception) { }
            val rt = Runtime.getRuntime()
            Log.e(TAG, "JVM heap: " + (rt.freeMemory() / 1024 / 1024) +
                " MB free / " + (rt.totalMemory() / 1024 / 1024) + " MB")
        }

        private fun meminfoValue(text: String, key: String): String {
            val line = text.lines().map { it.trim() }.find { it.startsWith("$key:") } ?: return "?"
            val parts = line.split("\\s+")
            return if (parts.size >= 2) parts[1] else "?"
        }

        /** Log the resolved recognizer config so the load failure can be correlated with the chosen model type. */
        private fun logModelConfig(config: OfflineRecognizerConfig) {
            val mc = config.modelConfig
            Log.e(TAG, "Detected config: modelType=${mc.modelType}, tokens=${mc.tokens}, numThreads=${mc.numThreads}")
            val tr = mc.transducer
            if (tr != null && tr.encoder.isNotEmpty()) {
                Log.e(TAG, "  transducer: encoder=${tr.encoder}")
                Log.e(TAG, "  transducer: decoder=${tr.decoder}")
                Log.e(TAG, "  transducer: joiner=${tr.joiner}")
            }
            val wh = mc.whisper
            if (wh != null && wh.encoder.isNotEmpty()) {
                Log.e(TAG, "  whisper: encoder=${wh.encoder}, decoder=${wh.decoder}")
            }
            val mn = mc.moonshine
            if (mn != null && mn.encoder.isNotEmpty()) {
                Log.e(TAG, "  moonshine: encoder=${mn.encoder}, mergedDecoder=${mn.mergedDecoder}")
            }
            val nm = mc.nemo
            if (nm != null && nm.model.isNotEmpty()) {
                Log.e(TAG, "  nemo: model=${nm.model}")
            }
        }

        /** Auto-detect model type from files present in the directory. */
        private fun detectModelConfig(dir: File): OfflineRecognizerConfig? {
            val p = dir.absolutePath

            // Find tokens.txt — it might be named literally "tokens.txt" or "xxx-tokens.txt" or "xxx.tokens.txt"
            val tokens = findTokensFile(p) ?: run {
                Log.e(TAG, "No tokens file found in $p")
                return null
            }

            // Moonshine v1: has preprocess.onnx, encode, uncached_decode, cached_decode
            val preprocessFile = findFileExact(p, "preprocess.onnx")
                ?: findFileStarting(p, "preprocess")
            if (preprocessFile != null) {
                val moonshineConfig = OfflineMoonshineModelConfig(
                    preprocessor = preprocessFile,
                    encoder = findFileStarting(p, "encode")?.takeIf { !it.contains("decode") } ?: return null,
                    uncachedDecoder = findFileStarting(p, "uncached_decode") ?: return null,
                    cachedDecoder = findFileStarting(p, "cached_decode") ?: return null
                )

                val modelConfig = OfflineModelConfig(
                    moonshine = moonshineConfig,
                    tokens = tokens,
                    numThreads = 2
                )

                return OfflineRecognizerConfig(
                    modelConfig = modelConfig
                )
            }

            // Moonshine v2: has encoder + mergedDecoder (no preprocess, no uncached/cached)
            val moonshineEncoder = findFileContaining(p, "-encoder-")
                ?: findFileContaining(p, "-encode")
            if (moonshineEncoder != null && findFileExact(p, "merged.onnx") != null) {
                val merged = findFileExact(p, "merged.onnx")
                    ?: findFileStarting(p, "merged")
                    ?: findFileContaining(p, "merged_decode")

                val moonshineConfig = OfflineMoonshineModelConfig(
                    encoder = moonshineEncoder,
                    mergedDecoder = merged ?: return null
                )

                val modelConfig = OfflineModelConfig(
                    moonshine = moonshineConfig,
                    tokens = tokens,
                    numThreads = 2
                )

                return OfflineRecognizerConfig(
                    modelConfig = modelConfig
                )
            }

            // Whisper: has encoder + decoder files containing "encoder" / "decoder" in their names, no joiner
            val whisperEncoder = findFileContaining(p, "-encoder")
                ?: findFileStarting(p, "encoder")
            val whisperDecoder = findFileContaining(p, "-decoder")
                ?: findFileStarting(p, "decoder")
            if (whisperEncoder != null && whisperDecoder != null) {
                // Make sure it's not a transducer (no joiner file)
                if (findFileContaining(p, "joiner") == null
                    && findFileStarting(p, "joiner") == null
                ) {
                    val whisperConfig = OfflineWhisperModelConfig(
                        encoder = whisperEncoder,
                        decoder = whisperDecoder
                    )

                    val modelConfig = OfflineModelConfig(
                        whisper = whisperConfig,
                        tokens = tokens,
                        numThreads = 2,
                        modelType = "whisper"
                    )

                    return OfflineRecognizerConfig(
                        modelConfig = modelConfig
                    )
                }
            }

            // NeMo transducer / Parakeet TDT (has encoder + decoder + joiner)
            val encoder = findFileContaining(p, "-encoder")
                ?: findFileStarting(p, "encoder")
            val decoder = findFileContaining(p, "-decoder")
                ?: findFileStarting(p, "decoder")
            val joiner = findFileContaining(p, "-joiner")
                ?: findFileStarting(p, "joiner")
            if (encoder != null && decoder != null && joiner != null) {
                val transducerConfig = OfflineTransducerModelConfig(
                    encoder = encoder,
                    decoder = decoder,
                    joiner = joiner
                )

                val modelConfig = OfflineModelConfig(
                    transducer = transducerConfig,
                    tokens = tokens,
                    numThreads = 2,
                    modelType = "nemo_transducer"
                )

                return OfflineRecognizerConfig(
                    modelConfig = modelConfig
                )
            }

            // NeMo CTC (single model.onnx / model.int8.onnx)
            val ctcModel = findFileStarting(p, "model")
            if (ctcModel != null) {
                val nemoConfig = OfflineNemoEncDecCtcModelConfig(
                    model = ctcModel
                )

                val modelConfig = OfflineModelConfig(
                    nemo = nemoConfig,
                    tokens = tokens,
                    numThreads = 2
                )

                return OfflineRecognizerConfig(
                    modelConfig = modelConfig
                )
            }

            return null
        }

        /** Find the tokens file, which can be named "tokens.txt", "xxx-tokens.txt", or "xxx.tokens.txt". */
        private fun findTokensFile(dir: String): String? {
            val d = File(dir)
            return d.listFiles()?.firstOrNull {
                it.name == "tokens.txt"
                    || it.name.endsWith("-tokens.txt")
                    || it.name.endsWith(".tokens.txt")
            }?.absolutePath
        }

        /** Find a file with exact name match (case-sensitive). */
        private fun findFileExact(dir: String, name: String): String? {
            val d = File(dir)
            return d.listFiles()?.firstOrNull { it.name == name }?.absolutePath
        }

        /** Find a file whose name starts with the given prefix. */
        private fun findFileStarting(dir: String, prefix: String): String? {
            val d = File(dir)
            // Prefer int8 quantized
            d.listFiles()?.firstOrNull { it.name.startsWith(prefix) && it.name.contains("int8") }
                ?.let { return it.absolutePath }
            // Fallback to any onnx/ort
            return d.listFiles()?.firstOrNull {
                it.name.startsWith(prefix) && (it.name.endsWith(".onnx") || it.name.endsWith(".ort"))
            }?.absolutePath
        }

        /** Find a file whose name contains the given substring. */
        private fun findFileContaining(dir: String, substring: String): String? {
            val d = File(dir)
            // Prefer int8 quantized
            d.listFiles()?.firstOrNull { it.name.contains(substring) && it.name.contains("int8") }
                ?.let { return it.absolutePath }
            // Fallback to any onnx/ort
            return d.listFiles()?.firstOrNull {
                it.name.contains(substring) && (it.name.endsWith(".onnx") || it.name.endsWith(".ort"))
            }?.absolutePath
        }

        /** Find first file matching prefix (prefer int8 quantized). Legacy — use findFileStarting or findFileContaining. */
        @Deprecated("Use findFileStarting or findFileContaining instead", ReplaceWith("findFileStarting(dir, prefix)"))
        private fun findFile(dir: String, prefix: String): String? = findFileStarting(dir, prefix)
    }
}