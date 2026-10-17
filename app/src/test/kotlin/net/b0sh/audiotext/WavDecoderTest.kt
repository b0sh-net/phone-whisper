package net.b0sh.audiotext

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Test JVM puri del decoder WAV/G.711 (nessun device, nessun Robolectric).
 * Vedi `specs/004-alaw-wav-support/tasks.md` (T007/T008/T011).
 */
class WavDecoderTest {

    private val delta = 1.0f / 32768f + 1e-6f

    // -- foundational (T007) --

    @Test fun `isWav detects RIFF WAVE and rejects others`() {
        assertTrue(WavDecoder.isWav(buildWav(1, 1, 8000, 16, byteArrayOf(0, 0))))
        assertFalse(WavDecoder.isWav("ID3\u0003not-a-wav".toByteArray(StandardCharsets.US_ASCII)))
        assertFalse(WavDecoder.isWav(byteArrayOf(0x52, 0x49))) // too short
    }

    @Test fun `decodes PCM 16-bit little-endian samples`() {
        val data = le16(0x1234) + le16(0x0000) + le16(0x8000)
        val wav = buildWav(1, 1, 8000, 16, data)
        val out = WavDecoder.decode(wav)!!
        assertEquals(8000, out.sampleRate)
        assertEquals(3, out.samples.size)
        assertEquals(4660.0 / 32768.0, out.samples[0].toDouble(), delta.toDouble())
        assertEquals(0.0, out.samples[1].toDouble(), delta.toDouble())
        assertEquals(-1.0, out.samples[2].toDouble(), delta.toDouble())
    }

    @Test fun `stereo is downmixed to mono`() {
        // frame1: L=+0.5 R=-0.5 -> 0 ; frame2: L=R=+0.25 -> +0.25
        val data = le16(0x4000) + le16(0xC000) + le16(0x2000) + le16(0x2000)
        val wav = buildWav(1, 2, 8000, 16, data)
        val out = WavDecoder.decode(wav)!!
        assertEquals(2, out.samples.size)
        assertEquals(0.0, out.samples[0].toDouble(), delta.toDouble())
        assertEquals(0.25, out.samples[1].toDouble(), delta.toDouble())
    }

    @Test fun `resample doubles length for 8k and is identity for 16k`() {
        val src = FloatArray(8) { it / 8f }
        assertEquals(16, WavDecoder.resampleTo16k(src, 8000).size)
        assertArrayEquals(src, WavDecoder.resampleTo16k(src, 16000), 1e-6f)
    }

    @Test fun `malformed or empty input returns null`() {
        assertNull(WavDecoder.decode("not a wav".toByteArray(StandardCharsets.US_ASCII)))
        assertNull(WavDecoder.decode(buildWav(1, 1, 8000, 16, ByteArray(0))))
    }

    // -- US1: A-law (T008) --

    @Test fun `decodes A-law known bytes`() {
        // 0xAA -> +32256, 0x2A -> -32256, 0xD5 -> +8 (near silence)
        val wav = buildWav(6, 1, 8000, 8, byteArrayOf(0xAA.toByte(), 0x2A, 0xD5.toByte()))
        val out = WavDecoder.decode(wav)!!
        assertEquals(32256.0 / 32768.0, out.samples[0].toDouble(), delta.toDouble())
        assertEquals(-32256.0 / 32768.0, out.samples[1].toDouble(), delta.toDouble())
        assertEquals(8.0 / 32768.0, out.samples[2].toDouble(), delta.toDouble())
    }

    @Test fun `decodes A-law real-shaped file with fmt 18 and fact chunk`() {
        val data = ByteArray(64) { 0xD5.toByte() }
        val wav = buildWav(
            audioFormat = 6,
            channels = 1,
            sampleRate = 8000,
            bitsPerSample = 8,
            data = data,
            fmtSize = 18,
            extraChunks = listOf("fact" to le32(64)),
        )
        val out = WavDecoder.decode(wav)!!
        assertEquals(8000, out.sampleRate)
        assertEquals(64, out.samples.size)
        assertEquals(8.0 / 32768.0, out.samples[0].toDouble(), delta.toDouble())
    }

