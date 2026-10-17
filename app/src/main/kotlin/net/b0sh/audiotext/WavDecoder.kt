package net.b0sh.audiotext

import kotlin.math.min

/**
 * Decoder RIFF/WAVE puro (nessun import `android.*`) per i WAV in formato
 * telefonico e non. Vedi `specs/004-alaw-wav-support/contracts/index.md` (C1).
 *
 * Gestisce:
 * - PCM unsigned 8 bit, signed 16/24/32 bit, IEEE float32;
 * - G.711 **A-law** (tag 6) e **µ-law** (tag 7) — i formati dei messaggi della
 *   segreteria Iliad;
 * - `fmt ` da 16/18/40 byte (incl. `WAVE_FORMAT_EXTENSIBLE`), chunk aggiuntivi
 *   (`fact`, `LIST`, …) e padding di allineamento.
 *
 * Output: campioni **mono** normalizzati in `[-1, 1]` alla frequenza di origine.
 * Qualunque contenuto non valido/non supportato restituisce `null` e non lancia.
 */
object WavDecoder {

    data class DecodedAudio(val samples: FloatArray, val sampleRate: Int)

    private const val TAG_PCM = 1
    private const val TAG_FLOAT = 3
    private const val TAG_ALAW = 6
    private const val TAG_MLAW = 7
    private const val TAG_EXTENSIBLE = 0xFFFE

    private const val TARGET_SAMPLE_RATE = 16000

    private class Fmt(val tag: Int, val channels: Int, val sampleRate: Int, val bitsPerSample: Int)

    /** True se i byte iniziano con la firma RIFF/WAVE. Non lancia. */
    fun isWav(bytes: ByteArray): Boolean =
        bytes.size >= 12 &&
            ascii(bytes, 0, 4) == "RIFF" &&
            ascii(bytes, 8, 4) == "WAVE"

    /** Decodifica il WAV a mono normalizzato alla frequenza di origine, oppure `null`. */
    fun decode(bytes: ByteArray): DecodedAudio? {
        if (!isWav(bytes)) return null

        var fmt: Fmt? = null
        var dataOffset = -1
        var dataSize = 0L

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = ascii(bytes, offset, 4)
            val size = u32le(bytes, offset + 4)
            val start = offset + 8
            if (start > bytes.size) break

            when (id) {
                "fmt " -> fmt = parseFmt(bytes, start, size)
                "data" -> {
                    dataOffset = start
                    dataSize = min(size, (bytes.size - start).toLong())
                    break
                }
            }

            val next = start.toLong() + size + (size and 1L) // padding a parola
            if (next <= offset) break
            offset = if (next >= bytes.size.toLong()) bytes.size else next.toInt()
        }

        val f = fmt ?: return null
        if (dataOffset < 0 || dataSize <= 0L) return null
        if (f.channels < 1 || f.sampleRate <= 0) return null

