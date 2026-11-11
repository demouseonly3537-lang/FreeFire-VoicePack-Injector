package com.ashxmay.ffvoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnOverlay: Button
    private lateinit var btnMic: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnStart: Button
    private lateinit var btnUpload: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updateStatus()
        if (permissions.values.any { !it }) {
            Toast.makeText(this, "Grant all permissions for full function", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            VoiceManager.addUserVoice(this, it)
            Toast.makeText(this, "Voice added", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnOverlay = findViewById(R.id.btnOverlay)
        btnMic = findViewById(R.id.btnMic)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnStart = findViewById(R.id.btnStart)
        btnUpload = findViewById(R.id.btnUpload)

        btnOverlay.setOnClickListener { requestOverlayPermission() }
        btnMic.setOnClickListener { requestMicAndStorage() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }
        btnStart.setOnClickListener { startOverlayService() }
        btnUpload.setOnClickListener { pickAudioLauncher.launch("audio/*") }

        VoiceManager.init(this)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        statusText.text = buildString {
            append("Overlay: ").append(if (overlay) "OK" else "NEED").append("\n")
            append("Mic: ").append(if (mic) "OK" else "NEED").append("\n")
            append("Storage: ").append(if (storage) "OK" else "NEED").append("\n")
            append("Voices loaded: ").append(VoiceManager.getAllVoices().size)
        }

        btnStart.isEnabled = overlay && mic
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Overlay already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestMicAndStorage() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant overlay first", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Overlay started. Open Free Fire.", Toast.LENGTH_LONG).show()
        moveTaskToBack(true)
    }
}
