package com.kafkasl.phonewhisper

import android.content.Context
import android.util.Log

/**
 * Singleton to manage and share the LocalTranscriber instance across activities.
 * This avoids reloading heavy models when switching between configuration and transcription.
 */
object TranscriberManager {
    private const val TAG = "TranscriberManager"
    private var localTranscriber: LocalTranscriber? = null

    @Synchronized
    fun getOrCreateTranscriber(ctx: Context): LocalTranscriber? {
        // If we already have one, just return it
        localTranscriber?.let { return it }

        val prefs = ctx.getSharedPreferences("phonewhisper", Context.MODE_PRIVATE)
        
        // Check if the user's preferred model is installed
        val preferredModel = prefs.getString("model_name", "")
        if (!preferredModel.isNullOrBlank()) {
            val installed = MODEL_CATALOG.find { it.archive == preferredModel }?.let {
                ModelDownloader.isInstalled(ctx, it)
            } ?: false
            
            if (installed) {
                Log.i(TAG, "Loading preferred model: $preferredModel")
                localTranscriber = LocalTranscriber.create(ctx, preferredModel!!)
                localTranscriber?.let { return it }
            }
        }

        // Fall back to any available installed model
        val available = LocalTranscriber.availableModels(ctx)
        if (available.isNotEmpty()) {
            Log.i(TAG, "Loading fallback model: ${available.first()}")
            localTranscriber = LocalTranscriber.create(ctx, available.first())
            return localTranscriber
        }

        Log.w(TAG, "No models available to load")
        return null
    }

    /** Clear the current instance, e.g., if a new model is selected. */
    @Synchronized
    fun reset() {
        localTranscriber = null
    }
}
