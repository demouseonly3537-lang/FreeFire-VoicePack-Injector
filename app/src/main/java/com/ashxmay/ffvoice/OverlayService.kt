package com.ashxmay.ffvoice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var panelView: View? = null
    private var isPanelOpen = false
    private val handler = Handler(Looper.getMainLooper())

    private val channelId = "ff_voice_overlay"
    private val TAG = "OverlayService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, buildNotification())
        // Delay slightly so permission is fully applied
        handler.postDelayed({ safeShowFloatingButton() }, 400)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingButton == null) {
            handler.post { safeShowFloatingButton() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeViews()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FF Voice Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FF Voice Pack Active")
            .setContentText("Floating button should be visible. Drag it.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun safeShowFloatingButton() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "No overlay permission")
                return
            }
            if (floatingButton != null) return

            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingButton = inflater.inflate(R.layout.floating_button, null)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 80
                y = 400
            }

            floatingButton?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var moved = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            moved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (kotlin.math.abs(dx) > 12 || kotlin.math.abs(dy) > 12) moved = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(floatingButton, params)
                            } catch (_: Exception) {}
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved) togglePanel()
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager.addView(floatingButton, params)
            Log.d(TAG, "Floating button added")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating button: ${e.message}")
            // Retry once after short delay
            handler.postDelayed({ 
                if (floatingButton == null) safeShowFloatingButton() 
            }, 1500)
        }
    }

    private fun togglePanel() {
        if (isPanelOpen) hidePanel() else showPanel()
    }

    private fun showPanel() {
        if (panelView != null) return
        try {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            panelView = inflater.inflate(R.layout.voice_panel, null)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                y = 80
            }

            val recycler = panelView?.findViewById<RecyclerView>(R.id.voiceRecycler)
            recycler?.layoutManager = GridLayoutManager(this, 3)
            recycler?.adapter = VoiceAdapter(VoiceManager.getAllVoices()) { voice ->
                VoicePlayer.play(this, voice)
            }

            panelView?.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
                hidePanel()
            }

            windowManager.addView(panelView, params)
            isPanelOpen = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show panel: ${e.message}")
            panelView = null
            isPanelOpen = false
        }
    }

    private fun hidePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
        isPanelOpen = false
    }

    private fun removeViews() {
        hidePanel()
        floatingButton?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        floatingButton = null
    }
}
