package com.ashxmay.ffvoice

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class VoiceItem(
    val id: String,
    val name: String,
    val emoji: String = "🔊",
    val resId: Int = 0,
    val uri: Uri? = null,
    val filePath: String? = null
)

object VoiceManager {

    private val builtIn = mutableListOf<VoiceItem>()
    private val userVoices = mutableListOf<VoiceItem>()
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        builtIn.clear()
        userVoices.clear()

        // Prefer user uploaded voices (the ones you send via Upload Custom Voice)
        val dir = File(context.filesDir, "user_voices")
        if (dir.exists()) {
            dir.listFiles()?.sortedBy { it.lastModified() }?.forEach { file ->
                if (file.extension.lowercase() in listOf("mp3", "ogg", "wav", "m4a")) {
                    userVoices.add(
                        VoiceItem(
                            id = "user_${file.name}",
                            name = file.nameWithoutExtension.take(20),
                            emoji = "🎤",
                            filePath = file.absolutePath
                        )
                    )
                }
            }
        }

        // If no user voices yet, show a few helpful placeholders
        if (userVoices.isEmpty()) {
            builtIn.add(VoiceItem("demo1", "Upload your voices", "📁", 0))
            builtIn.add(VoiceItem("demo2", "Use Upload button", "⬆️", 0))
            builtIn.add(VoiceItem("demo3", "Then they appear here", "✅", 0))
        }

        initialized = true
        Log.d("VoiceManager", "User voices: ${userVoices.size}, demo: ${builtIn.size}")
    }

    fun getAllVoices(): List<VoiceItem> = userVoices + builtIn

    fun addUserVoice(context: Context, uri: Uri) {
        try {
            val dir = File(context.filesDir, "user_voices")
            if (!dir.exists()) dir.mkdirs()

            val name = "voice_${System.currentTimeMillis()}.mp3"
            val dest = File(dir, name)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }

            // Reset so next init reloads
            initialized = false
            userVoices.add(
                VoiceItem(
                    id = "user_${dest.name}",
                    name = dest.nameWithoutExtension.take(18),
                    emoji = "🎤",
                    filePath = dest.absolutePath
                )
            )
            Log.d("VoiceManager", "Added user voice: ${dest.name}")
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to add voice: ${e.message}")
        }
    }
}
