# Okkhor Bangla Keyboard (অক্ষর বাংলা কিবোর্ড)

**Developer / Product Owner:** Chitron Bhattacharjee  
**Category:** Bangla-specialized, Multilingual, Highly Customizable Android Input Method Editor (IME)  
**Version:** 1.0.0

---

## 1. Product Overview
Okkhor Bangla Keyboard is a high-performance, responsive, and privacy-first Android keyboard that treats Bangla typing as a first-class experience. Built natively with modern Android Jetpack components, it offers seamless Avro-compatible phonetic transliteration, fixed national layout typing, English QWERTY, rich emoji & symbols, and an extensive Theme Studio.

---

## 2. Feature Matrix

| Feature | Status | Description |
|---|---|---|
| **Android IME Service** | Implemented | Genuine `InputMethodService` registered in OS with full lifecycle |
| **Bangla Phonetic Typing** | Implemented | Avro-style romanized transliteration with conjuncts, reph, rophola, jophola, and vowel signs |
| **Bangla Fixed Layout** | Implemented | National / Probhat style direct Bengali keyboard with shift states |
| **English QWERTY Layout** | Implemented | Full English typing with shift, caps, and punctuation |
| **Word Suggestions & Candidates** | Implemented | High-frequency Bengali dictionary lookup and candidate ranking strip |
| **Theme Studio** | Implemented | 6 curated presets (Classic Teal & Gold, Okkhor Emerald, Midnight AMOLED, Paper White, Sunset Amber, High Contrast) + live interactive preview |
| **Custom Styling Sliders** | Implemented | Real-time sliders for keyboard height scale, key corner rounding, and elevation/shadows |
| **Theme JSON Export/Import** | Implemented | Export theme to clipboard JSON and import validated custom themes |
| **Text Expansion Shortcuts** | Implemented | User-configurable abbreviations (e.g. `shb` -> `শুভ সকাল!`) |
| **Opt-in Clipboard Manager** | Implemented | Local-only clipboard history with pinning, item deletion, and clear all |
| **100% Offline Guarantee** | Implemented | Zero network permissions (`android.permission.INTERNET` omitted) |
| **Sensitive Field Protection** | Implemented | Automatic disabling of suggestions, clipboard, and previews in password fields |
| **Spacebar Cursor Control** | Implemented | Swipe left/right on spacebar to navigate cursor |
| **Haptic & Sound Feedback** | Implemented | Optional keypress vibration and system audio click |

---

## 3. Build Prerequisites
- **JDK:** Java 17 or Java 21
- **Android SDK:** Compile SDK 36, Min SDK 24
- **Gradle:** 8.14.3+

---

## 4. Build Commands
To run unit and Robolectric tests:
```bash
gradle testDebugUnitTest
```

To build debug APK:
```bash
gradle assembleDebug
```
The generated APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Installation & Setup
1. Install the APK on your Android device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. Open the **Okkhor Bangla Keyboard** app.
3. Follow the 2-step onboarding guide:
   - **Step 1:** Tap "Enable in Settings" and toggle Okkhor Bangla Keyboard ON.
   - **Step 2:** Tap "Switch Keyboard" and select Okkhor Bangla Keyboard as your active input method.
4. Test typing immediately in the in-app Typing Sandbox!

---

## 6. Privacy & Data Protection
- **No Internet Access:** Okkhor Keyboard does not request or hold `android.permission.INTERNET`. No typed characters, clipboard snippets, or metrics can ever leave your phone.
- **No Third-Party Analytics or Ads:** The app contains zero trackers or advertising SDKs.
- **Password Shield:** When typing into password fields or numeric PIN inputs, all suggestions, learning, and clipboard logging are automatically suppressed.

---

## 7. Architecture
```
com.example.
├── MainActivity.kt               # Setup, Theme Studio, and Settings entry point
├── okkhor/
│   ├── data/
│   │   ├── model/                # KeyboardTheme, KeyboardSettings, InputMode, ShortcutItem, ClipboardItem
│   │   └── preferences/          # KeyboardPreferencesRepository (StateFlow persistence)
│   ├── engine/
│   │   ├── BanglaPhoneticEngine  # Transliteration rules, conjuncts, kar signs
│   │   ├── BanglaUnicodeHelper   # Grapheme cluster calculation & Unicode constants
│   │   ├── BanglaDictionary      # Common Bengali vocabulary and candidate suggestions
│   │   └── SuggestionEngine      # Candidate ranking and autocomplete
│   ├── ime/
│   │   └── OkkhorInputMethodService # InputMethodService with Lifecycle & ComposeView
│   └── ui/
│       ├── keyboard/             # KeyboardLayouts, KeyboardContainer, Key buttons
│       └── settings/             # Onboarding, ThemeStudio, InputSettings, Shortcuts, Privacy
```

---

## 8. Attribution & Licenses
- **Crafted by:** Chitron Bhattacharjee
- **License:** Open Source / MIT Compatible
