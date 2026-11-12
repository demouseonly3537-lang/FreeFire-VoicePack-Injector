package com.ashxmay.ffvoice

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
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

        // 1. Load all user-uploaded voices (this is the main source of real sounds)
        val dir = File(context.filesDir, "user_voices")
        if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.extension.lowercase() in listOf("mp3", "ogg", "wav", "m4a") }
                ?.sortedBy { it.lastModified() }
                ?.forEach { file ->
                    userVoices.add(
                        VoiceItem(
                            id = "user_${file.name}",
                            name = file.nameWithoutExtension.take(22),
                            emoji = "🎤",
                            filePath = file.absolutePath
                        )
                    )
                }
        }

        // 2. If still empty, show clear instruction cards
        if (userVoices.isEmpty()) {
            builtIn.add(VoiceItem("help1", "1. Upload voices", "⬆️", 0))
            builtIn.add(VoiceItem("help2", "2. Then play in game", "🎮", 0))
            builtIn.add(VoiceItem("help3", "Mic ON in Free Fire", "🎙️", 0))
        }

        initialized = true
        Log.d("VoiceManager", "Loaded ${userVoices.size} user voices")
    }

    fun getAllVoices(): List<VoiceItem> = userVoices + builtIn

    fun getUserCount(): Int = userVoices.size

    fun forceReload(context: Context) {
        initialized = false
        init(context)
    }

    fun addUserVoice(context: Context, uri: Uri): Boolean {
        return try {
            val dir = File(context.filesDir, "user_voices")
            if (!dir.exists()) dir.mkdirs()

            val name = "voice_${System.currentTimeMillis()}.mp3"
            val dest = File(dir, name)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return false

            if (dest.length() < 500) {
                dest.delete()
                return false
            }

            // Immediately add to list
            initialized = false
            userVoices.add(
                VoiceItem(
                    id = "user_${dest.name}",
                    name = dest.nameWithoutExtension.take(20),
                    emoji = "🎤",
                    filePath = dest.absolutePath
                )
            )
            forceReload(context)

            Log.d("VoiceManager", "Added: ${dest.name} (${dest.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e("VoiceManager", "Add failed: ${e.message}")
            false
        }
    }
}
