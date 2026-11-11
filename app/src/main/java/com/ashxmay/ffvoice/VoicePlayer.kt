package com.ashxmay.ffvoice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object VoicePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private const val TAG = "VoicePlayer"

    fun play(context: Context, voice: VoiceItem) {
        stop()

        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Core bypass technique used by most non-root voice apps
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = false

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                when {
                    voice.resId != 0 -> {
                        val afd = context.resources.openRawResourceFd(voice.resId)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                    voice.uri != null -> {
                        setDataSource(context, voice.uri!!)
                    }
                    voice.filePath != null -> {
                        setDataSource(voice.filePath)
                    }
                    else -> {
                        Log.e(TAG, "No source for voice ${voice.name}")
                        return
                    }
                }

                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Play error $what $extra")
                    stop()
                    true
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play: ${e.message}")
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
