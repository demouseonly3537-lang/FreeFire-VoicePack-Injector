package com.ashxmay.ffvoice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object VoicePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private const val TAG = "VoicePlayer"
    private val handler = Handler(Looper.getMainLooper())

    fun play(context: Context, voice: VoiceItem) {
        stop()

        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Try to route into voice communication stream (for in-game mic)
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = false
            } catch (_: Exception) {}

            // Case 1: Real resource exists
            if (voice.resId != 0) {
                playFromResource(context, voice.resId)
                return
            }

            // Case 2: User uploaded file
            if (voice.filePath != null) {
                playFromFile(context, voice.filePath)
                return
            }

            // Case 3: URI
            if (voice.uri != null) {
                playFromUri(context, voice.uri)
                return
            }

            // Case 4: No real audio → play short tone so user gets feedback
            Log.w(TAG, "No audio file for ${voice.name}, playing tone fallback")
            playFallbackTone(context, voice.name)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play: ${e.message}", e)
            Toast.makeText(context, "Play failed: ${e.message}", Toast.LENGTH_SHORT).show()
            stop()
        }
    }

    private fun playFromResource(context: Context, resId: Int) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val afd = context.resources.openRawResourceFd(resId)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            setOnCompletionListener { stop() }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Resource play error $what $extra")
                stop()
                true
            }
            prepare()
            start()
        }
    }

    private fun playFromFile(context: Context, path: String) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(path)
            setOnCompletionListener { stop() }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "File play error $what $extra")
                stop()
                true
            }
            prepare()
            start()
        }
    }

    private fun playFromUri(context: Context, uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(context, uri)
            setOnCompletionListener { stop() }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "URI play error $what $extra")
                stop()
                true
            }
            prepare()
            start()
        }
    }

    private fun playFallbackTone(context: Context, name: String) {
        try {
            // STREAM_VOICE_CALL is closer to the mic path
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 85)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 450)

            handler.postDelayed({
                stop()
            }, 500)

            Toast.makeText(context, "Beep: $name\n(Upload real MP3 for actual voice)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Tone failed: ${e.message}")
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
                handler.postDelayed({ stop() }, 450)
            } catch (_: Exception) {}
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

        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
