package com.example.okkhor.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import com.example.okkhor.emoji.EmojiKitchenEngine
import com.example.okkhor.emoji.EmojiMashupRecord
import com.example.okkhor.emoji.EmojiMashupStyle
import com.example.okkhor.emoji.GifItem
import com.example.okkhor.emoji.GiphyRepository
import com.example.okkhor.emoji.UnicodeEmojiRepository
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.ClipboardSmartHint
import com.example.okkhor.engine.ClipboardSmartParser
import com.example.okkhor.engine.SmartWordMemory
import com.example.okkhor.engine.SuggestionEngine
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.okkhor.ui.keyboard.KeyboardLayouts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class OkkhorInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mViewModelStore = ViewModelStore()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var prefsRepo: KeyboardPreferencesRepository
    private lateinit var smartMemory: SmartWordMemory
    private val phoneticEngine = BanglaPhoneticEngine()
    private lateinit var suggestionEngine: SuggestionEngine
    private lateinit var emojiRepository: UnicodeEmojiRepository
    private lateinit var giphyRepository: GiphyRepository

    private data class EditOperation(
        val insertedText: String,
        val replacedText: String
    )

    private val undoStack = ArrayDeque<EditOperation>()
    private val redoStack = ArrayDeque<EditOperation>()

    // Test binding overrides (remain null in production)
    private var testInputConnectionOverride: InputConnection? = null
    private var testEditorInfoOverride: EditorInfo? = null

    internal fun setTestBinding(ic: InputConnection?, editorInfo: EditorInfo?) {
        testInputConnectionOverride = ic
        testEditorInfoOverride = editorInfo
    }

    private fun activeInputConnection(): InputConnection? = testInputConnectionOverride ?: currentInputConnection
    private fun activeEditorInfo(): EditorInfo? = testEditorInfoOverride ?: currentInputEditorInfo

    // IME State
    internal var currentMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
    private var lastTextMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
    private var inputSessionKey by mutableStateOf(0)
    internal var isShifted by mutableStateOf(false)
    internal var isCapsLock by mutableStateOf(false)
    private var lastShiftPressTime = 0L
    private var candidates by mutableStateOf<List<String>>(emptyList())
    private var activeComposition = StringBuilder()
    private var isPasswordField = false
    private var lastCommittedWord = ""
    private var lastSpaceTimestamp = 0L

    // Editing, Selection & History States
    internal var canUndoState by mutableStateOf(false)
    internal var canRedoState by mutableStateOf(false)
    internal var hasSelectionState by mutableStateOf(false)
    internal var canPasteState by mutableStateOf(false)
    internal var isSelectionMode by mutableStateOf(false)
    private var currentSelStart = -1
    private var currentSelEnd = -1
    private var selectionAnchor = -1

    // Rich Media Commit Capabilities & Honest Feedback
    internal var supportsGifCommit by mutableStateOf(false)
    internal var supportsImageCommit by mutableStateOf(false)
    internal var mediaInsertNotice by mutableStateOf<String?>(null)
    internal var clipboardSmartHint by mutableStateOf<ClipboardSmartHint?>(null)

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private lateinit var voiceController: VoiceInputController
    private var inputComposeView: ComposeView? = null

    override fun onCreate() {
        setTheme(R.style.Theme_OkkhorIme)
        super.onCreate()
        ensureTransparentImeWindow(window?.window, null)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        prefsRepo = KeyboardPreferencesRepository.getInstance(applicationContext)
        smartMemory = SmartWordMemory.getInstance(applicationContext)
        suggestionEngine = SuggestionEngine(phoneticEngine, smartMemory)
        emojiRepository = UnicodeEmojiRepository.getInstance(applicationContext)
        giphyRepository = GiphyRepository(applicationContext)
        lastTextMode = prefsRepo.getLastTextMode()
        currentMode = lastTextMode
        prefsRepo.pruneClipboardHistory()
        refreshClipboardCapability()

        voiceController = VoiceInputController(applicationContext) { spokenText ->
            val ic = activeInputConnection() ?: return@VoiceInputController
            if (activeComposition.isNotEmpty()) {
                val committed = getActiveComposingCommitText()
                if (committed.isNotEmpty()) {
                    ic.commitText("$committed ", 1)
                }
                resetComposition()
            }
            val commitText = "$spokenText "
            ic.commitText(commitText, 1)
            lastCommittedWord = spokenText.split("\\s+".toRegex()).lastOrNull().orEmpty()
            recordOperation(insertedText = commitText, replacedText = "")
        }

        // Automatic Clipboard History Listener
        setupClipboardListener()
    }

    private fun setupClipboardListener() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            refreshClipboardCapability()
            try {
                if (prefsRepo.settings.value.clipboardHistoryEnabled) {
                    val clip = cm.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            prefsRepo.addClipboardItem(text)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore background clip access security exceptions on Android 10+
            }
        }
        cm.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun refreshClipboardCapability() {
        val clip = getPrimaryClipboardText()
        canPasteState = !clip.isNullOrEmpty()
        clipboardSmartHint = if (!isPasswordField && prefsRepo.settings.value.clipboardHistoryEnabled) {
            ClipboardSmartParser.extractHint(clip)
        } else {
            null
        }
    }

    private fun getPrimaryClipboardText(): String? {
        return try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
            if (!cm.hasPrimaryClip()) return null
            val clip = cm.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            val item = clip.getItemAt(0)
            val txt = item?.text?.toString() ?: item?.coerceToText(this)?.toString()
            txt?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        ensureTransparentImeWindow(win, inputComposeView, isFullscreen)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        ensureTransparentImeWindow(window?.window, inputComposeView, isFullscreenMode)
        refreshClipboardCapability()
        refreshSelectionState(activeInputConnection())
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            background = null
            setBackgroundColor(Color.TRANSPARENT)
            elevation = 0f
            clipToOutline = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isForceDarkAllowed = false
            }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@OkkhorInputMethodService))
            setViewTreeLifecycleOwner(this@OkkhorInputMethodService)
            setViewTreeViewModelStoreOwner(this@OkkhorInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@OkkhorInputMethodService)
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    ensureTransparentImeWindow(window?.window, v, isFullscreenMode)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })

            setContent {
                val settings by prefsRepo.settings.collectAsState()
                val theme = prefsRepo.getActiveTheme()
                val clipboardList by prefsRepo.clipboardHistory.collectAsState()
                val voiceState by voiceController.state.collectAsState()

                val recentEmojis by prefsRepo.recentEmojis.collectAsState()
                val emojiFrequency by prefsRepo.emojiFrequency.collectAsState()
                val favoriteEmojis by prefsRepo.favoriteEmojis.collectAsState()
                val emojiSearchHistory by prefsRepo.emojiSearchHistory.collectAsState()
                val preferredVariants by prefsRepo.preferredEmojiVariants.collectAsState()

                val recentGifs by prefsRepo.recentGifs.collectAsState()
                val favoriteGifs by prefsRepo.favoriteGifs.collectAsState()
                val gifSearchHistory by prefsRepo.gifSearchHistory.collectAsState()

                val recentMashups by prefsRepo.recentMashups.collectAsState()
                val favoriteMashups by prefsRepo.favoriteMashups.collectAsState()

                val editorInfo = activeEditorInfo()
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
                val enterLabel = when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> "Search"
                    EditorInfo.IME_ACTION_GO -> "Go"
                    EditorInfo.IME_ACTION_SEND -> "Send"
                    EditorInfo.IME_ACTION_NEXT -> "Next"
                    EditorInfo.IME_ACTION_DONE -> "Done"
                    else -> "Enter"
                }

                KeyboardContainer(
                    settings = settings,
                    theme = theme,
                    currentMode = currentMode,
                    lastTextMode = lastTextMode,
                    inputSessionKey = inputSessionKey,
                    isShifted = isShifted,
                    isCapsLock = isCapsLock,
                    candidates = if (isPasswordField) emptyList() else candidates,
                    canUndo = !isPasswordField && settings.showUndoButton && canUndoState,
                    canRedo = !isPasswordField && settings.showUndoButton && canRedoState,
                    hasSelection = hasSelectionState,
                    canPaste = canPasteState,
                    isSelectionMode = isSelectionMode,
                    voiceState = if (isPasswordField) VoiceInputState.Idle else voiceState,
                    enterActionLabel = enterLabel,
                    clipboardItems = clipboardList,
                    clipboardSmartHint = if (isPasswordField) null else clipboardSmartHint,
                    emojiRepository = emojiRepository,
                    giphyRepository = giphyRepository,
                    recentEmojis = recentEmojis,
                    emojiFrequency = emojiFrequency,
                    favoriteEmojis = favoriteEmojis,
                    emojiSearchHistory = emojiSearchHistory,
                    preferredEmojiVariants = preferredVariants,
                    recentGifs = recentGifs,
                    favoriteGifs = favoriteGifs,
                    gifSearchHistory = gifSearchHistory,
                    recentMashups = recentMashups,
                    favoriteMashups = favoriteMashups,
                    supportsGifCommit = supportsGifCommit,
                    supportsImageCommit = supportsImageCommit,
                    mediaInsertNotice = mediaInsertNotice,
                    onDismissMediaNotice = { mediaInsertNotice = null },
                    onEmojiSelected = { base, inserted -> handleEmojiSelected(base, inserted) },
                    onSelectEmojiVariant = { base, variant ->
                        prefsRepo.setPreferredEmojiVariant(base, variant)
                        handleEmojiSelected(base, variant)
                    },
                    onToggleFavoriteEmoji = { prefsRepo.toggleFavoriteEmoji(it) },
                    onRecordEmojiSearch = { prefsRepo.addEmojiSearchHistory(it) },
                    onClearEmojiSearchHistory = { prefsRepo.clearEmojiSearchHistory() },
                    onInsertGif = { gif ->
                        serviceScope.launch { commitGif(gif) }
                    },
                    onShareGif = { gif ->
                        serviceScope.launch { shareGif(gif) }
                    },
                    onCopyGifUrl = { gif -> copyGifUrl(gif) },
                    onToggleFavoriteGif = { prefsRepo.toggleFavoriteGif(it) },
                    onRecordGifSearch = { prefsRepo.addGifSearchHistory(it) },
                    onClearGifSearchHistory = { prefsRepo.clearGifSearchHistory() },
                    onInsertMashup = { first, second, style ->
                        commitMashup(first, second, style)
                    },
                    onShareMashup = { first, second, style ->
                        shareMashup(first, second, style)
                    },
                    onToggleFavoriteMashup = { prefsRepo.toggleFavoriteMashup(it) },
                    onKeyPress = { key -> handleKeyPress(key) },
                    onActionKey = { code -> handleActionKey(code) },
                    onCandidateClick = { candidate -> commitCandidate(candidate) },
                    onUndoClick = { performUndo() },
                    onRedoClick = { performRedo() },
                    onToggleSelectionMode = { handleTextEditAction("TOGGLE_SELECTION_MODE") },
                    onToggleVoice = {
                        if (voiceState is VoiceInputState.Listening) {
                            voiceController.stopListening()
                        } else {
                            val activeVoiceMode = if (currentMode.isTextMode) currentMode else lastTextMode
                            voiceController.startListening(activeVoiceMode)
                        }
                    },
                    onStopVoice = { voiceController.stopListening() },
                    onDismissVoice = { voiceController.dismissNotice() },
                    onGrantMicPermission = {
                        voiceController.dismissNotice()
                        val intent = Intent(this@OkkhorInputMethodService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            putExtra(MainActivity.EXTRA_REQUEST_MIC_PERMISSION, true)
                        }
                        startActivity(intent)
                    },
                    onModeSwitch = { newMode ->
                        switchInputMode(newMode)
                    },
                    onCursorMove = { delta -> moveCursor(delta) },
                    onOpenSettings = {
                        val intent = Intent(this@OkkhorInputMethodService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        }
                        startActivity(intent)
                    },
                    onOpenClipboard = {
                        checkSystemClipboard()
                        refreshClipboardCapability()
                    },
                    onTogglePinClipboard = { id -> prefsRepo.togglePinClipboardItem(id) },
                    onRemoveClipboardItem = { id -> prefsRepo.removeClipboardItem(id) },
                    onClearClipboard = { prefsRepo.clearClipboard() },
                    onUpdateOneHandedMode = { mode ->
                        prefsRepo.updateSettings { it.copy(oneHandedMode = mode) }
                    },
                    onUpdateHeightPercent = { height ->
                        prefsRepo.updateSettings {
                            it.copy(keyboardHeightPercent = height.coerceIn(75, 140))
                        }
                    },
                    onUpdateDimensions = { height, width ->
                        prefsRepo.updateSettings {
                            it.copy(
                                keyboardHeightPercent = height.coerceIn(75, 140),
                                keyboardWidthPercent = width.coerceIn(75, 100)
                            )
                        }
                    },
                    onCycleTheme = {
                        val all = KeyboardTheme.ALL_PRESETS
                        val currIdx = all.indexOfFirst { it.id == settings.selectedThemeId }.coerceAtLeast(0)
                        val next = all[(currIdx + 1) % all.size]
                        prefsRepo.updateSettings { it.copy(selectedThemeId = next.id) }
                    },
                    onUpdateSettings = { transform ->
                        prefsRepo.updateSettings(transform)
                    },
                    onTextEditAction = { action ->
                        handleTextEditAction(action)
                    },
                    onDeleteWordSwipe = {
                        deleteWordBeforeCursor()
                    },
                    onInsertTextSnippet = { snippet ->
                        insertTextSnippet(snippet)
                    },
                    onCommitGlideWord = { word, alts ->
                        commitGlideWord(word, alts)
                    }
                )
            }
        }
        inputComposeView = composeView
        ensureTransparentImeWindow(window?.window, composeView, isFullscreenMode)
        return composeView
    }

    internal fun getCurrentSelectedText(ic: InputConnection?): CharSequence? {
        if (ic == null) return null
        val direct = runCatching { ic.getSelectedText(0) }.getOrNull()
        if (!direct.isNullOrEmpty()) return direct

        val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
        val text = extracted?.text ?: return null
        val start = extracted.selectionStart
        val end = extracted.selectionEnd
        if (start >= 0 && end >= 0 && start != end) {
            val lo = minOf(start, end).coerceIn(0, text.length)
            val hi = maxOf(start, end).coerceIn(0, text.length)
            if (hi > lo) {
                return text.subSequence(lo, hi)
            }
        }
        return null
    }

    private fun refreshSelectionState(ic: InputConnection?) {
        if (ic == null) {
            hasSelectionState = false
            return
        }
        val selected = getCurrentSelectedText(ic)
        if (!selected.isNullOrEmpty()) {
            hasSelectionState = true
            val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
            if (extracted != null && extracted.selectionStart >= 0 && extracted.selectionEnd >= 0) {
                currentSelStart = extracted.selectionStart
                currentSelEnd = extracted.selectionEnd
            }
        } else {
            val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
            if (extracted != null && extracted.selectionStart >= 0 && extracted.selectionEnd >= 0) {
                currentSelStart = extracted.selectionStart
                currentSelEnd = extracted.selectionEnd
                hasSelectionState = currentSelStart != currentSelEnd
            } else {
                hasSelectionState = currentSelStart >= 0 && currentSelEnd >= 0 && currentSelStart != currentSelEnd
            }
        }
    }

    private fun recordOperation(insertedText: String, replacedText: String) {
        if (insertedText.isEmpty() && replacedText.isEmpty()) return
        undoStack.addLast(EditOperation(insertedText = insertedText, replacedText = replacedText))
        while (undoStack.size > MAX_HISTORY_STEPS) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        syncHistoryFlags()
    }

    private fun clearEditHistory() {
        undoStack.clear()
        redoStack.clear()
        syncHistoryFlags()
    }

    private fun syncHistoryFlags() {
        canUndoState = undoStack.isNotEmpty()
        canRedoState = redoStack.isNotEmpty()
    }

    internal fun handleTextEditAction(action: String) {
        val ic = activeInputConnection() ?: return
        when (action) {
            "SELECT_ALL" -> {
                clearLocalComposition()
                val handled = ic.performContextMenuAction(android.R.id.selectAll)
                val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
                if ((!handled || (extracted != null && extracted.selectionStart == extracted.selectionEnd)) &&
                    extracted?.text != null && extracted.text.isNotEmpty()
                ) {
                    ic.setSelection(0, extracted.text.length)
                }
                val after = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
                if (after != null && after.selectionStart >= 0 && after.selectionEnd >= 0) {
                    currentSelStart = after.selectionStart
                    currentSelEnd = after.selectionEnd
                    selectionAnchor = minOf(after.selectionStart, after.selectionEnd)
                    hasSelectionState = after.selectionStart != after.selectionEnd
                } else {
                    refreshSelectionState(ic)
                }
            }

            "COPY" -> {
                val selected = getCurrentSelectedText(ic)?.toString().orEmpty()
                if (selected.isEmpty()) {
                    refreshSelectionState(ic)
                    return
                }
                writeToSystemClipboard(selected)
                if (prefsRepo.settings.value.clipboardHistoryEnabled) {
                    prefsRepo.addClipboardItem(selected)
                }
                refreshClipboardCapability()
            }

            "CUT" -> {
                val selected = getCurrentSelectedText(ic)?.toString().orEmpty()
                if (selected.isEmpty()) {
                    refreshSelectionState(ic)
                    return
                }
                val hadComposition = activeComposition.isNotEmpty()
                clearLocalComposition()
                writeToSystemClipboard(selected)
                ic.beginBatchEdit()
                if (hadComposition) {
                    ic.finishComposingText()
                }
                val committed = ic.commitText("", 1)
                ic.endBatchEdit()
                val remainingSelection = getCurrentSelectedText(ic)?.toString().orEmpty()
                if (!committed || remainingSelection == selected) {
                    if (!ic.performContextMenuAction(android.R.id.cut)) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                    }
                }
                if (prefsRepo.settings.value.clipboardHistoryEnabled) {
                    prefsRepo.addClipboardItem(selected)
                }
                recordOperation(insertedText = "", replacedText = selected)
                hasSelectionState = false
                isSelectionMode = false
                selectionAnchor = -1
                refreshSelectionState(ic)
                refreshClipboardCapability()
                updateAutoCapitalization()
            }

            "PASTE" -> {
                val clipText = getPrimaryClipboardText() ?: return
                val replaced = getCurrentSelectedText(ic)?.toString().orEmpty()
                val hadComposition = activeComposition.isNotEmpty()
                clearLocalComposition()
                ic.beginBatchEdit()
                if (hadComposition) {
                    ic.finishComposingText()
                }
                val committed = ic.commitText(clipText, 1)
                ic.endBatchEdit()
                if (!committed) {
                    ic.performContextMenuAction(android.R.id.paste)
                }
                recordOperation(insertedText = clipText, replacedText = replaced)
                hasSelectionState = false
                isSelectionMode = false
                selectionAnchor = -1
                refreshSelectionState(ic)
                updateAutoCapitalization()
            }

            "UNDO" -> performUndo()

            "REDO" -> performRedo()

            "TOGGLE_SELECTION_MODE" -> {
                isSelectionMode = !isSelectionMode
                if (isSelectionMode) {
                    val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
                    selectionAnchor = when {
                        extracted != null && extracted.selectionStart >= 0 -> extracted.selectionStart
                        currentSelStart >= 0 -> currentSelStart
                        else -> 0
                    }
                } else {
                    selectionAnchor = -1
                }
            }

            "CURSOR_UP" -> {
                val meta = if (isSelectionMode) KeyEvent.META_SHIFT_ON else 0
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, meta))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, 0, meta))
            }

            "CURSOR_DOWN" -> {
                val meta = if (isSelectionMode) KeyEvent.META_SHIFT_ON else 0
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, meta))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0, meta))
            }

            "SELECT_WORD" -> selectWordAtCursor()

            "DELETE_WORD" -> deleteWordBeforeCursor()

            "PREV_WORD" -> {
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
            }

            "NEXT_WORD" -> {
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
            }

            "LINE_START" -> {
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME, 0, 0))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME, 0, 0))
            }

            "LINE_END" -> {
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END, 0, 0))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END, 0, 0))
            }
        }
    }

    private fun selectWordAtCursor() {
        val ic = activeInputConnection() ?: return
        val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
        val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(64, 0)?.toString().orEmpty()
        val combined = before + after
        if (combined.isEmpty()) return
        val cursorLocal = before.length
        var start = cursorLocal.coerceIn(0, combined.length)
        var end = start
        while (start > 0 && isWordChar(combined[start - 1])) start--
        while (end < combined.length && isWordChar(combined[end])) end++
        if (start < end) {
            val base = (extracted?.selectionStart ?: cursorLocal) - before.length
            ic.setSelection(base + start, base + end)
            refreshSelectionState(ic)
        }
    }

    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c in 'ক'..'হ' || c in 'া'..'ৌ' || c == 'ৎ' || c == 'ং' || c == 'ঃ' || c == 'ঁ' || c == '়' || c.toString() == "র্" || c == '্'
    }

    private fun writeToSystemClipboard(text: String) {
        if (text.isEmpty()) return
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val existing = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            if (existing != text) {
                cm.setPrimaryClip(ClipData.newPlainText("Okkhor", text))
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    private fun checkSystemClipboard() {
        try {
            val text = getPrimaryClipboardText()
            if (!text.isNullOrBlank()) {
                prefsRepo.addClipboardItem(text)
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED || lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        val effectiveInfo = info ?: testEditorInfoOverride
        isPasswordField = effectiveInfo?.let { checkIsPasswordField(it) } ?: false
        lastTextMode = prefsRepo.getLastTextMode()
        currentMode = resolveModeForEditor(effectiveInfo, lastTextMode)
        isCapsLock = false
        isShifted = false
        isSelectionMode = false
        selectionAnchor = -1
        mediaInsertNotice = null
        clearEditHistory()

        // Evaluate editor's InputConnectionCompat MIME support honestly
        val contentMimes = effectiveInfo?.let { EditorInfoCompat.getContentMimeTypes(it) } ?: emptyArray()
        supportsGifCommit = contentMimes.any { mime ->
            mime.equals("image/gif", ignoreCase = true) || mime.equals("image/*", ignoreCase = true)
        }
        supportsImageCommit = contentMimes.any { mime ->
            mime.equals("image/png", ignoreCase = true) ||
                mime.equals("image/jpeg", ignoreCase = true) ||
                mime.equals("image/*", ignoreCase = true)
        }

        // Preserve any existing selection in host editor instead of calling finishComposingText()
        clearLocalComposition()
        currentSelStart = effectiveInfo?.initialSelStart ?: -1
        currentSelEnd = effectiveInfo?.initialSelEnd ?: -1
        refreshSelectionState(activeInputConnection())

        inputSessionKey++

        prefsRepo.pruneClipboardHistory()
        checkSystemClipboard()
        refreshClipboardCapability()
        updateAutoCapitalization()
        ensureTransparentImeWindow(window?.window, inputComposeView, isFullscreenMode)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        voiceController.stopListening()
        if (activeComposition.isNotEmpty()) {
            resetComposition()
        } else {
            clearLocalComposition()
        }
        clearEditHistory()
        isSelectionMode = false
        selectionAnchor = -1
        hasSelectionState = false
        isShifted = false
        isCapsLock = false
        mediaInsertNotice = null
        clipboardSmartHint = null
        if (!currentMode.isTextMode) {
            currentMode = lastTextMode
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        currentSelStart = newSelStart
        currentSelEnd = newSelEnd
        val nowHasSelection = newSelStart >= 0 && newSelEnd >= 0 && newSelStart != newSelEnd
        hasSelectionState = nowHasSelection
        if (!nowHasSelection && !isSelectionMode) {
            selectionAnchor = -1
        }
        if (activeComposition.isNotEmpty() && (candidatesStart < 0 || nowHasSelection || newSelStart != candidatesEnd)) {
            if (nowHasSelection || candidatesStart < 0) {
                // Never call finishComposingText() while user has active native selection handles
                clearLocalComposition()
            } else {
                resetComposition()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        inputComposeView = null
        voiceController.destroy()
        clipboardListener?.let {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.removePrimaryClipChangedListener(it)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
    }

    private fun ensureTransparentImeWindow(
        win: Window?,
        composeRoot: View?,
        isFullscreen: Boolean = false
    ) {
        if (win == null) return
        win.setFormat(PixelFormat.TRANSLUCENT)
        win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        win.setDimAmount(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            win.isNavigationBarContrastEnforced = false
            win.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            win.navigationBarColor = Color.TRANSPARENT
        }

        val decor = win.decorView
        decor.setViewTreeLifecycleOwner(this@OkkhorInputMethodService)
        decor.setViewTreeViewModelStoreOwner(this@OkkhorInputMethodService)
        decor.setViewTreeSavedStateRegistryOwner(this@OkkhorInputMethodService)

        clearNonComposeHierarchy(decor, composeRoot, skipExtractArea = isFullscreen)

        composeRoot?.let { root ->
            var parent = root.parent
            while (parent is View) {
                clearSingleViewBackground(parent)
                if (parent is ViewGroup) {
                    parent.clipChildren = false
                    parent.clipToPadding = false
                }
                parent = parent.parent
            }
        }
    }

    private fun clearSingleViewBackground(view: View) {
        view.background = null
        view.elevation = 0f
        view.clipToOutline = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.isForceDarkAllowed = false
        }
    }

    private fun clearNonComposeHierarchy(
        view: View,
        composeRoot: View?,
        skipExtractArea: Boolean
    ) {
        if (view === composeRoot) return
        if (skipExtractArea && view.id == android.R.id.extractArea) return
        clearSingleViewBackground(view)
        if (view is ViewGroup) {
            view.clipChildren = false
            view.clipToPadding = false
            for (i in 0 until view.childCount) {
                clearNonComposeHierarchy(view.getChildAt(i), composeRoot, skipExtractArea)
            }
        }
    }

    private fun checkIsPasswordField(info: EditorInfo): Boolean {
        val mask = info.inputType and InputType.TYPE_MASK_CLASS
        return when (mask) {
            InputType.TYPE_CLASS_TEXT -> {
                val variation = info.inputType and InputType.TYPE_MASK_VARIATION
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            }
            InputType.TYPE_CLASS_NUMBER -> {
                val variation = info.inputType and InputType.TYPE_MASK_VARIATION
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            else -> false
        }
    }

    internal fun switchInputMode(newMode: InputMode) {
        val ic = activeInputConnection()
        if (ic != null && activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
                lastCommittedWord = committed
                recordOperation(insertedText = committed, replacedText = "")
            }
        }
        resetComposition()
        mediaInsertNotice = null
        if (newMode.isTextMode) {
            lastTextMode = newMode
            prefsRepo.setLastTextMode(newMode)
        }
        currentMode = newMode
        if (newMode != InputMode.ENGLISH) {
            isShifted = false
            isCapsLock = false
        } else {
            updateAutoCapitalization()
        }
    }

    private fun resolveModeForEditor(info: EditorInfo?, preferredTextMode: InputMode): InputMode {
        if (info == null) return preferredTextMode
        val mask = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return when {
            mask == InputType.TYPE_CLASS_NUMBER ||
                mask == InputType.TYPE_CLASS_PHONE ||
                mask == InputType.TYPE_CLASS_DATETIME -> InputMode.NUMBERS_SYMBOLS
            isPasswordField ||
                (mask == InputType.TYPE_CLASS_TEXT && (
                    variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_URI
                    )) -> InputMode.ENGLISH
            else -> preferredTextMode
        }
    }

    private fun updateAutoCapitalization() {
        if (currentMode != InputMode.ENGLISH || isPasswordField || isCapsLock) return
        val info = activeEditorInfo()
        if (info != null) {
            val mask = info.inputType and InputType.TYPE_MASK_CLASS
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            if (mask == InputType.TYPE_CLASS_TEXT && (
                    variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_URI
                    )
            ) {
                isShifted = false
                return
            }
        }
        val ic = activeInputConnection() ?: return
        val before = ic.getTextBeforeCursor(4, 0)?.toString()
        isShifted = when {
            before == null -> false
            before.isEmpty() -> true
            before.endsWith("\n") -> true
            before.endsWith(". ") || before.endsWith("! ") || before.endsWith("? ") || before.endsWith("। ") -> true
            else -> false
        }
    }

    private fun getActiveComposingCommitText(): String {
        val raw = activeComposition.toString()
        if (raw.isEmpty()) return ""
        return when (currentMode) {
            InputMode.BANGLA_PHONETIC -> {
                if (prefsRepo.settings.value.autoCorrect && candidates.isNotEmpty()) {
                    candidates.first()
                } else {
                    phoneticEngine.transliterate(raw)
                }
            }
            InputMode.ENGLISH -> {
                if (prefsRepo.settings.value.autoCorrect && candidates.size > 1) {
                    candidates[1]
                } else {
                    raw
                }
            }
            else -> raw
        }
    }

    internal fun handleEmojiSelected(baseUnicode: String, insertedUnicode: String) {
        val ic = activeInputConnection() ?: return
        val replaced = getCurrentSelectedText(ic)?.toString().orEmpty()
        if (activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
                lastCommittedWord = committed
            }
            clearLocalComposition()
        }
        ic.commitText(insertedUnicode, 1)
        recordOperation(insertedText = insertedUnicode, replacedText = replaced)
        prefsRepo.recordEmojiUse(insertedUnicode)
        hasSelectionState = false
        isSelectionMode = false
        selectionAnchor = -1
    }

    internal suspend fun commitGif(gif: GifItem): Boolean {
        if (!supportsGifCommit) {
            mediaInsertNotice = "This text field doesn't support direct GIF pasting. Use Share or Link."
            return false
        }
        val file = giphyRepository.downloadGifToCache(gif)
        if (file == null) {
            mediaInsertNotice = "Unable to download GIF. Check connection or copy GIF link."
            return false
        }
        val committed = commitFileViaContentConnection(
            file = file,
            mimeType = "image/gif",
            description = gif.title.ifBlank { "Okkhor GIF" },
            linkUrl = gif.gifUrl
        )
        return if (committed) {
            mediaInsertNotice = null
            prefsRepo.recordGifUse(gif)
            true
        } else {
            mediaInsertNotice = "Editor declined direct GIF insertion. Tap Share or Link."
            false
        }
    }

    private suspend fun shareGif(gif: GifItem) {
        val file = giphyRepository.downloadGifToCache(gif)
        if (file != null) {
            shareMediaFile(file, "image/gif", gif.title.ifBlank { "Share GIF" })
            prefsRepo.recordGifUse(gif)
        } else if (gif.gifUrl.isNotBlank()) {
            val textShare = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, gif.gifUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(Intent.createChooser(textShare, "Share GIF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        } else {
            mediaInsertNotice = "Unable to share GIF without an internet connection."
        }
    }

    private fun copyGifUrl(gif: GifItem) {
        val url = gif.gifUrl.ifBlank { gif.previewUrl }
        if (url.isNotBlank()) {
            writeToSystemClipboard(url)
            refreshClipboardCapability()
            mediaInsertNotice = "GIF link copied to clipboard."
        }
    }

    internal fun commitMashup(first: String, second: String, style: EmojiMashupStyle): Boolean {
        val outFile = EmojiKitchenEngine.saveMashupPng(applicationContext, first, second, style)
        val record = EmojiMashupRecord(
            first = first,
            second = second,
            styleId = style.id,
            label = "$first + $second"
        )
        if (!supportsImageCommit) {
            mediaInsertNotice = "This field doesn't support PNG stickers. Tap Share to send as image."
            return false
        }
        val committed = commitFileViaContentConnection(
            file = outFile,
            mimeType = "image/png",
            description = "$first + $second Mashup",
            linkUrl = null
        )
        return if (committed) {
            mediaInsertNotice = null
            prefsRepo.recordMashupUse(record)
            true
        } else {
            mediaInsertNotice = "Editor declined direct sticker insertion. Tap Share to send PNG."
            false
        }
    }

    private fun shareMashup(first: String, second: String, style: EmojiMashupStyle) {
        val outFile = EmojiKitchenEngine.saveMashupPng(applicationContext, first, second, style)
        val record = EmojiMashupRecord(first, second, style.id, "$first + $second")
        prefsRepo.recordMashupUse(record)
        shareMediaFile(outFile, "image/png", "$first + $second Sticker")
    }

    private fun commitFileViaContentConnection(
        file: File,
        mimeType: String,
        description: String,
        linkUrl: String?
    ): Boolean {
        val ic = activeInputConnection() ?: return false
        val editorInfo = activeEditorInfo() ?: return false
        val authority = "${packageName}.fileprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(this, authority, file)
        }.getOrNull() ?: return false

        val clipDescription = ClipDescription(description, arrayOf(mimeType))
        val parsedLink = linkUrl?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val inputContentInfo = InputContentInfoCompat(contentUri, clipDescription, parsedLink)
        var flags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        } else {
            editorInfo.packageName?.takeIf { it.isNotBlank() }?.let { targetPkg ->
                runCatching { grantUriPermission(targetPkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
        }
        return runCatching {
            InputConnectionCompat.commitContent(ic, editorInfo, inputContentInfo, flags, null)
        }.getOrDefault(false)
    }

    private fun shareMediaFile(file: File, mimeType: String, title: String) {
        val authority = "${packageName}.fileprovider"
        val contentUri = runCatching { FileProvider.getUriForFile(this, authority, file) }.getOrNull() ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(chooser) }
    }

    internal fun handleKeyPress(key: String) {
        val ic = activeInputConnection() ?: return

        // If user has an active selection and types a printable key outside composition, track replaced selection cleanly
        val selectedBefore = if (activeComposition.isEmpty()) getCurrentSelectedText(ic)?.toString().orEmpty() else ""

        // If Shift Lock / Caps Lock is ON in non-English mode (e.g. Bangla Phonetic), commit uppercase English/acronyms directly
        if (isCapsLock && currentMode != InputMode.ENGLISH) {
            if (activeComposition.isNotEmpty()) {
                val transliterated = phoneticEngine.transliterate(activeComposition.toString())
                val raw = activeComposition.toString()
                smartMemory.recordCorrection(raw, transliterated)
                smartMemory.recordWordUsage(transliterated)
                ic.commitText(transliterated, 1)
                lastCommittedWord = transliterated
                recordOperation(insertedText = transliterated, replacedText = "")
                resetComposition()
            }
            val upperKey = key.uppercase()
            ic.commitText(upperKey, 1)
            recordOperation(insertedText = upperKey, replacedText = selectedBefore)
            hasSelectionState = false
            isSelectionMode = false
            selectionAnchor = -1
            lastCommittedWord = (lastCommittedWord + upperKey).takeLast(10)
            if (prefsRepo.settings.value.showSuggestions) {
                candidates = if (lastCommittedWord.equals("OK", ignoreCase = true)) {
                    listOf("OK", "ওকে", "ঠিক আছে")
                } else {
                    listOf(lastCommittedWord)
                }
            }
            return
        }

        if (currentMode == InputMode.BANGLA_PHONETIC && !isPasswordField) {
            // Check if key is a letter/char that participates in phonetic transliteration
            if (key.length == 1 && (key[0] in 'a'..'z' || key[0] in 'A'..'Z' || key == "^" || key == "`" || key == "'" || key == "," || key == ":" || key == "|")) {
                activeComposition.append(key)
                val transliterated = phoneticEngine.transliterate(activeComposition.toString())
                ic.setComposingText(transliterated, 1)
                hasSelectionState = false
                isSelectionMode = false
                selectionAnchor = -1

                if (prefsRepo.settings.value.showSuggestions) {
                    candidates = suggestionEngine.getSuggestions(
                        activeComposition.toString(),
                        currentMode,
                        lastCommittedWord = lastCommittedWord
                    )
                }

                // If single shift was applied, automatically revert back to unshifted for the next key
                if (isShifted && !isCapsLock) {
                    isShifted = false
                }
                return
            } else {
                // Non-phonetic key, commit any active composition first
                if (activeComposition.isNotEmpty()) {
                    val transliterated = phoneticEngine.transliterate(activeComposition.toString())
                    val raw = activeComposition.toString()
                    smartMemory.recordCorrection(raw, transliterated)
                    if (lastCommittedWord.isNotEmpty()) {
                        smartMemory.recordNextWord(lastCommittedWord, transliterated)
                    }
                    smartMemory.recordWordUsage(transliterated)

                    ic.commitText(transliterated, 1)
                    lastCommittedWord = transliterated
                    recordOperation(insertedText = transliterated, replacedText = "")
                    resetComposition()
                }
                ic.commitText(key, 1)
                recordOperation(insertedText = key, replacedText = selectedBefore)
            }
        } else if (currentMode == InputMode.ENGLISH && !isPasswordField) {
            val effectiveKey = if (isCapsLock) key.uppercase() else key
            if (effectiveKey.length == 1 && (effectiveKey[0] in 'a'..'z' || effectiveKey[0] in 'A'..'Z' || effectiveKey == "'")) {
                activeComposition.append(effectiveKey)
                ic.setComposingText(activeComposition.toString(), 1)
                hasSelectionState = false
                isSelectionMode = false
                selectionAnchor = -1

                if (prefsRepo.settings.value.showSuggestions) {
                    candidates = suggestionEngine.getSuggestions(
                        activeComposition.toString(),
                        InputMode.ENGLISH,
                        lastCommittedWord = lastCommittedWord
                    )
                }

                if (isShifted && !isCapsLock) {
                    isShifted = false
                }
                return
            } else {
                // Punctuation, digit, or emoji typed in English mode
                if (activeComposition.isNotEmpty()) {
                    val word = activeComposition.toString()
                    smartMemory.recordWordUsage(word)
                    if (lastCommittedWord.isNotBlank()) {
                        smartMemory.recordNextWord(lastCommittedWord, word)
                    }
                    ic.commitText(word, 1)
                    lastCommittedWord = word
                    recordOperation(insertedText = word, replacedText = "")
                    resetComposition()
                }
                ic.commitText(effectiveKey, 1)
                recordOperation(insertedText = effectiveKey, replacedText = selectedBefore)
                if (effectiveKey == " ") {
                    checkShortcutExpansion()
                    learnSurroundingWords(ic)
                    updateAutoCapitalization()
                }
            }
        } else {
            // Direct insertion (Bangla Fixed, Symbols, Emoji, or Password fields)
            resetComposition()
            val effectiveKey = if (isCapsLock && currentMode == InputMode.ENGLISH) key.uppercase() else key
            ic.commitText(effectiveKey, 1)
            recordOperation(insertedText = effectiveKey, replacedText = selectedBefore)

            // Check shortcuts expansion on space
            if (!isPasswordField && effectiveKey == " ") {
                checkShortcutExpansion()
                learnSurroundingWords(ic)
            }
        }

        hasSelectionState = false
        isSelectionMode = false
        selectionAnchor = -1

        if (isShifted && !isCapsLock) {
            isShifted = false
        }
    }

    internal fun handleActionKey(code: Int) {
        val ic = activeInputConnection() ?: return

        when (code) {
            KeyboardLayouts.CODE_SHIFT -> {
                val now = System.currentTimeMillis()
                if (isCapsLock) {
                    isCapsLock = false
                    isShifted = false
                } else if (isShifted) {
                    if (now - lastShiftPressTime < 380) {
                        isCapsLock = true
                        isShifted = true
                    } else {
                        isShifted = false
                        isCapsLock = false
                    }
                } else {
                    isShifted = true
                    isCapsLock = false
                }
                lastShiftPressTime = now
            }

            KeyboardLayouts.CODE_DELETE -> {
                val selectedText = getCurrentSelectedText(ic)
                val hasActiveSelection = hasSelectionState || !selectedText.isNullOrEmpty()

                if (hasActiveSelection) {
                    val removedStr = selectedText?.toString().orEmpty()
                    val hadComposition = activeComposition.isNotEmpty()
                    clearLocalComposition()
                    ic.beginBatchEdit()
                    if (hadComposition) {
                        ic.finishComposingText()
                    }
                    val committed = ic.commitText("", 1)
                    ic.endBatchEdit()
                    if (!committed) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                    }
                    if (removedStr.isNotEmpty()) {
                        recordOperation(insertedText = "", replacedText = removedStr)
                    }
                    if (currentSelStart >= 0 && currentSelEnd >= 0) {
                        val collapsed = minOf(currentSelStart, currentSelEnd)
                        currentSelStart = collapsed
                        currentSelEnd = collapsed
                    }
                    hasSelectionState = false
                    isSelectionMode = false
                    selectionAnchor = -1
                    refreshSelectionState(ic)
                    updateAutoCapitalization()
                    return
                }

                if (activeComposition.isNotEmpty()) {
                    activeComposition.deleteCharAt(activeComposition.length - 1)
                    if (activeComposition.isEmpty()) {
                        ic.finishComposingText()
                        candidates = if (prefsRepo.settings.value.showSuggestions && !isPasswordField) {
                            suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = lastCommittedWord)
                        } else {
                            emptyList()
                        }
                        updateAutoCapitalization()
                    } else {
                        val composingDisplay = if (currentMode == InputMode.BANGLA_PHONETIC) {
                            phoneticEngine.transliterate(activeComposition.toString())
                        } else {
                            activeComposition.toString()
                        }
                        ic.setComposingText(composingDisplay, 1)
                        if (prefsRepo.settings.value.showSuggestions) {
                            candidates = suggestionEngine.getSuggestions(
                                activeComposition.toString(),
                                currentMode,
                                lastCommittedWord = lastCommittedWord
                            )
                        }
                    }
                } else {
                    val before = ic.getTextBeforeCursor(24, 0)
                    val len = BanglaUnicodeHelper.calculateBackspaceLength(before ?: "")
                    if (len > 0) {
                        val deleted = before?.takeLast(len)?.toString().orEmpty()
                        ic.deleteSurroundingText(len, 0)
                        if (deleted.isNotEmpty()) {
                            recordOperation(insertedText = "", replacedText = deleted)
                        }
                    } else {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                    }
                    refreshSelectionState(ic)
                    updateAutoCapitalization()
                }
            }

            KeyboardLayouts.CODE_SPACE -> {
                val now = System.currentTimeMillis()
                val selectedBefore = if (activeComposition.isEmpty()) getCurrentSelectedText(ic)?.toString().orEmpty() else ""

                // Smart double-space for period/Dari
                if (prefsRepo.settings.value.smartDoubleSpacePeriod && activeComposition.isEmpty() && selectedBefore.isEmpty() && now - lastSpaceTimestamp < 450) {
                    val punct = if (currentMode.isBangla) "। " else ". "
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(punct, 1)
                    recordOperation(insertedText = punct, replacedText = " ")
                    lastSpaceTimestamp = 0
                    updateAutoCapitalization()
                    return
                }
                lastSpaceTimestamp = now

                var committedForUndo: Pair<String, String>? = null
                if (activeComposition.isNotEmpty()) {
                    val raw = activeComposition.toString()
                    val wordToCommit = when {
                        currentMode == InputMode.BANGLA_PHONETIC ->
                            candidates.firstOrNull() ?: phoneticEngine.transliterate(raw)
                        currentMode == InputMode.ENGLISH && prefsRepo.settings.value.autoCorrect && candidates.size > 1 ->
                            candidates[1]
                        else -> raw
                    }
                    if (raw.isNotBlank()) {
                        smartMemory.recordCorrection(raw, wordToCommit)
                    }
                    if (lastCommittedWord.isNotBlank()) {
                        smartMemory.recordNextWord(lastCommittedWord, wordToCommit)
                    }
                    smartMemory.recordWordUsage(wordToCommit)

                    ic.commitText(wordToCommit, 1)
                    if (wordToCommit != raw) {
                        val rawDisplay = if (currentMode == InputMode.BANGLA_PHONETIC) {
                            phoneticEngine.transliterate(raw)
                        } else {
                            raw
                        }
                        if (rawDisplay != wordToCommit) {
                            committedForUndo = "$wordToCommit " to "$rawDisplay "
                        }
                    }
                    lastCommittedWord = wordToCommit
                    resetComposition()
                }

                ic.commitText(" ", 1)
                val expanded = checkShortcutExpansion()
                if (!expanded) {
                    if (committedForUndo != null) {
                        recordOperation(insertedText = committedForUndo.first, replacedText = committedForUndo.second)
                    } else {
                        recordOperation(insertedText = " ", replacedText = selectedBefore)
                    }
                }
                hasSelectionState = false
                isSelectionMode = false
                selectionAnchor = -1
                learnSurroundingWords(ic)
                updateAutoCapitalization()

                if (prefsRepo.settings.value.showSuggestions && !isPasswordField && lastCommittedWord.isNotEmpty()) {
                    candidates = suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = lastCommittedWord)
                }
            }

            KeyboardLayouts.CODE_ENTER -> {
                val selectedBefore = if (activeComposition.isEmpty()) getCurrentSelectedText(ic)?.toString().orEmpty() else ""
                if (activeComposition.isNotEmpty()) {
                    val wordToCommit = if (currentMode == InputMode.BANGLA_PHONETIC) {
                        candidates.firstOrNull() ?: phoneticEngine.transliterate(activeComposition.toString())
                    } else {
                        activeComposition.toString()
                    }
                    val raw = activeComposition.toString()
                    if (raw.isNotBlank()) {
                        smartMemory.recordCorrection(raw, wordToCommit)
                    }
                    if (lastCommittedWord.isNotBlank()) {
                        smartMemory.recordNextWord(lastCommittedWord, wordToCommit)
                    }
                    smartMemory.recordWordUsage(wordToCommit)

                    ic.commitText(wordToCommit, 1)
                    lastCommittedWord = wordToCommit
                    recordOperation(insertedText = wordToCommit, replacedText = "")
                    resetComposition()
                }

                val editorInfo = activeEditorInfo()
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION

                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_GO,
                    EditorInfo.IME_ACTION_SEND,
                    EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_DONE -> {
                        ic.performEditorAction(actionId)
                    }
                    else -> {
                        ic.commitText("\n", 1)
                        recordOperation(insertedText = "\n", replacedText = selectedBefore)
                        hasSelectionState = false
                        isSelectionMode = false
                        selectionAnchor = -1
                        updateAutoCapitalization()
                    }
                }
            }
        }
    }

    private fun commitCandidate(candidate: String) {
        val ic = activeInputConnection() ?: return
        val raw = activeComposition.toString()
        val hadActiveWord = raw.isNotBlank()
        if (hadActiveWord) {
            smartMemory.recordCorrection(raw, candidate)
        }
        if (lastCommittedWord.isNotBlank()) {
            smartMemory.recordNextWord(lastCommittedWord, candidate)
        }
        smartMemory.recordWordUsage(candidate)

        val previousDisplay = when {
            !hadActiveWord -> ""
            currentMode == InputMode.BANGLA_PHONETIC -> phoneticEngine.transliterate(raw)
            else -> raw
        }

        ic.commitText(candidate, 1)
        recordOperation(insertedText = candidate, replacedText = previousDisplay)
        lastCommittedWord = candidate.trim()
        resetComposition()
        if (isShifted && !isCapsLock) {
            isShifted = false
        }

        if (prefsRepo.settings.value.showSuggestions && !isPasswordField && lastCommittedWord.isNotEmpty()) {
            candidates = suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = lastCommittedWord)
        }
    }

    internal fun performUndo() {
        val op = undoStack.removeLastOrNull() ?: return
        val ic = activeInputConnection()
        if (ic == null) {
            syncHistoryFlags()
            return
        }
        clearLocalComposition()

        if (op.insertedText.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(op.insertedText.length, 0)?.toString()
            if (before == op.insertedText) {
                ic.deleteSurroundingText(op.insertedText.length, 0)
            } else {
                syncHistoryFlags()
                return
            }
        }
        if (op.replacedText.isNotEmpty()) {
            ic.commitText(op.replacedText, 1)
        }
        redoStack.addLast(op)
        while (redoStack.size > MAX_HISTORY_STEPS) {
            redoStack.removeFirst()
        }
        syncHistoryFlags()
        refreshSelectionState(ic)
        updateAutoCapitalization()
    }

    internal fun performRedo() {
        val op = redoStack.removeLastOrNull() ?: return
        val ic = activeInputConnection()
        if (ic == null) {
            syncHistoryFlags()
            return
        }
        clearLocalComposition()

        if (op.replacedText.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(op.replacedText.length, 0)?.toString()
            if (before == op.replacedText) {
                ic.deleteSurroundingText(op.replacedText.length, 0)
            } else {
                syncHistoryFlags()
                return
            }
        }
        if (op.insertedText.isNotEmpty()) {
            ic.commitText(op.insertedText, 1)
        }
        undoStack.addLast(op)
        while (undoStack.size > MAX_HISTORY_STEPS) {
            undoStack.removeFirst()
        }
        syncHistoryFlags()
        refreshSelectionState(ic)
        updateAutoCapitalization()
    }

    private fun learnSurroundingWords(ic: InputConnection) {
        try {
            val textBefore = ic.getTextBeforeCursor(60, 0)?.toString() ?: return
            val words = textBefore.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.size >= 2) {
                val prev = words[words.size - 2]
                val current = words[words.size - 1]
                smartMemory.recordNextWord(prev, current)
                smartMemory.recordWordUsage(current)
                lastCommittedWord = current
            } else if (words.size == 1) {
                smartMemory.recordWordUsage(words.last())
                lastCommittedWord = words.last()
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    private fun clearLocalComposition() {
        activeComposition.clear()
        candidates = emptyList()
    }

    private fun resetComposition() {
        clearLocalComposition()
        activeInputConnection()?.finishComposingText()
    }

    internal fun moveCursor(delta: Int) {
        val ic = activeInputConnection() ?: return
        if (delta == 0) return

        if (activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
                lastCommittedWord = committed
                recordOperation(insertedText = committed, replacedText = "")
            }
            resetComposition()
        }

        val extracted = runCatching { ic.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
        val text = extracted?.text
        if (text != null && extracted.selectionStart >= 0 && extracted.selectionEnd >= 0) {
            if (isSelectionMode) {
                if (selectionAnchor < 0) {
                    selectionAnchor = extracted.selectionStart
                }
                val newActiveEnd = BanglaUnicodeHelper.moveCodePointOffset(text, extracted.selectionEnd, delta)
                val boundedAnchor = selectionAnchor.coerceIn(0, text.length)
                ic.setSelection(boundedAnchor, newActiveEnd)
                currentSelStart = boundedAnchor
                currentSelEnd = newActiveEnd
                hasSelectionState = boundedAnchor != newActiveEnd
            } else {
                val basePos = if (extracted.selectionStart != extracted.selectionEnd) {
                    if (delta < 0) minOf(extracted.selectionStart, extracted.selectionEnd)
                    else maxOf(extracted.selectionStart, extracted.selectionEnd)
                } else {
                    extracted.selectionStart
                }
                val target = BanglaUnicodeHelper.moveCodePointOffset(text, basePos, delta)
                ic.setSelection(target, target)
                currentSelStart = target
                currentSelEnd = target
                hasSelectionState = false
                selectionAnchor = -1
            }
        } else {
            val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
            val meta = if (isSelectionMode) KeyEvent.META_SHIFT_ON else 0
            repeat(abs(delta)) {
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
                ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, meta))
            }
            refreshSelectionState(ic)
        }
    }

    internal fun deleteWordBeforeCursor() {
        val ic = activeInputConnection() ?: return
        val selectedText = getCurrentSelectedText(ic)
        if (hasSelectionState || !selectedText.isNullOrEmpty()) {
            handleActionKey(KeyboardLayouts.CODE_DELETE)
            return
        }

        if (activeComposition.isNotEmpty()) {
            val raw = activeComposition.toString()
            val deletedDisplay = if (currentMode == InputMode.BANGLA_PHONETIC) {
                phoneticEngine.transliterate(raw)
            } else {
                raw
            }
            ic.setComposingText("", 1)
            ic.finishComposingText()
            clearLocalComposition()
            if (deletedDisplay.isNotEmpty()) {
                recordOperation(insertedText = "", replacedText = deletedDisplay)
            }
            updateAutoCapitalization()
            return
        }

        val before = ic.getTextBeforeCursor(80, 0) ?: return
        val deleteLen = BanglaUnicodeHelper.calculateWordDeleteLength(before)
        if (deleteLen > 0) {
            val deleted = before.takeLast(deleteLen).toString()
            ic.deleteSurroundingText(deleteLen, 0)
            if (deleted.isNotEmpty()) {
                recordOperation(insertedText = "", replacedText = deleted)
            }
            refreshSelectionState(ic)
            updateAutoCapitalization()
        }
    }

    internal fun insertTextSnippet(snippet: String) {
        val ic = activeInputConnection() ?: return
        if (snippet.isEmpty()) return
        val replaced = getCurrentSelectedText(ic)?.toString().orEmpty()
        if (activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
                lastCommittedWord = committed
            }
            clearLocalComposition()
        }
        ic.commitText(snippet, 1)
        recordOperation(insertedText = snippet, replacedText = replaced)
        lastCommittedWord = snippet.trim().split(Regex("\\s+")).lastOrNull().orEmpty()
        hasSelectionState = false
        isSelectionMode = false
        selectionAnchor = -1
        refreshSelectionState(ic)
        updateAutoCapitalization()
    }

    internal fun commitGlideWord(word: String, alternatives: List<String> = emptyList()) {
        val ic = activeInputConnection() ?: return
        val clean = word.trim()
        if (clean.isEmpty()) return

        val replaced = getCurrentSelectedText(ic)?.toString().orEmpty()
        if (activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
            }
            clearLocalComposition()
        }

        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val prefixSpace = if (before.isNotEmpty() && !before.last().isWhitespace() && replaced.isEmpty()) " " else ""
        val commitStr = "$prefixSpace$clean "
        ic.commitText(commitStr, 1)
        recordOperation(insertedText = commitStr, replacedText = replaced)
        if (lastCommittedWord.isNotBlank()) {
            smartMemory.recordNextWord(lastCommittedWord, clean)
        }
        smartMemory.recordWordUsage(clean)
        lastCommittedWord = clean
        hasSelectionState = false
        isSelectionMode = false
        selectionAnchor = -1

        if (prefsRepo.settings.value.showSuggestions && !isPasswordField) {
            val altList = alternatives.filter { it.isNotBlank() && it != clean }.take(4)
            val nextWords = suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = clean)
            candidates = (altList + nextWords).distinct().take(6)
        }
        updateAutoCapitalization()
    }

    private fun checkShortcutExpansion(): Boolean {
        val ic = activeInputConnection() ?: return false
        val shortcuts = prefsRepo.shortcuts.value
        if (shortcuts.isEmpty()) return false

        val textBefore = ic.getTextBeforeCursor(30, 0)?.toString() ?: return false
        val words = textBefore.trimEnd().split("\\s+".toRegex())
        val lastWord = words.lastOrNull() ?: return false

        val match = shortcuts.find { it.trigger.equals(lastWord, ignoreCase = true) }
        if (match != null) {
            val deleteCount = lastWord.length + 1
            val expandedText = match.expansion + " "
            ic.deleteSurroundingText(deleteCount, 0)
            ic.commitText(expandedText, 1)
            recordOperation(insertedText = expandedText, replacedText = "$lastWord ")
            return true
        }
        return false
    }

    companion object {
        private const val MAX_HISTORY_STEPS = 30
    }
}

