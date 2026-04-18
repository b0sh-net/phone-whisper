package com.kafkasl.phonewhisper

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File

/**
 * Local on-device transcription via sherpa-onnx.
 * Models are loaded from the app's external files dir.
 */
class LocalTranscriber private constructor(private val recognizer: OfflineRecognizer) {

    /** Transcribe raw PCM float samples. Blocking — call from background thread. */
    fun transcribe(samples: FloatArray, sampleRate: Int = 16000): String {
        val stream = recognizer.createStream()
        stream.acceptWaveform(samples, sampleRate)
        recognizer.decode(stream)
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

            val config = detectModelConfig(modelDir) ?: run {
                Log.e(TAG, "Could not detect model type in $modelDir")
                return null
            }

            return try {
                val recognizer = OfflineRecognizer(assetManager = null, config = config)
                Log.i(TAG, "Loaded model: $modelName")
                LocalTranscriber(recognizer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: ${e.message}")
                null
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
                return OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        moonshine = OfflineMoonshineModelConfig(
                            preprocessor = preprocessFile,
                            encoder = findFileStarting(p, "encode")?.takeIf { !it.contains("decode") }
                                ?: return null,
                            uncachedDecoder = findFileStarting(p, "uncached_decode") ?: return null,
                            cachedDecoder = findFileStarting(p, "cached_decode") ?: return null,
                        ),
                        tokens = tokens,
                        numThreads = 2,
                    )
                )
            }

            // Moonshine v2: has encoder + mergedDecoder (no preprocess, no uncached/cached)
            val moonshineEncoder = findFileContaining(p, "-encoder-")
                ?: findFileContaining(p, "-encode")
            if (moonshineEncoder != null && findFileExact(p, "merged.onnx") != null) {
                val merged = findFileExact(p, "merged.onnx")
                    ?: findFileStarting(p, "merged")
                    ?: findFileContaining(p, "merged_decode")
                return OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        moonshine = OfflineMoonshineModelConfig(
                            encoder = moonshineEncoder,
                            mergedDecoder = merged ?: return null,
                        ),
                        tokens = tokens,
                        numThreads = 2,
                    )
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
                    return OfflineRecognizerConfig(
                        modelConfig = OfflineModelConfig(
                            whisper = OfflineWhisperModelConfig(
                                encoder = whisperEncoder,
                                decoder = whisperDecoder,
                            ),
                            tokens = tokens,
                            numThreads = 2,
                            modelType = "whisper",
                        )
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
                return OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(
                            encoder = encoder,
                            decoder = decoder,
                            joiner = joiner,
                        ),
                        tokens = tokens,
                        numThreads = 2,
                        modelType = "nemo_transducer",
                    )
                )
            }

            // NeMo CTC (single model.onnx / model.int8.onnx)
            val ctcModel = findFileStarting(p, "model")
            if (ctcModel != null) {
                return OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        nemo = OfflineNemoEncDecCtcModelConfig(model = ctcModel),
                        tokens = tokens,
                        numThreads = 2,
                    )
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
