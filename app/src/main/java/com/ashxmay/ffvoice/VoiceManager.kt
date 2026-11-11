package com.ashxmay.ffvoice

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class VoiceItem(
    val id: String,
    val name: String,
    val resId: Int = 0,
    val uri: Uri? = null,
    val filePath: String? = null
)

object VoiceManager {

    private val builtIn = mutableListOf<VoiceItem>()
    private val userVoices = mutableListOf<VoiceItem>()
    private var initialized = false

    // 50+ placeholder names. Replace raw files later with real short voice clips.
    private val voiceNames = listOf(
        "Hello", "Enemy", "Push", "Fall Back", "Reload", "Cover Me",
        "Nice Shot", "Thanks", "Sorry", "Go Go", "Hold Position",
        "Flank Left", "Flank Right", "Need Ammo", "Need Med", "Knocked",
        "Reviving", "Safe", "Danger", "Behind You", "Watch Out",
        "Good Game", "Let's Win", "Rush", "Camp", "Sniper",
        "Airdrop", "Zone Coming", "Healing", "Gloo Wall",
        "Drop Here", "Vehicle", "Bridge", "Building", "Roof",
        "One Left", "Two Left", "Squad Wipe", "Clutch", "Ace",
        "Fire", "Grenade", "Smoke", "Flash", "Molotov",
        "Help", "Come Here", "Wait", "Ready", "Go Silent",
        "High Ground", "Low Ground", "Rotate", "Hold Angle",
        "Peek", "Spray", "Tap", "Scope", "No Scope"
    )

    fun init(context: Context) {
        if (initialized) return
        builtIn.clear()

        // Map names to raw resources if they exist, otherwise keep name only
        // User will replace with real audio later. For now generate silent placeholders via code if needed.
        voiceNames.forEachIndexed { index, name ->
            val resName = "voice_${index + 1}"
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            builtIn.add(
                VoiceItem(
                    id = "built_$index",
                    name = name,
                    resId = resId
                )
            )
        }

        // Load any previously uploaded user voices from internal storage
        val dir = File(context.filesDir, "user_voices")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.extension in listOf("mp3", "ogg", "wav", "m4a")) {
                    userVoices.add(
                        VoiceItem(
                            id = "user_${file.name}",
                            name = file.nameWithoutExtension,
                            filePath = file.absolutePath
                        )
                    )
                }
            }
        }

        initialized = true
        Log.d("VoiceManager", "Loaded ${builtIn.size} built-in + ${userVoices.size} user voices")
    }

    fun getAllVoices(): List<VoiceItem> = builtIn + userVoices

    fun addUserVoice(context: Context, uri: Uri) {
        try {
            val dir = File(context.filesDir, "user_voices")
            if (!dir.exists()) dir.mkdirs()

            val name = "user_${System.currentTimeMillis()}.mp3"
            val dest = File(dir, name)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }

            userVoices.add(
                VoiceItem(
                    id = "user_${dest.name}",
                    name = dest.nameWithoutExtension,
                    filePath = dest.absolutePath
                )
            )
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to add voice: ${e.message}")
        }
    }
}
