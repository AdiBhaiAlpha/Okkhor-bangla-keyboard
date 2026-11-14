package com.example.okkhor.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.SmartWordMemory
import com.example.okkhor.engine.SuggestionEngine
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.okkhor.ui.keyboard.KeyboardLayouts

class OkkhorInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var prefsRepo: KeyboardPreferencesRepository
    private lateinit var smartMemory: SmartWordMemory
    private val phoneticEngine = BanglaPhoneticEngine()
    private lateinit var suggestionEngine: SuggestionEngine

    private data class UndoRecord(
        val textToRemove: String,
        val textToRestore: String
    )

    // IME State
    private var currentMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
    private var lastTextMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
    private var inputSessionKey by mutableStateOf(0)
    private var isShifted by mutableStateOf(false)
    private var isCapsLock by mutableStateOf(false)
    private var lastShiftPressTime = 0L
    private var candidates by mutableStateOf<List<String>>(emptyList())
    private var activeComposition = StringBuilder()
    private var isPasswordField = false
    private var lastCommittedWord = ""
    private var lastSpaceTimestamp = 0L
    private var undoRecord by mutableStateOf<UndoRecord?>(null)

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private lateinit var voiceController: VoiceInputController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        prefsRepo = KeyboardPreferencesRepository.getInstance(applicationContext)
        smartMemory = SmartWordMemory.getInstance(applicationContext)
        suggestionEngine = SuggestionEngine(phoneticEngine, smartMemory)
        lastTextMode = prefsRepo.getLastTextMode()
        currentMode = lastTextMode
        prefsRepo.pruneClipboardHistory()

        voiceController = VoiceInputController(applicationContext) { spokenText ->
            val ic = currentInputConnection ?: return@VoiceInputController
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
            undoRecord = UndoRecord(textToRemove = commitText, textToRestore = "")
        }

        // Automatic Clipboard History Listener
        setupClipboardListener()
    }

    private fun setupClipboardListener() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
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
            } catch (e: Exception) {
                // Ignore background clip access security exceptions on Android 10+
            }
        }
        cm.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onCreateInputView(): View {
        window?.window?.let { win ->
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            win.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            win.decorView.setViewTreeLifecycleOwner(this@OkkhorInputMethodService)
            win.decorView.setViewTreeViewModelStoreOwner(this@OkkhorInputMethodService)
            win.decorView.setViewTreeSavedStateRegistryOwner(this@OkkhorInputMethodService)
        }

        val composeView = ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@OkkhorInputMethodService))
            setViewTreeLifecycleOwner(this@OkkhorInputMethodService)
            setViewTreeViewModelStoreOwner(this@OkkhorInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@OkkhorInputMethodService)

            setContent {
                val settings by prefsRepo.settings.collectAsState()
                val theme = prefsRepo.getActiveTheme()
                val clipboardList by prefsRepo.clipboardHistory.collectAsState()
                val voiceState by voiceController.state.collectAsState()

                val editorInfo = currentInputEditorInfo
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
                    canUndo = !isPasswordField && settings.showUndoButton && undoRecord != null,
                    voiceState = if (isPasswordField) VoiceInputState.Idle else voiceState,
                    enterActionLabel = enterLabel,
                    clipboardItems = clipboardList,
                    onKeyPress = { key -> handleKeyPress(key) },
                    onActionKey = { code -> handleActionKey(code) },
                    onCandidateClick = { candidate -> commitCandidate(candidate) },
                    onUndoClick = { performUndo() },
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
                        // Check if system clipboard has new text to ingest
                        checkSystemClipboard()
                    },
                    onTogglePinClipboard = { id -> prefsRepo.togglePinClipboardItem(id) },
                    onRemoveClipboardItem = { id -> prefsRepo.removeClipboardItem(id) },
                    onClearClipboard = { prefsRepo.clearClipboard() },
                    onUpdateOneHandedMode = { mode ->
                        prefsRepo.updateSettings { it.copy(oneHandedMode = mode) }
                    },
                    onUpdateHeightPercent = { height ->
                        prefsRepo.updateSettings { it.copy(keyboardHeightPercent = height) }
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
                    }
                )
            }
        }
        return composeView
    }

    private fun handleTextEditAction(action: String) {
        val ic = currentInputConnection ?: return
        when (action) {
            "SELECT_ALL" -> ic.performContextMenuAction(android.R.id.selectAll)
            "CUT" -> ic.performContextMenuAction(android.R.id.cut)
            "COPY" -> ic.performContextMenuAction(android.R.id.copy)
            "PASTE" -> ic.performContextMenuAction(android.R.id.paste)
            "CURSOR_UP" -> {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_UP))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_UP))
            }
            "CURSOR_DOWN" -> {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_DOWN))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN))
            }
        }
    }

    private fun checkSystemClipboard() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) {
                    prefsRepo.addClipboardItem(text)
                }
            }
        } catch (e: Exception) {
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

        isPasswordField = info?.let { checkIsPasswordField(it) } ?: false
        lastTextMode = prefsRepo.getLastTextMode()
        currentMode = resolveModeForEditor(info, lastTextMode)
        isCapsLock = false
        isShifted = false
        undoRecord = null
        inputSessionKey++

        prefsRepo.pruneClipboardHistory()
        checkSystemClipboard()
        resetComposition()
        updateAutoCapitalization()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        voiceController.stopListening()
        resetComposition()
        undoRecord = null
        isShifted = false
        isCapsLock = false
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
        if (activeComposition.isNotEmpty() && (candidatesStart < 0 || newSelStart != candidatesEnd)) {
            resetComposition()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.destroy()
        clipboardListener?.let {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.removePrimaryClipChangedListener(it)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
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

    private fun switchInputMode(newMode: InputMode) {
        val ic = currentInputConnection
        if (ic != null && activeComposition.isNotEmpty()) {
            val committed = getActiveComposingCommitText()
            if (committed.isNotEmpty()) {
                ic.commitText(committed, 1)
                lastCommittedWord = committed
            }
        }
        resetComposition()
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
        val info = currentInputEditorInfo
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
        val ic = currentInputConnection ?: return
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

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return
        undoRecord = null

        // If Shift Lock / Caps Lock is ON in non-English mode (e.g. Bangla Phonetic), commit uppercase English/acronyms directly
        if (isCapsLock && currentMode != InputMode.ENGLISH) {
            if (activeComposition.isNotEmpty()) {
                val transliterated = phoneticEngine.transliterate(activeComposition.toString())
                val raw = activeComposition.toString()
                smartMemory.recordCorrection(raw, transliterated)
                smartMemory.recordWordUsage(transliterated)
                ic.commitText(transliterated, 1)
                lastCommittedWord = transliterated
                resetComposition()
            }
            val upperKey = key.uppercase()
            ic.commitText(upperKey, 1)
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
                    resetComposition()
                }
                ic.commitText(key, 1)
            }
        } else if (currentMode == InputMode.ENGLISH && !isPasswordField) {
            val effectiveKey = if (isCapsLock) key.uppercase() else key
            if (effectiveKey.length == 1 && (effectiveKey[0] in 'a'..'z' || effectiveKey[0] in 'A'..'Z' || effectiveKey == "'")) {
                activeComposition.append(effectiveKey)
                ic.setComposingText(activeComposition.toString(), 1)

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
                    resetComposition()
                }
                ic.commitText(effectiveKey, 1)
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

            // Check shortcuts expansion on space
            if (!isPasswordField && effectiveKey == " ") {
                checkShortcutExpansion()
                learnSurroundingWords(ic)
            }
        }

        if (isShifted && !isCapsLock) {
            isShifted = false
        }
    }

    private fun handleActionKey(code: Int) {
        val ic = currentInputConnection ?: return

        when (code) {
            KeyboardLayouts.CODE_SHIFT -> {
                val now = System.currentTimeMillis()
                if (isCapsLock) {
                    // Unlock CapsLock back to lowercase
                    isCapsLock = false
                    isShifted = false
                } else if (isShifted) {
                    // Already in single-shift mode.
                    // If user clicks again within 380ms, activate CapsLock!
                    if (now - lastShiftPressTime < 380) {
                        isCapsLock = true
                        isShifted = true
                    } else {
                        // Normal toggle off
                        isShifted = false
                        isCapsLock = false
                    }
                } else {
                    // Single click shifts the next character only
                    isShifted = true
                    isCapsLock = false
                }
                lastShiftPressTime = now
            }

            KeyboardLayouts.CODE_DELETE -> {
                if (activeComposition.isNotEmpty()) {
                    undoRecord = null
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
                    val before = ic.getTextBeforeCursor(4, 0)
                    val len = BanglaUnicodeHelper.calculateBackspaceLength(before ?: "")
                    if (len > 0) {
                        val deleted = before?.takeLast(len)?.toString().orEmpty()
                        ic.deleteSurroundingText(len, 0)
                        if (deleted.isNotEmpty()) {
                            undoRecord = UndoRecord(textToRemove = "", textToRestore = deleted)
                        }
                    } else {
                        sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
                    }
                    updateAutoCapitalization()
                }
            }

            KeyboardLayouts.CODE_SPACE -> {
                val now = System.currentTimeMillis()

                // Smart double-space for period/Dari
                if (prefsRepo.settings.value.smartDoubleSpacePeriod && activeComposition.isEmpty() && now - lastSpaceTimestamp < 450) {
                    val punct = if (currentMode.isBangla) "। " else ". "
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(punct, 1)
                    undoRecord = UndoRecord(textToRemove = punct, textToRestore = " ")
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
                    undoRecord = committedForUndo?.let { (removed, restored) ->
                        UndoRecord(textToRemove = removed, textToRestore = restored)
                    }
                }
                learnSurroundingWords(ic)
                updateAutoCapitalization()

                // Update candidates with smart next-word suggestions
                if (prefsRepo.settings.value.showSuggestions && !isPasswordField && lastCommittedWord.isNotEmpty()) {
                    candidates = suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = lastCommittedWord)
                }
            }

            KeyboardLayouts.CODE_ENTER -> {
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
                    resetComposition()
                }

                val editorInfo = currentInputEditorInfo
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
                        updateAutoCapitalization()
                    }
                }
            }
        }
    }

    private fun commitCandidate(candidate: String) {
        val ic = currentInputConnection ?: return
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
        undoRecord = UndoRecord(textToRemove = candidate, textToRestore = previousDisplay)
        lastCommittedWord = candidate.trim()
        resetComposition()
        if (isShifted && !isCapsLock) {
            isShifted = false
        }

        // Provide next-word suggestions right after committing a word
        if (prefsRepo.settings.value.showSuggestions && !isPasswordField && lastCommittedWord.isNotEmpty()) {
            candidates = suggestionEngine.getSuggestions("", currentMode, lastCommittedWord = lastCommittedWord)
        }
    }

    private fun performUndo() {
        val record = undoRecord ?: return
        val ic = currentInputConnection ?: return
        undoRecord = null
        resetComposition()

        if (record.textToRemove.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(record.textToRemove.length, 0)?.toString()
            if (before == record.textToRemove) {
                ic.deleteSurroundingText(record.textToRemove.length, 0)
            } else {
                return
            }
        }
        if (record.textToRestore.isNotEmpty()) {
            ic.commitText(record.textToRestore, 1)
        }
        updateAutoCapitalization()
    }

    private fun learnSurroundingWords(ic: android.view.inputmethod.InputConnection) {
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
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun resetComposition() {
        activeComposition.clear()
        candidates = emptyList()
        currentInputConnection?.finishComposingText()
    }

    private fun moveCursor(delta: Int) {
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val pos = extracted?.selectionStart ?: return
        val target = (pos + delta).coerceAtLeast(0)
        ic.setSelection(target, target)
    }

    private fun checkShortcutExpansion(): Boolean {
        val ic = currentInputConnection ?: return false
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
            undoRecord = UndoRecord(
                textToRemove = expandedText,
                textToRestore = "$lastWord "
            )
            return true
        }
        return false
    }
}
