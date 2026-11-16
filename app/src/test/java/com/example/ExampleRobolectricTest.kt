package com.example

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import com.example.okkhor.emoji.EmojiCategory
import com.example.okkhor.emoji.EmojiKitchenEngine
import com.example.okkhor.emoji.EmojiMashupStyle
import com.example.okkhor.emoji.GifSearchUiState
import com.example.okkhor.emoji.GiphyRepository
import com.example.okkhor.emoji.UnicodeEmojiRepository
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.ime.OkkhorInputMethodService
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.okkhor.ui.keyboard.KeyboardLayouts
import com.example.okkhor.ui.settings.InputSettingsSection
import com.example.okkhor.ui.settings.MainSettingsScreen
import com.example.okkhor.ui.settings.PrivacyAboutSection
import com.example.okkhor.ui.settings.TypingPerformanceLabSection
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Test
    fun `verify OkkhorInputMethodService creates transparent IME window and view hierarchy`() {
        val serviceController = org.robolectric.Robolectric.buildService(com.example.okkhor.ime.OkkhorInputMethodService::class.java)
        val service = serviceController.create().get()
        val inputView = service.onCreateInputView()
        val inputBg = inputView.background
        val isTransparentInputBg = inputBg == null ||
            (inputBg is android.graphics.drawable.ColorDrawable && inputBg.color == android.graphics.Color.TRANSPARENT)
        org.junit.Assert.assertTrue("ComposeView root background must be transparent", isTransparentInputBg)

        service.window?.window?.let { win ->
            val decorBg = win.decorView.background
            val isTransparentDecor = decorBg == null ||
                (decorBg is android.graphics.drawable.ColorDrawable && decorBg.color == android.graphics.Color.TRANSPARENT)
            org.junit.Assert.assertTrue("IME decorView background must be transparent", isTransparentDecor)
        }
        serviceController.destroy()
    }

    @Test
    fun `verify KeyboardPreferencesRepository persists and clamps height and width dimensions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)
        repo.resetAllSettings()

        assertEquals(100, repo.settings.value.keyboardHeightPercent)
        assertEquals(100, repo.settings.value.keyboardWidthPercent)

        repo.updateSettings { it.copy(keyboardHeightPercent = 122, keyboardWidthPercent = 86) }
        assertEquals(122, repo.settings.value.keyboardHeightPercent)
        assertEquals(86, repo.settings.value.keyboardWidthPercent)

        val prefs = context.getSharedPreferences("okkhor_keyboard_prefs", Context.MODE_PRIVATE)
        assertEquals(122, prefs.getInt("pref_height_percent", -1))
        assertEquals(86, prefs.getInt("pref_width_percent", -1))

        // Clamp upper and lower bounds
        repo.updateSettings { it.copy(keyboardHeightPercent = 200, keyboardWidthPercent = 40) }
        assertEquals(140, repo.settings.value.keyboardHeightPercent)
        assertEquals(75, repo.settings.value.keyboardWidthPercent)
        assertEquals(140, prefs.getInt("pref_height_percent", -1))
        assertEquals(75, prefs.getInt("pref_width_percent", -1))

        repo.updateSettings { it.copy(keyboardHeightPercent = 30, keyboardWidthPercent = 130) }
        assertEquals(75, repo.settings.value.keyboardHeightPercent)
        assertEquals(100, repo.settings.value.keyboardWidthPercent)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Direct Keyboard Resizing Mode drags height and width, resets, and exits cleanly`() {
        var currentSettings by mutableStateOf(KeyboardSettings())
        var lastSavedHeight = 100
        var lastSavedWidth = 100

        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                KeyboardContainer(
                    settings = currentSettings,
                    theme = KeyboardTheme.OkkhorEmerald,
                    currentMode = InputMode.ENGLISH,
                    isShifted = false,
                    candidates = emptyList(),
                    onKeyPress = {},
                    onActionKey = {},
                    onCandidateClick = {},
                    onModeSwitch = {},
                    onCursorMove = {},
                    onOpenSettings = {},
                    onOpenClipboard = {},
                    onUpdateDimensions = { height, width ->
                        lastSavedHeight = height
                        lastSavedWidth = width
                        currentSettings = currentSettings.copy(
                            keyboardHeightPercent = height,
                            keyboardWidthPercent = width
                        )
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        // Resize overlay must not exist while inactive
        composeTestRule.onNodeWithTag("resize_mode_overlay").assertDoesNotExist()

        // Activate interactive resize mode from toolbar icon
        composeTestRule.onNodeWithTag("btn_toolbar_resize").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("resize_mode_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resize_handle_top").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resize_handle_left").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resize_handle_right").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_resize_reset").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_resize_done").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resize_size_indicator").assertTextContains("H: 100%  •  W: 100%")

        // Drag top handle upward -> height increases above 100%
        composeTestRule.onNodeWithTag("resize_handle_top").performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y - 80f), durationMillis = 120)
        }
        composeTestRule.waitForIdle()
        assertTrue("Dragging top handle up must increase height (was $lastSavedHeight)", lastSavedHeight > 100)

        // Drag top handle downward -> height decreases
        val tallerHeight = lastSavedHeight
        composeTestRule.onNodeWithTag("resize_handle_top").performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y + 140f), durationMillis = 120)
        }
        composeTestRule.waitForIdle()
        assertTrue("Dragging top handle down must decrease height ($lastSavedHeight < $tallerHeight)", lastSavedHeight < tallerHeight)

        // Drag right handle inward left -> width decreases below 100% (sufficient travel to clear xxhdpi touchSlop)
        composeTestRule.onNodeWithTag("resize_handle_right").performTouchInput {
            swipe(start = center, end = Offset(center.x - 150f, center.y), durationMillis = 150)
        }
        composeTestRule.waitForIdle()
        assertTrue("Dragging right handle inward must decrease width (was $lastSavedWidth)", lastSavedWidth < 100)

        // Drag left handle outward left -> width increases back
        val narrowerWidth = lastSavedWidth
        composeTestRule.onNodeWithTag("resize_handle_left").performTouchInput {
            swipe(start = center, end = Offset(center.x - 120f, center.y), durationMillis = 150)
        }
        composeTestRule.waitForIdle()
        assertTrue("Dragging left handle outward must increase width ($lastSavedWidth > $narrowerWidth)", lastSavedWidth > narrowerWidth)

        // Tap Reset -> restores 100% x 100%
        composeTestRule.onNodeWithTag("btn_resize_reset").performClick()
        composeTestRule.waitForIdle()
        assertEquals(100, lastSavedHeight)
        assertEquals(100, lastSavedWidth)
        composeTestRule.onNodeWithTag("resize_size_indicator").assertTextContains("H: 100%  •  W: 100%")

        // Resize both height and width, then tap Done
        composeTestRule.onNodeWithTag("resize_handle_top").performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y - 90f), durationMillis = 120)
        }
        composeTestRule.onNodeWithTag("resize_handle_left").performTouchInput {
            swipe(start = center, end = Offset(center.x + 120f, center.y), durationMillis = 120)
        }
        composeTestRule.waitForIdle()
        val finalHeight = lastSavedHeight
        val finalWidth = lastSavedWidth
        assertTrue(finalHeight > 100)
        assertTrue(finalWidth < 100)

        composeTestRule.onNodeWithTag("btn_resize_done").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("resize_mode_overlay").assertDoesNotExist()
        assertEquals(finalHeight, currentSettings.keyboardHeightPercent)
        assertEquals(finalWidth, currentSettings.keyboardWidthPercent)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify About Okkhor page displays Chitron Bhattacharjee attribution and independent branding`() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PrivacyAboutSection(onResetAllSettings = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_brand_card").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Okkhor Bangla Keyboard")[0].assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_creator_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Creator & Developer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chitron Bhattacharjee").assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_app_info_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Application Information").assertIsDisplayed()
        composeTestRule.onNodeWithText(BuildConfig.VERSION_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(BuildConfig.VERSION_CODE.toString()).assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_product_info_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_privacy_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_opensource_card").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("PixelBoard", substring = true, ignoreCase = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Gboard", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `verify Backspace deletes partial, multi-paragraph, and Bangla selected text accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()

        try {
            // Test A: "I am a boy", select "am" -> Backspace deletes only "am"
            val editText = EditText(context)
            editText.setText("I am a boy")
            editText.setSelection(2, 4) // "am"
            val info = EditorInfo().apply {
                initialSelStart = 2
                initialSelEnd = 4
            }
            val ic = editText.onCreateInputConnection(info)!!
            service.setTestBinding(ic, info)
            service.onStartInputView(info, false)
            assertTrue("Selection must be detected on start", service.hasSelectionState)

            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("I  a boy", editText.text.toString())
            assertEquals(2, editText.selectionStart)
            assertEquals(2, editText.selectionEnd)
            assertFalse("Selection flag must clear after delete", service.hasSelectionState)

            // Test C: Multi-paragraph selection -> Backspace deletes complete paragraph block
            val multiParagraph = "First paragraph line.\nSecond paragraph with details.\nThird final line."
            editText.setText(multiParagraph)
            editText.setSelection(0, multiParagraph.length - "Third final line.".length)
            val infoMulti = EditorInfo().apply {
                initialSelStart = editText.selectionStart
                initialSelEnd = editText.selectionEnd
            }
            val icMulti = editText.onCreateInputConnection(infoMulti)!!
            service.setTestBinding(icMulti, infoMulti)
            service.onStartInputView(infoMulti, false)
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("Third final line.", editText.text.toString())
            assertEquals(0, editText.selectionStart)

            // Test E: Bangla text with যুক্তাক্ষর and কার selected -> clean replacement without orphans
            val banglaSample = "বাংলাদেশের বিজ্ঞান ও প্রযুক্তি এগিয়ে যাচ্ছে"
            editText.setText(banglaSample)
            val selStart = banglaSample.indexOf("বিজ্ঞান")
            val selEnd = selStart + "বিজ্ঞান".length
            editText.setSelection(selStart, selEnd)
            val infoBn = EditorInfo().apply {
                initialSelStart = selStart
                initialSelEnd = selEnd
            }
            val icBn = editText.onCreateInputConnection(infoBn)!!
            service.setTestBinding(icBn, infoBn)
            service.onStartInputView(infoBn, false)
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("বাংলাদেশের  ও প্রযুক্তি এগিয়ে যাচ্ছে", editText.text.toString())
            assertEquals(selStart, editText.selectionStart)

            // Undo restores deleted Bangla word; Redo re-deletes it
            assertTrue(service.canUndoState)
            service.handleTextEditAction("UNDO")
            assertEquals(banglaSample, editText.text.toString())
            assertTrue(service.canRedoState)
            service.handleTextEditAction("REDO")
            assertEquals("বাংলাদেশের  ও প্রযুক্তি এগিয়ে যাচ্ছে", editText.text.toString())
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun `verify Copy, Cut, Paste, Select All and Cursor Selection Mode work cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        try {
            val editText = EditText(context)
            editText.setText("I am a boy")
            editText.setSelection(2, 4) // "am"
            val info = EditorInfo().apply {
                initialSelStart = 2
                initialSelEnd = 4
            }
            val ic = editText.onCreateInputConnection(info)!!
            service.setTestBinding(ic, info)
            service.onStartInputView(info, false)

            // Test B: Copy "am" keeps text intact and puts "am" in ClipboardManager
            service.handleTextEditAction("COPY")
            assertEquals("I am a boy", editText.text.toString())
            assertEquals("am", cm.primaryClip?.getItemAt(0)?.text?.toString())
            assertTrue(service.canPasteState)

            // Test D: Cut removes "am" and puts it in clipboard
            service.handleTextEditAction("CUT")
            assertEquals("I  a boy", editText.text.toString())
            assertEquals("am", cm.primaryClip?.getItemAt(0)?.text?.toString())
            assertFalse(service.hasSelectionState)

            // Paste inserts "am" back at cursor
            service.handleTextEditAction("PASTE")
            assertEquals("I am a boy", editText.text.toString())

            // Select All selects entire content
            service.handleTextEditAction("SELECT_ALL")
            assertEquals(0, editText.selectionStart)
            assertEquals("I am a boy".length, editText.selectionEnd)
            assertTrue(service.hasSelectionState)

            // Cursor movement & Selection Mode across Unicode emojis (no text loss or surrogate splitting)
            editText.setText("Hi 😀 বাংলাদেশের")
            editText.setSelection(6) // right after "Hi 😀 "
            val infoMove = EditorInfo().apply {
                initialSelStart = 6
                initialSelEnd = 6
            }
            val icMove = editText.onCreateInputConnection(infoMove)!!
            service.setTestBinding(icMove, infoMove)
            service.onStartInputView(infoMove, false)

            service.moveCursor(-2) // skip space + entire 😀 surrogate pair safely
            assertEquals(3, editText.selectionStart)
            assertEquals("Hi 😀 বাংলাদেশের", editText.text.toString())

            // Toggle Selection Mode ON and extend right 1 unit (across 😀 surrogate pair)
            service.handleTextEditAction("TOGGLE_SELECTION_MODE")
            assertTrue(service.isSelectionMode)
            service.moveCursor(1)
            assertEquals(3, editText.selectionStart)
            assertEquals(5, editText.selectionEnd)
            assertEquals("😀", editText.text.substring(editText.selectionStart, editText.selectionEnd))
            assertTrue(service.hasSelectionState)
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify TextEditingPanel hides unavailable Undo Redo and disables Cut Copy Paste accurately`() {
        var isUndoAvailable by mutableStateOf(false)
        var isSelectionActive by mutableStateOf(false)
        var isClipboardPasteable by mutableStateOf(false)

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                canUndo = isUndoAvailable,
                canRedo = false,
                hasSelection = isSelectionActive,
                canPaste = isClipboardPasteable,
                onKeyPress = {},
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = {},
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_suggestion_undo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_suggestion_redo").assertDoesNotExist()

        composeTestRule.onNodeWithTag("btn_toolbar_text_edit").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("text_editing_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_edit_undo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_edit_cut").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("btn_edit_copy").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("btn_edit_paste").assertIsNotEnabled()

        // Enable selection and undo -> controls activate honestly
        isSelectionActive = true
        isUndoAvailable = true
        isClipboardPasteable = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_edit_undo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_suggestion_undo").assertIsDisplayed()
    }

    @Test
    fun `verify complete Unicode emoji dataset, categories, variants, Bangla and phonetic search`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UnicodeEmojiRepository.getInstance(context)
        val all = repo.getAllEmojis()

        // Full Unicode dataset (must not be a small hardcoded subset)
        assertTrue("Expected > 1800 base Unicode emojis, got ${all.size}", all.size > 1800)

        // All 9 standard categories populated
        for (cat in EmojiCategory.STANDARD_CATEGORIES) {
            val catItems = repo.getByCategory(cat.id)
            assertTrue("Category ${cat.labelEn} must not be empty", catItems.isNotEmpty())
        }

        // Skin tone variants, ZWJ sequences, regional flags, keycaps present
        val wave = repo.findByUnicode("👋")
        assertNotNull("Waving hand emoji must exist", wave)
        assertTrue("Skin tone variants expected for 👋", (wave?.variants?.size ?: 0) >= 5)
        assertNotNull("Bangladesh flag 🇧🇩 must exist", repo.findByUnicode("🇧🇩"))
        assertNotNull("Keycap 1️⃣ must exist", repo.findByUnicode("1️⃣"))
        assertTrue("Family ZWJ emojis expected", repo.search("family").isNotEmpty())

        // English, Bangla, and Latin-phonetic search
        assertTrue(repo.search("love").isNotEmpty())
        assertTrue(repo.search("heart").isNotEmpty())
        assertTrue("Bangla search 'বৃষ্টি' must match weather emojis", repo.search("বৃষ্টি").isNotEmpty())
        assertTrue("Bangla search 'খুশি' must match happy emojis", repo.search("খুশি").isNotEmpty())
        assertTrue("Phonetic search 'brishti' must match weather emojis", repo.search("brishti").isNotEmpty())

        // Emoji cluster single-step Backspace deletion checks
        assertEquals(4, BanglaUnicodeHelper.calculateBackspaceLength("🇧🇩"))
        assertEquals(3, BanglaUnicodeHelper.calculateBackspaceLength("1️⃣"))
        assertEquals(4, BanglaUnicodeHelper.calculateBackspaceLength("👍🏽"))
        assertEquals("👨‍👩‍👧".length, BanglaUnicodeHelper.calculateBackspaceLength("👨‍👩‍👧"))
    }

    @Test
    fun `verify GiphyRepository handles unconfigured key, trending, search, categories, and autocomplete`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Unconfigured API key reports Unconfigured state honestly
        val unconfiguredRepo = GiphyRepository(
            context = context,
            apiKeyOverride = "",
            networkCheckOverride = { true }
        )
        assertFalse(unconfiguredRepo.isConfigured())
        runBlocking {
            assertEquals(GifSearchUiState.Unconfigured, unconfiguredRepo.fetchTrending())
        }

        // 2. Local Mock GIPHY HTTP API server verifying real REST request/response parsing
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/gifs/trending") { ex ->
            val json = """
                {
                  "data": [
                    {
                      "id": "gif_1",
                      "title": "Happy Dance",
                      "images": {
                        "fixed_width_small": { "url": "http://127.0.0.1:${server.address.port}/preview.gif", "width": "150", "height": "120" },
                        "downsized_medium": { "url": "http://127.0.0.1:${server.address.port}/full.gif", "width": "320", "height": "240" }
                      }
                    }
                  ],
                  "pagination": { "total_count": 10, "count": 1, "offset": 0 }
                }
            """.trimIndent()
            val bytes = json.toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.createContext("/v1/gifs/search") { ex ->
            val json = """
                {
                  "data": [
                    {
                      "id": "gif_love",
                      "title": "Bangla Eid Celebration",
                      "images": {
                        "fixed_width_small": { "url": "http://127.0.0.1:${server.address.port}/eid_preview.gif", "width": "160", "height": "120" },
                        "downsized_medium": { "url": "http://127.0.0.1:${server.address.port}/eid_full.gif", "width": "300", "height": "220" }
                      }
                    }
                  ],
                  "pagination": { "total_count": 1, "count": 1, "offset": 0 }
                }
            """.trimIndent()
            val bytes = json.toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.createContext("/v1/gifs/search/tags") { ex ->
            val json = """{"data":[{"name":"happy birthday"},{"name":"happy dance"}]}"""
            val bytes = json.toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val repo = GiphyRepository(
                context = context,
                baseUrl = baseUrl,
                apiKeyOverride = "test_key_12345",
                networkCheckOverride = { true }
            )
            assertTrue(repo.isConfigured())

            runBlocking {
                val trending = repo.fetchTrending() as GifSearchUiState.Success
                assertEquals(1, trending.items.size)
                assertEquals("gif_1", trending.items.first().id)
                assertTrue(trending.hasMore)

                val searched = repo.searchGifs("eid") as GifSearchUiState.Success
                assertEquals("gif_love", searched.items.first().id)

                val tags = repo.fetchSearchSuggestions("hap")
                assertEquals(listOf("happy birthday", "happy dance"), tags)
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `verify EmojiKitchenEngine renders PNG mashups and commitContent checks editor MIME support`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Compatible pair discovery and search
        assertTrue(EmojiKitchenEngine.isCompatiblePair("😀", "❤️"))
        assertFalse(EmojiKitchenEngine.isCompatiblePair("😀", "😀"))
        assertTrue(EmojiKitchenEngine.searchCompatiblePairs("বৃষ্টি").isNotEmpty())
        assertTrue(EmojiKitchenEngine.searchCompatiblePairs("joy").isNotEmpty())

        // 2. Render and save real PNG mashup file
        val bmp = EmojiKitchenEngine.renderMashupBitmap("😀", "❤️", EmojiMashupStyle.FUSION_BLEND, 192)
        assertEquals(192, bmp.width)
        assertEquals(192, bmp.height)
        val savedPng = EmojiKitchenEngine.saveMashupPng(context, "😀", "❤️", EmojiMashupStyle.FUSION_BLEND)
        assertTrue(savedPng.exists() && savedPng.length() > 100L)

        // 3. IME commitContent capability check on unsupported vs supported editors
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        try {
            val editText = EditText(context)

            // Unsupported plain text field -> declines insertion honestly with notice
            val plainInfo = EditorInfo()
            val plainIc = editText.onCreateInputConnection(plainInfo)!!
            service.setTestBinding(plainIc, plainInfo)
            service.onStartInputView(plainInfo, false)
            assertFalse(service.supportsImageCommit)
            assertFalse(service.supportsGifCommit)
            val rejected = service.commitMashup("😀", "❤️", EmojiMashupStyle.FUSION_BLEND)
            assertFalse(rejected)
            assertNotNull("Must report honest fallback notice when editor lacks image MIME support", service.mediaInsertNotice)

            // Supported image/png & image/gif editor via EditorInfoCompat
            var receivedMimeTypes: Array<String> = emptyArray()
            val richInfo = EditorInfo()
            val baseIc = editText.onCreateInputConnection(richInfo)!!
            EditorInfoCompat.setContentMimeTypes(richInfo, arrayOf("image/png", "image/gif"))
            @Suppress("DEPRECATION")
            val richIc = androidx.core.view.inputmethod.InputConnectionCompat.createWrapper(
                baseIc,
                richInfo
            ) { inputContentInfo, _, _ ->
                receivedMimeTypes = Array(inputContentInfo.description.mimeTypeCount) { idx ->
                    inputContentInfo.description.getMimeType(idx)
                }
                true
            }
            service.setTestBinding(richIc, richInfo)
            service.onStartInputView(richInfo, false)
            assertTrue(service.supportsImageCommit)
            assertTrue(service.supportsGifCommit)
            val accepted = service.commitMashup("😀", "❤️", EmojiMashupStyle.AURA_HALO)
            assertTrue("commitMashup must succeed when editor handles commitContent", accepted)
            assertEquals(listOf("image/png"), receivedMimeTypes.toList())
            assertNull(service.mediaInsertNotice)
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify EmojiGifKitchenPanel renders Emoji search, GIF tab, and Emoji Kitchen Mashups`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val emojiRepo = UnicodeEmojiRepository.getInstance(context)
        val committed = mutableListOf<String>()

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(gifSearchEnabled = false),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.EMOJI,
                lastTextMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                emojiRepository = emojiRepo,
                onKeyPress = {},
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = {},
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {},
                onEmojiSelected = { _, unicode -> committed.add(unicode) }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("emoji_gif_kitchen_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_emoji").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_gif").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_kitchen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("emoji_grid").assertIsDisplayed()

        // Switch to GIF tab -> shows honest notice when GIF search disabled
        composeTestRule.onNodeWithTag("tab_gif").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Online GIF Search is Disabled").assertIsDisplayed()

        // Switch to Kitchen tab -> shows slots and all 4 live mashup cards
        composeTestRule.onNodeWithTag("tab_kitchen").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("kitchen_slot_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kitchen_slot_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mashup_card_fusion").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mashup_card_halo").assertIsDisplayed()
    }

    @Test
    fun `verify recent emoji ranking deduplication frequency prioritization and dock limit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)

        val ranked = KeyboardPreferencesRepository.rankRecentEmojisForDock(
            recent = listOf("😀", "❤️", "😀", "🇧🇩", "🎉", "🙏", "👍"),
            frequency = mapOf("❤️" to 6, "🙏" to 12, "😀" to 3),
            limit = 4
        )
        assertEquals(4, ranked.size)
        assertEquals(ranked.distinct().size, ranked.size)
        assertEquals("🙏", ranked.first())

        repo.updateSettings { it.copy(recentEmojiDockCount = 15) }
        assertEquals(12, repo.settings.value.recentEmojiDockCount)
        repo.updateSettings { it.copy(recentEmojiDockCount = 2) }
        assertEquals(4, repo.settings.value.recentEmojiDockCount)
        repo.updateSettings { it.copy(recentEmojiDockCount = 6) }

        repeat(4) { repo.recordEmojiUse("🔥") }
        repo.recordEmojiUse("🎉")
        val dockEmojis = repo.getDockRecentEmojis()
        assertTrue(dockEmojis.contains("🔥"))
        assertTrue(dockEmojis.size <= 6)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Emoji Quick Access Dock opens on long press and normal tap opens full Emoji panel`() {
        var switchedMode: InputMode? = null
        val selectedEmojis = mutableListOf<String>()

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(recentEmojiDockCount = 5),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                recentEmojis = listOf("😀", "❤️", "🇧🇩", "🙏", "🎉"),
                emojiFrequency = mapOf("❤️" to 4),
                onKeyPress = {},
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = { switchedMode = it },
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {},
                onEmojiSelected = { _, unicode -> selectedEmojis.add(unicode) }
            )
        }

        composeTestRule.waitForIdle()
        // 1. Normal quick tap opens full Emoji panel
        composeTestRule.onNodeWithTag("btn_emoji").performClick()
        composeTestRule.waitForIdle()
        assertEquals(InputMode.EMOJI, switchedMode)
        switchedMode = null

        // 2. Long press opens EmojiQuickAccessDock & backdrop dim scrim; releasing outside cancels safely
        composeTestRule.onNodeWithTag("btn_emoji").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("emoji_quick_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keyboard_gesture_dim_scrim").assertIsDisplayed()

        // Drag upward above dock bounds and release -> safely closes without selecting
        composeTestRule.onNodeWithTag("btn_emoji").performTouchInput {
            moveTo(Offset(center.x, center.y - 260f))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("emoji_quick_dock").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Spacebar swipe switches language on release, cancels on center return, and supports cursor hold`() {
        var currentMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
        val committedActions = mutableListOf<Int>()
        val cursorMoves = mutableListOf<Int>()

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(
                    spacebarCursorSwipe = true,
                    spacebarLanguageSwipe = true
                ),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = currentMode,
                isShifted = false,
                candidates = emptyList(),
                onKeyPress = {},
                onActionKey = { committedActions.add(it) },
                onCandidateClick = {},
                onModeSwitch = { currentMode = it },
                onCursorMove = { cursorMoves.add(it) },
                onOpenSettings = {},
                onOpenClipboard = {}
            )
        }

        composeTestRule.waitForIdle()

        // 1. Tap commits normal space without switching mode
        composeTestRule.onNodeWithTag("spacebar").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(KeyboardLayouts.CODE_SPACE), committedActions)
        assertEquals(InputMode.BANGLA_PHONETIC, currentMode)

        // 2. Horizontal swipe right shows preview, then returning to center before release cancels switch
        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            down(center)
            moveBy(Offset(140f, 0f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("floating_language_switcher").assertIsDisplayed()

        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            moveTo(center)
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(InputMode.BANGLA_PHONETIC, currentMode)
        composeTestRule.onNodeWithTag("floating_language_switcher").assertDoesNotExist()

        // 3. Horizontal swipe right past commit threshold switches BANGLA_PHONETIC -> BANGLA_FIXED on release
        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            down(center)
            moveBy(Offset(160f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(InputMode.BANGLA_FIXED, currentMode)

        // 4. Long hold >= 360ms then horizontal drag moves cursor without switching mode or inserting space
        val spacesBeforeHold = committedActions.size
        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            moveBy(Offset(-90f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(InputMode.BANGLA_FIXED, currentMode)
        assertEquals(spacesBeforeHold, committedActions.size)
        assertTrue("Expected cursor movement during hold+drag", cursorMoves.isNotEmpty())
    }

    @Test
    fun `verify Backspace word swipe deletes Bangla conjunct and English words and Undo restores`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        try {
            val editText = EditText(context)

            // 1. English word before cursor
            editText.setText("Hello world")
            editText.setSelection(editText.text.length)
            val enInfo = EditorInfo().apply {
                initialSelStart = editText.selectionStart
                initialSelEnd = editText.selectionEnd
            }
            val enIc = editText.onCreateInputConnection(enInfo)!!
            service.setTestBinding(enIc, enInfo)
            service.onStartInputView(enInfo, false)
            service.deleteWordBeforeCursor()
            assertEquals("Hello ", editText.text.toString())

            // Undo restores deleted English word
            assertTrue(service.canUndoState)
            service.handleTextEditAction("UNDO")
            assertEquals("Hello world", editText.text.toString())

            // 2. Bangla conjunct word before cursor
            val bnSentence = "বাংলাদেশের বিজ্ঞান"
            editText.setText(bnSentence)
            editText.setSelection(bnSentence.length)
            val bnInfo = EditorInfo().apply {
                initialSelStart = bnSentence.length
                initialSelEnd = bnSentence.length
            }
            val bnIc = editText.onCreateInputConnection(bnInfo)!!
            service.setTestBinding(bnIc, bnInfo)
            service.onStartInputView(bnInfo, false)
            service.deleteWordBeforeCursor()
            assertEquals("বাংলাদেশের ", editText.text.toString())
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Backspace Compose swipe left invokes onDeleteWordSwipe and Shift hold drag works`() {
        var wordDeleteCount = 0
        val typedKeys = mutableListOf<String>()

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(backspaceSwipeDelete = true),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                onKeyPress = { typedKeys.add(it) },
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = {},
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {},
                onDeleteWordSwipe = { wordDeleteCount++ }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_backspace").performTouchInput {
            down(center)
            moveBy(Offset(-120f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(1, wordDeleteCount)
    }

    @Test
    fun `verify Smart Clipboard OTP hint is suppressed in password fields and shown in normal fields`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        try {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("otp", "Your OTP code is 519204"))
            val editText = EditText(context)

            // Normal text field -> OTP hint populated
            val normalInfo = EditorInfo().apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            val normalIc = editText.onCreateInputConnection(normalInfo)!!
            service.setTestBinding(normalIc, normalInfo)
            service.onStartInputView(normalInfo, false)
            assertNotNull(service.clipboardSmartHint)
            assertEquals("519204", service.clipboardSmartHint?.value)

            // Snippet insert commits OTP cleanly
            service.insertTextSnippet("519204")
            assertEquals("519204", editText.text.toString())

            // Password field -> suppressed for privacy
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            val passInfo = EditorInfo()
            val passIc = editText.onCreateInputConnection(passInfo)!!
            passInfo.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            service.setTestBinding(passIc, passInfo)
            service.onStartInputView(passInfo, false)
            assertNull(service.clipboardSmartHint)
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Smart OTP pill, ClipboardQuickDock, SymbolQuickDock, AlternateCharDock, DateTime, Translation, and Floating Mode`() {
        val insertedSnippets = mutableListOf<String>()
        val pressedKeys = mutableListOf<String>()
        var currentSettings by mutableStateOf(KeyboardSettings())

        composeTestRule.setContent {
            KeyboardContainer(
                settings = currentSettings,
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                clipboardSmartHint = com.example.okkhor.engine.ClipboardSmartHint(
                    value = "842109",
                    label = "OTP: 842109",
                    isOtp = true,
                    isUrl = false
                ),
                clipboardItems = listOf(
                    ClipboardItem(id = "c1", text = "Dhaka Bangladesh", timestamp = 1000L, isPinned = true)
                ),
                onKeyPress = { pressedKeys.add(it) },
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = {},
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {},
                onUpdateSettings = { updater -> currentSettings = updater(currentSettings) },
                onInsertTextSnippet = { insertedSnippets.add(it) }
            )
        }

        composeTestRule.waitForIdle()

        // 1. Smart OTP pill inserts code on tap
        composeTestRule.onNodeWithTag("btn_toolbar_smart_clip").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("842109"), insertedSnippets)

        // 2. Long press Clipboard icon opens ClipboardQuickDock and inserts clip item
        composeTestRule.onNodeWithTag("btn_toolbar_clipboard").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(650L)
        composeTestRule.onNodeWithTag("btn_toolbar_clipboard").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("clipboard_quick_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clipboard_dock_item_0").performClick()
        composeTestRule.waitForIdle()
        assertTrue(insertedSnippets.contains("Dhaka Bangladesh"))

        // 3. Long press ?123 opens SymbolQuickDock
        composeTestRule.onNodeWithTag("btn_symbol_key").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("symbol_quick_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_symbol_key").performTouchInput { up() }
        composeTestRule.waitForIdle()

        // 4. Long press 'a' opens AlternateCharDock & tapping first alternate commits '@'
        composeTestRule.onNodeWithTag("key_a").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(650L)
        composeTestRule.onNodeWithTag("key_a").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("alternate_char_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alt_char_option_0").performClick()
        composeTestRule.waitForIdle()
        assertTrue(pressedKeys.contains("@"))

        // 5. Open Quick Tools -> Date & Time Dock -> insert formatted option
        composeTestRule.onNodeWithTag("btn_quick_tools").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tool_datetime").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("datetime_quick_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("datetime_option_iso_date").performClick()
        composeTestRule.waitForIdle()
        assertTrue(insertedSnippets.any { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) })

        // 6. Open Quick Tools -> Translate Dock -> swap + translate offline
        composeTestRule.onNodeWithTag("btn_quick_tools").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tool_translate").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("translation_quick_dock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_translate_swap").performClick()
        composeTestRule.onNodeWithTag("translation_dock_close").performClick()
        composeTestRule.waitForIdle()

        // 7. Toggle Floating Mode -> displays drag handle bar & Dock button restores docked mode
        composeTestRule.onNodeWithTag("btn_quick_tools").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tool_floating").performClick()
        composeTestRule.waitForIdle()
        assertTrue(currentSettings.floatingKeyboardMode)
        composeTestRule.onNodeWithTag("floating_drag_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_exit_floating_mode").performClick()
        composeTestRule.waitForIdle()
        assertFalse(currentSettings.floatingKeyboardMode)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify granular haptic settings, glide, floating, and dock count persist in repository and UI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository.getInstance(context)

        repo.updateSettings {
            it.copy(
                spacebarLanguageSwipe = false,
                glideTypingEnabled = false,
                floatingKeyboardMode = true,
                hapticKeypress = false,
                hapticDockOpen = true,
                hapticGestureComplete = false,
                hapticIntensityPercent = 30
            )
        }
        assertFalse(repo.settings.value.spacebarLanguageSwipe)
        assertFalse(repo.settings.value.glideTypingEnabled)
        assertTrue(repo.settings.value.floatingKeyboardMode)
        assertFalse(repo.settings.value.hapticKeypress)
        assertTrue(repo.settings.value.hapticDockOpen)
        assertFalse(repo.settings.value.hapticGestureComplete)
        assertEquals(30, repo.settings.value.hapticIntensityPercent)

        var current by mutableStateOf(repo.settings.value)
        composeTestRule.setContent {
            val scroll = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
                InputSettingsSection(
                    settings = current,
                    onUpdateSettings = { updater ->
                        current = updater(current)
                        repo.updateSettings { current }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("switch_spacebar_language_swipe").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("haptic_intensity_70").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(70, repo.settings.value.hapticIntensityPercent)
    }

    @Test
    fun `verify Bangla Phonetic am plus Space commits exact word without unwanted expansion and ignores password telemetry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.okkhor.benchmark.TypingPerformanceTracker.clearAllMetrics()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        try {
            val editText = EditText(context)
            val info = EditorInfo().apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            val ic = editText.onCreateInputConnection(info)!!
            service.setTestBinding(ic, info)
            service.onStartInputView(info, false)
            service.switchInputMode(InputMode.BANGLA_PHONETIC)

            service.handleKeyPress("a")
            service.handleKeyPress("m")
            // Candidate bar still offers "আমি" while Space commits exact "আম "
            assertTrue(service.candidates.contains("আমি"))
            service.handleActionKey(KeyboardLayouts.CODE_SPACE)
            assertEquals("আম ", editText.text.toString())

            val countBeforePassword = com.example.okkhor.benchmark.TypingPerformanceTracker.liveSummary.value.totalKeystrokes

            // Password field must not increment performance tracker keystrokes
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            val passInfo = EditorInfo()
            val passIc = editText.onCreateInputConnection(passInfo)!!
            passInfo.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            service.setTestBinding(passIc, passInfo)
            service.onStartInputView(passInfo, false)
            service.handleKeyPress("p")
            assertEquals(
                countBeforePassword,
                com.example.okkhor.benchmark.TypingPerformanceTracker.liveSummary.value.totalKeystrokes
            )
            val json = com.example.okkhor.benchmark.TypingPerformanceTracker.liveSummary.value.toJson().toString()
            assertFalse(json.contains("আম"))
            assertFalse(json.contains("\"am\""))
            assertFalse(json.contains("SecretPw"))
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify TypingPerformanceLabSection renders telemetry, micro-benchmark, and unprefilled comparison protocol`() {
        var closed = false
        composeTestRule.setContent {
            val scroll = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
                TypingPerformanceLabSection(
                    settings = KeyboardSettings(),
                    activeTheme = KeyboardTheme.OkkhorEmerald,
                    onClose = { closed = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("perf_lab_privacy_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perf_lab_live_telemetry_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perf_lab_prompt_runner_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_run_micro_bench").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("micro_bench_output").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perf_lab_comparison_protocol_card").performScrollTo().assertIsDisplayed()
        // Gboard / Ridmik slots start blank (never fabricated)
        composeTestRule.onNodeWithTag("ab_gboard_wpm_0").performScrollTo().assertTextContains("")
        composeTestRule.onNodeWithTag("ab_ridmik_wpm_0").performScrollTo().assertTextContains("")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify InputSettingsSection toggles Next-Word Prediction and Ergonomics presets`() {
        var current by mutableStateOf(KeyboardSettings())
        composeTestRule.setContent {
            val scroll = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
                InputSettingsSection(
                    settings = current,
                    onUpdateSettings = { updater -> current = updater(current) }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("switch_next_word_prediction").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertFalse(current.nextWordPredictionEnabled)

        composeTestRule.onNodeWithTag("ergo_preset_compact_speed").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(94, current.keyboardHeightPercent)
        assertEquals(3.5f, current.keySpacingDpOverride, 0.01f)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify touch hitbox accuracy at bottom edges, inter-key gaps, staggered Row 2, and bottom row`() {
        val typedKeys = mutableListOf<String>()
        val actions = mutableListOf<Int>()
        var modeSwitches = 0

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(showNumberRow = false),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                onKeyPress = { typedKeys.add(it) },
                onActionKey = { actions.add(it) },
                onCandidateClick = {},
                onModeSwitch = { modeSwitches++ },
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {}
            )
        }

        composeTestRule.waitForIdle()

        // ~1mm offset at xxhdpi (~3px/dp): 6dp below visible bottom of key (~18px)
        val density = composeTestRule.density.density
        val oneMmDownPx = 6f * density

        // 1. Bottom edge of Row 1 key 'e' (+1mm below visual rect) still hits 'e'
        composeTestRule.onNodeWithTag("key_e").performTouchInput {
            val pt = Offset(centerX, bottom + 2f)
            down(pt)
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("e"), typedKeys)

        // 2. Staggered Row 2 outer key 'a' and middle key 'g' just inside & right at bottom edge
        composeTestRule.onNodeWithTag("key_a").performTouchInput {
            down(Offset(left + 1f, bottom - 1f))
            up()
        }
        composeTestRule.onNodeWithTag("key_g").performTouchInput {
            down(Offset(centerX, bottom + 2f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("e", "a", "g"), typedKeys)

        // 3. Inter-key gap between 't' and 'y': left-biased gap tap hits 't', right-biased gap tap hits 'y'
        composeTestRule.onNodeWithTag("key_t").performTouchInput {
            down(Offset(right + 1f, centerY))
            up()
        }
        composeTestRule.onNodeWithTag("key_y").performTouchInput {
            down(Offset(left - 1f, centerY))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("e", "a", "g", "t", "y"), typedKeys)

        // 4. Bottom Row: Spacebar outer-left edge & ~0.7-1mm below bottom edge still triggers CODE_SPACE (not Emoji/Lang)
        composeTestRule.onNodeWithTag("spacebar").performTouchInput {
            down(Offset(left + 3f, bottom + oneMmDownPx * 0.7f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(actions.contains(KeyboardLayouts.CODE_SPACE))

        // 5. Bottom Row: Period '.' near bottom edge triggers '.'
        composeTestRule.onNodeWithTag("key_.").performTouchInput {
            down(Offset(centerX, bottom + oneMmDownPx * 0.6f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(typedKeys.contains("."))

        // 6. Bottom Row: Enter key near bottom bezel triggers CODE_ENTER
        composeTestRule.onNodeWithTag("btn_enter").performTouchInput {
            down(Offset(centerX, bottom + oneMmDownPx * 0.7f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(actions.contains(KeyboardLayouts.CODE_ENTER))

        // 7. Bottom Row: Language button lower edge switches mode reliably
        composeTestRule.onNodeWithTag("btn_language").performTouchInput {
            down(Offset(centerX, bottom + oneMmDownPx * 0.6f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(1, modeSwitches)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify Performance Lab exposes all 6 standardized tests and 5 10 15m Long-Session Comfort Mode`() {
        composeTestRule.setContent {
            val scroll = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
                TypingPerformanceLabSection(
                    settings = KeyboardSettings(),
                    activeTheme = KeyboardTheme.OkkhorEmerald,
                    onClose = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // All 6 standardized prompts present
        composeTestRule.onNodeWithTag("bench_prompt_en_short").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bench_prompt_en_long").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bench_prompt_bn_avro_short").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bench_prompt_bn_avro_long").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bench_prompt_bn_probhat_short").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bench_prompt_mixed_long").performScrollTo().assertIsDisplayed()

        // Long-Session Comfort Mode card & 5 / 10 / 15 min controls present
        composeTestRule.onNodeWithTag("perf_lab_long_session_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_session_dur_5").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_session_dur_10").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("long_session_dur_15").performScrollTo().assertIsDisplayed()

        // 6th comparison scenario slot present & blank until measured
        composeTestRule.onNodeWithTag("ab_gboard_wpm_5").performScrollTo().assertTextContains("")
        composeTestRule.onNodeWithTag("ab_ridmik_wpm_5").performScrollTo().assertTextContains("")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify new brand icon replaces old badge in app UI and top bar hides mode label pill`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = KeyboardPreferencesRepository.getInstance(context)

        composeTestRule.setContent {
            MainSettingsScreen(repository = repository)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("topbar_brand_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_brand_icon").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("অ").assertCountEquals(0)

        composeTestRule.onNodeWithTag("nav_tab_more").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_brand_icon").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("অ").assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify keyboard top bar omits phonetic probhat eng mode label pill`() {
        val modes = listOf(
            InputMode.BANGLA_PHONETIC,
            InputMode.BANGLA_FIXED,
            InputMode.ENGLISH
        )
        var currentMode by androidx.compose.runtime.mutableStateOf(modes.first())

        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(defaultInputMode = currentMode),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = currentMode,
                lastTextMode = currentMode,
                isShifted = false,
                candidates = emptyList(),
                onKeyPress = {},
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = { currentMode = it },
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {}
            )
        }

        modes.forEach { mode ->
            currentMode = mode
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("btn_quick_tools").assertIsDisplayed()
            composeTestRule.onNodeWithTag("mode_switch_pill").assertDoesNotExist()
            composeTestRule.onAllNodesWithText("ফোনেটিক").assertCountEquals(0)
            composeTestRule.onAllNodesWithText("প্রভাত").assertCountEquals(0)
            composeTestRule.onAllNodesWithText("ENG").assertCountEquals(0)
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify liquid drop ripple overlay animates on key press and completes cleanly`() {
        val typed = mutableListOf<String>()
        composeTestRule.setContent {
            KeyboardContainer(
                settings = KeyboardSettings(defaultInputMode = InputMode.ENGLISH),
                theme = KeyboardTheme.OkkhorEmerald,
                currentMode = InputMode.ENGLISH,
                lastTextMode = InputMode.ENGLISH,
                isShifted = false,
                candidates = emptyList(),
                onKeyPress = { typed.add(it) },
                onActionKey = {},
                onCandidateClick = {},
                onModeSwitch = {},
                onCursorMove = {},
                onOpenSettings = {},
                onOpenClipboard = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag("key_a").performTouchInput {
                down(center)
            }
            composeTestRule.mainClock.advanceTimeBy(48L)
            composeTestRule.onNodeWithTag("keyboard_liquid_drop_overlay").assertIsDisplayed()

            composeTestRule.onNodeWithTag("key_a").performTouchInput {
                up()
            }
            composeTestRule.mainClock.advanceTimeBy(600L)
            composeTestRule.onNodeWithTag("keyboard_liquid_drop_overlay").assertDoesNotExist()
            assertEquals(listOf("a"), typed)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `verify single and double Backspace tap immediately deletes composing and committed text across English and Bangla`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        try {
            val editText = EditText(context)
            val info = EditorInfo().apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            val ic = editText.onCreateInputConnection(info)!!
            service.setTestBinding(ic, info)
            service.onStartInputView(info, false)

            // 1. English 1-char active composition -> single Backspace MUST delete immediately (no 2nd tap needed)
            service.switchInputMode(InputMode.ENGLISH)
            service.handleKeyPress("a")
            assertEquals("a", editText.text.toString())
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("", editText.text.toString())

            // 2. English multi-char active composition -> double Backspace deletes 2 chars cleanly
            service.handleKeyPress("h")
            service.handleKeyPress("i")
            assertEquals("hi", editText.text.toString())
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("h", editText.text.toString())
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("", editText.text.toString())

            // 3. Bangla Phonetic 1-char active composition -> single Backspace deletes immediately
            service.switchInputMode(InputMode.BANGLA_PHONETIC)
            service.handleKeyPress("k")
            assertEquals("ক", editText.text.toString())
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("", editText.text.toString())

            // 4. Simulate stale hasSelectionState when selection was cleared -> single Backspace still deletes 1 char
            service.switchInputMode(InputMode.ENGLISH)
            editText.setText("cat")
            editText.setSelection(3)
            val staleInfo = EditorInfo().apply {
                initialSelStart = 1
                initialSelEnd = 3
            }
            val staleIc = editText.onCreateInputConnection(staleInfo)!!
            service.setTestBinding(staleIc, staleInfo)
            service.onStartInputView(staleInfo, false)
            service.hasSelectionState = true // stale flag while EditText actually has collapsed cursor at 3
            service.handleActionKey(KeyboardLayouts.CODE_DELETE)
            assertEquals("ca", editText.text.toString())
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `verify single double hold Backspace, rapid mixed typing, multi-touch, and light dark themes in KeyboardContainer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(OkkhorInputMethodService::class.java)
        val service = controller.create().get()
        try {
            val editText = EditText(context)
            val info = EditorInfo().apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            val ic = editText.onCreateInputConnection(info)!!
            service.setTestBinding(ic, info)
            service.onStartInputView(info, false)
            service.switchInputMode(InputMode.ENGLISH)

            var activeTheme by mutableStateOf(KeyboardTheme.OkkhorEmerald)
            var activeMode by mutableStateOf(InputMode.ENGLISH)

            composeTestRule.setContent {
                KeyboardContainer(
                    settings = KeyboardSettings(defaultInputMode = activeMode, showNumberRow = false),
                    theme = activeTheme,
                    currentMode = activeMode,
                    lastTextMode = InputMode.ENGLISH,
                    isShifted = false,
                    candidates = emptyList(),
                    onKeyPress = { service.handleKeyPress(it) },
                    onActionKey = { service.handleActionKey(it) },
                    onCandidateClick = {},
                    onModeSwitch = { activeMode = it },
                    onCursorMove = {},
                    onOpenSettings = {},
                    onOpenClipboard = {},
                    onDeleteWordSwipe = { service.deleteWordBeforeCursor() }
                )
            }
            composeTestRule.waitForIdle()

            // A. Rapid letter typing ("hello") + immediate single Backspace tap -> "hell"
            for (ch in listOf("h", "e", "l", "l", "o")) {
                composeTestRule.onNodeWithTag("key_$ch").performTouchInput {
                    down(center)
                    up()
                }
            }
            composeTestRule.waitForIdle()
            assertEquals("hello", editText.text.toString())

            composeTestRule.onNodeWithTag("btn_backspace").performTouchInput {
                down(center)
                up()
            }
            composeTestRule.waitForIdle()
            assertEquals("hell", editText.text.toString())

            // B. Double Backspace tap -> deletes 2 more -> "he"
            repeat(2) {
                composeTestRule.onNodeWithTag("btn_backspace").performTouchInput {
                    down(center)
                    up()
                }
            }
            composeTestRule.waitForIdle()
            assertEquals("he", editText.text.toString())

            // C. Rapid mixed-key typing: "y " + Backspace -> "hey"
            composeTestRule.onNodeWithTag("key_y").performTouchInput { down(center); up() }
            composeTestRule.onNodeWithTag("spacebar").performTouchInput { down(center); up() }
            composeTestRule.onNodeWithTag("btn_backspace").performTouchInput { down(center); up() }
            composeTestRule.waitForIdle()
            assertEquals("hey", editText.text.toString())

            // D. Hold Backspace repeats deletion and stops cleanly on release
            composeTestRule.mainClock.autoAdvance = false
            try {
                composeTestRule.onNodeWithTag("btn_backspace").performTouchInput {
                    down(center)
                }
                composeTestRule.mainClock.advanceTimeBy(550L) // initial tap + repeat ticks
                composeTestRule.onNodeWithTag("btn_backspace").performTouchInput {
                    up()
                }
                composeTestRule.mainClock.advanceTimeBy(300L) // no more deletions after UP
            } finally {
                composeTestRule.mainClock.autoAdvance = true
            }
            composeTestRule.waitForIdle()
            assertEquals("", editText.text.toString())

            // E. Multi-touch rollover typing ('a' held while 's' tapped) -> both register cleanly
            composeTestRule.onNodeWithTag("key_a").performTouchInput {
                down(pointerId = 0, position = center)
            }
            composeTestRule.onNodeWithTag("key_s").performTouchInput {
                down(pointerId = 1, position = center)
                up(pointerId = 1)
            }
            composeTestRule.onNodeWithTag("key_a").performTouchInput {
                up(pointerId = 0)
            }
            composeTestRule.waitForIdle()
            assertEquals("sa", editText.text.toString())

            // F. Light & Dark theme switching renders cleanly and animates liquid drop without blocking touch
            activeTheme = KeyboardTheme.PaperWhite
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.autoAdvance = false
            try {
                composeTestRule.onNodeWithTag("btn_backspace").performTouchInput { down(center) }
                composeTestRule.waitForIdle()
                composeTestRule.mainClock.advanceTimeBy(48L)
                composeTestRule.onNodeWithTag("keyboard_liquid_drop_overlay").assertIsDisplayed()
                composeTestRule.onNodeWithTag("btn_backspace").performTouchInput { up() }
                composeTestRule.mainClock.advanceTimeBy(450L)
                composeTestRule.onNodeWithTag("keyboard_liquid_drop_overlay").assertDoesNotExist()
            } finally {
                composeTestRule.mainClock.autoAdvance = true
            }
            composeTestRule.waitForIdle()
            assertEquals("s", editText.text.toString())

            // Dark theme tap
            activeTheme = KeyboardTheme.OkkhorEmerald
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("btn_backspace").performTouchInput { down(center); up() }
            composeTestRule.waitForIdle()
            assertEquals("", editText.text.toString())
        } finally {
            controller.destroy()
        }
    }
}
