package net.b0sh.audiotext

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodifica audio da una `Uri` a 16 kHz mono (PCM float).
 *
 * Percorso principale: se il contenuto è un WAV (firma `RIFF`/`WAVE`) viene
 * decodificato direttamente con [WavDecoder] — include i WAV telefonici
 * **G.711 A-law/µ-law** usati dai messaggi della segreteria, per i quali non
 * esiste un decoder di piattaforma garantito.
 *
 * Fallback: per tutti gli altri contenitori (MP3, M4A/AAC, …) resta il percorso
 * `MediaExtractor` + `MediaCodec` esistente. Nessuna eccezione sfugge: in caso
 * di fallimento la funzione restituisce `null`.
 */
object AudioDecoder {
    private const val TAG = "AudioDecoder"
    private const val TARGET_SAMPLE_RATE = 16000

    fun decodeToPcm(context: Context, uri: Uri): FloatArray? {
        // 1) WAV diretto (PCM/float/G.711): sniff dei primi byte, poi decodifica pura.
        val head = readHead(context, uri)
        if (head != null && WavDecoder.isWav(head)) {
            val bytes = readAll(context, uri)
            if (bytes != null && WavDecoder.isWav(bytes)) {
                val decoded = try {
                    WavDecoder.decode(bytes)
                } catch (e: Exception) {
                    Log.e(TAG, "WavDecoder failed: ${e.message}")
                    null
                }
                if (decoded != null) {
                    return WavDecoder.resampleTo16k(decoded.samples, decoded.sampleRate)
                }
            }
        }

        // 2) Fallback MediaCodec per gli altri formati.
        return try {
            decodeWithMediaCodec(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec decode failed: ${e.message}")
            null
        }
    }

    /** Legge i primi [count] byte del contenuto (per lo sniff del formato). */
    private fun readHead(context: Context, uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(12)
            var off = 0
            while (off < buf.size) {
                val r = ins.read(buf, off, buf.size - off)
                if (r < 0) break
                off += r
            }
            if (off < 4) null else buf.copyOf(off)
        }
    } catch (e: Exception) {
        null
    }

    private fun readAll(context: Context, uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read uri: ${e.message}")
        null
    }

    private fun decodeWithMediaCodec(context: Context, uri: Uri): FloatArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set data source: ${e.message}")
            return null
        }

        val trackIndex = selectAudioTrack(extractor)
        if (trackIndex < 0) {
            Log.e(TAG, "No audio track found")
            extractor.release()
            return null
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime == null) {
            extractor.release()
            return null
        }

        // Un decoder assente per il MIME del track (es. formati opzionali) NON deve
        // propagare l'eccezione: si degrada a null.
        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            Log.e(TAG, "No decoder available for $mime: ${e.message}")
            extractor.release()
            return null
        }

        codec.configure(format, null, null, 0)
        codec.start()

        val pcmData = mutableListOf<Short>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers

        // Il loop prosegue finché il decoder non segnala EOS anche in OUTPUT:
        // quando si accoda l'EOS in input possono restare output in coda (l'ultimo
        // chunk di audio reale); fermarsi prima scarterebbe l'audio finale.
        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = inputBuffers[inIndex]
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(info, 10000)
            if (outIndex >= 0) {
                val buffer = outputBuffers[outIndex]
                val chunk = ShortArray(info.size / 2)
                buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(chunk)

                if (channels == 2) {
                    for (i in 0 until chunk.size / 2) {
                        pcmData.add(chunk[i * 2])
                    }
                } else {
                    for (s in chunk) pcmData.add(s)
                }

                val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIndex, false)
                if (eos) outputDone = true
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val inputRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val floats = FloatArray(pcmData.size) { pcmData[it] / 32768f }
        return WavDecoder.resampleTo16k(floats, inputRate)
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }
}
