package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Okkhor Bangla Keyboard", appName)
    }

    @Test
    fun `verify preferences repository initializes correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)
        assertNotNull(repo)
        assertNotNull(repo.settings.value)
        assertEquals(com.example.okkhor.data.model.KeyboardTheme.OkkhorEmerald.id, repo.settings.value.selectedThemeId)
    }

    @Test
    fun `verify OkkhorInputMethodService is registered and instantiable`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val serviceIntent = Intent("android.view.InputMethod").setPackage(context.packageName)
        val services = pm.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA)
        assertFalse("Okkhor InputMethodService must be registered in manifest", services.isEmpty())

        val serviceInfo = services.first().serviceInfo
        assertEquals("com.example.okkhor.ime.OkkhorInputMethodService", serviceInfo.name)

        // Ensure class loads and instantiates cleanly without ClassNotFoundException
        val clazz = Class.forName(serviceInfo.name)
        assertNotNull(clazz)
        val serviceInstance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(serviceInstance)
    }

    @Test
    fun `verify SmartWordMemory learns user corrections and bigrams`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memory = com.example.okkhor.engine.SmartWordMemory.getInstance(context)
        memory.clearMemory()

        // 1. Learn correction: "kalke" -> "কালকে"
        memory.recordCorrection("kalke", "কালকে")
        val learned = memory.getLearnedCorrections("kalke")
        assertEquals("কালকে", learned.firstOrNull())

        // 2. Bigram next-word prediction: "আমি" -> "ভাত"
        memory.recordNextWord("আমি", "ভাত")
        val nextWords = memory.getLearnedNextWords("আমি")
        assertEquals("ভাত", nextWords.firstOrNull())

        // 3. Verify SuggestionEngine ranks learned correction first
        val engine = com.example.okkhor.engine.SuggestionEngine(smartMemory = memory)
        val suggestions = engine.getSuggestions("kalke", com.example.okkhor.data.model.InputMode.BANGLA_PHONETIC)
        assertEquals("কালকে", suggestions.firstOrNull())
    }

    @Test
    fun `verify OK and uppercase acronyms are handled properly without broken Bengali`() {
        val phoneticEngine = com.example.okkhor.engine.BanglaPhoneticEngine()
        val suggestionEngine = com.example.okkhor.engine.SuggestionEngine(phoneticEngine)

        // 1. "OK" must remain "OK" (not "ওk")
        assertEquals("OK", phoneticEngine.transliterate("OK"))

        // 2. "ok" converts to "ওকে"
        assertEquals("ওকে", phoneticEngine.transliterate("ok"))

        // 3. "USA" acronym preserved
        assertEquals("USA", phoneticEngine.transliterate("USA"))

        // 4. Suggestions for "OK" should prioritize "OK" and include "ওকে"
        val okSuggestions = suggestionEngine.getSuggestions("OK", com.example.okkhor.data.model.InputMode.BANGLA_PHONETIC)
        assertEquals("OK", okSuggestions.firstOrNull())
        org.junit.Assert.assertTrue("Should contain 'ওকে'", okSuggestions.contains("ওকে"))
    }

    @Test
    fun `verify English active mode persists across Emoji, Symbols, and minimize cycles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)
        repo.resetAllSettings()

        // Default starts at BANGLA_PHONETIC
        assertEquals(com.example.okkhor.data.model.InputMode.BANGLA_PHONETIC, repo.getLastTextMode())

        // Switch to ENGLISH
        repo.setLastTextMode(com.example.okkhor.data.model.InputMode.ENGLISH)
        assertEquals(com.example.okkhor.data.model.InputMode.ENGLISH, repo.getLastTextMode())

        // Trying to save transient modes (EMOJI / NUMBERS_SYMBOLS) must NOT overwrite lastTextMode
        repo.setLastTextMode(com.example.okkhor.data.model.InputMode.EMOJI)
        assertEquals(com.example.okkhor.data.model.InputMode.ENGLISH, repo.getLastTextMode())

        repo.setLastTextMode(com.example.okkhor.data.model.InputMode.NUMBERS_SYMBOLS)
        assertEquals(com.example.okkhor.data.model.InputMode.ENGLISH, repo.getLastTextMode())
    }

    @Test
    fun `verify English suggestions and next-word predictions work in English mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memory = com.example.okkhor.engine.SmartWordMemory.getInstance(context)
        memory.clearMemory()

        val engine = com.example.okkhor.engine.SuggestionEngine(smartMemory = memory)

        // Prefix suggestions in ENGLISH
        val helSuggestions = engine.getSuggestions("hel", com.example.okkhor.data.model.InputMode.ENGLISH)
        assertEquals("hel", helSuggestions.firstOrNull())
        org.junit.Assert.assertTrue(helSuggestions.contains("hello") || helSuggestions.contains("help"))

        // Next-word prediction after "good"
        val nextWords = engine.getSuggestions("", com.example.okkhor.data.model.InputMode.ENGLISH, lastCommittedWord = "good")
        org.junit.Assert.assertTrue(nextWords.contains("morning"))
    }

    @Test
    fun `verify clipboard stores up to 100 items and prunes expired unpinned items`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)
        repo.resetAllSettings()
        repo.clearClipboard()

        val now = 1_700_000_000_000L
        // Default retention is 24 hours
        assertEquals(24, repo.settings.value.clipboardRetentionHours)

        // Add 105 recent items -> should cap cleanly at 100
        for (i in 1..105) {
            repo.addClipboardItem("Clip item $i", timestamp = now + i)
        }
        assertEquals(100, repo.clipboardHistory.value.size)

        // Pin one item and age all timestamps beyond 24 hours
        val pinnedTarget = repo.clipboardHistory.value.last()
        repo.togglePinClipboardItem(pinnedTarget.id)
        repo.pruneClipboardHistory(nowMs = now + 26L * 60L * 60L * 1000L)

        // Only pinned item survives 24h pruning
        assertEquals(1, repo.clipboardHistory.value.size)
        assertEquals(pinnedTarget.id, repo.clipboardHistory.value.first().id)
    }

    @Test
    fun `verify clipboard retention Never preserves unpinned items across prune`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)
        repo.resetAllSettings()
        repo.clearClipboard()

        val now = 1_700_000_000_000L
        repo.updateSettings { it.copy(clipboardRetentionHours = -1) }
        repo.addClipboardItem("Persistent note", timestamp = now)
        repo.pruneClipboardHistory(nowMs = now + 60L * 24L * 60L * 60L * 1000L)

        assertEquals(1, repo.clipboardHistory.value.size)
        assertEquals("Persistent note", repo.clipboardHistory.value.first().text)
    }

    @Test
    fun `verify VoiceInputController gates on RECORD_AUDIO permission cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = com.example.okkhor.ime.VoiceInputController(context) {}
        controller.startListening(com.example.okkhor.data.model.InputMode.BANGLA_PHONETIC)
        assertEquals(
            com.example.okkhor.ime.VoiceInputState.PermissionRequired,
            controller.state.value
        )
        controller.destroy()
    }
}



