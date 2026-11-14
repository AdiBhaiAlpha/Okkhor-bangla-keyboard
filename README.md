# Okkhor Bangla Keyboard (অক্ষর বাংলা কিবোর্ড)

**Creator & Developer:** Chitron Bhattacharjee  
**Package ID:** `com.app.okkhorkeyboard`  
**Category:** Input Method Editor (IME) — Bangla & English Android Keyboard  
**Version:** 1.0.1 (Version Code 2)

---

## 1. Product Overview
Okkhor Bangla Keyboard is a dedicated Android keyboard application designed for comfortable Bangla and English typing. Built with Kotlin and Jetpack Compose, it provides Bangla Phonetic (Avro-compatible) transliteration, fixed Probhat layout, English QWERTY, emoji and symbol panels, theme customization, local clipboard history, text shortcuts, voice typing, and direct on-keyboard resizing.

---

## 2. Implemented Capabilities

| Feature | Status | Description |
|---|---|---|
| **Android IME Service** | Implemented | Registered `InputMethodService` with transparent rounded top corners and system-wide typing support |
| **Bangla Phonetic Typing** | Implemented | Avro-compatible romanized transliteration with conjuncts, reph, rophola, jophola, and kar signs |
| **Bangla Fixed Layout** | Implemented | Probhat direct Bengali keyboard layout with shift states |
| **English QWERTY Layout** | Implemented | Stable English typing mode with single-tap shift, double-tap Caps Lock, and punctuation |
| **Word Suggestions & Memory** | Implemented | Local Bengali/English dictionary lookup and on-device word correction & bigram memory |
| **Emoji & Symbol Panels** | Implemented | Scrollable emoji picker, numeric keypad, and secondary symbol layouts |
| **Direct Keyboard Resizing** | Implemented | Interactive in-keyboard height (75%–140%) and width (75%–100%) resize handles with Reset & Done |
| **Theme Customization** | Implemented | Preset themes, accent swatches, key spacing/corner/elevation controls, and theme JSON export/import |
| **Text Expansion Shortcuts** | Implemented | User-configurable abbreviations and phrase expansions |
| **Clipboard Manager** | Implemented | Opt-in local clipboard history (up to 100 items) with pinning and configurable retention |
| **Voice Typing** | Implemented | Language-aware Bangla (`bn-BD`) and English (`en-US`) dictation via Android `SpeechRecognizer` |
| **Sensitive Field Protection** | Implemented | Automatic suppression of suggestions, learning, popups, and clipboard capture in password fields |

---

## 3. Build Prerequisites
- **JDK:** Java 17 or Java 21
- **Android SDK:** Compile SDK 36, Min SDK 24
- **Gradle:** 8.14.3+

---

## 4. Build & Test Commands
To run unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

To build the debug APK:
```bash
gradle :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

To build the signed release APK (with keystore environment variables configured):
```bash
gradle :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

---

## 5. Privacy & Data Handling
- **No Internet Permission:** Okkhor Bangla Keyboard does not request `android.permission.INTERNET`. Typed text, shortcuts, learned words, and clipboard items remain on your device.
- **No Advertising or Analytics SDKs:** No third-party ad networks or telemetry trackers are included.
- **On-Demand Microphone Access:** `RECORD_AUDIO` is requested only if you enable and use Voice Typing, and audio is processed via Android's system `SpeechRecognizer`.
- **Sensitive Field Protection:** Autocorrect, candidate suggestions, word learning, key popups, and clipboard capture are automatically disabled in password and sensitive input fields.

---

## 6. Attribution & Third-Party Licenses
- **Creator & Developer:** Chitron Bhattacharjee
- **Third-Party Libraries:** Okkhor uses open-source libraries including Kotlin, AndroidX / Android Jetpack, Jetpack Compose, Material 3, Material Icons, and Robolectric under their respective open-source licenses (such as the Apache License 2.0). All third-party libraries remain the property of their respective authors.