        return decodePayload(bytes, f, dataOffset, dataSize)
    }

    /** Ricampionamento lineare a 16 kHz (identità se la sorgente è già a 16 kHz). */
    fun resampleTo16k(samples: FloatArray, fromRate: Int): FloatArray {
        if (fromRate <= 0 || fromRate == TARGET_SAMPLE_RATE || samples.isEmpty()) return samples
        val ratio = fromRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        val outSize = (samples.size / ratio).toInt()
        if (outSize <= 0) return FloatArray(0)
        val out = FloatArray(outSize)
        for (i in 0 until outSize) {
            val pos = i * ratio
            val index = pos.toInt()
            val frac = (pos - index).toFloat()
            out[i] = if (index + 1 < samples.size) {
                samples[index] + frac * (samples[index + 1] - samples[index])
            } else {
                samples[index.coerceAtMost(samples.size - 1)]
            }
        }
        return out
    }

    private fun parseFmt(bytes: ByteArray, start: Int, chunkSize: Long): Fmt? {
        if (chunkSize < 16L || start + 16 > bytes.size) return null
        var tag = u16le(bytes, start)
        val channels = u16le(bytes, start + 2)
        val sampleRate = u32le(bytes, start + 4).toInt()
        val bits = u16le(bytes, start + 14)
        if (tag == TAG_EXTENSIBLE) {
            if (chunkSize < 40L || start + 26 > bytes.size) return null
            tag = u16le(bytes, start + 24) // primo campo del SubFormat GUID
        }
        return Fmt(tag, channels, sampleRate, bits)
    }

    private fun decodePayload(bytes: ByteArray, f: Fmt, dataOffset: Int, dataSize: Long): DecodedAudio? {
        val bytesPerSample = when (f.tag) {
            TAG_PCM -> f.bitsPerSample / 8
            TAG_FLOAT -> 4
            TAG_ALAW, TAG_MLAW -> 1
            else -> return null
        }
        if (bytesPerSample <= 0) return null
        val frameSize = f.channels * bytesPerSample
        val frames = (dataSize / frameSize).toInt()
        if (frames <= 0) return null

        val out = FloatArray(frames)
        var p = dataOffset
        for (i in 0 until frames) {
            var acc = 0f
            for (c in 0 until f.channels) {
                acc += sampleAt(bytes, p, f.tag, f.bitsPerSample)
                p += bytesPerSample
            }
            out[i] = acc / f.channels
        }
        return DecodedAudio(out, f.sampleRate)
    }

    private fun sampleAt(b: ByteArray, p: Int, tag: Int, bits: Int): Float = when (tag) {
        TAG_ALAW -> alawToPcm16(b[p].toInt() and 0xFF) / 32768f
        TAG_MLAW -> mlawToPcm16(b[p].toInt() and 0xFF) / 32768f
        TAG_FLOAT -> Float.fromBits(i32le(b, p))
        TAG_PCM -> when (bits) {
            8 -> ((b[p].toInt() and 0xFF) - 128) / 128f
            16 -> s16le(b, p) / 32768f
            24 -> s24le(b, p) / 8388608f
            32 -> i32le(b, p) / 2147483648f
            else -> 0f
        }
        else -> 0f
    }

    /** G.711 A-law -> PCM lineare 16-bit (algoritmo canonico). */
    private fun alawToPcm16(aVal: Int): Int {
        val a = aVal xor 0x55
        var t = (a and 0x0F) shl 4
        when ((a and 0x70) shr 4) {
            0 -> t += 8
            1 -> t += 0x108
            else -> { t += 0x108; t = t shl (((a and 0x70) shr 4) - 1) }
        }
        return if (a and 0x80 != 0) t else -t
    }

    /** G.711 µ-law -> PCM lineare 16-bit (algoritmo canonico). */
    private fun mlawToPcm16(uVal: Int): Int {
        val u = uVal.inv() and 0xFF
        var t = ((u and 0x0F) shl 3) + 0x84
        t = t shl ((u and 0x70) shr 4)
        return if (u and 0x80 != 0) 0x84 - t else t - 0x84
    }

    private fun ascii(b: ByteArray, p: Int, n: Int): String = String(b, p, n, Charsets.US_ASCII)

    private fun u16le(b: ByteArray, p: Int): Int =
        (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8)

    private fun u32le(b: ByteArray, p: Int): Long =
        (b[p].toLong() and 0xFF) or
            ((b[p + 1].toLong() and 0xFF) shl 8) or
            ((b[p + 2].toLong() and 0xFF) shl 16) or
            ((b[p + 3].toLong() and 0xFF) shl 24)

    private fun i32le(b: ByteArray, p: Int): Int =
        (b[p].toInt() and 0xFF) or
            ((b[p + 1].toInt() and 0xFF) shl 8) or
            ((b[p + 2].toInt() and 0xFF) shl 16) or
            ((b[p + 3].toInt() and 0xFF) shl 24)

    private fun s16le(b: ByteArray, p: Int): Int {
        val v = u16le(b, p)
        return if (v >= 0x8000) v - 0x10000 else v
    }

    private fun s24le(b: ByteArray, p: Int): Int {
        val v = (b[p].toInt() and 0xFF) or
            ((b[p + 1].toInt() and 0xFF) shl 8) or
            ((b[p + 2].toInt() and 0xFF) shl 16)
        return if (v >= 0x800000) v - 0x1000000 else v
    }
}
