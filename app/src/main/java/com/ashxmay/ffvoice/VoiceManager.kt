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

    // Voxie-style names + emojis. 60 entries.
    private val voiceData = listOf(
        "😱" to "FAHHH",
        "✌️" to "bruh",
        "🟢" to "gogo gaga",
        "🔫" to "Shotgun",
        "☣️" to "Bass drop",
        "🔫" to "shot",
        "🐱" to "Meow",
        "✋" to "Slap",
        "👊" to "knock knock",
        "🟢" to "sus",
        "🟢" to "Wasted",
        "🟢" to "GopGopGop",
        "✅" to "True",
        "❌" to "Lie",
        "🌹" to "Aww",
        "😐" to "Rizz",
        "🟢" to "screenshot",
        "🟢" to "Waves",
        "🪖" to "Headshot",
        "☢️" to "Boom",
        "🟢" to "Elevator mu...",
        "🤔" to "you what?",
        "🟢" to "letmeknow",
        "😎" to "Brother eww...",
        "🟢" to "relax",
        "🟢" to "Lets go gam...",
        "🐱" to "cat laughing",
        "🟢" to "metal pipe",
        "🔴" to "error sounds",
        "⚔️" to "Enemy",
        "🔥" to "Push",
        "🛡️" to "Cover Me",
        "🎯" to "Nice Shot",
        "🙏" to "Thanks",
        "😅" to "Sorry",
        "🏃" to "Go Go",
        "📍" to "Hold Position",
        "⬅️" to "Flank Left",
        "➡️" to "Flank Right",
        "💊" to "Need Med",
        "💀" to "Knocked",
        "❤️" to "Reviving",
        "✅" to "Safe",
        "⚠️" to "Danger",
        "👀" to "Behind You",
        "🚨" to "Watch Out",
        "🏆" to "Good Game",
        "💪" to "Let's Win",
        "⚡" to "Rush",
        "🏕️" to "Camp",
        "🔭" to "Sniper",
        "📦" to "Airdrop",
        "🌀" to "Zone Coming",
        "🧱" to "Gloo Wall",
        "🚗" to "Vehicle",
        "1️⃣" to "One Left",
        "2️⃣" to "Two Left",
        "💥" to "Squad Wipe",
        "👑" to "Clutch",
        "🎖️" to "Ace",
        "💣" to "Grenade",
        "💨" to "Smoke",
        "🆘" to "Help",
        "✋" to "Wait",
        "✅" to "Ready"
    )

    fun init(context: Context) {
        if (initialized) return
        builtIn.clear()

        voiceData.forEachIndexed { index, pair ->
            val resName = "voice_${index + 1}"
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            builtIn.add(
                VoiceItem(
                    id = "built_$index",
                    name = pair.second,
                    emoji = pair.first,
                    resId = resId
                )
            )
        }

        val dir = File(context.filesDir, "user_voices")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.extension in listOf("mp3", "ogg", "wav", "m4a")) {
                    userVoices.add(
                        VoiceItem(
                            id = "user_${file.name}",
                            name = file.nameWithoutExtension,
                            emoji = "📁",
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
                    emoji = "📁",
                    filePath = dest.absolutePath
                )
            )
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to add voice: ${e.message}")
        }
    }
}