    // -- US2: mu-law / PCM8 / EXTENSIBLE (T011) --

    @Test fun `decodes mu-law known bytes`() {
        // 0xFF -> 0, 0x80 -> +32124, 0x00 -> -32124
        val wav = buildWav(7, 1, 8000, 8, byteArrayOf(0xFF.toByte(), 0x80.toByte(), 0x00))
        val out = WavDecoder.decode(wav)!!
        assertEquals(0.0, out.samples[0].toDouble(), delta.toDouble())
        assertEquals(32124.0 / 32768.0, out.samples[1].toDouble(), delta.toDouble())
        assertEquals(-32124.0 / 32768.0, out.samples[2].toDouble(), delta.toDouble())
    }

    @Test fun `decodes unsigned 8-bit PCM`() {
        val wav = buildWav(1, 1, 8000, 8, byteArrayOf(0x00, 0x80.toByte(), 0xFF.toByte()))
        val out = WavDecoder.decode(wav)!!
        assertEquals(-1.0, out.samples[0].toDouble(), delta.toDouble())
        assertEquals(0.0, out.samples[1].toDouble(), delta.toDouble())
        assertEquals(255.0 / 128.0 - 1.0, out.samples[2].toDouble(), delta.toDouble())
    }

    @Test fun `decodes WAVE_FORMAT_EXTENSIBLE with A-law sub-format`() {
        val wav = buildWav(
            audioFormat = 0xFFFE,
            channels = 1,
            sampleRate = 8000,
            bitsPerSample = 8,
            data = byteArrayOf(0xAA.toByte()),
            fmtSize = 40,
            subFormatTag = 6,
        )
        val out = WavDecoder.decode(wav)!!
        assertEquals(32256.0 / 32768.0, out.samples[0].toDouble(), delta.toDouble())
    }

    // -- helpers --

    private fun le16(v: Int) = byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())

    private fun le32(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte(),
    )

    private fun ascii(s: String) = s.toByteArray(StandardCharsets.US_ASCII)

    private fun buildWav(
        audioFormat: Int,
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        data: ByteArray,
        fmtSize: Int = 16,
        subFormatTag: Int? = null,
        extraChunks: List<Pair<String, ByteArray>> = emptyList(),
        declaredDataSize: Int? = null,
    ): ByteArray {
        val bytesPerSample = bitsPerSample / 8
        val byteRate = sampleRate * channels * bytesPerSample
        val blockAlign = channels * bytesPerSample

        val fmt = ByteArrayOutputStream().apply {
            write(le16(audioFormat)); write(le16(channels)); write(le32(sampleRate))
            write(le32(byteRate)); write(le16(blockAlign)); write(le16(bitsPerSample))
            if (fmtSize >= 18) write(le16(0))
            if (fmtSize >= 40) {
                write(le16(bitsPerSample)); write(le32(0))
                val tag = subFormatTag ?: audioFormat
                write(byteArrayOf(
                    (tag and 0xFF).toByte(), ((tag shr 8) and 0xFF).toByte(),
                    0, 0, 0, 0, 0x10, 0, 0x80.toByte(), 0, 0, 0xAA.toByte(), 0, 0x38, 0x9B.toByte(), 0x71,
                ))
            }
        }.toByteArray()

        val body = ByteArrayOutputStream().apply {
            write(ascii("fmt ")); write(le32(fmt.size)); write(fmt)
            for ((id, payload) in extraChunks) {
                write(ascii(id)); write(le32(payload.size)); write(payload)
                if (payload.size % 2 == 1) write(0)
            }
            write(ascii("data")); write(le32(declaredDataSize ?: data.size)); write(data)
            if (data.size % 2 == 1) write(0)
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            write(ascii("RIFF")); write(le32(4 + body.size)); write(ascii("WAVE")); write(body)
        }.toByteArray()
    }
}
