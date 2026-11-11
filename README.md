# FreeFire VoicePack Injector

Floating overlay voice pack app for Free Fire.

## Features
- Floating popup over Free Fire screen
- 50+ preloaded voice packs (placeholders ready for real audio)
- Manual upload of your own MP3/OGG voice files
- SYSTEM_ALERT_WINDOW overlay
- Foreground service keeps it alive
- Audio routed via STREAM_VOICE_CALL + MODE_IN_COMMUNICATION (common bypass technique for game voice chat)
- Accessibility service helper for detection
- Clean, no-crash design

## How it works
1. Install APK
2. Grant all permissions (Overlay, Mic, Accessibility, Storage)
3. Open Free Fire
4. Open the floating button / popup
5. Tap any voice → plays through voice communication stream so friends can hear in game
6. Upload more voices from gallery/files

## Build APK
GitHub Actions builds release APK on every push to main.
Download from Actions → Artifacts.

## Project structure
```
app/
  src/main/
    java/com/ashxmay/ffvoice/
      MainActivity.kt
      OverlayService.kt
      VoicePlayer.kt
      VoiceManager.kt
      AccessibilityHelper.kt
    res/
      layout/
      raw/          (50+ voice placeholders)
      xml/
    AndroidManifest.xml
```

## Permissions required
- SYSTEM_ALERT_WINDOW (draw over other apps)
- RECORD_AUDIO
- MODIFY_AUDIO_SETTINGS
- FOREGROUND_SERVICE
- READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE
- BIND_ACCESSIBILITY_SERVICE

## Note on audio injection
True system-level mic injection needs root or Magisk modules.
This app uses the standard non-root method used by most voice changer apps:
AudioManager.MODE_IN_COMMUNICATION + AudioTrack STREAM_VOICE_CALL.
Works on many devices when Free Fire voice chat is active.

Built clean. No bloat. Cached.
