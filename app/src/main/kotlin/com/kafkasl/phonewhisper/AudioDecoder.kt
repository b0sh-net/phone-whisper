package com.kafkasl.phonewhisper

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes audio from a Uri to 16kHz Mono FloatArray (PCM) using MediaCodec.
 */
object AudioDecoder {
    private const val TAG = "AudioDecoder"
    private const val TARGET_SAMPLE_RATE = 16000

    fun decodeToPcm(context: Context, uri: Uri): FloatArray? {
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
            return null
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        
        // We want to force the output to 16kHz Mono
        val codec = MediaCodec.createDecoderByType(mime)
        val targetFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, TARGET_SAMPLE_RATE, 1)
        
        // Some decoders might not support direct resampling via configure, 
        // but we'll try to get what we can and resample manually if needed.
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmData = mutableListOf<Short>()
        val info = MediaCodec.BufferInfo()
        var isEOS = false
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers

        while (!isEOS) {
            val inIndex = codec.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val buffer = inputBuffers[inIndex]
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            var outIndex = codec.dequeueOutputBuffer(info, 10000)
            while (outIndex >= 0) {
                val buffer = outputBuffers[outIndex]
                val chunk = ShortArray(info.size / 2)
                buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(chunk)
                
                // Basic mono conversion if needed (if output was stereo)
                val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                if (channels == 2) {
                    for (i in 0 until chunk.size / 2) {
                        pcmData.add(chunk[i * 2])
                    }
                } else {
                    for (s in chunk) pcmData.add(s)
                }

                codec.releaseOutputBuffer(outIndex, false)
                outIndex = codec.dequeueOutputBuffer(info, 0)
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        // Convert ShortArray to FloatArray (-1.0 to 1.0)
        // Note: This logic assumes 16kHz was achieved. 
        // If not, we should technically resample here.
        val inputRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val samples = if (inputRate != TARGET_SAMPLE_RATE) {
            resample(pcmData.toShortArray(), inputRate, TARGET_SAMPLE_RATE)
        } else {
            FloatArray(pcmData.size) { pcmData[it].toFloat() / 32768f }
        }

        return samples
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun resample(input: ShortArray, fromRate: Int, toRate: Int): FloatArray {
        // Very basic linear interpolation for resampling
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outSize = (input.size / ratio).toInt()
        val output = FloatArray(outSize)
        for (i in 0 until outSize) {
            val pos = i * ratio
            val index = pos.toInt()
            val frac = pos - index
            if (index + 1 < input.size) {
                val s1 = input[index].toFloat() / 32768f
                val s2 = input[index + 1].toFloat() / 32768f
                output[i] = s1 + frac.toFloat() * (s2 - s1)
            } else if (index < input.size) {
                output[i] = input[index].toFloat() / 32768f
            }
        }
        return output
    }
}
