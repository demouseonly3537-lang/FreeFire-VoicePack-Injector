package com.ashxmay.ffvoice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object VoicePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var localPlayer: MediaPlayer? = null   // second player so user always hears in earphones
    private var toneGenerator: ToneGenerator? = null
    private var audioManager: AudioManager? = null
    private var previousMode = AudioManager.MODE_NORMAL
    private var focusRequest: AudioFocusRequest? = null
    private const val TAG = "VoicePlayer"
    private val handler = Handler(Looper.getMainLooper())

    fun play(context: Context, voice: VoiceItem) {
        stop()

        try {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val am = audioManager!!

            previousMode = am.mode

            // 1. Force communication mode so system treats this as voice chat audio
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
            } catch (_: Exception) {}

            // 2. Request audio focus for voice communication
            requestFocus(am)

            // 3. Headset / earphone handling
            val isWiredHeadset = am.isWiredHeadsetOn
            val isBluetooth = am.isBluetoothA2dpOn || am.isBluetoothScoOn

            if (isWiredHeadset || isBluetooth) {
                // Keep SCO / communication path for injection attempt
                try {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                } catch (_: Exception) {}
                // Do NOT force speaker on when headset is connected
                am.isSpeakerphoneOn = false
            } else {
                // No headset → allow speaker so user can hear
                am.isSpeakerphoneOn = true
            }

            // 4. Play the real voice on VOICE_COMMUNICATION stream (this is what may reach teammates)
            when {
                voice.resId != 0 -> playOnCommStream(context, voice.resId, null, null)
                voice.filePath != null -> playOnCommStream(context, 0, voice.filePath, null)
                voice.uri != null -> playOnCommStream(context, 0, null, voice.uri)
                else -> {
                    playFallbackTone(context, voice.name)
                    return
                }
            }

            // 5. Also play a local copy on MUSIC stream so user ALWAYS hears it
            //    (even when earphones are connected and communication stream is silent for them)
            handler.postDelayed({
                playLocalCopy(context, voice)
            }, 80)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play: ${e.message}", e)
            Toast.makeText(context, "Play failed: ${e.message}", Toast.LENGTH_SHORT).show()
            stop()
        }
    }

    private fun requestFocus(am: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attr)
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                am.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (_: Exception) {}
    }

    private fun playOnCommStream(context: Context, resId: Int, path: String?, uri: Uri?) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                    .build()
            )

            when {
                resId != 0 -> {
                    val afd = context.resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                path != null -> setDataSource(path)
                uri != null -> setDataSource(context, uri)
            }

            setOnCompletionListener {
                // keep mode a bit longer so game can finish sending
                handler.postDelayed({ stop() }, 300)
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Comm play error $what $extra")
                true
            }
            prepare()
            start()
        }
    }

    /** Local copy so user hears the sound even with earphones */
    private fun playLocalCopy(context: Context, voice: VoiceItem) {
        try {
            localPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                when {
                    voice.resId != 0 -> {
                        val afd = context.resources.openRawResourceFd(voice.resId)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                    voice.filePath != null -> setDataSource(voice.filePath)
                    voice.uri != null -> setDataSource(context, voice.uri!!)
                    else -> return
                }

                setOnCompletionListener {
                    try { release() } catch (_: Exception) {}
                    localPlayer = null
                }
                setVolume(0.85f, 0.85f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local copy failed: ${e.message}")
        }
    }

    private fun playFallbackTone(context: Context, name: String) {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 90)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
            handler.postDelayed({ stop() }, 450)
            Toast.makeText(context, "Beep: $name", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                handler.postDelayed({ stop() }, 400)
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
            localPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        localPlayer = null

        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null

        // Restore previous audio mode
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    am.abandonAudioFocusRequest(focusRequest!!)
                }
                am.mode = previousMode
                am.isSpeakerphoneOn = false
                try {
                    am.stopBluetoothSco()
                    am.isBluetoothScoOn = false
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
