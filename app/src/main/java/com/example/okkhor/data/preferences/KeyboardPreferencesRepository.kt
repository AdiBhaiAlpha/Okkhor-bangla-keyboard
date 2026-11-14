package com.example.okkhor.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.model.OneHandedMode
import com.example.okkhor.data.model.ShortcutItem
import com.example.okkhor.emoji.EmojiMashupRecord
import com.example.okkhor.emoji.GifItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class KeyboardPreferencesRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<KeyboardSettings> = _settings.asStateFlow()

    private val _shortcuts = MutableStateFlow(loadShortcuts())
    val shortcuts: StateFlow<List<ShortcutItem>> = _shortcuts.asStateFlow()

    private val _clipboardHistory = MutableStateFlow(loadClipboard())
    val clipboardHistory: StateFlow<List<ClipboardItem>> = _clipboardHistory.asStateFlow()

    // Emoji Recents, Frequency, Favorites, Search History & Sticky Variants
    private val _recentEmojis = MutableStateFlow(loadStringList(KEY_RECENT_EMOJIS))
    val recentEmojis: StateFlow<List<String>> = _recentEmojis.asStateFlow()

    private val _emojiFrequency = MutableStateFlow(loadFrequencyMap(KEY_EMOJI_FREQUENCY))
    val emojiFrequency: StateFlow<Map<String, Int>> = _emojiFrequency.asStateFlow()

    private val _favoriteEmojis = MutableStateFlow(loadStringList(KEY_FAVORITE_EMOJIS))
    val favoriteEmojis: StateFlow<List<String>> = _favoriteEmojis.asStateFlow()

    private val _emojiSearchHistory = MutableStateFlow(loadStringList(KEY_EMOJI_SEARCH_HISTORY))
    val emojiSearchHistory: StateFlow<List<String>> = _emojiSearchHistory.asStateFlow()

    private val _preferredEmojiVariants = MutableStateFlow(loadStringMap(KEY_PREFERRED_EMOJI_VARIANTS))
    val preferredEmojiVariants: StateFlow<Map<String, String>> = _preferredEmojiVariants.asStateFlow()

    // GIF Recents, Favorites & Search History
    private val _recentGifs = MutableStateFlow(loadGifList(KEY_RECENT_GIFS))
    val recentGifs: StateFlow<List<GifItem>> = _recentGifs.asStateFlow()

    private val _favoriteGifs = MutableStateFlow(loadGifList(KEY_FAVORITE_GIFS))
    val favoriteGifs: StateFlow<List<GifItem>> = _favoriteGifs.asStateFlow()

    private val _gifSearchHistory = MutableStateFlow(loadStringList(KEY_GIF_SEARCH_HISTORY))
    val gifSearchHistory: StateFlow<List<String>> = _gifSearchHistory.asStateFlow()

    // Emoji Kitchen Mashup Recents & Favorites
    private val _recentMashups = MutableStateFlow(loadMashupList(KEY_RECENT_MASHUPS))
    val recentMashups: StateFlow<List<EmojiMashupRecord>> = _recentMashups.asStateFlow()

    private val _favoriteMashups = MutableStateFlow(loadMashupList(KEY_FAVORITE_MASHUPS))
    val favoriteMashups: StateFlow<List<EmojiMashupRecord>> = _favoriteMashups.asStateFlow()

    init {
        if (_shortcuts.value.isEmpty()) {
            val defaults = listOf(
                ShortcutItem("shb", "শুভ সকাল!"),
                ShortcutItem("dhon", "অনেক ধন্যবাদ!"),
                ShortcutItem("omw", "আমি আসার পথে আছি।"),
                ShortcutItem("kmn", "কেমন আছেন সবাই?")
            )
            saveShortcuts(defaults)
        }
    }

    fun getActiveTheme(): KeyboardTheme {
        val current = _settings.value
        if (current.selectedThemeId == "custom" && current.customThemeJson.isNotBlank()) {
            KeyboardTheme.fromJson(current.customThemeJson)?.let { return it }
        }
        return KeyboardTheme.getById(current.selectedThemeId)
    }

    private fun loadSettings(): KeyboardSettings {
        val modeStr = prefs.getString(KEY_DEFAULT_MODE, InputMode.BANGLA_PHONETIC.name)
        val mode = try {
            InputMode.valueOf(modeStr ?: InputMode.BANGLA_PHONETIC.name)
        } catch (e: Exception) {
            InputMode.BANGLA_PHONETIC
        }

        val oneHandedModeStr = prefs.getString(KEY_ONE_HANDED_MODE, OneHandedMode.OFF.name)
        val oneHandedMode = try {
            OneHandedMode.valueOf(oneHandedModeStr ?: OneHandedMode.OFF.name)
        } catch (e: Exception) {
            OneHandedMode.OFF
        }

        return KeyboardSettings(
            selectedThemeId = prefs.getString(KEY_THEME_ID, KeyboardTheme.OkkhorEmerald.id) ?: KeyboardTheme.OkkhorEmerald.id,
            defaultInputMode = mode,
            showNumberRow = prefs.getBoolean(KEY_SHOW_NUMBER_ROW, true),
            showSuggestions = prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true),
            autoCorrect = prefs.getBoolean(KEY_AUTOCORRECT, false),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            soundFeedback = prefs.getBoolean(KEY_SOUND, true),
            keyPopupPreview = prefs.getBoolean(KEY_KEY_POPUP, true),
            spacebarCursorSwipe = prefs.getBoolean(KEY_SPACEBAR_SWIPE, true),
            backspaceSwipeDelete = prefs.getBoolean(KEY_BACKSPACE_SWIPE, true),
            keyboardHeightPercent = prefs.getInt(KEY_HEIGHT_PERCENT, 100).coerceIn(75, 140),
            keyboardWidthPercent = prefs.getInt(KEY_WIDTH_PERCENT, 100).coerceIn(75, 100),
            clipboardHistoryEnabled = prefs.getBoolean(KEY_CLIPBOARD_ENABLED, true),
            clipboardRetentionHours = prefs.getInt(KEY_CLIPBOARD_RETENTION_HOURS, 24),
            showUndoButton = prefs.getBoolean(KEY_SHOW_UNDO_BUTTON, true),
            voiceTypingEnabled = prefs.getBoolean(KEY_VOICE_TYPING_ENABLED, true),
            gifSearchEnabled = prefs.getBoolean(KEY_GIF_SEARCH_ENABLED, true),
            roundedPillKeys = prefs.getBoolean(KEY_ROUNDED_PILL_KEYS, false),
            oneHandedMode = oneHandedMode,
            longPressSymbolsEnabled = prefs.getBoolean(KEY_LONG_PRESS_SYMBOLS, true),
            smartDoubleSpacePeriod = prefs.getBoolean(KEY_DOUBLE_SPACE_PERIOD, true),
            recentEmojiDockCount = prefs.getInt(KEY_RECENT_EMOJI_DOCK_COUNT, 8).coerceIn(4, 12),
            spacebarLanguageSwipe = prefs.getBoolean(KEY_SPACEBAR_LANGUAGE_SWIPE, true),
            hapticKeypress = prefs.getBoolean(KEY_HAPTIC_KEYPRESS, true),
            hapticLongPress = prefs.getBoolean(KEY_HAPTIC_LONG_PRESS, true),
            hapticGestureComplete = prefs.getBoolean(KEY_HAPTIC_GESTURE_COMPLETE, true),
            hapticDockOpen = prefs.getBoolean(KEY_HAPTIC_DOCK_OPEN, true),
            hapticLanguageSwitch = prefs.getBoolean(KEY_HAPTIC_LANGUAGE_SWITCH, true),
            hapticIntensityPercent = prefs.getInt(KEY_HAPTIC_INTENSITY_PERCENT, 70).coerceIn(10, 100),
            glideTypingEnabled = prefs.getBoolean(KEY_GLIDE_TYPING_ENABLED, true),
            floatingKeyboardMode = prefs.getBoolean(KEY_FLOATING_KEYBOARD_MODE, false),
            floatingOffsetX = prefs.getFloat(KEY_FLOATING_OFFSET_X, 0f).coerceIn(-120f, 120f),
            floatingOffsetY = prefs.getFloat(KEY_FLOATING_OFFSET_Y, 0f).coerceIn(-160f, 0f),
            customTranslationUrl = prefs.getString(KEY_CUSTOM_TRANSLATION_URL, "") ?: "",
            customThemeJson = prefs.getString(KEY_CUSTOM_THEME_JSON, "") ?: ""
        )
    }

    fun updateSettings(transform: (KeyboardSettings) -> KeyboardSettings) {
        val previous = _settings.value
        val rawUpdated = transform(previous)
        val updated = rawUpdated.copy(
            keyboardHeightPercent = rawUpdated.keyboardHeightPercent.coerceIn(75, 140),
            keyboardWidthPercent = rawUpdated.keyboardWidthPercent.coerceIn(75, 100),
            recentEmojiDockCount = rawUpdated.recentEmojiDockCount.coerceIn(4, 12),
            hapticIntensityPercent = rawUpdated.hapticIntensityPercent.coerceIn(10, 100),
            floatingOffsetX = rawUpdated.floatingOffsetX.coerceIn(-120f, 120f),
            floatingOffsetY = rawUpdated.floatingOffsetY.coerceIn(-160f, 0f)
        )
        _settings.value = updated
        prefs.edit().apply {
            putString(KEY_THEME_ID, updated.selectedThemeId)
            putString(KEY_DEFAULT_MODE, updated.defaultInputMode.name)
            if (updated.defaultInputMode != previous.defaultInputMode && updated.defaultInputMode.isTextMode) {
                putString(KEY_LAST_TEXT_MODE, updated.defaultInputMode.name)
            }
            putBoolean(KEY_SHOW_NUMBER_ROW, updated.showNumberRow)
            putBoolean(KEY_SHOW_SUGGESTIONS, updated.showSuggestions)
            putBoolean(KEY_AUTOCORRECT, updated.autoCorrect)
            putBoolean(KEY_HAPTIC, updated.hapticFeedback)
            putBoolean(KEY_SOUND, updated.soundFeedback)
            putBoolean(KEY_KEY_POPUP, updated.keyPopupPreview)
            putBoolean(KEY_SPACEBAR_SWIPE, updated.spacebarCursorSwipe)
            putBoolean(KEY_BACKSPACE_SWIPE, updated.backspaceSwipeDelete)
            putInt(KEY_HEIGHT_PERCENT, updated.keyboardHeightPercent.coerceIn(75, 140))
            putInt(KEY_WIDTH_PERCENT, updated.keyboardWidthPercent.coerceIn(75, 100))
            putBoolean(KEY_CLIPBOARD_ENABLED, updated.clipboardHistoryEnabled)
            putInt(KEY_CLIPBOARD_RETENTION_HOURS, updated.clipboardRetentionHours)
            putBoolean(KEY_SHOW_UNDO_BUTTON, updated.showUndoButton)
            putBoolean(KEY_VOICE_TYPING_ENABLED, updated.voiceTypingEnabled)
            putBoolean(KEY_GIF_SEARCH_ENABLED, updated.gifSearchEnabled)
            putBoolean(KEY_ROUNDED_PILL_KEYS, updated.roundedPillKeys)
            putString(KEY_ONE_HANDED_MODE, updated.oneHandedMode.name)
            putBoolean(KEY_LONG_PRESS_SYMBOLS, updated.longPressSymbolsEnabled)
            putBoolean(KEY_DOUBLE_SPACE_PERIOD, updated.smartDoubleSpacePeriod)
            putInt(KEY_RECENT_EMOJI_DOCK_COUNT, updated.recentEmojiDockCount.coerceIn(4, 12))
            putBoolean(KEY_SPACEBAR_LANGUAGE_SWIPE, updated.spacebarLanguageSwipe)
            putBoolean(KEY_HAPTIC_KEYPRESS, updated.hapticKeypress)
            putBoolean(KEY_HAPTIC_LONG_PRESS, updated.hapticLongPress)
            putBoolean(KEY_HAPTIC_GESTURE_COMPLETE, updated.hapticGestureComplete)
            putBoolean(KEY_HAPTIC_DOCK_OPEN, updated.hapticDockOpen)
            putBoolean(KEY_HAPTIC_LANGUAGE_SWITCH, updated.hapticLanguageSwitch)
            putInt(KEY_HAPTIC_INTENSITY_PERCENT, updated.hapticIntensityPercent)
            putBoolean(KEY_GLIDE_TYPING_ENABLED, updated.glideTypingEnabled)
            putBoolean(KEY_FLOATING_KEYBOARD_MODE, updated.floatingKeyboardMode)
            putFloat(KEY_FLOATING_OFFSET_X, updated.floatingOffsetX)
            putFloat(KEY_FLOATING_OFFSET_Y, updated.floatingOffsetY)
            putString(KEY_CUSTOM_TRANSLATION_URL, updated.customTranslationUrl)
            putString(KEY_CUSTOM_THEME_JSON, updated.customThemeJson)
            apply()
        }
        if (updated.clipboardRetentionHours != previous.clipboardRetentionHours) {
            pruneClipboardHistory()
        }
    }

    fun getLastTextMode(): InputMode {
        val fallback = _settings.value.defaultInputMode.let {
            if (it.isTextMode) it else InputMode.BANGLA_PHONETIC
        }
        val modeStr = prefs.getString(KEY_LAST_TEXT_MODE, null) ?: return fallback
        return try {
            val parsed = InputMode.valueOf(modeStr)
            if (parsed.isTextMode) parsed else fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun setLastTextMode(mode: InputMode) {
        if (!mode.isTextMode) return
        prefs.edit().putString(KEY_LAST_TEXT_MODE, mode.name).apply()
    }

    fun resetAllSettings() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
        _recentEmojis.value = emptyList()
        _emojiFrequency.value = emptyMap()
        _favoriteEmojis.value = emptyList()
        _emojiSearchHistory.value = emptyList()
        _preferredEmojiVariants.value = emptyMap()
        _recentGifs.value = emptyList()
        _favoriteGifs.value = emptyList()
        _gifSearchHistory.value = emptyList()
        _recentMashups.value = emptyList()
        _favoriteMashups.value = emptyList()
    }

    // --- Emoji History, Frequency, Favorites & Variants ---
    fun recordEmojiUse(emoji: String) {
        if (emoji.isBlank()) return
        val updatedRecent = (listOf(emoji) + _recentEmojis.value.filterNot { it == emoji }).take(36)
        _recentEmojis.value = updatedRecent
        saveStringList(KEY_RECENT_EMOJIS, updatedRecent)

        val freq = _emojiFrequency.value.toMutableMap()
        freq[emoji] = (freq[emoji] ?: 0) + 1
        _emojiFrequency.value = freq
        saveFrequencyMap(KEY_EMOJI_FREQUENCY, freq)
    }

    fun getDockRecentEmojis(limit: Int = _settings.value.recentEmojiDockCount): List<String> {
        return rankRecentEmojisForDock(_recentEmojis.value, _emojiFrequency.value, limit)
    }

    fun toggleFavoriteEmoji(emoji: String) {
        if (emoji.isBlank()) return
        val current = _favoriteEmojis.value
        val updated = if (emoji in current) {
            current.filterNot { it == emoji }
        } else {
            (listOf(emoji) + current).take(60)
        }
        _favoriteEmojis.value = updated
        saveStringList(KEY_FAVORITE_EMOJIS, updated)
    }

    fun addEmojiSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val updated = (listOf(clean) + _emojiSearchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }).take(12)
        _emojiSearchHistory.value = updated
        saveStringList(KEY_EMOJI_SEARCH_HISTORY, updated)
    }

    fun clearEmojiSearchHistory() {
        _emojiSearchHistory.value = emptyList()
        saveStringList(KEY_EMOJI_SEARCH_HISTORY, emptyList())
    }

    fun setPreferredEmojiVariant(baseUnicode: String, selectedVariant: String) {
        val map = _preferredEmojiVariants.value.toMutableMap()
        map[baseUnicode] = selectedVariant
        _preferredEmojiVariants.value = map
        saveStringMap(KEY_PREFERRED_EMOJI_VARIANTS, map)
    }

    // --- GIF History & Favorites ---
    fun recordGifUse(gif: GifItem) {
        val updated = (listOf(gif) + _recentGifs.value.filterNot { it.id == gif.id }).take(24)
        _recentGifs.value = updated
        saveGifList(KEY_RECENT_GIFS, updated)
    }

    fun toggleFavoriteGif(gif: GifItem) {
        val exists = _favoriteGifs.value.any { it.id == gif.id }
        val updated = if (exists) {
            _favoriteGifs.value.filterNot { it.id == gif.id }
        } else {
            (listOf(gif) + _favoriteGifs.value).take(36)
        }
        _favoriteGifs.value = updated
        saveGifList(KEY_FAVORITE_GIFS, updated)
    }

    fun addGifSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val updated = (listOf(clean) + _gifSearchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }).take(12)
        _gifSearchHistory.value = updated
        saveStringList(KEY_GIF_SEARCH_HISTORY, updated)
    }

    fun clearGifSearchHistory() {
        _gifSearchHistory.value = emptyList()
        saveStringList(KEY_GIF_SEARCH_HISTORY, emptyList())
    }

    // --- Emoji Kitchen Mashups History & Favorites ---
    fun recordMashupUse(record: EmojiMashupRecord) {
        val updated = (listOf(record) + _recentMashups.value.filterNot { it.id == record.id }).take(24)
        _recentMashups.value = updated
        saveMashupList(KEY_RECENT_MASHUPS, updated)
    }

    fun toggleFavoriteMashup(record: EmojiMashupRecord) {
        val exists = _favoriteMashups.value.any { it.id == record.id }
        val updated = if (exists) {
            _favoriteMashups.value.filterNot { it.id == record.id }
        } else {
            (listOf(record) + _favoriteMashups.value).take(36)
        }
        _favoriteMashups.value = updated
        saveMashupList(KEY_FAVORITE_MASHUPS, updated)
    }

    // Shortcuts
    private fun loadShortcuts(): List<ShortcutItem> {
        val raw = prefs.getString(KEY_SHORTCUTS, null) ?: return emptyList()
        val list = mutableListOf<ShortcutItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ShortcutItem(obj.getString("trigger"), obj.getString("expansion")))
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun saveShortcuts(list: List<ShortcutItem>) {
        _shortcuts.value = list
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("trigger", item.trigger)
            obj.put("expansion", item.expansion)
            array.put(obj)
        }
        prefs.edit().putString(KEY_SHORTCUTS, array.toString()).apply()
    }

    fun addShortcut(trigger: String, expansion: String) {
        val current = _shortcuts.value.filter { it.trigger != trigger }
        saveShortcuts(current + ShortcutItem(trigger.trim(), expansion.trim()))
    }

    fun removeShortcut(trigger: String) {
        val current = _shortcuts.value.filter { it.trigger != trigger }
        saveShortcuts(current)
    }

    // Clipboard (strictly local & opt-in, up to 100 items, configurable retention)
    private fun loadClipboard(): List<ClipboardItem> {
        val raw = prefs.getString(KEY_CLIPBOARD, null) ?: return emptyList()
        val list = mutableListOf<ClipboardItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ClipboardItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        timestamp = obj.getLong("timestamp"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return filterExpiredAndCap(list, _settings.value.clipboardRetentionHours)
    }

    fun addClipboardItem(text: String, timestamp: Long = System.currentTimeMillis()) {
        if (!_settings.value.clipboardHistoryEnabled || text.isBlank()) return
        val normalized = text.trim()
        val current = _clipboardHistory.value.toMutableList()
        current.removeAll { it.text == normalized }
        current.add(0, ClipboardItem(text = normalized, timestamp = timestamp))
        val cleaned = filterExpiredAndCap(current, _settings.value.clipboardRetentionHours, timestamp)
        saveClipboard(cleaned)
    }

    fun pruneClipboardHistory(nowMs: Long = System.currentTimeMillis()) {
        val cleaned = filterExpiredAndCap(_clipboardHistory.value, _settings.value.clipboardRetentionHours, nowMs)
        if (cleaned != _clipboardHistory.value) {
            saveClipboard(cleaned)
        }
    }

    private fun filterExpiredAndCap(
        items: List<ClipboardItem>,
        retentionHours: Int,
        nowMs: Long = System.currentTimeMillis()
    ): List<ClipboardItem> {
        val retentionFiltered = if (retentionHours <= 0) {
            items
        } else {
            val cutoffMs = nowMs - (retentionHours.toLong() * 60L * 60L * 1000L)
            items.filter { it.isPinned || it.timestamp >= cutoffMs }
        }
        val pinned = retentionFiltered.filter { it.isPinned }
        val unpinned = retentionFiltered.filterNot { it.isPinned }
        val remainingSlots = (MAX_CLIPBOARD_ITEMS - pinned.size).coerceAtLeast(0)
        val keptUnpinned = unpinned.take(remainingSlots)
        val keptIds = (pinned.map { it.id } + keptUnpinned.map { it.id }).toHashSet()
        return retentionFiltered.filter { it.id in keptIds }.take(MAX_CLIPBOARD_ITEMS)
    }

    fun togglePinClipboardItem(id: String) {
        val current = _clipboardHistory.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        saveClipboard(current)
    }

    fun removeClipboardItem(id: String) {
        val current = _clipboardHistory.value.filter { it.id != id }
        saveClipboard(current)
    }

    fun clearClipboard() {
        saveClipboard(emptyList())
    }

    private fun saveClipboard(list: List<ClipboardItem>) {
        _clipboardHistory.value = list
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("timestamp", item.timestamp)
            obj.put("isPinned", item.isPinned)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CLIPBOARD, array.toString()).apply()
    }

    private fun loadStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.getString(idx) }
        }.getOrDefault(emptyList())
    }

    private fun saveStringList(key: String, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun loadFrequencyMap(key: String): Map<String, Int> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = LinkedHashMap<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun saveFrequencyMap(key: String, map: Map<String, Int>) {
        val obj = JSONObject()
        map.entries.sortedByDescending { it.value }.take(120).forEach { (k, v) ->
            obj.put(k, v)
        }
        prefs.edit().putString(key, obj.toString()).apply()
    }

    private fun loadStringMap(key: String): Map<String, String> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "")
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun saveStringMap(key: String, map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(key, obj.toString()).apply()
    }

    private fun loadGifList(key: String): List<GifItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                GifItem(
                    id = obj.getString("id"),
                    title = obj.optString("title", "GIF"),
                    previewUrl = obj.getString("previewUrl"),
                    gifUrl = obj.getString("gifUrl"),
                    width = obj.optInt("width", 200),
                    height = obj.optInt("height", 200)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveGifList(key: String, list: List<GifItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("previewUrl", item.previewUrl)
                    put("gifUrl", item.gifUrl)
                    put("width", item.width)
                    put("height", item.height)
                }
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun loadMashupList(key: String): List<EmojiMashupRecord> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                EmojiMashupRecord(
                    first = obj.getString("first"),
                    second = obj.getString("second"),
                    styleId = obj.getString("styleId"),
                    label = obj.optString("label", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveMashupList(key: String, list: List<EmojiMashupRecord>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("first", item.first)
                    put("second", item.second)
                    put("styleId", item.styleId)
                    put("label", item.label)
                }
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "okkhor_keyboard_prefs"
        private const val KEY_THEME_ID = "pref_theme_id"
        private const val KEY_DEFAULT_MODE = "pref_default_mode"
        private const val KEY_LAST_TEXT_MODE = "pref_last_text_mode"
        private const val KEY_SHOW_NUMBER_ROW = "pref_show_number_row"
        private const val KEY_SHOW_SUGGESTIONS = "pref_show_suggestions"
        private const val KEY_AUTOCORRECT = "pref_autocorrect"
        private const val KEY_HAPTIC = "pref_haptic"
        private const val KEY_SOUND = "pref_sound"
        private const val KEY_KEY_POPUP = "pref_key_popup"
        private const val KEY_SPACEBAR_SWIPE = "pref_spacebar_swipe"
        private const val KEY_BACKSPACE_SWIPE = "pref_backspace_swipe"
        private const val KEY_HEIGHT_PERCENT = "pref_height_percent"
        private const val KEY_WIDTH_PERCENT = "pref_width_percent"
        const val MAX_CLIPBOARD_ITEMS = 100
        private const val KEY_CLIPBOARD_ENABLED = "pref_clipboard_enabled"
        private const val KEY_CLIPBOARD_RETENTION_HOURS = "pref_clipboard_retention_hours"
        private const val KEY_SHOW_UNDO_BUTTON = "pref_show_undo_button"
        private const val KEY_VOICE_TYPING_ENABLED = "pref_voice_typing_enabled"
        private const val KEY_GIF_SEARCH_ENABLED = "pref_gif_search_enabled"
        private const val KEY_ROUNDED_PILL_KEYS = "pref_rounded_pill_keys"
        private const val KEY_ONE_HANDED_MODE = "pref_one_handed_mode"
        private const val KEY_LONG_PRESS_SYMBOLS = "pref_long_press_symbols"
        private const val KEY_DOUBLE_SPACE_PERIOD = "pref_double_space_period"
        private const val KEY_RECENT_EMOJI_DOCK_COUNT = "pref_recent_emoji_dock_count"
        private const val KEY_SPACEBAR_LANGUAGE_SWIPE = "pref_spacebar_language_swipe"
        private const val KEY_HAPTIC_KEYPRESS = "pref_haptic_keypress"
        private const val KEY_HAPTIC_LONG_PRESS = "pref_haptic_long_press"
        private const val KEY_HAPTIC_GESTURE_COMPLETE = "pref_haptic_gesture_complete"
        private const val KEY_HAPTIC_DOCK_OPEN = "pref_haptic_dock_open"
        private const val KEY_HAPTIC_LANGUAGE_SWITCH = "pref_haptic_language_switch"
        private const val KEY_HAPTIC_INTENSITY_PERCENT = "pref_haptic_intensity_percent"
        private const val KEY_GLIDE_TYPING_ENABLED = "pref_glide_typing_enabled"
        private const val KEY_FLOATING_KEYBOARD_MODE = "pref_floating_keyboard_mode"
        private const val KEY_FLOATING_OFFSET_X = "pref_floating_offset_x"
        private const val KEY_FLOATING_OFFSET_Y = "pref_floating_offset_y"
        private const val KEY_CUSTOM_TRANSLATION_URL = "pref_custom_translation_url"
        private const val KEY_CUSTOM_THEME_JSON = "pref_custom_theme_json"
        private const val KEY_SHORTCUTS = "pref_shortcuts_json"
        private const val KEY_CLIPBOARD = "pref_clipboard_json"
        private const val KEY_RECENT_EMOJIS = "pref_recent_emojis"
        private const val KEY_EMOJI_FREQUENCY = "pref_emoji_frequency"
        private const val KEY_FAVORITE_EMOJIS = "pref_favorite_emojis"
        private const val KEY_EMOJI_SEARCH_HISTORY = "pref_emoji_search_history"
        private const val KEY_PREFERRED_EMOJI_VARIANTS = "pref_preferred_emoji_variants"
        private const val KEY_RECENT_GIFS = "pref_recent_gifs"
        private const val KEY_FAVORITE_GIFS = "pref_favorite_gifs"
        private const val KEY_GIF_SEARCH_HISTORY = "pref_gif_search_history"
        private const val KEY_RECENT_MASHUPS = "pref_recent_mashups"
        private const val KEY_FAVORITE_MASHUPS = "pref_favorite_mashups"

        fun rankRecentEmojisForDock(
            recent: List<String>,
            frequency: Map<String, Int>,
            limit: Int
        ): List<String> {
            val safeLimit = limit.coerceIn(4, 12)
            return recent
                .asSequence()
                .filter { it.isNotBlank() }
                .distinct()
                .sortedByDescending { frequency[it] ?: 1 }
                .take(safeLimit)
                .toList()
        }

        @Volatile
        private var INSTANCE: KeyboardPreferencesRepository? = null

        fun getInstance(context: Context): KeyboardPreferencesRepository {
            val appCtx = context.applicationContext
            val existing = INSTANCE
            if (existing != null && existing.appContext === appCtx) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && current.appContext === appCtx) {
                    current
                } else {
                    KeyboardPreferencesRepository(appCtx).also { INSTANCE = it }
                }
            }
        }
    }
}
