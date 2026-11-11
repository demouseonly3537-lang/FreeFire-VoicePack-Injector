package com.ashxmay.ffvoice

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal accessibility service.
 * Used only for package detection / future enhancements.
 * Does not inject audio itself — audio route is handled by VoicePlayer.
 */
class AccessibilityHelper : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: detect Free Fire package and auto-show panel
        // event?.packageName?.toString() == "com.dts.freefireth"
    }

    override fun onInterrupt() {}
}
