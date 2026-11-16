package com.example.okkhor.ui.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.okkhor.benchmark.TypingPerformanceTracker
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.ClipboardRetentionOption
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.model.OneHandedMode
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import com.example.okkhor.emoji.EmojiMashupRecord
import com.example.okkhor.emoji.EmojiMashupStyle
import com.example.okkhor.emoji.GifItem
import com.example.okkhor.emoji.GiphyRepository
import com.example.okkhor.emoji.UnicodeEmojiRepository
import com.example.okkhor.engine.BanglaConjunctHelper
import com.example.okkhor.engine.ClipboardSmartHint
import com.example.okkhor.engine.GestureTypingEngine
import com.example.okkhor.ime.VoiceInputState
import com.example.okkhor.ui.keyboard.gesture.FloatingDockType
import com.example.okkhor.ui.keyboard.gesture.GestureMode
import com.example.okkhor.ui.keyboard.gesture.GesturePriority
import com.example.okkhor.ui.keyboard.gesture.HapticEventType
import com.example.okkhor.ui.keyboard.gesture.HapticFeedbackController
import com.example.okkhor.ui.keyboard.gesture.KeyboardGestureManager
import com.example.ui.theme.HindSiliguri
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Okkhor Bangla Keyboard Container
 *
 * Implements:
 * 1. Immediate-response backspace with rapid auto-repeat on hold
 * 2. Smart CapsLock: single-tap capitalizes next letter, double-tap locks Caps Lock
 * 3. Compact One-Handed mode (74% keyboard with enclosed candidate bar + 26% utility side rail)
 * 4. Rounded pill candidate bar & keys
 * 5. 4x3 Quick Tools Grid Drawer
 * 6. Text Editing Controller with D-Pad & clipboard actions
 * 7. In-keyboard clipboard history panel
 * 8. Direct interactive keyboard height & width resizing
 * 9. Keypress sound & haptics feedback
 * 10. Long-press secondary symbols & numbers
 */
@Composable
fun KeyboardContainer(
    settings: KeyboardSettings,
    theme: KeyboardTheme,
    currentMode: InputMode,
    isShifted: Boolean,
    isCapsLock: Boolean = false,
    candidates: List<String>,
    onKeyPress: (String) -> Unit,
    onActionKey: (Int) -> Unit,
    onCandidateClick: (String) -> Unit,
    onModeSwitch: (InputMode) -> Unit,
    onCursorMove: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenClipboard: () -> Unit,
    modifier: Modifier = Modifier,
    lastTextMode: InputMode = if (currentMode.isTextMode) currentMode else settings.defaultInputMode,
    inputSessionKey: Int = 0,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    hasSelection: Boolean = false,
    canPaste: Boolean = false,
    isSelectionMode: Boolean = false,
    voiceState: VoiceInputState = VoiceInputState.Idle,
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {},
    onToggleSelectionMode: () -> Unit = {},
    onToggleVoice: () -> Unit = {},
    onStopVoice: () -> Unit = {},
    onDismissVoice: () -> Unit = {},
    onGrantMicPermission: () -> Unit = {},
    enterActionLabel: String = "Enter",
    clipboardItems: List<ClipboardItem> = emptyList(),
    clipboardSmartHint: ClipboardSmartHint? = null,
    emojiRepository: UnicodeEmojiRepository? = null,
    giphyRepository: GiphyRepository? = null,
    recentEmojis: List<String> = emptyList(),
    emojiFrequency: Map<String, Int> = emptyMap(),
    favoriteEmojis: List<String> = emptyList(),
    emojiSearchHistory: List<String> = emptyList(),
    preferredEmojiVariants: Map<String, String> = emptyMap(),
    recentGifs: List<GifItem> = emptyList(),
    favoriteGifs: List<GifItem> = emptyList(),
    gifSearchHistory: List<String> = emptyList(),
    recentMashups: List<EmojiMashupRecord> = emptyList(),
    favoriteMashups: List<EmojiMashupRecord> = emptyList(),
    supportsGifCommit: Boolean = false,
    supportsImageCommit: Boolean = false,
    mediaInsertNotice: String? = null,
    onDismissMediaNotice: () -> Unit = {},
    onEmojiSelected: (baseUnicode: String, insertedUnicode: String) -> Unit = { _, inserted -> onKeyPress(inserted) },
    onSelectEmojiVariant: (baseUnicode: String, variantUnicode: String) -> Unit = { _, variant -> onKeyPress(variant) },
    onToggleFavoriteEmoji: (String) -> Unit = {},
    onRecordEmojiSearch: (String) -> Unit = {},
    onClearEmojiSearchHistory: () -> Unit = {},
    onInsertGif: (GifItem) -> Unit = {},
    onShareGif: (GifItem) -> Unit = {},
    onCopyGifUrl: (GifItem) -> Unit = {},
    onToggleFavoriteGif: (GifItem) -> Unit = {},
    onRecordGifSearch: (String) -> Unit = {},
    onClearGifSearchHistory: () -> Unit = {},
    onInsertMashup: (first: String, second: String, style: EmojiMashupStyle) -> Unit = { _, _, _ -> },
    onShareMashup: (first: String, second: String, style: EmojiMashupStyle) -> Unit = { _, _, _ -> },
    onToggleFavoriteMashup: (EmojiMashupRecord) -> Unit = {},
    onTogglePinClipboard: (String) -> Unit = {},
    onRemoveClipboardItem: (String) -> Unit = {},
    onClearClipboard: () -> Unit = {},
    onUpdateOneHandedMode: (OneHandedMode) -> Unit = {},
    onUpdateHeightPercent: (Int) -> Unit = {},
    onUpdateDimensions: (Int, Int) -> Unit = { height, _ -> onUpdateHeightPercent(height) },
    onCycleTheme: () -> Unit = {},
    onUpdateSettings: ((KeyboardSettings) -> KeyboardSettings) -> Unit = {},
    onTextEditAction: (String) -> Unit = {},
    onDeleteWordSwipe: () -> Unit = { onActionKey(KeyboardLayouts.CODE_DELETE) },
    onInsertTextSnippet: (String) -> Unit = { onKeyPress(it) },
    onCommitGlideWord: (String, List<String>) -> Unit = { word, _ -> onCandidateClick(word) }
) {
    val view = LocalView.current
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val hapticController = remember(context) { HapticFeedbackController(context) }
    val gestureManager = remember { KeyboardGestureManager() }
    val resolvedEmojiRepository = emojiRepository ?: remember(context) { UnicodeEmojiRepository.getInstance(context) }
    val resolvedGiphyRepository = giphyRepository ?: remember(context) { GiphyRepository(context) }

    val isOneHanded = settings.oneHandedMode != OneHandedMode.OFF
    val isFloating = settings.floatingKeyboardMode && !isOneHanded
    val scaleMultiplier = if (isOneHanded) 0.90f else 1.0f
    val elevation = theme.keyElevationDp.dp
    val spacing = if (isOneHanded) 2.5.dp else settings.keySpacingDpOverride.coerceIn(2.0f, 8.0f).dp
    var pendingRenderStartNs by remember { mutableLongStateOf(0L) }
    SideEffect {
        if (pendingRenderStartNs > 0L) {
            val frameMs = (System.nanoTime() - pendingRenderStartNs) / 1_000_000.0
            pendingRenderStartNs = 0L
            TypingPerformanceTracker.recordFrameDurationMs(frameMs)
        }
    }

    // Sub-panel states
    var showToolsDrawer by remember { mutableStateOf(false) }
    var showClipboardPanel by remember { mutableStateOf(false) }
    var showTextEditingPanel by remember { mutableStateOf(false) }
    var showResizeBar by remember { mutableStateOf(false) }

    // Interactive Direct Keyboard Resizing Mode state
    var isResizeMode by remember { mutableStateOf(false) }
    var isDraggingResize by remember { mutableStateOf(false) }
    var draftHeightPercent by remember {
        mutableFloatStateOf(settings.keyboardHeightPercent.coerceIn(75, 140).toFloat())
    }
    var draftWidthPercent by remember {
        mutableFloatStateOf(settings.keyboardWidthPercent.coerceIn(75, 100).toFloat())
    }
    var draftFloatX by remember { mutableFloatStateOf(settings.floatingOffsetX) }
    var draftFloatY by remember { mutableFloatStateOf(settings.floatingOffsetY) }
    var isDraggingFloat by remember { mutableStateOf(false) }

    // Dock & Gesture tracking bounds
    var dockViewportBounds by remember { mutableStateOf<Rect?>(null) }
    val dockEmojiBounds = remember { mutableStateMapOf<String, Rect>() }
    var symbolViewportBounds by remember { mutableStateOf<Rect?>(null) }
    val symbolDockBounds = remember { mutableStateMapOf<String, Rect>() }
    var shiftDragHighlightKey by remember { mutableStateOf<String?>(null) }

    val isEmojiDockOpen = gestureManager.activeDock == FloatingDockType.EMOJI_QUICK_DOCK
    val isLanguageSwipeActive = gestureManager.activeDock == FloatingDockType.LANGUAGE_SWITCHER
    val isSymbolDockOpen = gestureManager.activeDock == FloatingDockType.SYMBOL_QUICK_DOCK
    val isClipboardDockOpen = gestureManager.activeDock == FloatingDockType.CLIPBOARD_QUICK_DOCK
    val isAlternateDockOpen = gestureManager.activeDock == FloatingDockType.ALTERNATE_CHARS_DOCK
    val isDateTimeDockOpen = gestureManager.activeDock == FloatingDockType.DATE_TIME_DOCK
    val isTranslationDockOpen = gestureManager.activeDock == FloatingDockType.TRANSLATION_DOCK
    val highlightedDockEmoji = gestureManager.highlightedDockTarget
    val languageSwipeBaseMode = gestureManager.languageBaseMode
    val languageSwipePreviewStep = gestureManager.languagePreviewStep
    val languageSwipeDragProgress = gestureManager.languageDragProgress
    val isSpaceCursorHoldActive = gestureManager.isCursorHoldActive

    val dockRecentEmojis = remember(recentEmojis, emojiFrequency, settings.recentEmojiDockCount) {
        KeyboardPreferencesRepository.rankRecentEmojisForDock(
            recent = recentEmojis,
            frequency = emojiFrequency,
            limit = settings.recentEmojiDockCount
        )
    }

    LaunchedEffect(settings.keyboardHeightPercent, settings.keyboardWidthPercent, isDraggingResize) {
        if (!isDraggingResize) {
            draftHeightPercent = settings.keyboardHeightPercent.coerceIn(75, 140).toFloat()
            draftWidthPercent = settings.keyboardWidthPercent.coerceIn(75, 100).toFloat()
        }
    }
    LaunchedEffect(settings.floatingOffsetX, settings.floatingOffsetY, isDraggingFloat) {
        if (!isDraggingFloat) {
            draftFloatX = settings.floatingOffsetX
            draftFloatY = settings.floatingOffsetY
        }
    }

    // Remember the active typing language (BANGLA_PHONETIC, BANGLA_FIXED, or ENGLISH)
    var rememberedTextMode by remember {
        mutableStateOf(
            when {
                currentMode.isTextMode -> currentMode
                lastTextMode.isTextMode -> lastTextMode
                else -> InputMode.BANGLA_PHONETIC
            }
        )
    }

    LaunchedEffect(currentMode, lastTextMode) {
        if (currentMode.isTextMode) {
            rememberedTextMode = currentMode
        } else if (lastTextMode.isTextMode) {
            rememberedTextMode = lastTextMode
        }
    }

    LaunchedEffect(currentMode, isResizeMode) {
        if (currentMode == InputMode.EMOJI || isResizeMode) {
            gestureManager.cancelAll()
            shiftDragHighlightKey = null
        }
    }

    LaunchedEffect(inputSessionKey) {
        showToolsDrawer = false
        showClipboardPanel = false
        showTextEditingPanel = false
        showResizeBar = false
        isResizeMode = false
        isDraggingResize = false
        dockViewportBounds = null
        dockEmojiBounds.clear()
        symbolViewportBounds = null
        symbolDockBounds.clear()
        shiftDragHighlightKey = null
        gestureManager.cancelAll()
    }

    fun nextLanguageMode(): InputMode {
        val base = if (currentMode.isTextMode) currentMode else rememberedTextMode
        return when (base) {
            InputMode.BANGLA_PHONETIC -> InputMode.BANGLA_FIXED
            InputMode.BANGLA_FIXED -> InputMode.ENGLISH
            InputMode.ENGLISH -> InputMode.BANGLA_PHONETIC
            else -> rememberedTextMode
        }
    }

    fun playFeedback() {
        pendingRenderStartNs = System.nanoTime()
        hapticController.trigger(view, settings, HapticEventType.KEYPRESS)
        if (settings.soundFeedback) {
            if (audioManager != null) {
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.7f)
            } else {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }

    fun playSpecialFeedback() {
        pendingRenderStartNs = System.nanoTime()
        hapticController.trigger(view, settings, HapticEventType.GESTURE_COMPLETE)
        if (settings.soundFeedback) {
            if (audioManager != null) {
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.85f)
            } else {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }

    // Content lambda that renders Candidate Bar + Keys/Panels
    @Composable
    fun KeyboardMainBlock(
        baseKeyHeight: Dp,
        cornerRadius: Dp,
        compactToolbar: Boolean,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            // --- 1. Candidate Suggestion Strip & Quick Toolbar ---
            CandidateToolbar(
                theme = theme,
                candidates = candidates,
                clipboardSmartHint = clipboardSmartHint,
                canUndo = canUndo,
                canRedo = canRedo,
                hasSelection = hasSelection,
                canPaste = canPaste,
                showMicButton = settings.voiceTypingEnabled,
                isVoiceListening = voiceState is VoiceInputState.Listening,
                isResizeMode = isResizeMode,
                isTextEditOpen = showTextEditingPanel,
                compactToolbar = compactToolbar,
                onCandidateClick = { candidate ->
                    playFeedback()
                    onCandidateClick(candidate)
                },
                onInsertSmartHint = { snippet ->
                    playSpecialFeedback()
                    onInsertTextSnippet(snippet)
                },
                onUndoClick = {
                    playSpecialFeedback()
                    onUndoClick()
                },
                onRedoClick = {
                    playSpecialFeedback()
                    onRedoClick()
                },
                onTextEditAction = { action ->
                    playSpecialFeedback()
                    onTextEditAction(action)
                },
                onToggleVoice = {
                    playSpecialFeedback()
                    onToggleVoice()
                },
                onToggleTextEditing = {
                    playFeedback()
                    isResizeMode = false
                    gestureManager.cancelAll()
                    showTextEditingPanel = !showTextEditingPanel
                    showToolsDrawer = false
                    showClipboardPanel = false
                },
                onToggleResizeMode = {
                    playSpecialFeedback()
                    showToolsDrawer = false
                    showClipboardPanel = false
                    showTextEditingPanel = false
                    showResizeBar = false
                    gestureManager.cancelAll()
                    isResizeMode = !isResizeMode
                },
                onOpenTools = {
                    playFeedback()
                    isResizeMode = false
                    gestureManager.cancelAll()
                    showToolsDrawer = !showToolsDrawer
                    showClipboardPanel = false
                    showTextEditingPanel = false
                },
                onOpenClipboard = {
                    playFeedback()
                    isResizeMode = false
                    gestureManager.cancelAll()
                    showClipboardPanel = !showClipboardPanel
                    showToolsDrawer = false
                    showTextEditingPanel = false
                    if (showClipboardPanel) {
                        onOpenClipboard()
                    }
                },
                onOpenClipboardDock = {
                    hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                    isResizeMode = false
                    showToolsDrawer = false
                    showClipboardPanel = false
                    showTextEditingPanel = false
                    onOpenClipboard()
                    gestureManager.openDock(FloatingDockType.CLIPBOARD_QUICK_DOCK)
                },
                onOpenThemes = {
                    playFeedback()
                    onCycleTheme()
                },
                onOpenSettings = {
                    playFeedback()
                    onOpenSettings()
                }
            )

            // --- Minimizable Voice Dictation Bar ---
            AnimatedVisibility(visible = voiceState !is VoiceInputState.Idle) {
                VoiceInputBar(
                    voiceState = voiceState,
                    theme = theme,
                    onStop = {
                        playSpecialFeedback()
                        onStopVoice()
                    },
                    onGrantPermission = {
                        playSpecialFeedback()
                        onGrantMicPermission()
                    },
                    onDismiss = {
                        playFeedback()
                        onDismissVoice()
                    }
                )
            }

            // --- Quick Resize Height Bar ---
            AnimatedVisibility(visible = showResizeBar) {
                QuickResizeBar(
                    theme = theme,
                    currentPercent = settings.keyboardHeightPercent,
                    onSelectPercent = { percent ->
                        playFeedback()
                        onUpdateHeightPercent(percent)
                    },
                    onClose = { showResizeBar = false }
                )
            }

            // --- In-Keyboard Sub-Panels ---
            when {
                showToolsDrawer -> {
                    // 4x3 Quick Tools Grid Drawer
                    QuickToolsGridDrawer(
                        theme = theme,
                        settings = settings,
                        height = (baseKeyHeight * 4.35f),
                        onClose = { showToolsDrawer = false },
                        onOpenClipboard = {
                            showToolsDrawer = false
                            showClipboardPanel = true
                            onOpenClipboard()
                        },
                        onOpenTheme = {
                            onCycleTheme()
                        },
                        onToggleResize = {
                            showToolsDrawer = false
                            showClipboardPanel = false
                            showTextEditingPanel = false
                            showResizeBar = false
                            isResizeMode = true
                        },
                        onToggleOneHanded = {
                            val next = when (settings.oneHandedMode) {
                                OneHandedMode.OFF -> OneHandedMode.RIGHT
                                OneHandedMode.RIGHT -> OneHandedMode.LEFT
                                OneHandedMode.LEFT -> OneHandedMode.OFF
                            }
                            onUpdateOneHandedMode(next)
                        },
                        onOpenTextEditing = {
                            showToolsDrawer = false
                            showTextEditingPanel = true
                        },
                        onSwitchLayout = {
                            onModeSwitch(nextLanguageMode())
                        },
                        onToggleSound = {
                            onUpdateSettings { it.copy(soundFeedback = !it.soundFeedback) }
                        },
                        onToggleNumberRow = {
                            onUpdateSettings { it.copy(showNumberRow = !it.showNumberRow) }
                        },
                        onToggleSymbols = {
                            onUpdateSettings { it.copy(longPressSymbolsEnabled = !it.longPressSymbolsEnabled) }
                        },
                        onToggleSuggestions = {
                            onUpdateSettings { it.copy(showSuggestions = !it.showSuggestions) }
                        },
                        onToggleRoundedKeys = {
                            onUpdateSettings { it.copy(roundedPillKeys = !it.roundedPillKeys) }
                        },
                        onStartVoice = {
                            showToolsDrawer = false
                            onToggleVoice()
                        },
                        onToggleDoubleSpace = {
                            onUpdateSettings { it.copy(smartDoubleSpacePeriod = !it.smartDoubleSpacePeriod) }
                        },
                        onOpenTranslateDock = {
                            hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                            showToolsDrawer = false
                            gestureManager.openDock(FloatingDockType.TRANSLATION_DOCK)
                        },
                        onOpenDateTimeDock = {
                            hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                            showToolsDrawer = false
                            gestureManager.openDock(FloatingDockType.DATE_TIME_DOCK)
                        },
                        onToggleFloating = {
                            playSpecialFeedback()
                            showToolsDrawer = false
                            onUpdateSettings {
                                val nextFloat = !it.floatingKeyboardMode
                                it.copy(
                                    floatingKeyboardMode = nextFloat,
                                    oneHandedMode = if (nextFloat) OneHandedMode.OFF else it.oneHandedMode
                                )
                            }
                        },
                        onToggleGlide = {
                            onUpdateSettings { it.copy(glideTypingEnabled = !it.glideTypingEnabled) }
                        },
                        onOpenSettings = {
                            showToolsDrawer = false
                            onOpenSettings()
                        }
                    )
                }

                showClipboardPanel -> {
                    // In-Keyboard Clipboard History Panel
                    KeyboardClipboardPanel(
                        theme = theme,
                        items = clipboardItems,
                        retentionHours = settings.clipboardRetentionHours,
                        height = (baseKeyHeight * 4.35f),
                        onPasteItem = { text ->
                            playFeedback()
                            onCandidateClick(text)
                            showClipboardPanel = false
                        },
                        onTogglePin = { id ->
                            playFeedback()
                            onTogglePinClipboard(id)
                        },
                        onDeleteItem = { id ->
                            playFeedback()
                            onRemoveClipboardItem(id)
                        },
                        onClearAll = {
                            playFeedback()
                            onClearClipboard()
                        },
                        onClose = {
                            playFeedback()
                            showClipboardPanel = false
                        }
                    )
                }

                showTextEditingPanel -> {
                    // Text Editing Controller Panel with D-Pad, Selection Mode & Contextual Edit Actions
                    TextEditingPanel(
                        theme = theme,
                        height = (baseKeyHeight * 4.35f),
                        canUndo = canUndo,
                        canRedo = canRedo,
                        hasSelection = hasSelection,
                        canPaste = canPaste,
                        isSelectionMode = isSelectionMode,
                        onClose = { showTextEditingPanel = false },
                        onCursorMove = { delta ->
                            playFeedback()
                            onCursorMove(delta)
                        },
                        onActionKey = { code ->
                            playFeedback()
                            onActionKey(code)
                        },
                        onUndoClick = {
                            playSpecialFeedback()
                            onUndoClick()
                        },
                        onRedoClick = {
                            playSpecialFeedback()
                            onRedoClick()
                        },
                        onToggleSelectionMode = {
                            playSpecialFeedback()
                            onToggleSelectionMode()
                        },
                        onTextEditAction = { action ->
                            playSpecialFeedback()
                            onTextEditAction(action)
                        }
                    )
                }

                else -> {
                    val localDensity = LocalDensity.current
                    val dockVertTolPx = with(localDensity) { 12.dp.toPx() }
                    val dockHorizTolPx = with(localDensity) { 6.dp.toPx() }
                    val langCommitThresholdPx = with(localDensity) { 28.dp.toPx() }

                    // Main Matrix Content
                    MainKeyboardContent(
                        settings = settings,
                        theme = theme,
                        currentMode = currentMode,
                        lastTextMode = rememberedTextMode,
                        isShifted = isShifted,
                        isCapsLock = isCapsLock,
                        baseKeyHeight = baseKeyHeight,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        spacing = spacing,
                        enterActionLabel = enterActionLabel,
                        isSpaceCursorHoldActive = isSpaceCursorHoldActive,
                        isOverlayBlockingTouch = gestureManager.isBackdropBlurActive || isResizeMode,
                        playFeedback = { playFeedback() },
                        playSpecialFeedback = { playSpecialFeedback() },
                        emojiRepository = resolvedEmojiRepository,
                        giphyRepository = resolvedGiphyRepository,
                        recentEmojis = recentEmojis,
                        emojiFrequency = emojiFrequency,
                        favoriteEmojis = favoriteEmojis,
                        emojiSearchHistory = emojiSearchHistory,
                        preferredEmojiVariants = preferredEmojiVariants,
                        recentGifs = recentGifs,
                        favoriteGifs = favoriteGifs,
                        gifSearchHistory = gifSearchHistory,
                        recentMashups = recentMashups,
                        favoriteMashups = favoriteMashups,
                        supportsGifCommit = supportsGifCommit,
                        supportsImageCommit = supportsImageCommit,
                        mediaInsertNotice = mediaInsertNotice,
                        onDismissMediaNotice = onDismissMediaNotice,
                        onEmojiSelected = onEmojiSelected,
                        onSelectEmojiVariant = onSelectEmojiVariant,
                        onToggleFavoriteEmoji = onToggleFavoriteEmoji,
                        onRecordEmojiSearch = onRecordEmojiSearch,
                        onClearEmojiSearchHistory = onClearEmojiSearchHistory,
                        onInsertGif = onInsertGif,
                        onShareGif = onShareGif,
                        onCopyGifUrl = onCopyGifUrl,
                        onToggleFavoriteGif = onToggleFavoriteGif,
                        onRecordGifSearch = onRecordGifSearch,
                        onClearGifSearchHistory = onClearGifSearchHistory,
                        onInsertMashup = onInsertMashup,
                        onShareMashup = onShareMashup,
                        onToggleFavoriteMashup = onToggleFavoriteMashup,
                        onKeyPress = onKeyPress,
                        onActionKey = onActionKey,
                        onModeSwitch = onModeSwitch,
                        onCursorMove = onCursorMove,
                        onEmojiLongPressStart = {
                            hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                            dockEmojiBounds.clear()
                            gestureManager.beginGesture(GestureMode.EMOJI_DOCK_DRAG, GesturePriority.DOCK_DRAG)
                            gestureManager.openDock(FloatingDockType.EMOJI_QUICK_DOCK)
                        },
                        onEmojiDockDrag = { pointerRoot ->
                            val hit = KeyboardGestureHelper.resolveDockEmojiHit(
                                pointerRoot = pointerRoot,
                                viewportBounds = dockViewportBounds,
                                emojiBounds = dockEmojiBounds,
                                verticalTolerancePx = dockVertTolPx,
                                horizontalTolerancePx = dockHorizTolPx
                            )
                            if (hit != gestureManager.highlightedDockTarget) {
                                gestureManager.updateDockHighlight(hit)
                                if (hit != null) {
                                    hapticController.trigger(view, settings, HapticEventType.LANGUAGE_SWITCH)
                                }
                            }
                        },
                        onEmojiDockRelease = {
                            val chosen = gestureManager.highlightedDockTarget
                            gestureManager.cancelAll()
                            dockEmojiBounds.clear()
                            if (chosen != null) {
                                playFeedback()
                                onEmojiSelected(chosen, chosen)
                            }
                        },
                        onEmojiDockCancel = {
                            gestureManager.cancelAll()
                            dockEmojiBounds.clear()
                        },
                        onLanguageSwipeStart = { netDx ->
                            val base = if (currentMode.isTextMode) currentMode else rememberedTextMode
                            val safeBase = if (base in KeyboardGestureHelper.LANGUAGE_ORDER) {
                                base
                            } else {
                                InputMode.BANGLA_PHONETIC
                            }
                            val step = when {
                                netDx >= langCommitThresholdPx -> 1
                                netDx <= -langCommitThresholdPx -> -1
                                else -> 0
                            }
                            gestureManager.startLanguagePreview(
                                baseMode = safeBase,
                                step = step,
                                progress = netDx / langCommitThresholdPx
                            )
                            hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                        },
                        onLanguageSwipeUpdate = { netDx ->
                            val newStep = when {
                                netDx >= langCommitThresholdPx -> 1
                                netDx <= -langCommitThresholdPx -> -1
                                else -> 0
                            }
                            if (newStep != gestureManager.languagePreviewStep) {
                                hapticController.trigger(view, settings, HapticEventType.LANGUAGE_SWITCH)
                            }
                            gestureManager.updateLanguagePreview(
                                step = newStep,
                                progress = netDx / langCommitThresholdPx
                            )
                        },
                        onLanguageSwipeCommit = {
                            val targetMode = KeyboardGestureHelper.stepLanguageMode(
                                baseMode = gestureManager.languageBaseMode,
                                step = gestureManager.languagePreviewStep
                            )
                            val shouldSwitch = gestureManager.activeDock == FloatingDockType.LANGUAGE_SWITCHER &&
                                gestureManager.languagePreviewStep != 0 &&
                                targetMode != currentMode
                            gestureManager.cancelAll()
                            if (shouldSwitch) {
                                playSpecialFeedback()
                                onModeSwitch(targetMode)
                            }
                        },
                        onLanguageSwipeCancel = {
                            gestureManager.cancelAll()
                        },
                        onSpaceCursorHoldStateChanged = { active ->
                            if (active && !gestureManager.isCursorHoldActive) {
                                hapticController.trigger(view, settings, HapticEventType.LONG_PRESS)
                            }
                            gestureManager.setCursorHold(active)
                        },
                        onSymbolLongPressStart = {
                            hapticController.trigger(view, settings, HapticEventType.DOCK_OPEN)
                            symbolDockBounds.clear()
                            gestureManager.beginGesture(GestureMode.SYMBOL_DOCK_DRAG, GesturePriority.DOCK_DRAG)
                            gestureManager.openDock(FloatingDockType.SYMBOL_QUICK_DOCK)
                        },
                        onSymbolDockDrag = { pointerRoot ->
                            val hit = KeyboardGestureHelper.resolveBoundsHit(
                                pointerRoot = pointerRoot,
                                viewportBounds = symbolViewportBounds,
                                itemBounds = symbolDockBounds,
                                verticalTolerancePx = dockVertTolPx,
                                horizontalTolerancePx = dockHorizTolPx
                            )
                            if (hit != gestureManager.highlightedDockTarget) {
                                gestureManager.updateDockHighlight(hit)
                                if (hit != null) {
                                    hapticController.trigger(view, settings, HapticEventType.LANGUAGE_SWITCH)
                                }
                            }
                        },
                        onSymbolDockRelease = {
                            val chosen = gestureManager.highlightedDockTarget
                            gestureManager.cancelAll()
                            symbolDockBounds.clear()
                            if (chosen != null) {
                                playFeedback()
                                onKeyPress(chosen)
                            }
                        },
                        onSymbolDockCancel = {
                            gestureManager.cancelAll()
                            symbolDockBounds.clear()
                        },
                        onShowAlternates = { key, alternates ->
                            hapticController.trigger(view, settings, HapticEventType.LONG_PRESS)
                            gestureManager.openAlternateCharsDock(key, alternates)
                        },
                        onDeleteWordSwipe = {
                            if (settings.backspaceSwipeDelete) {
                                playSpecialFeedback()
                                onDeleteWordSwipe()
                            } else {
                                playFeedback()
                                onActionKey(KeyboardLayouts.CODE_DELETE)
                            }
                        },
                        onCommitGlideWord = { topWord, alts ->
                            playSpecialFeedback()
                            onCommitGlideWord(topWord, alts)
                        },
                        shiftDragHighlightKey = shiftDragHighlightKey,
                        onShiftDragHighlightChanged = { shiftDragHighlightKey = it }
                    )
                }
            }
        }
    }

    // Outer Layout with Responsive Rounded Top Corners, Transparent Outside, & Direct Resize Mode
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val density = LocalDensity.current
        val fullWidth = maxWidth
        val fullWidthDp = fullWidth.value.coerceAtLeast(1f)
        val effectiveHeightPercent = if (isResizeMode) {
            draftHeightPercent
        } else {
            settings.keyboardHeightPercent.coerceIn(75, 140).toFloat()
        }
        val effectiveWidthPercent = if (isResizeMode) {
            draftWidthPercent
        } else {
            settings.keyboardWidthPercent.coerceIn(75, 100).toFloat()
        }

        val targetKeyHeight = (54f * (effectiveHeightPercent / 100f) * scaleMultiplier).dp
        val animatedKeyHeight by animateDpAsState(
            targetValue = targetKeyHeight,
            animationSpec = if (isDraggingResize) snap() else spring(stiffness = Spring.StiffnessMediumLow),
            label = "keyboard_key_height"
        )
        val activeCornerRadius = if (settings.roundedPillKeys) {
            (animatedKeyHeight * 0.40f).coerceAtLeast(18.dp)
        } else {
            theme.keyCornerRadiusDp.dp
        }

        val targetWidth = if (isFloating) {
            (fullWidth * 0.88f * (effectiveWidthPercent / 100f)).coerceIn(fullWidth * 0.72f, fullWidth)
        } else {
            (fullWidth * (effectiveWidthPercent / 100f)).coerceIn(fullWidth * 0.75f, fullWidth)
        }
        val animatedWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = if (isDraggingResize) snap() else spring(stiffness = Spring.StiffnessMediumLow),
            label = "keyboard_width"
        )

        val compactToolbar = isOneHanded || isFloating || animatedWidth < 330.dp
        val topCornerRadius = (animatedWidth * 0.065f).coerceIn(22.dp, 28.dp)
        val containerShape = RoundedCornerShape(
            topStart = topCornerRadius,
            topEnd = topCornerRadius,
            bottomStart = if (isFloating) 20.dp else 0.dp,
            bottomEnd = if (isFloating) 20.dp else 0.dp
        )
        val oneHandedInnerShape = RoundedCornerShape(
            topStart = (topCornerRadius - 4.dp).coerceAtLeast(18.dp),
            topEnd = (topCornerRadius - 4.dp).coerceAtLeast(18.dp),
            bottomStart = 14.dp,
            bottomEnd = 14.dp
        )
        val outerBorderColor = when {
            isResizeMode -> Color(theme.accentColor).copy(alpha = 0.92f)
            isFloating -> Color(theme.accentColor).copy(alpha = 0.58f)
            else -> Color(theme.keyBorderColor).copy(alpha = 0.45f)
        }
        val outerBorderWidth = if (isResizeMode || isFloating) 1.6.dp else 1.dp

        val containerAlignment = when (settings.oneHandedMode) {
            OneHandedMode.OFF -> Alignment.BottomCenter
            OneHandedMode.LEFT -> Alignment.BottomStart
            OneHandedMode.RIGHT -> Alignment.BottomEnd
        }

        val hasGestureOverlay = gestureManager.isBackdropBlurActive
        val backdropBlur by animateDpAsState(
            targetValue = if (hasGestureOverlay) 5.dp else 0.dp,
            animationSpec = tween(140),
            label = "gesture_backdrop_blur"
        )
        val backdropDimAlpha by animateFloatAsState(
            targetValue = if (hasGestureOverlay) 0.34f else 0f,
            animationSpec = tween(140),
            label = "gesture_backdrop_dim"
        )

        Box(
            modifier = Modifier
                .width(animatedWidth)
                .align(containerAlignment)
                .then(
                    if (isFloating) {
                        Modifier.offset(x = draftFloatX.dp, y = draftFloatY.dp)
                    } else Modifier
                )
                .shadow(if (isFloating) 16.dp else 0.dp, containerShape, clip = false)
                .clip(containerShape)
                .background(Color(theme.backgroundColor), containerShape)
                .border(outerBorderWidth, outerBorderColor, containerShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (backdropBlur > 0.dp) Modifier.blur(backdropBlur) else Modifier)
            ) {
                if (isFloating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color(theme.candidateBarBackgroundColor).copy(alpha = 0.92f))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingFloat = true },
                                    onDragEnd = {
                                        isDraggingFloat = false
                                        onUpdateSettings {
                                            it.copy(
                                                floatingOffsetX = draftFloatX.coerceIn(-120f, 120f),
                                                floatingOffsetY = draftFloatY.coerceIn(-160f, 0f)
                                            )
                                        }
                                    },
                                    onDragCancel = { isDraggingFloat = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = with(density) { dragAmount.x.toDp().value }
                                        val dyDp = with(density) { dragAmount.y.toDp().value }
                                        draftFloatX = (draftFloatX + dxDp).coerceIn(-120f, 120f)
                                        draftFloatY = (draftFloatY + dyDp).coerceIn(-160f, 0f)
                                    }
                                )
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("floating_drag_bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Move floating keyboard",
                                tint = Color(theme.accentColor),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Floating Keyboard",
                                color = Color(theme.candidateBarTextColor).copy(alpha = 0.78f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguri
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playSpecialFeedback()
                                    onUpdateSettings {
                                        it.copy(floatingKeyboardMode = false, floatingOffsetX = 0f, floatingOffsetY = 0f)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("btn_exit_floating_mode"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Dock",
                                color = Color(theme.accentColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dock keyboard",
                                tint = Color(theme.accentColor),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                when (settings.oneHandedMode) {
                    OneHandedMode.OFF -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .testTag("keyboard_container")
                        ) {
                            KeyboardMainBlock(
                                baseKeyHeight = animatedKeyHeight,
                                cornerRadius = activeCornerRadius,
                                compactToolbar = compactToolbar
                            )
                        }
                    }

                    OneHandedMode.LEFT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("keyboard_container_left"),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.74f)
                                    .clip(oneHandedInnerShape)
                                    .background(Color(theme.backgroundColor), oneHandedInnerShape)
                                    .border(1.dp, Color(theme.keyBorderColor).copy(alpha = 0.55f), oneHandedInnerShape)
                            ) {
                                KeyboardMainBlock(
                                    baseKeyHeight = animatedKeyHeight,
                                    cornerRadius = activeCornerRadius,
                                    compactToolbar = true
                                )
                            }

                            OneHandedSideRail(
                                theme = theme,
                                isLeft = true,
                                onExpand = { onUpdateOneHandedMode(OneHandedMode.OFF) },
                                onSwitchSide = { onUpdateOneHandedMode(OneHandedMode.RIGHT) },
                                onResize = {
                                    showResizeBar = false
                                    isResizeMode = !isResizeMode
                                },
                                modifier = Modifier.weight(0.26f)
                            )
                        }
                    }

                    OneHandedMode.RIGHT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("keyboard_container_right"),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OneHandedSideRail(
                                theme = theme,
                                isLeft = false,
                                onExpand = { onUpdateOneHandedMode(OneHandedMode.OFF) },
                                onSwitchSide = { onUpdateOneHandedMode(OneHandedMode.LEFT) },
                                onResize = {
                                    showResizeBar = false
                                    isResizeMode = !isResizeMode
                                },
                                modifier = Modifier.weight(0.26f)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(0.74f)
                                    .clip(oneHandedInnerShape)
                                    .background(Color(theme.backgroundColor), oneHandedInnerShape)
                                    .border(1.dp, Color(theme.keyBorderColor).copy(alpha = 0.55f), oneHandedInnerShape)
                            ) {
                                KeyboardMainBlock(
                                    baseKeyHeight = animatedKeyHeight,
                                    cornerRadius = activeCornerRadius,
                                    compactToolbar = true
                                )
                            }
                        }
                    }
                }
            }
            }

            // Controlled dim layer over blurred keyboard backdrop while gesture dock or switcher is active
            if (backdropDimAlpha > 0.01f) {
                val tapToDismiss = gestureManager.activeDock in setOf(
                    FloatingDockType.CLIPBOARD_QUICK_DOCK,
                    FloatingDockType.ALTERNATE_CHARS_DOCK,
                    FloatingDockType.DATE_TIME_DOCK,
                    FloatingDockType.TRANSLATION_DOCK
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = backdropDimAlpha))
                        .then(
                            if (tapToDismiss) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { gestureManager.cancelAll() }
                            } else Modifier
                        )
                        .testTag("keyboard_gesture_dim_scrim")
                )
            }

            // Floating Glassmorphic Emoji Quick Access Dock (anchored inside keyboard bounds above BottomControlRow)
            EmojiQuickAccessDock(
                visible = isEmojiDockOpen,
                emojis = dockRecentEmojis,
                highlightedEmoji = highlightedDockEmoji,
                theme = theme,
                onViewportBoundsChanged = { dockViewportBounds = it },
                onEmojiBoundsChanged = { emoji, rect -> dockEmojiBounds[emoji] = rect },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Language Switcher Overlay (anchored above spacebar during horizontal swipe)
            FloatingLanguageSwitcherOverlay(
                visible = isLanguageSwipeActive,
                baseMode = languageSwipeBaseMode,
                previewStep = languageSwipePreviewStep,
                dragProgress = languageSwipeDragProgress,
                theme = theme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Clipboard Quick Dock
            ClipboardQuickDock(
                visible = isClipboardDockOpen,
                items = clipboardItems,
                theme = theme,
                onInsertClip = { text ->
                    playFeedback()
                    gestureManager.cancelAll()
                    onInsertTextSnippet(text)
                },
                onTogglePin = { id ->
                    playFeedback()
                    onTogglePinClipboard(id)
                },
                onDeleteItem = { id ->
                    playFeedback()
                    onRemoveClipboardItem(id)
                },
                onClearAll = {
                    playSpecialFeedback()
                    onClearClipboard()
                },
                onClose = { gestureManager.cancelAll() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Quick Symbol Dock
            SymbolQuickDock(
                visible = isSymbolDockOpen,
                symbols = KeyboardGestureHelper.QUICK_SYMBOLS,
                highlightedSymbol = highlightedDockEmoji,
                theme = theme,
                onViewportBoundsChanged = { symbolViewportBounds = it },
                onSymbolBoundsChanged = { sym, rect -> symbolDockBounds[sym] = rect },
                onSymbolClick = { sym ->
                    playFeedback()
                    gestureManager.cancelAll()
                    onKeyPress(sym)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Alternate Characters Dock
            AlternateCharDock(
                visible = isAlternateDockOpen,
                baseKey = gestureManager.activeAlternateKey,
                alternates = gestureManager.alternateOptions,
                highlightedChar = gestureManager.highlightedDockTarget,
                theme = theme,
                onSelectChar = { chosen ->
                    playFeedback()
                    gestureManager.cancelAll()
                    onKeyPress(chosen)
                },
                onClose = { gestureManager.cancelAll() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Quick Date/Time Dock
            DateTimeQuickDock(
                visible = isDateTimeDockOpen,
                theme = theme,
                onInsertValue = { value ->
                    playSpecialFeedback()
                    gestureManager.cancelAll()
                    onInsertTextSnippet(value)
                },
                onClose = { gestureManager.cancelAll() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Floating Translation Dock
            TranslationQuickDock(
                visible = isTranslationDockOpen,
                theme = theme,
                customEndpointUrl = settings.customTranslationUrl,
                initialClipText = clipboardItems.firstOrNull()?.text.orEmpty(),
                onInsertTranslation = { translated ->
                    playSpecialFeedback()
                    gestureManager.cancelAll()
                    onInsertTextSnippet(translated)
                },
                onClose = { gestureManager.cancelAll() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedKeyHeight + 10.dp)
                    .zIndex(60f)
            )

            // Interactive Direct Keyboard Resizing Overlay (visible only while resize mode is active)
            if (isResizeMode) {
                val rowCount = if (settings.showNumberRow) 5f else 4f
                val widthDragFactor = if (settings.oneHandedMode == OneHandedMode.OFF) 2f else 1f

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(theme.backgroundColor).copy(alpha = 0.52f))
                        .pointerInput(Unit) { detectTapGestures {} }
                        .testTag("resize_mode_overlay")
                ) {
                    // 1. Top-center draggable height handle (up = taller, down = shorter)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 148.dp, height = 48.dp)
                            .pointerInput(rowCount, scaleMultiplier) {
                                detectDragGestures(
                                    onDragStart = { isDraggingResize = true },
                                    onDragEnd = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDragCancel = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val baseTotalDp = 54f * rowCount * scaleMultiplier
                                        val dyDp = with(density) { (-dragAmount.y).toDp().value }
                                        val deltaPercent = (dyDp / baseTotalDp) * 100f
                                        draftHeightPercent = (draftHeightPercent + deltaPercent).coerceIn(75f, 140f)
                                    }
                                )
                            }
                            .testTag("resize_handle_top"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(width = 58.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(theme.accentColor))
                        )
                    }

                    // 2. Left-edge draggable width handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(width = 48.dp, height = 120.dp)
                            .pointerInput(fullWidthDp, widthDragFactor, settings.oneHandedMode) {
                                detectDragGestures(
                                    onDragStart = { isDraggingResize = true },
                                    onDragEnd = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDragCancel = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = with(density) { (-dragAmount.x).toDp().value }
                                        val deltaPercent = (dxDp * widthDragFactor / fullWidthDp) * 100f
                                        draftWidthPercent = (draftWidthPercent + deltaPercent).coerceIn(75f, 100f)
                                    }
                                )
                            }
                            .testTag("resize_handle_left"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(width = 6.dp, height = 52.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(theme.accentColor))
                        )
                    }

                    // 3. Right-edge draggable width handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 48.dp, height = 120.dp)
                            .pointerInput(fullWidthDp, widthDragFactor, settings.oneHandedMode) {
                                detectDragGestures(
                                    onDragStart = { isDraggingResize = true },
                                    onDragEnd = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDragCancel = {
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = with(density) { dragAmount.x.toDp().value }
                                        val deltaPercent = (dxDp * widthDragFactor / fullWidthDp) * 100f
                                        draftWidthPercent = (draftWidthPercent + deltaPercent).coerceIn(75f, 100f)
                                    }
                                )
                            }
                            .testTag("resize_handle_right"),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(width = 6.dp, height = 52.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(theme.accentColor))
                        )
                    }

                    // 4. Center Size Indicator Pill + Reset & Done Action Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 44.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(theme.candidateBarBackgroundColor).copy(alpha = 0.94f))
                            .border(1.dp, Color(theme.accentColor).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hInt = draftHeightPercent.roundToInt().coerceIn(75, 140)
                        val wInt = draftWidthPercent.roundToInt().coerceIn(75, 100)
                        Text(
                            text = "H: $hInt%  •  W: $wInt%",
                            color = Color(theme.accentColor),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri,
                            modifier = Modifier.testTag("resize_size_indicator")
                        )

                        Text(
                            text = "উপর ও পাশের হ্যান্ডেল টেনে আকার বদলান",
                            color = Color(theme.candidateBarTextColor).copy(alpha = 0.78f),
                            fontSize = 10.5.sp,
                            fontFamily = HindSiliguri,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(theme.keyBackgroundColor))
                                    .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(14.dp))
                                    .clickable {
                                        playFeedback()
                                        isDraggingResize = false
                                        draftHeightPercent = 100f
                                        draftWidthPercent = 100f
                                        onUpdateDimensions(100, 100)
                                    }
                                    .padding(horizontal = 14.dp)
                                    .testTag("btn_resize_reset"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset to Default",
                                    tint = Color(theme.keyTextColor),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reset",
                                    color = Color(theme.keyTextColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = HindSiliguri
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(theme.accentColor))
                                    .clickable {
                                        playSpecialFeedback()
                                        isDraggingResize = false
                                        onUpdateDimensions(
                                            draftHeightPercent.roundToInt().coerceIn(75, 140),
                                            draftWidthPercent.roundToInt().coerceIn(75, 100)
                                        )
                                        isResizeMode = false
                                    }
                                    .padding(horizontal = 16.dp)
                                    .testTag("btn_resize_done"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done Resizing",
                                    tint = Color(theme.accentTextColor),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Done",
                                    color = Color(theme.accentTextColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = HindSiliguri
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Candidate Toolbar with pill styling
 */
@Composable
private fun CandidateToolbar(
    theme: KeyboardTheme,
    candidates: List<String>,
    clipboardSmartHint: ClipboardSmartHint? = null,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    canPaste: Boolean,
    showMicButton: Boolean,
    isVoiceListening: Boolean,
    isResizeMode: Boolean,
    isTextEditOpen: Boolean,
    compactToolbar: Boolean,
    onCandidateClick: (String) -> Unit,
    onInsertSmartHint: (String) -> Unit = {},
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onTextEditAction: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onToggleTextEditing: () -> Unit,
    onToggleResizeMode: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenClipboardDock: () -> Unit = {},
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val actionBtnWidth = if (compactToolbar) 25.dp else 28.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(theme.candidateBarBackgroundColor))
            .drawBehind {
                val hairline = 1.dp.toPx()
                drawLine(
                    color = Color(theme.keyBorderColor).copy(alpha = 0.5f),
                    start = Offset(0f, size.height - hairline),
                    end = Offset(size.width, size.height - hairline),
                    strokeWidth = hairline
                )
            }
            .padding(horizontal = if (compactToolbar) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick Tools Launcher Icon
        IconButton(
            onClick = onOpenTools,
            modifier = Modifier
                .size(if (compactToolbar) 28.dp else 32.dp)
                .clip(CircleShape)
                .testTag("btn_quick_tools")
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Quick Tools",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(19.dp)
            )
        }

        val hasActiveCandidates = candidates.isNotEmpty()

        // Quick 1-Tap Undo Pill (hidden during active candidate typing so suggestion pills don't jump horizontally)
        if (canUndo && !hasActiveCandidates) {
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(theme.accentColor).copy(alpha = 0.2f))
                    .border(1.dp, Color(theme.accentColor).copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                    .clickable { onUndoClick() }
                    .padding(horizontal = 7.dp)
                    .testTag("btn_suggestion_undo"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = Color(theme.accentColor),
                    modifier = Modifier.size(14.dp)
                )
                if (!compactToolbar) {
                    Text(
                        text = "Undo",
                        color = Color(theme.accentColor),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                }
            }
        }

        // Quick 1-Tap Redo Pill (hidden during active candidate typing to keep suggestion bar wide and steady)
        if (canRedo && !hasActiveCandidates) {
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(theme.accentColor).copy(alpha = 0.2f))
                    .border(1.dp, Color(theme.accentColor).copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                    .clickable { onRedoClick() }
                    .padding(horizontal = 7.dp)
                    .testTag("btn_suggestion_redo"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = Color(theme.accentColor),
                    modifier = Modifier.size(14.dp)
                )
                if (!compactToolbar) {
                    Text(
                        text = "Redo",
                        color = Color(theme.accentColor),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Candidate suggestions list OR contextual selection/paste chips (lightweight Row avoids LazyLayout overhead)
        val candidatePillShape = remember { RoundedCornerShape(20.dp) }
        val pillScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .horizontalScroll(pillScrollState),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (candidates.isEmpty()) {
                if (hasSelection || canPaste || clipboardSmartHint != null) {
                    if (clipboardSmartHint != null && !hasSelection) {
                        ToolbarEditPill(
                            label = clipboardSmartHint.label,
                            icon = if (clipboardSmartHint.isUrl) Icons.Default.Link else Icons.Default.Password,
                            theme = theme,
                            tag = "btn_toolbar_smart_clip",
                            onClick = { onInsertSmartHint(clipboardSmartHint.value) }
                        )
                    }
                    if (hasSelection) {
                        ToolbarEditPill(
                            label = "Cut",
                            icon = Icons.Default.ContentCut,
                            theme = theme,
                            tag = "btn_toolbar_cut",
                            onClick = { onTextEditAction("CUT") }
                        )
                        ToolbarEditPill(
                            label = "Copy",
                            icon = Icons.Default.ContentCopy,
                            theme = theme,
                            tag = "btn_toolbar_copy",
                            onClick = { onTextEditAction("COPY") }
                        )
                    }
                    if (canPaste) {
                        ToolbarEditPill(
                            label = "Paste",
                            icon = Icons.Default.ContentPaste,
                            theme = theme,
                            tag = "btn_toolbar_paste",
                            onClick = { onTextEditAction("PASTE") }
                        )
                    }
                } else {
                    Text(
                        text = "অক্ষর • Okkhor",
                        color = Color(theme.candidateBarTextColor).copy(alpha = 0.4f),
                        fontSize = 12.5.sp,
                        fontFamily = HindSiliguri,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                candidates.forEachIndexed { index, candidate ->
                    val isFirst = index == 0
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(candidatePillShape)
                            .background(
                                if (isFirst) Color(theme.candidateHighlightColor)
                                else Color(theme.keyBackgroundColor).copy(alpha = 0.88f)
                            )
                            .border(
                                width = if (isFirst) 0.dp else 1.dp,
                                color = Color(theme.keyBorderColor).copy(alpha = 0.65f),
                                shape = candidatePillShape
                            )
                            .clickable { onCandidateClick(candidate) }
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candidate,
                            color = if (isFirst) Color(theme.candidateHighlightTextColor) else Color(theme.candidateBarTextColor),
                            fontSize = 14.sp,
                            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Language-Aware Voice Typing Mic Button
        if (showMicButton) {
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .width(actionBtnWidth)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isVoiceListening) Color(theme.accentColor).copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("btn_voice_mic")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Typing",
                    tint = if (isVoiceListening) Color(theme.accentColor) else Color(theme.candidateBarTextColor).copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Collapse secondary utility icons while user is actively picking word candidates so
        // suggestion pills have generous space without horizontal clipping or crowding.
        if (!hasActiveCandidates) {
            // Direct Text Editing Controller Button
            IconButton(
                onClick = onToggleTextEditing,
                modifier = Modifier
                    .width(actionBtnWidth)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isTextEditOpen) Color(theme.accentColor).copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("btn_toolbar_text_edit")
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Text Editing",
                    tint = if (isTextEditOpen) Color(theme.accentColor) else Color(theme.candidateBarTextColor).copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Direct Interactive Keyboard Resize Button
            IconButton(
                onClick = onToggleResizeMode,
                modifier = Modifier
                    .width(actionBtnWidth)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(if (isResizeMode) Color(theme.accentColor).copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("btn_toolbar_resize")
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Resize Keyboard",
                    tint = if (isResizeMode) Color(theme.accentColor) else Color(theme.candidateBarTextColor).copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Clipboard snippet button (tap = panel, long-press = Floating Quick Dock)
            val onOpenClipboardState = rememberUpdatedState(onOpenClipboard)
            val onOpenClipboardDockState = rememberUpdatedState(onOpenClipboardDock)
            Box(
                modifier = Modifier
                    .width(actionBtnWidth)
                    .height(40.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Clipboard. Tap for clipboard panel or hold for quick dock."
                        onClick {
                            onOpenClipboardState.value()
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onOpenClipboardState.value() },
                            onLongPress = { onOpenClipboardDockState.value() }
                        )
                    }
                    .testTag("btn_toolbar_clipboard"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = "Clipboard",
                    tint = Color(theme.candidateBarTextColor).copy(alpha = 0.82f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Quick Theme switcher button
            if (!compactToolbar) {
                IconButton(
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .width(actionBtnWidth)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = Color(theme.candidateBarTextColor).copy(alpha = 0.82f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Okkhor Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .width(actionBtnWidth)
                    .height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(theme.candidateBarTextColor).copy(alpha = 0.82f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarEditPill(
    label: String,
    icon: ImageVector,
    theme: KeyboardTheme,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(theme.keyBackgroundColor))
            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(theme.accentColor),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = Color(theme.keyTextColor),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = HindSiliguri
        )
    }
}

/**
 * 4x3 Quick Tools Grid Drawer
 */
@Composable
private fun QuickToolsGridDrawer(
    theme: KeyboardTheme,
    settings: KeyboardSettings,
    height: Dp,
    onClose: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenTheme: () -> Unit,
    onToggleResize: () -> Unit,
    onToggleOneHanded: () -> Unit,
    onOpenTextEditing: () -> Unit,
    onSwitchLayout: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleNumberRow: () -> Unit,
    onToggleSymbols: () -> Unit,
    onToggleSuggestions: () -> Unit,
    onToggleRoundedKeys: () -> Unit,
    onStartVoice: () -> Unit,
    onToggleDoubleSpace: () -> Unit,
    onOpenTranslateDock: () -> Unit = {},
    onOpenDateTimeDock: () -> Unit = {},
    onToggleFloating: () -> Unit = {},
    onToggleGlide: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    data class ToolItem(
        val titleBn: String,
        val subtitleEn: String,
        val icon: ImageVector,
        val tag: String,
        val isActive: Boolean = false,
        val onClick: () -> Unit
    )

    val tools = listOf(
        ToolItem("ক্লিপবোর্ড", "Clipboard", Icons.AutoMirrored.Filled.Assignment, "tool_clipboard", onClick = onOpenClipboard),
        ToolItem("অনুবাদ", "Translate", Icons.Default.Translate, "tool_translate", onClick = onOpenTranslateDock),
        ToolItem("তারিখ ও সময়", "Date & Time", Icons.Default.CalendarMonth, "tool_datetime", onClick = onOpenDateTimeDock),
        ToolItem("ভাসমান", "Floating", Icons.Default.OpenWith, "tool_floating", isActive = settings.floatingKeyboardMode, onClick = onToggleFloating),
        ToolItem("ভয়েস টাইপ", "Voice Typing", Icons.Default.Mic, "tool_voice", isActive = settings.voiceTypingEnabled, onClick = onStartVoice),
        ToolItem("গ্লাইড টাইপ", "Glide", Icons.Default.Gesture, "tool_glide", isActive = settings.glideTypingEnabled, onClick = onToggleGlide),
        ToolItem("থিমস", "Themes", Icons.Default.Palette, "tool_themes", onClick = onOpenTheme),
        ToolItem("গোল কী", "Rounded Keys", Icons.Default.RoundedCorner, "tool_rounded", isActive = settings.roundedPillKeys, onClick = onToggleRoundedKeys),
        ToolItem("আকার", "Resize", Icons.Default.AspectRatio, "tool_resize", onClick = onToggleResize),
        ToolItem("এক হাত", "One-handed", Icons.Default.StayCurrentPortrait, "tool_one_handed", isActive = settings.oneHandedMode != OneHandedMode.OFF, onClick = onToggleOneHanded),
        ToolItem("টেক্সট এডিট", "Text editing", Icons.Default.TextFields, "tool_text_edit", onClick = onOpenTextEditing),
        ToolItem("লেআউট", "Layout", Icons.Default.Language, "tool_layout", onClick = onSwitchLayout),
        ToolItem("সংখ্যা সারি", "Number Row", Icons.Default.Tune, "tool_number_row", isActive = settings.showNumberRow, onClick = onToggleNumberRow),
        ToolItem("শব্দ ও ভাইব", "Sound & Haptic", Icons.AutoMirrored.Filled.VolumeUp, "tool_sound", isActive = settings.soundFeedback, onClick = onToggleSound),
        ToolItem("শব্দ পরামর্শ", "Suggestions", Icons.Default.Search, "tool_suggestions", isActive = settings.showSuggestions, onClick = onToggleSuggestions),
        ToolItem("অ্যাপ সেটিংস", "Settings", Icons.Default.Settings, "tool_settings", onClick = onOpenSettings)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(theme.backgroundColor))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Drawer Header with Return arrow & quick actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Keyboard",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "কুইক টুলস ও সেটিংস • Quick Tools",
                    color = Color(theme.keyTextColor),
                    fontSize = 13.5.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "বন্ধ করুন",
                color = Color(theme.accentColor),
                fontSize = 12.5.sp,
                fontFamily = HindSiliguri,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // 4-Column x 3-Row Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tools) { item ->
                val cardShape = RoundedCornerShape(12.dp)
                val cardBg = if (item.isActive) Color(theme.accentColor).copy(alpha = 0.22f)
                else Color(theme.keyBackgroundColor)
                val cardBorder = if (item.isActive) Color(theme.accentColor)
                else Color(theme.keyBorderColor)

                Column(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, cardShape)
                        .clickable { item.onClick() }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag(item.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.titleBn,
                        tint = if (item.isActive) Color(theme.accentColor) else Color(theme.keyTextColor),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.titleBn,
                        color = Color(theme.keyTextColor),
                        fontSize = 10.5.sp,
                        fontFamily = HindSiliguri,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Text Editing Controller Panel with D-Pad & Edit Actions
 */
@Composable
private fun TextEditingPanel(
    theme: KeyboardTheme,
    height: Dp,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    canPaste: Boolean,
    isSelectionMode: Boolean,
    onClose: () -> Unit,
    onCursorMove: (Int) -> Unit,
    onActionKey: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onTextEditAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(theme.backgroundColor))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("text_editing_panel")
    ) {
        // Header with conditional Undo/Redo (only visible when available)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = "টেক্সট এডিটিং",
                    color = Color(theme.keyTextColor),
                    fontSize = 13.5.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.Bold
                )
                if (canUndo) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(theme.accentColor).copy(alpha = 0.16f))
                            .border(1.dp, Color(theme.accentColor).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .clickable { onUndoClick() }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("btn_edit_undo"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color(theme.accentColor), modifier = Modifier.size(14.dp))
                        Text("Undo", color = Color(theme.accentColor), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (canRedo) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(theme.accentColor).copy(alpha = 0.16f))
                            .border(1.dp, Color(theme.accentColor).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .clickable { onRedoClick() }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("btn_edit_redo"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = Color(theme.accentColor), modifier = Modifier.size(14.dp))
                        Text("Redo", color = Color(theme.accentColor), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "সম্পন্ন",
                color = Color(theme.accentColor),
                fontSize = 12.5.sp,
                fontFamily = HindSiliguri,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Quick Clipboard Actions Row (Select All, Cut, Copy, Paste) with genuine availability state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            data class EditActionSpec(
                val label: String,
                val code: String,
                val enabled: Boolean,
                val tag: String
            )

            val actions = listOf(
                EditActionSpec("সব নির্বাচন", "SELECT_ALL", enabled = true, tag = "btn_edit_select_all"),
                EditActionSpec("কাট", "CUT", enabled = hasSelection, tag = "btn_edit_cut"),
                EditActionSpec("কপি", "COPY", enabled = hasSelection, tag = "btn_edit_copy"),
                EditActionSpec("পেস্ট", "PASTE", enabled = canPaste, tag = "btn_edit_paste")
            )

            actions.forEach { spec ->
                val shape = RoundedCornerShape(8.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .alpha(if (spec.enabled) 1f else 0.36f)
                        .clip(shape)
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), shape)
                        .semantics {
                            if (!spec.enabled) disabled()
                        }
                        .clickable(enabled = spec.enabled) { onTextEditAction(spec.code) }
                        .testTag(spec.tag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.label,
                        color = Color(theme.keyTextColor),
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguri,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Ergonomic D-Pad Cursor Navigator + Selection Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Mode Toggle on the left of the D-Pad
            Column(
                modifier = Modifier.padding(end = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val selectShape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .size(width = 76.dp, height = 68.dp)
                        .clip(selectShape)
                        .background(
                            if (isSelectionMode) Color(theme.accentColor).copy(alpha = 0.22f)
                            else Color(theme.keyBackgroundColor)
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isSelectionMode) Color(theme.accentColor) else Color(theme.keyBorderColor),
                            shape = selectShape
                        )
                        .clickable { onToggleSelectionMode() }
                        .testTag("btn_edit_select_mode"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HighlightAlt,
                        contentDescription = "Selection Mode",
                        tint = if (isSelectionMode) Color(theme.accentColor) else Color(theme.keyTextColor),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSelectionMode) "Selecting" else "Select",
                        color = if (isSelectionMode) Color(theme.accentColor) else Color(theme.keyTextColor),
                        fontSize = 11.sp,
                        fontWeight = if (isSelectionMode) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // UP
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                        .clickable { onTextEditAction("CURSOR_UP") }
                        .testTag("btn_dpad_up"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // LEFT - BACKSPACE - RIGHT
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onCursorMove(-1) }
                            .testTag("btn_dpad_left"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Left",
                            tint = Color(theme.accentColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.specialKeyBackgroundColor))
                            .border(1.dp, Color(theme.specialKeyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onActionKey(KeyboardLayouts.CODE_DELETE) }
                            .testTag("btn_dpad_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color(theme.specialKeyTextColor),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onCursorMove(1) }
                            .testTag("btn_dpad_right"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Right",
                            tint = Color(theme.accentColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // DOWN
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                        .clickable { onTextEditAction("CURSOR_DOWN") }
                        .testTag("btn_dpad_down"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Down",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Quick Keyboard Height Resize Bar
 */
@Composable
private fun QuickResizeBar(
    theme: KeyboardTheme,
    currentPercent: Int,
    onSelectPercent: (Int) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(theme.candidateBarBackgroundColor).copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "কিবোর্ড উচ্চতা (Height):",
            color = Color(theme.candidateBarTextColor),
            fontSize = 12.sp,
            fontFamily = HindSiliguri,
            fontWeight = FontWeight.Medium
        )

        val heightOptions = listOf(
            85 to "ছোট (85%)",
            100 to "স্বাভাবিক (100%)",
            115 to "বড় (115%)",
            125 to "সর্বোচ্চ (125%)"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            heightOptions.forEach { (percent, label) ->
                val isSelected = currentPercent == percent
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) Color(theme.accentColor)
                            else Color(theme.keyBackgroundColor)
                        )
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(6.dp))
                        .clickable { onSelectPercent(percent) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(theme.accentTextColor) else Color(theme.keyTextColor),
                        fontSize = 11.sp,
                        fontFamily = HindSiliguri,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Close",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * In-Keyboard Clipboard History Panel
 */
@Composable
private fun KeyboardClipboardPanel(
    theme: KeyboardTheme,
    items: List<ClipboardItem>,
    retentionHours: Int,
    height: Dp,
    onPasteItem: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    val retentionOption = ClipboardRetentionOption.fromHours(retentionHours)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(theme.backgroundColor))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Clipboard Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ক্লিপবোর্ড (${items.size}/100 • ${retentionOption.labelBn})",
                    color = Color(theme.keyTextColor),
                    fontSize = 13.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.Bold
                )
            }

            if (items.isNotEmpty()) {
                Text(
                    text = "সব মুছুন",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onClearAll() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val retentionMsg = if (retentionHours <= 0) {
                    "পিন না করা ক্লিপও ম্যানুয়ালি না মোছা পর্যন্ত থাকবে।"
                } else {
                    "পিন না করা ক্লিপ ${retentionOption.labelBn} পর্যন্ত সংরক্ষিত থাকবে।"
                }
                Text(
                    text = "কোনো কপি করা টেক্সট নেই।\n$retentionMsg",
                    color = Color(theme.keySecondaryTextColor),
                    fontSize = 12.5.sp,
                    fontFamily = HindSiliguri,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val itemShape = RoundedCornerShape(8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), itemShape)
                            .clickable { onPasteItem(item.text) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.text,
                            color = Color(theme.keyTextColor),
                            fontSize = 13.5.sp,
                            fontFamily = HindSiliguri,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onTogglePin(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (item.isPinned) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteItem(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact One-Handed Side Rail (Switch Side, Resize, Fullscreen)
 */
@Composable
private fun OneHandedSideRail(
    theme: KeyboardTheme,
    isLeft: Boolean,
    onExpand: () -> Unit,
    onSwitchSide: () -> Unit,
    onResize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Switch side button (Left or Right arrow)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(theme.keyBackgroundColor))
                .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(12.dp))
                .clickable { onSwitchSide() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLeft) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isLeft) "Switch to Right" else "Switch to Left",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(24.dp)
            )
        }

        // 2. Quick Resize button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(theme.keyBackgroundColor))
                .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(12.dp))
                .clickable { onResize() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Resize Height",
                tint = Color(theme.keyTextColor),
                modifier = Modifier.size(22.dp)
            )
        }

        // 3. Fullscreen exit button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(theme.specialKeyBackgroundColor))
                .border(1.dp, Color(theme.specialKeyBorderColor), RoundedCornerShape(12.dp))
                .clickable { onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Exit One-Handed Mode",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Core Keyboard Content Rows (Numbers, Matrix, Bottom Control Row)
 */
@Composable
private fun MainKeyboardContent(
    settings: KeyboardSettings,
    theme: KeyboardTheme,
    currentMode: InputMode,
    lastTextMode: InputMode,
    isShifted: Boolean,
    isCapsLock: Boolean,
    baseKeyHeight: Dp,
    cornerRadius: Dp,
    elevation: Dp,
    spacing: Dp,
    enterActionLabel: String,
    isSpaceCursorHoldActive: Boolean = false,
    isOverlayBlockingTouch: Boolean = false,
    playFeedback: () -> Unit,
    playSpecialFeedback: () -> Unit,
    emojiRepository: UnicodeEmojiRepository,
    giphyRepository: GiphyRepository,
    recentEmojis: List<String>,
    emojiFrequency: Map<String, Int>,
    favoriteEmojis: List<String>,
    emojiSearchHistory: List<String>,
    preferredEmojiVariants: Map<String, String>,
    recentGifs: List<GifItem>,
    favoriteGifs: List<GifItem>,
    gifSearchHistory: List<String>,
    recentMashups: List<EmojiMashupRecord>,
    favoriteMashups: List<EmojiMashupRecord>,
    supportsGifCommit: Boolean,
    supportsImageCommit: Boolean,
    mediaInsertNotice: String?,
    onDismissMediaNotice: () -> Unit,
    onEmojiSelected: (baseUnicode: String, insertedUnicode: String) -> Unit,
    onSelectEmojiVariant: (baseUnicode: String, variantUnicode: String) -> Unit,
    onToggleFavoriteEmoji: (String) -> Unit,
    onRecordEmojiSearch: (String) -> Unit,
    onClearEmojiSearchHistory: () -> Unit,
    onInsertGif: (GifItem) -> Unit,
    onShareGif: (GifItem) -> Unit,
    onCopyGifUrl: (GifItem) -> Unit,
    onToggleFavoriteGif: (GifItem) -> Unit,
    onRecordGifSearch: (String) -> Unit,
    onClearGifSearchHistory: () -> Unit,
    onInsertMashup: (first: String, second: String, style: EmojiMashupStyle) -> Unit,
    onShareMashup: (first: String, second: String, style: EmojiMashupStyle) -> Unit,
    onToggleFavoriteMashup: (EmojiMashupRecord) -> Unit,
    onKeyPress: (String) -> Unit,
    onActionKey: (Int) -> Unit,
    onModeSwitch: (InputMode) -> Unit,
    onCursorMove: (Int) -> Unit,
    onEmojiLongPressStart: () -> Unit,
    onEmojiDockDrag: (Offset) -> Unit,
    onEmojiDockRelease: () -> Unit,
    onEmojiDockCancel: () -> Unit,
    onLanguageSwipeStart: (Float) -> Unit,
    onLanguageSwipeUpdate: (Float) -> Unit,
    onLanguageSwipeCommit: () -> Unit,
    onLanguageSwipeCancel: () -> Unit,
    onSpaceCursorHoldStateChanged: (Boolean) -> Unit,
    onSymbolLongPressStart: () -> Unit = {},
    onSymbolDockDrag: (Offset) -> Unit = {},
    onSymbolDockRelease: () -> Unit = {},
    onSymbolDockCancel: () -> Unit = {},
    onShowAlternates: (String, List<String>) -> Unit = { _, _ -> },
    onDeleteWordSwipe: () -> Unit = { onActionKey(KeyboardLayouts.CODE_DELETE) },
    onCommitGlideWord: (String, List<String>) -> Unit = { _, _ -> },
    shiftDragHighlightKey: String? = null,
    onShiftDragHighlightChanged: (String?) -> Unit = {}
) {
    val surfaceState = rememberKeyboardSurfaceTouchState()
    val surfaceScope = rememberCoroutineScope()
    val surfaceDensity = LocalDensity.current
    val fixedShiftCharByNormal = remember { mutableStateMapOf<String, String>() }
    var glideTrailPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val settingsState = rememberUpdatedState(settings)
    val currentModeState = rememberUpdatedState(currentMode)
    val isOverlayBlockingState = rememberUpdatedState(isOverlayBlockingTouch)
    val onCommitGlideWordState = rememberUpdatedState(onCommitGlideWord)
    val shiftHighlightState = rememberUpdatedState(shiftDragHighlightKey)
    val onShiftHighlightState = rememberUpdatedState(onShiftDragHighlightChanged)
    val onKeyPressState = rememberUpdatedState(onKeyPress)
    val onCursorMoveState = rememberUpdatedState(onCursorMove)
    val onLanguageSwipeStartState = rememberUpdatedState(onLanguageSwipeStart)
    val onLanguageSwipeUpdateState = rememberUpdatedState(onLanguageSwipeUpdate)
    val onLanguageSwipeCommitState = rememberUpdatedState(onLanguageSwipeCommit)
    val onLanguageSwipeCancelState = rememberUpdatedState(onLanguageSwipeCancel)
    val onSpaceCursorHoldState = rememberUpdatedState(onSpaceCursorHoldStateChanged)
    val onEmojiDockDragState = rememberUpdatedState(onEmojiDockDrag)
    val onEmojiDockReleaseState = rememberUpdatedState(onEmojiDockRelease)
    val onEmojiDockCancelState = rememberUpdatedState(onEmojiDockCancel)
    val onSymbolDockDragState = rememberUpdatedState(onSymbolDockDrag)
    val onSymbolDockReleaseState = rememberUpdatedState(onSymbolDockRelease)
    val onSymbolDockCancelState = rememberUpdatedState(onSymbolDockCancel)
    val playFeedbackState = rememberUpdatedState(playFeedback)

    LaunchedEffect(currentMode, settings.showNumberRow) {
        surfaceState.resetTransientState()
        fixedShiftCharByNormal.clear()
        glideTrailPoints = emptyList()
    }

    fun handleKeyLongPress(primaryKey: String, secondaryHint: String) {
        if (!settings.longPressSymbolsEnabled) return
        val alternates = BanglaConjunctHelper.getAlternateChars(primaryKey, secondaryHint)
        if (alternates.size > 1) {
            onShowAlternates(primaryKey, alternates)
        } else if (secondaryHint.isNotEmpty()) {
            playSpecialFeedback()
            onKeyPress(secondaryHint)
        }
    }

    val keySurfaceModifier = if (currentMode != InputMode.EMOJI) {
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> surfaceState.recomputeAllBounds(coords) }
            .keyboardSurfaceTouchDispatcher(
                key = Pair(currentMode, settings.showNumberRow),
                surfaceState = surfaceState,
                coroutineScope = surfaceScope,
                density = surfaceDensity,
                isOverlayBlockingTouch = { isOverlayBlockingState.value },
                glideEnabled = {
                    settingsState.value.glideTypingEnabled &&
                        (currentModeState.value == InputMode.ENGLISH || currentModeState.value == InputMode.BANGLA_PHONETIC)
                },
                spacebarCursorSwipeEnabled = { settingsState.value.spacebarCursorSwipe },
                spacebarLanguageSwipeEnabled = { settingsState.value.spacebarLanguageSwipe },
                onSpaceCursorMove = { delta -> onCursorMoveState.value(delta) },
                onSpaceLanguageStart = { dx -> onLanguageSwipeStartState.value(dx) },
                onSpaceLanguageUpdate = { dx -> onLanguageSwipeUpdateState.value(dx) },
                onSpaceLanguageCommit = { onLanguageSwipeCommitState.value() },
                onSpaceLanguageCancel = { onLanguageSwipeCancelState.value() },
                onSpaceCursorHoldStateChanged = { active -> onSpaceCursorHoldState.value(active) },
                onDockDragRoot = { category, rootPos ->
                    if (category == KeyHitCategory.EMOJI) onEmojiDockDragState.value(rootPos)
                    else onSymbolDockDragState.value(rootPos)
                },
                onDockRelease = { category ->
                    if (category == KeyHitCategory.EMOJI) onEmojiDockReleaseState.value()
                    else onSymbolDockReleaseState.value()
                },
                onDockCancel = { category ->
                    if (category == KeyHitCategory.EMOJI) onEmojiDockCancelState.value()
                    else onSymbolDockCancelState.value()
                },
                onShiftDragTarget = { target -> onShiftHighlightState.value(target) },
                onShiftDragRelease = {
                    val target = shiftHighlightState.value
                    onShiftHighlightState.value(null)
                    if (!target.isNullOrEmpty()) {
                        val commitChar = if (currentModeState.value == InputMode.BANGLA_FIXED) {
                            fixedShiftCharByNormal[target] ?: target
                        } else {
                            target.uppercase()
                        }
                        if (commitChar.isNotEmpty()) {
                            playFeedbackState.value()
                            onKeyPressState.value(commitChar)
                        }
                    }
                },
                onShiftDragCancel = { onShiftHighlightState.value(null) },
                onGlideTrailChanged = { points -> glideTrailPoints = points },
                onCommitGlidePath = { visited ->
                    val matches = GestureTypingEngine.matchPathToWords(visited, currentModeState.value)
                    if (matches.isNotEmpty()) {
                        onCommitGlideWordState.value(matches.first(), matches)
                    }
                }
            )
            .padding(bottom = 4.dp)
            .testTag("keyboard_key_matrix")
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = keySurfaceModifier) {
        // --- Optional Dedicated Number Row ---
        if (settings.showNumberRow && currentMode != InputMode.NUMBERS_SYMBOLS && currentMode != InputMode.SECONDARY_SYMBOLS && currentMode != InputMode.EMOJI) {
            val numDigits = if (currentMode.isBangla) KeyboardLayouts.NUMBER_ROW_BANGLA else KeyboardLayouts.NUMBER_ROW_ENGLISH
            val secDigits = if (currentMode.isBangla) KeyboardLayouts.NUMBER_ROW_ENGLISH else KeyboardLayouts.NUMBER_ROW_BANGLA

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                numDigits.forEachIndexed { idx, digit ->
                    val sec = secDigits.getOrNull(idx) ?: ""
                    KeyButton(
                        primary = digit,
                        secondary = sec,
                        weight = 1f,
                        height = (baseKeyHeight * 0.82f),
                        theme = theme,
                        isSpecial = false,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        showPopupPreview = settings.keyPopupPreview,
                        surfaceState = surfaceState,
                        surfaceKeyId = "num_$idx",
                        surfaceRowIndex = 0,
                        onClick = {
                            playFeedback()
                            onKeyPress(digit)
                        },
                        onLongClick = {
                            playSpecialFeedback()
                            if (sec.isNotEmpty()) onKeyPress(sec)
                        }
                    )
                }
            }
        }

        // --- Main Rows Matrix ---
        when (currentMode) {
            InputMode.BANGLA_PHONETIC -> {
                val r1 = KeyboardLayouts.ENGLISH_ROW_1
                val r2 = KeyboardLayouts.ENGLISH_ROW_2
                val r3 = KeyboardLayouts.ENGLISH_ROW_3

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        r1.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "bn_r1_$key",
                                surfaceRowIndex = 1,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Spacer(modifier = Modifier.weight(0.45f))
                        r2.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "bn_r2_$key",
                                surfaceRowIndex = 2,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.weight(0.45f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShiftKeyButton(
                            isShifted = isShifted,
                            isCapsLock = isCapsLock,
                            weight = 1.45f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            surfaceState = surfaceState,
                            surfaceKeyId = "btn_shift",
                            surfaceRowIndex = 3,
                            onClick = {
                                playSpecialFeedback()
                                onActionKey(KeyboardLayouts.CODE_SHIFT)
                            }
                        )

                        r3.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "bn_r3_$key",
                                surfaceRowIndex = 3,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }

                        RepeatableSpecialKeyButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color(theme.specialKeyTextColor),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            weight = 1.45f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            surfaceState = surfaceState,
                            surfaceKeyId = "btn_backspace",
                            surfaceRowIndex = 3,
                            modifier = Modifier.testTag("btn_backspace"),
                            onClick = {
                                playFeedback()
                                onActionKey(KeyboardLayouts.CODE_DELETE)
                            },
                            onSwipeLeft = {
                                onDeleteWordSwipe()
                            }
                        )
                    }
                }
            }

            InputMode.ENGLISH -> {
                val r1 = KeyboardLayouts.ENGLISH_ROW_1
                val r2 = KeyboardLayouts.ENGLISH_ROW_2
                val r3 = KeyboardLayouts.ENGLISH_ROW_3

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        r1.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "en_r1_$key",
                                surfaceRowIndex = 1,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Spacer(modifier = Modifier.weight(0.45f))
                        r2.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "en_r2_$key",
                                surfaceRowIndex = 2,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.weight(0.45f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShiftKeyButton(
                            isShifted = isShifted,
                            isCapsLock = isCapsLock,
                            weight = 1.45f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            surfaceState = surfaceState,
                            surfaceKeyId = "btn_shift",
                            surfaceRowIndex = 3,
                            onClick = {
                                playSpecialFeedback()
                                onActionKey(KeyboardLayouts.CODE_SHIFT)
                            }
                        )

                        r3.forEach { key ->
                            val char = if (isShifted) key.uppercase() else key
                            val secondary = if (settings.longPressSymbolsEnabled) {
                                KeyboardLayouts.LONG_PRESS_SYMBOLS[key] ?: ""
                            } else ""

                            KeyButton(
                                primary = char,
                                secondary = secondary,
                                weight = 1f,
                                height = baseKeyHeight,
                                theme = theme,
                                cornerRadius = cornerRadius,
                                elevation = elevation,
                                showPopupPreview = settings.keyPopupPreview,
                                isGestureHighlighted = shiftDragHighlightKey.equals(key, ignoreCase = true),
                                surfaceState = surfaceState,
                                surfaceKeyId = "en_r3_$key",
                                surfaceRowIndex = 3,
                                isGlideEligible = true,
                                letterLookupKey = key,
                                onClick = {
                                    playFeedback()
                                    onKeyPress(char)
                                },
                                onLongClick = {
                                    handleKeyLongPress(char, secondary)
                                }
                            )
                        }

                        RepeatableSpecialKeyButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color(theme.specialKeyTextColor),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            weight = 1.45f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            surfaceState = surfaceState,
                            surfaceKeyId = "btn_backspace",
                            surfaceRowIndex = 3,
                            modifier = Modifier.testTag("btn_backspace"),
                            onClick = {
                                playFeedback()
                                onActionKey(KeyboardLayouts.CODE_DELETE)
                            },
                            onSwipeLeft = {
                                onDeleteWordSwipe()
                            }
                        )
                    }
                }
            }

            InputMode.BANGLA_FIXED -> {
                val r1Norm = KeyboardLayouts.PRABHAT_ROW_1_NORMAL
                val r1Shift = KeyboardLayouts.PRABHAT_ROW_1_SHIFT
                val r2Norm = KeyboardLayouts.PRABHAT_ROW_2_NORMAL
                val r2Shift = KeyboardLayouts.PRABHAT_ROW_2_SHIFT
                val r3Norm = KeyboardLayouts.PRABHAT_ROW_3_NORMAL
                val r3Shift = KeyboardLayouts.PRABHAT_ROW_3_SHIFT

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (idx in r1Norm.indices) {
                        val normChar = r1Norm[idx]
                        val shiftChar = r1Shift.getOrElse(idx) { "" }
                        fixedShiftCharByNormal[normChar] = shiftChar
                        val primary = if (isShifted) shiftChar else normChar
                        val secondary = if (isShifted) normChar else shiftChar
                        KeyButton(
                            primary = primary,
                            secondary = secondary,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            isGestureHighlighted = shiftDragHighlightKey == normChar,
                            surfaceState = surfaceState,
                            surfaceKeyId = "prabhat_r1_$idx",
                            surfaceRowIndex = 1,
                            letterLookupKey = normChar,
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                handleKeyLongPress(primary, secondary)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (idx in r2Norm.indices) {
                        val normChar = r2Norm[idx]
                        val shiftChar = r2Shift.getOrElse(idx) { "" }
                        fixedShiftCharByNormal[normChar] = shiftChar
                        val primary = if (isShifted) shiftChar else normChar
                        val secondary = if (isShifted) normChar else shiftChar
                        KeyButton(
                            primary = primary,
                            secondary = secondary,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            isGestureHighlighted = shiftDragHighlightKey == normChar,
                            surfaceState = surfaceState,
                            surfaceKeyId = "prabhat_r2_$idx",
                            surfaceRowIndex = 2,
                            letterLookupKey = normChar,
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                handleKeyLongPress(primary, secondary)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShiftKeyButton(
                        isShifted = isShifted,
                        isCapsLock = isCapsLock,
                        weight = 1.25f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "btn_shift",
                        surfaceRowIndex = 3,
                        onClick = {
                            playSpecialFeedback()
                            onActionKey(KeyboardLayouts.CODE_SHIFT)
                        }
                    )

                    for (idx in r3Norm.indices) {
                        val normChar = r3Norm[idx]
                        val shiftChar = r3Shift.getOrElse(idx) { "" }
                        fixedShiftCharByNormal[normChar] = shiftChar
                        val primary = if (isShifted) shiftChar else normChar
                        val secondary = if (isShifted) normChar else shiftChar
                        KeyButton(
                            primary = primary,
                            secondary = secondary,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            isGestureHighlighted = shiftDragHighlightKey == normChar,
                            surfaceState = surfaceState,
                            surfaceKeyId = "prabhat_r3_$idx",
                            surfaceRowIndex = 3,
                            letterLookupKey = normChar,
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                handleKeyLongPress(primary, secondary)
                            }
                        )
                    }

                    RepeatableSpecialKeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color(theme.specialKeyTextColor),
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        weight = 1.25f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "btn_backspace",
                        surfaceRowIndex = 3,
                        modifier = Modifier.testTag("btn_backspace"),
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        },
                        onSwipeLeft = {
                            onDeleteWordSwipe()
                        }
                    )
                }
            }

            InputMode.NUMBERS_SYMBOLS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KeyboardLayouts.SYMBOLS_ROW_1.forEachIndexed { idx, digit ->
                        val bDigit = KeyboardLayouts.NUMBER_ROW_BANGLA.getOrNull(idx) ?: ""
                        KeyButton(
                            primary = digit,
                            secondary = bDigit,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "sym1_$idx",
                            surfaceRowIndex = 1,
                            onClick = {
                                playFeedback()
                                onKeyPress(digit)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (bDigit.isNotEmpty()) onKeyPress(bDigit)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KeyboardLayouts.SYMBOLS_ROW_2.forEachIndexed { idx, sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "sym2_$idx",
                            surfaceRowIndex = 2,
                            onClick = {
                                playFeedback()
                                onKeyPress(sym)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKeyTextButton(
                        text = "=\\<",
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "sym_more_toggle",
                        surfaceRowIndex = 3,
                        onClick = {
                            playSpecialFeedback()
                            onModeSwitch(InputMode.SECONDARY_SYMBOLS)
                        }
                    )

                    KeyboardLayouts.SYMBOLS_ROW_3.forEachIndexed { idx, sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "sym3_$idx",
                            surfaceRowIndex = 3,
                            onClick = {
                                playFeedback()
                                onKeyPress(sym)
                            }
                        )
                    }

                    RepeatableSpecialKeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color(theme.specialKeyTextColor),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "btn_backspace",
                        surfaceRowIndex = 3,
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        }
                    )
                }
            }

            InputMode.SECONDARY_SYMBOLS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KeyboardLayouts.MORE_SYMBOLS_ROW_1.forEachIndexed { idx, sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "msym1_$idx",
                            surfaceRowIndex = 1,
                            onClick = {
                                playFeedback()
                                onKeyPress(sym)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KeyboardLayouts.MORE_SYMBOLS_ROW_2.forEachIndexed { idx, sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "msym2_$idx",
                            surfaceRowIndex = 2,
                            onClick = {
                                playFeedback()
                                onKeyPress(sym)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKeyTextButton(
                        text = "?123",
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "msym_back_toggle",
                        surfaceRowIndex = 3,
                        onClick = {
                            playSpecialFeedback()
                            onModeSwitch(InputMode.NUMBERS_SYMBOLS)
                        }
                    )

                    KeyboardLayouts.MORE_SYMBOLS_ROW_3.forEachIndexed { idx, sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
                            surfaceState = surfaceState,
                            surfaceKeyId = "msym3_$idx",
                            surfaceRowIndex = 3,
                            onClick = {
                                playFeedback()
                                onKeyPress(sym)
                            }
                        )
                    }

                    RepeatableSpecialKeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color(theme.specialKeyTextColor),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        surfaceState = surfaceState,
                        surfaceKeyId = "btn_backspace",
                        surfaceRowIndex = 3,
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        }
                    )
                }
            }

            InputMode.EMOJI -> {
                EmojiGifKitchenPanel(
                    height = (baseKeyHeight * 5.2f),
                    theme = theme,
                    settings = settings,
                    lastTextMode = lastTextMode,
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    emojiRepository = emojiRepository,
                    giphyRepository = giphyRepository,
                    recentEmojis = recentEmojis,
                    emojiFrequency = emojiFrequency,
                    favoriteEmojis = favoriteEmojis,
                    emojiSearchHistory = emojiSearchHistory,
                    preferredVariants = preferredEmojiVariants,
                    recentGifs = recentGifs,
                    favoriteGifs = favoriteGifs,
                    gifSearchHistory = gifSearchHistory,
                    recentMashups = recentMashups,
                    favoriteMashups = favoriteMashups,
                    supportsGifCommit = supportsGifCommit,
                    supportsImageCommit = supportsImageCommit,
                    mediaInsertNotice = mediaInsertNotice,
                    onDismissNotice = onDismissMediaNotice,
                    onEmojiSelected = { base, inserted ->
                        playFeedback()
                        onEmojiSelected(base, inserted)
                    },
                    onSelectVariant = { base, variant ->
                        playSpecialFeedback()
                        onSelectEmojiVariant(base, variant)
                    },
                    onToggleFavoriteEmoji = onToggleFavoriteEmoji,
                    onRecordEmojiSearch = onRecordEmojiSearch,
                    onClearEmojiSearchHistory = onClearEmojiSearchHistory,
                    onInsertGif = { gif ->
                        playSpecialFeedback()
                        onInsertGif(gif)
                    },
                    onShareGif = onShareGif,
                    onCopyGifUrl = onCopyGifUrl,
                    onToggleFavoriteGif = onToggleFavoriteGif,
                    onRecordGifSearch = onRecordGifSearch,
                    onClearGifSearchHistory = onClearGifSearchHistory,
                    onInsertMashup = { f, s, st ->
                        playSpecialFeedback()
                        onInsertMashup(f, s, st)
                    },
                    onShareMashup = onShareMashup,
                    onToggleFavoriteMashup = onToggleFavoriteMashup,
                    onBackspace = {
                        playFeedback()
                        onActionKey(KeyboardLayouts.CODE_DELETE)
                    },
                    onBackToText = {
                        playSpecialFeedback()
                        onModeSwitch(lastTextMode)
                    }
                )
            }
        }

        // --- Bottom Control Row ---
        if (currentMode != InputMode.EMOJI) {
            BottomControlRow(
                settings = settings,
                theme = theme,
                currentMode = currentMode,
                baseHeight = baseKeyHeight,
                spacing = spacing,
                cornerRadius = cornerRadius,
                elevation = elevation,
                enterActionLabel = enterActionLabel,
                spacebarSwipeEnabled = settings.spacebarCursorSwipe,
                spacebarLanguageSwipeEnabled = settings.spacebarLanguageSwipe,
                isSpaceCursorHoldActive = isSpaceCursorHoldActive,
                surfaceState = surfaceState,
                onSymbolToggle = {
                    playSpecialFeedback()
                    if (currentMode == InputMode.NUMBERS_SYMBOLS || currentMode == InputMode.SECONDARY_SYMBOLS) {
                        onModeSwitch(lastTextMode)
                    } else {
                        onModeSwitch(InputMode.NUMBERS_SYMBOLS)
                    }
                },
                onSymbolLongPressStart = onSymbolLongPressStart,
                onEmojiClick = {
                    playSpecialFeedback()
                    onModeSwitch(InputMode.EMOJI)
                },
                onEmojiLongPressStart = onEmojiLongPressStart,
                onLanguageToggle = {
                    playSpecialFeedback()
                    val base = if (currentMode.isTextMode) currentMode else lastTextMode
                    val next = KeyboardGestureHelper.stepLanguageMode(base, +1)
                    onModeSwitch(next)
                },
                onSpaceClick = {
                    playFeedback()
                    onActionKey(KeyboardLayouts.CODE_SPACE)
                },
                onDotClick = {
                    playFeedback()
                    val symbol = if (currentMode.isBangla) "।" else "."
                    onKeyPress(symbol)
                },
                onEnterClick = {
                    playSpecialFeedback()
                    onActionKey(KeyboardLayouts.CODE_ENTER)
                }
            )
        }
    }

    KeyboardLiquidDropOverlay(
        surfaceState = surfaceState,
        theme = theme,
        cornerRadius = cornerRadius,
        modifier = Modifier.matchParentSize()
    )

    if (glideTrailPoints.size >= 2) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .testTag("glide_typing_trail")
        ) {
            val path = Path().apply {
                moveTo(glideTrailPoints.first().x, glideTrailPoints.first().y)
                for (i in 1 until glideTrailPoints.size) {
                    lineTo(glideTrailPoints[i].x, glideTrailPoints[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color(theme.accentColor).copy(alpha = 0.72f),
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
    }
}

@Composable
private fun SurfaceKeyRegistration(
    surfaceState: KeyboardSurfaceTouchState?,
    keyId: String?,
    letterLookupKey: String? = null,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null
) {
    if (surfaceState == null || keyId == null) return
    val ownershipToken = remember(surfaceState, keyId) { Any() }
    SideEffect {
        surfaceState.claimKeyOwner(keyId, ownershipToken)
        surfaceState.updateKeyCallbacks(
            id = keyId,
            letterLookupKey = letterLookupKey,
            onTap = onTap,
            onLongPress = onLongPress,
            onSwipeLeft = onSwipeLeft
        )
    }
    DisposableEffect(surfaceState, keyId) {
        surfaceState.claimKeyOwner(keyId, ownershipToken)
        onDispose {
            surfaceState.unregisterKeyIfOwner(keyId, ownershipToken)
        }
    }
}

private fun Modifier.registerSurfaceBounds(
    surfaceState: KeyboardSurfaceTouchState?,
    keyId: String?,
    category: KeyHitCategory,
    rowIndex: Int,
    isBottomRow: Boolean = false,
    isGlideEligible: Boolean = false,
    letterLookupKey: String? = null,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null
): Modifier {
    if (surfaceState == null || keyId == null) return this
    return this.onGloballyPositioned { coords ->
        surfaceState.registerKeyLayout(
            id = keyId,
            coords = coords,
            targetTemplate = KeyHitTarget(
                id = keyId,
                category = category,
                rowIndex = rowIndex,
                bounds = Rect.Zero,
                isBottomRow = isBottomRow,
                isGlideEligible = isGlideEligible
            ),
            letterLookupKey = letterLookupKey,
            onTap = onTap,
            onLongPress = onLongPress,
            onSwipeLeft = onSwipeLeft
        )
    }
}

/**
 * Shift Keycap with Single-Tap Temporary Upper, Double-Tap CapsLock Lock,
 * and Hold-and-Drag onto any letter key for direct uppercase / shifted input.
 */
@Composable
private fun RowScope.ShiftKeyButton(
    isShifted: Boolean,
    isCapsLock: Boolean,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 3,
    onClick: () -> Unit,
    onDragRoot: ((Offset) -> Unit)? = null,
    onDragRelease: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(cornerRadius)

    val onClickState = rememberUpdatedState(onClick)
    val onDragRootState = rememberUpdatedState(onDragRoot)
    val onDragReleaseState = rememberUpdatedState(onDragRelease)
    val onDragCancelState = rememberUpdatedState(onDragCancel)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onClickState.value() }
    )

    val bg = when {
        isPressed -> Color(theme.pressedKeyBackgroundColor)
        isShifted -> Color(theme.accentColor).copy(alpha = 0.25f)
        else -> Color(theme.specialKeyBackgroundColor)
    }

    val borderClr = when {
        isShifted -> Color(theme.accentColor)
        else -> Color(theme.specialKeyBorderColor)
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = KeyHitCategory.SHIFT,
                rowIndex = surfaceRowIndex,
                onTap = { onClickState.value() }
            )
            .shadow(if (isPressed) 0.dp else elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(bg)
            .border(width = theme.borderWidthDp.dp, color = borderClr, shape = shape)
            .then(if (surfaceState == null) Modifier.onGloballyPositioned { coords = it } else Modifier)
            .semantics {
                role = Role.Button
                contentDescription = if (isCapsLock) "Caps Lock" else "Shift"
                onClick {
                    onClickState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            localPressed = true
                            var isDragging = false
                            var cancelled = false

                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1) {
                                    cancelled = true
                                    if (isDragging) onDragCancelState.value?.invoke()
                                    break
                                }
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                if (!change.pressed) {
                                    if (isDragging) {
                                        onDragReleaseState.value?.invoke()
                                    } else if (!cancelled) {
                                        onClickState.value()
                                    }
                                    break
                                }
                                val dist = (change.position - downPos).getDistance()
                                if (!isDragging && dist > slop && onDragRootState.value != null) {
                                    isDragging = true
                                }
                                if (isDragging) {
                                    change.consume()
                                    val rootPos = coords?.let { if (it.isAttached) it.localToRoot(change.position) else null }
                                    if (rootPos != null) {
                                        onDragRootState.value?.invoke(rootPos)
                                    }
                                }
                            }
                            localPressed = false
                        }
                    }
                } else Modifier
            )
            .testTag("btn_shift"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isCapsLock) "Caps Lock" else "Shift",
                tint = if (isShifted) Color(theme.accentColor) else Color(theme.specialKeyTextColor),
                modifier = Modifier.size(21.dp)
            )

            // Underline bar indication when CapsLock is locked
            if (isCapsLock) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .width(14.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(theme.accentColor))
                )
            }
        }
    }
}

private enum class SpacebarGestureMode {
    NONE,
    LANGUAGE_SWITCH,
    CURSOR_SWIPE,
    CANCELLED
}

/**
 * Bottom Control Row with ?123 (tap + long-press quick symbol dock), Emoji (tap + long-press dock), Language, Spacebar, Dot/Dari, and Action Key
 */
@Composable
private fun BottomControlRow(
    settings: KeyboardSettings,
    theme: KeyboardTheme,
    currentMode: InputMode,
    baseHeight: Dp,
    spacing: Dp,
    cornerRadius: Dp,
    elevation: Dp,
    enterActionLabel: String,
    spacebarSwipeEnabled: Boolean,
    spacebarLanguageSwipeEnabled: Boolean,
    isSpaceCursorHoldActive: Boolean,
    surfaceState: KeyboardSurfaceTouchState? = null,
    onSymbolToggle: () -> Unit,
    onSymbolLongPressStart: () -> Unit = {},
    onSymbolDockDrag: (Offset) -> Unit = {},
    onSymbolDockRelease: () -> Unit = {},
    onSymbolDockCancel: () -> Unit = {},
    onEmojiClick: () -> Unit,
    onEmojiLongPressStart: () -> Unit = {},
    onEmojiDockDrag: (Offset) -> Unit = {},
    onEmojiDockRelease: () -> Unit = {},
    onEmojiDockCancel: () -> Unit = {},
    onLanguageToggle: () -> Unit,
    onSpaceClick: () -> Unit,
    onCursorSwipe: (Int) -> Unit = {},
    onLanguageSwipeStart: (Float) -> Unit = {},
    onLanguageSwipeUpdate: (Float) -> Unit = {},
    onLanguageSwipeCommit: () -> Unit = {},
    onLanguageSwipeCancel: () -> Unit = {},
    onSpaceCursorHoldStateChanged: (Boolean) -> Unit = {},
    onDotClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    var localSpacePressed by remember { mutableStateOf(false) }
    val isSpacePressed = if (surfaceState != null) surfaceState.isPressed("spacebar") else localSpacePressed
    val density = LocalDensity.current
    val gestureScope = rememberCoroutineScope()

    val spacebarSwipeEnabledState = rememberUpdatedState(spacebarSwipeEnabled)
    val spacebarLanguageSwipeState = rememberUpdatedState(spacebarLanguageSwipeEnabled)
    val onSpaceClickState = rememberUpdatedState(onSpaceClick)
    val onCursorSwipeState = rememberUpdatedState(onCursorSwipe)
    val onLanguageSwipeStartState = rememberUpdatedState(onLanguageSwipeStart)
    val onLanguageSwipeUpdateState = rememberUpdatedState(onLanguageSwipeUpdate)
    val onLanguageSwipeCommitState = rememberUpdatedState(onLanguageSwipeCommit)
    val onLanguageSwipeCancelState = rememberUpdatedState(onLanguageSwipeCancel)
    val onCursorHoldChangedState = rememberUpdatedState(onSpaceCursorHoldStateChanged)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = "spacebar",
        onTap = { onSpaceClickState.value() }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val symbolLabel = if (currentMode == InputMode.NUMBERS_SYMBOLS || currentMode == InputMode.SECONDARY_SYMBOLS) "ABC" else "?123"
        SymbolDockTriggerTextButton(
            text = symbolLabel,
            weight = 1.25f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            surfaceState = surfaceState,
            surfaceKeyId = "btn_symbol_key",
            surfaceRowIndex = 4,
            isBottomRow = true,
            onTap = onSymbolToggle,
            onLongPressStart = onSymbolLongPressStart,
            onDragRoot = onSymbolDockDrag,
            onRelease = onSymbolDockRelease,
            onCancel = onSymbolDockCancel
        )

        EmojiDockTriggerKeyButton(
            weight = 1.0f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            surfaceState = surfaceState,
            surfaceKeyId = "btn_emoji",
            surfaceRowIndex = 4,
            isBottomRow = true,
            onTap = onEmojiClick,
            onLongPressStart = onEmojiLongPressStart,
            onDragRoot = onEmojiDockDrag,
            onRelease = onEmojiDockRelease,
            onCancel = onEmojiDockCancel
        )

        SpecialKeyButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Switch Language",
                    tint = Color(theme.accentColor),
                    modifier = Modifier.size(20.dp)
                )
            },
            weight = 1.0f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            surfaceState = surfaceState,
            surfaceKeyId = "btn_language",
            surfaceRowIndex = 4,
            isBottomRow = true,
            surfaceCategory = KeyHitCategory.LANGUAGE,
            modifier = Modifier.testTag("btn_language"),
            onClick = onLanguageToggle
        )

        val spaceLabel = when {
            isSpaceCursorHoldActive -> "◀  Cursor  ▶"
            currentMode == InputMode.BANGLA_PHONETIC -> "বাংলা ফোনেটিক"
            currentMode == InputMode.BANGLA_FIXED -> "বাংলা প্রভাত"
            currentMode == InputMode.ENGLISH -> "English"
            else -> "Space"
        }

        val spaceShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

        // Spacebar (Tap = space, Horizontal Swipe = Language Switcher, Hold + Drag = Cursor Move)
        Box(
            modifier = Modifier
                .weight(4.4f)
                .height(baseHeight)
                .registerSurfaceBounds(
                    surfaceState = surfaceState,
                    keyId = "spacebar",
                    category = KeyHitCategory.SPACEBAR,
                    rowIndex = 4,
                    isBottomRow = true,
                    onTap = { onSpaceClickState.value() }
                )
                .shadow(elevation, spaceShape)
                .clip(spaceShape)
                .drawBehind {
                    val rimHeight = 2.dp.toPx()
                    drawRoundRect(
                        color = Color(theme.keyBottomRimColor),
                        topLeft = Offset(0f, size.height - rimHeight),
                        size = Size(size.width, rimHeight),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
                .background(
                    when {
                        isSpaceCursorHoldActive -> Color(theme.accentColor).copy(alpha = 0.22f)
                        isSpacePressed -> Color(theme.pressedKeyBackgroundColor)
                        else -> Color(theme.keyBackgroundColor)
                    }
                )
                .border(
                    width = theme.borderWidthDp.dp,
                    color = if (isSpaceCursorHoldActive) Color(theme.accentColor) else Color(theme.keyBorderColor),
                    shape = spaceShape
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "$spaceLabel spacebar. Tap for space, swipe left or right to switch language."
                    onClick {
                        onSpaceClickState.value()
                        true
                    }
                }
                .then(
                    if (surfaceState == null) {
                        Modifier.pointerInput(Unit) {
                            val slop = viewConfiguration.touchSlop
                            val swipeActivatePx = maxOf(slop * 2.4f, with(density) { 20.dp.toPx() })
                            val verticalJitterPx = maxOf(slop * 2.5f, with(density) { 22.dp.toPx() })
                            val cursorHoldDelayMs = 340L
                            val cancelYUpPx = with(density) { 110.dp.toPx() }
                            val cancelYDownPx = with(density) { 64.dp.toPx() }
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val activePointerId = down.id
                                val downPos = down.position
                                localSpacePressed = true

                                var gestureMode = SpacebarGestureMode.NONE
                                var accumulatedCursorDrag = 0f

                                val cursorHoldJob = if (spacebarSwipeEnabledState.value) {
                                    gestureScope.launch {
                                        delay(cursorHoldDelayMs)
                                        if (gestureMode == SpacebarGestureMode.NONE) {
                                            gestureMode = SpacebarGestureMode.CURSOR_SWIPE
                                            onCursorHoldChangedState.value(true)
                                        }
                                    }
                                } else null

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount > 1) {
                                        cursorHoldJob?.cancel()
                                        if (gestureMode == SpacebarGestureMode.LANGUAGE_SWITCH) {
                                            onLanguageSwipeCancelState.value()
                                        }
                                        if (gestureMode == SpacebarGestureMode.CURSOR_SWIPE) {
                                            onCursorHoldChangedState.value(false)
                                        }
                                        gestureMode = SpacebarGestureMode.CANCELLED
                                        break
                                    }
                                    val change = event.changes.firstOrNull { it.id == activePointerId }
                                        ?: event.changes.firstOrNull()
                                        ?: break

                                    if (!change.pressed) {
                                        cursorHoldJob?.cancel()
                                        when (gestureMode) {
                                            SpacebarGestureMode.NONE -> onSpaceClickState.value()
                                            SpacebarGestureMode.LANGUAGE_SWITCH -> onLanguageSwipeCommitState.value()
                                            SpacebarGestureMode.CURSOR_SWIPE -> onCursorHoldChangedState.value(false)
                                            SpacebarGestureMode.CANCELLED -> Unit
                                        }
                                        break
                                    }

                                    val dx = change.position.x - change.previousPosition.x
                                    val netDx = change.position.x - downPos.x
                                    val netDy = change.position.y - downPos.y

                                    when (gestureMode) {
                                        SpacebarGestureMode.NONE -> {
                                            if (abs(netDx) > slop || abs(netDy) > slop) {
                                                cursorHoldJob?.cancel()
                                            }
                                            if (abs(netDy) > abs(netDx) * 1.6f && abs(netDy) > verticalJitterPx) {
                                                gestureMode = SpacebarGestureMode.CANCELLED
                                            } else if (spacebarLanguageSwipeState.value && abs(netDx) > swipeActivatePx) {
                                                gestureMode = SpacebarGestureMode.LANGUAGE_SWITCH
                                                change.consume()
                                                onLanguageSwipeStartState.value(netDx)
                                            } else if (!spacebarLanguageSwipeState.value && spacebarSwipeEnabledState.value && abs(netDx) > swipeActivatePx) {
                                                gestureMode = SpacebarGestureMode.CURSOR_SWIPE
                                                change.consume()
                                                accumulatedCursorDrag += dx
                                            }
                                        }

                                        SpacebarGestureMode.LANGUAGE_SWITCH -> {
                                            change.consume()
                                            if (netDy < -cancelYUpPx || netDy > cancelYDownPx) {
                                                onLanguageSwipeCancelState.value()
                                                gestureMode = SpacebarGestureMode.CANCELLED
                                            } else {
                                                onLanguageSwipeUpdateState.value(netDx)
                                            }
                                        }

                                        SpacebarGestureMode.CURSOR_SWIPE -> {
                                            change.consume()
                                            accumulatedCursorDrag += dx
                                            val threshold = 32f
                                            while (abs(accumulatedCursorDrag) >= threshold) {
                                                val direction = if (accumulatedCursorDrag > 0) 1 else -1
                                                onCursorSwipeState.value(direction)
                                                accumulatedCursorDrag -= direction * threshold
                                            }
                                        }

                                        SpacebarGestureMode.CANCELLED -> {
                                            change.consume()
                                        }
                                    }
                                }

                                cursorHoldJob?.cancel()
                                onCursorHoldChangedState.value(false)
                                localSpacePressed = false
                            }
                        }
                    } else Modifier
                )
                .testTag("spacebar"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.25f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (spacebarSwipeEnabled || spacebarLanguageSwipeEnabled) {
                    Text(
                        text = "‹",
                        color = Color(theme.keySecondaryTextColor).copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = spaceLabel,
                    color = if (isSpaceCursorHoldActive) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                    fontSize = 13.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = if (isSpaceCursorHoldActive) FontWeight.Bold else FontWeight.Medium
                )
                if (spacebarSwipeEnabled || spacebarLanguageSwipeEnabled) {
                    Text(
                        text = "›",
                        color = Color(theme.keySecondaryTextColor).copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        val punct = if (currentMode.isBangla) "।" else "."
        KeyButton(
            primary = punct,
            secondary = if (currentMode.isBangla) "." else "!",
            weight = 1.0f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            showPopupPreview = settings.keyPopupPreview,
            surfaceState = surfaceState,
            surfaceKeyId = "key_dot",
            surfaceRowIndex = 4,
            isBottomRow = true,
            onClick = onDotClick
        )

        val enterShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
        var localEnterPressed by remember { mutableStateOf(false) }
        val isEnterPressed = if (surfaceState != null) surfaceState.isPressed("btn_enter") else localEnterPressed
        val onEnterClickState = rememberUpdatedState(onEnterClick)

        SurfaceKeyRegistration(
            surfaceState = surfaceState,
            keyId = "btn_enter",
            onTap = { onEnterClickState.value() }
        )

        Box(
            modifier = Modifier
                .weight(1.4f)
                .height(baseHeight)
                .registerSurfaceBounds(
                    surfaceState = surfaceState,
                    keyId = "btn_enter",
                    category = KeyHitCategory.ENTER,
                    rowIndex = 4,
                    isBottomRow = true,
                    onTap = { onEnterClickState.value() }
                )
                .shadow(elevation, enterShape)
                .clip(enterShape)
                .drawBehind {
                    val rimHeight = 2.dp.toPx()
                    drawRoundRect(
                        color = Color(theme.keyBottomRimColor),
                        topLeft = Offset(0f, size.height - rimHeight),
                        size = Size(size.width, rimHeight),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
                .background(
                    if (isEnterPressed) Color(theme.accentColor).copy(alpha = 0.85f)
                    else Color(theme.accentColor)
                )
                .border(
                    width = theme.borderWidthDp.dp,
                    color = Color(theme.accentColor).copy(alpha = 0.7f),
                    shape = enterShape
                )
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = enterActionLabel
                    onClick {
                        onEnterClickState.value()
                        true
                    }
                }
                .then(
                    if (surfaceState == null) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                localEnterPressed = true
                                val up = waitForUpOrCancellation()
                                localEnterPressed = false
                                if (up != null && up.id == down.id && !up.isConsumed) {
                                    onEnterClickState.value()
                                }
                            }
                        }
                    } else Modifier
                )
                .testTag("btn_enter"),
            contentAlignment = Alignment.Center
        ) {
            val actionIcon = when (enterActionLabel.lowercase()) {
                "search" -> Icons.Default.Search
                "send" -> Icons.AutoMirrored.Filled.Send
                "done", "check" -> Icons.Default.Check
                "next", "go" -> Icons.AutoMirrored.Filled.ArrowForward
                else -> Icons.AutoMirrored.Filled.KeyboardReturn
            }

            Icon(
                imageVector = actionIcon,
                contentDescription = enterActionLabel,
                tint = Color(theme.accentTextColor),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Premium Keycap with Long-Press Detection, Tactile 3D Depth, and Magnifier Popup
 */
@Composable
fun RowScope.KeyButton(
    primary: String,
    secondary: String = "",
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    isSpecial: Boolean = false,
    cornerRadius: Dp,
    elevation: Dp,
    showPopupPreview: Boolean = true,
    isGestureHighlighted: Boolean = false,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 1,
    isBottomRow: Boolean = false,
    isGlideEligible: Boolean = false,
    letterLookupKey: String? = null,
    onRootBoundsChanged: ((Rect) -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val popupShape = remember { RoundedCornerShape(10.dp) }
    val touchScope = rememberCoroutineScope()
    val onClickState = rememberUpdatedState(onClick)
    val onLongClickState = rememberUpdatedState(onLongClick)

    val longPressCallback: (() -> Unit)? = if (onLongClick != null) {
        { onLongClickState.value?.invoke() }
    } else null
    val resolvedLookupKey = letterLookupKey ?: primary
    val hitCategory = if (surfaceRowIndex == 0 || isBottomRow || letterLookupKey == null) {
        KeyHitCategory.NUMBER_OR_SYMBOL
    } else {
        KeyHitCategory.LETTER
    }

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        letterLookupKey = resolvedLookupKey,
        onTap = { onClickState.value() },
        onLongPress = longPressCallback
    )

    val scale = when {
        isGestureHighlighted -> 1.07f
        isPressed -> 0.97f
        else -> 1.0f
    }

    val bg = when {
        isGestureHighlighted -> Color(theme.accentColor).copy(alpha = 0.28f)
        isPressed -> Color(theme.pressedKeyBackgroundColor)
        isSpecial -> Color(theme.specialKeyBackgroundColor)
        else -> Color(theme.keyBackgroundColor)
    }

    val borderClr = when {
        isGestureHighlighted -> Color(theme.accentColor)
        isSpecial -> Color(theme.specialKeyBorderColor)
        else -> Color(theme.keyBorderColor)
    }

    val textClr = when {
        isSpecial -> Color(theme.specialKeyTextColor)
        else -> Color(theme.keyTextColor)
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = hitCategory,
                rowIndex = surfaceRowIndex,
                isBottomRow = isBottomRow,
                isGlideEligible = isGlideEligible,
                letterLookupKey = resolvedLookupKey,
                onTap = { onClickState.value() },
                onLongPress = longPressCallback
            )
            .then(if (scale != 1.0f) Modifier.scale(scale) else Modifier)
            .shadow(elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(bg)
            .border(width = theme.borderWidthDp.dp, color = borderClr, shape = shape)
            .then(
                if (onRootBoundsChanged != null) {
                    Modifier.onGloballyPositioned { coords ->
                        if (coords.isAttached) onRootBoundsChanged(coords.boundsInRoot())
                    }
                } else Modifier
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick {
                    onClickState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        val longPressMs = viewConfiguration.longPressTimeoutMillis
                        val releaseSlopPx = maxOf(slop * 2.2f, 22.dp.toPx())
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            localPressed = true

                            var longPressTriggered = false
                            var movedBeyondLongPress = false

                            val longPressJob = touchScope.launch {
                                delay(longPressMs)
                                if (!movedBeyondLongPress && onLongClickState.value != null) {
                                    longPressTriggered = true
                                    onLongClickState.value?.invoke()
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    longPressJob.cancel()
                                    if (!longPressTriggered && !change.isConsumed) {
                                        val dist = (change.position - downPos).getDistance()
                                        val inBounds = change.position.x >= -14.dp.toPx() &&
                                            change.position.x <= size.width + 14.dp.toPx() &&
                                            change.position.y >= -18.dp.toPx() &&
                                            change.position.y <= size.height + 14.dp.toPx()
                                        if (inBounds || dist <= releaseSlopPx) {
                                            onClickState.value()
                                        } else {
                                            TypingPerformanceTracker.recordMissedTouch()
                                        }
                                    }
                                    break
                                }

                                if (!movedBeyondLongPress) {
                                    val dist = (change.position - downPos).getDistance()
                                    if (dist > slop * 1.4f) {
                                        movedBeyondLongPress = true
                                        longPressJob.cancel()
                                    }
                                }
                            }
                            longPressJob.cancel()
                            localPressed = false
                        }
                    }
                } else Modifier
            )
            .testTag("key_$primary"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle top specular edge highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.09f else 0.28f))
        )

        // Floating Magnifier Popup Preview
        if (isPressed && showPopupPreview && primary.length <= 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-52).dp)
                    .zIndex(100f)
                    .size(width = 48.dp, height = 50.dp)
                    .shadow(8.dp, popupShape)
                    .clip(popupShape)
                    .background(Color(theme.popupBackgroundColor))
                    .border(1.2.dp, Color(theme.keyBorderColor), popupShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = primary,
                    color = Color(theme.popupTextColor),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Secondary character / number hint in top-right corner
        if (secondary.isNotEmpty()) {
            Text(
                text = secondary,
                color = Color(theme.keySecondaryTextColor).copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontFamily = HindSiliguri,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 3.5.dp)
            )
        }

        // Primary glyph with Bengali typography optical centering
        Text(
            text = primary,
            color = textClr,
            fontSize = if (primary.length > 1) 14.sp else 19.5.sp,
            fontFamily = HindSiliguri,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = if (secondary.isNotEmpty()) 4.dp else 0.dp,
                bottom = 1.dp
            )
        )
    }
}

@Composable
private fun RowScope.EmojiDockTriggerKeyButton(
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 4,
    isBottomRow: Boolean = true,
    onTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onDragRoot: (Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    var buttonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(cornerRadius)
    val scope = rememberCoroutineScope()

    val onTapState = rememberUpdatedState(onTap)
    val onLongPressStartState = rememberUpdatedState(onLongPressStart)
    val onDragRootState = rememberUpdatedState(onDragRoot)
    val onReleaseState = rememberUpdatedState(onRelease)
    val onCancelState = rememberUpdatedState(onCancel)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onTapState.value() },
        onLongPress = { onLongPressStartState.value() }
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = KeyHitCategory.EMOJI,
                rowIndex = surfaceRowIndex,
                isBottomRow = isBottomRow,
                onTap = { onTapState.value() },
                onLongPress = { onLongPressStartState.value() }
            )
            .shadow(if (isPressed) 0.dp else elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(
                if (isPressed) Color(theme.pressedKeyBackgroundColor)
                else Color(theme.specialKeyBackgroundColor)
            )
            .border(
                width = theme.borderWidthDp.dp,
                color = Color(theme.specialKeyBorderColor),
                shape = shape
            )
            .then(if (surfaceState == null) Modifier.onGloballyPositioned { buttonCoords = it } else Modifier)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Emoji. Tap for emoji panel or hold and drag for quick recent emojis."
                onClick {
                    onTapState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        val longPressMs = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val activePointerId = down.id
                            val downPos = down.position
                            localPressed = true

                            var dockOpened = false
                            var gestureCancelled = false
                            var movedBeforeLongPress = false

                            val longPressJob = scope.launch {
                                delay(longPressMs)
                                if (!gestureCancelled && !movedBeforeLongPress) {
                                    dockOpened = true
                                    onLongPressStartState.value()
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount > 1) {
                                    gestureCancelled = true
                                    longPressJob.cancel()
                                    if (dockOpened) {
                                        onCancelState.value()
                                    }
                                    break
                                }
                                val change = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break

                                if (!change.pressed) {
                                    longPressJob.cancel()
                                    if (dockOpened) {
                                        onReleaseState.value()
                                    } else if (!gestureCancelled && !movedBeforeLongPress) {
                                        onTapState.value()
                                    }
                                    break
                                }

                                val distFromDown = (change.position - downPos).getDistance()
                                if (!dockOpened) {
                                    if (distFromDown > slop) {
                                        movedBeforeLongPress = true
                                        longPressJob.cancel()
                                    }
                                } else {
                                    change.consume()
                                    val rootPos = buttonCoords?.let { coords ->
                                        if (coords.isAttached) coords.localToRoot(change.position) else null
                                    }
                                    if (rootPos != null) {
                                        onDragRootState.value(rootPos)
                                    }
                                }
                            }

                            longPressJob.cancel()
                            localPressed = false
                        }
                    }
                } else Modifier
            )
            .testTag("btn_emoji"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )
        Icon(
            imageVector = Icons.Default.Mood,
            contentDescription = "Emoji",
            tint = Color(theme.specialKeyTextColor),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Functional Key Button for Language, Emoji
 */
@Composable
fun RowScope.SpecialKeyButton(
    icon: @Composable () -> Unit,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    isHighlighted: Boolean = false,
    cornerRadius: Dp,
    elevation: Dp,
    modifier: Modifier = Modifier,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 4,
    isBottomRow: Boolean = true,
    surfaceCategory: KeyHitCategory = KeyHitCategory.LANGUAGE,
    onClick: () -> Unit
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val onClickState = rememberUpdatedState(onClick)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onClickState.value() }
    )

    val bg = when {
        isPressed -> Color(theme.pressedKeyBackgroundColor)
        isHighlighted -> Color(theme.accentColor).copy(alpha = 0.25f)
        else -> Color(theme.specialKeyBackgroundColor)
    }

    val borderClr = when {
        isHighlighted -> Color(theme.accentColor)
        else -> Color(theme.specialKeyBorderColor)
    }

    Box(
        modifier = modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = surfaceCategory,
                rowIndex = surfaceRowIndex,
                isBottomRow = isBottomRow,
                onTap = { onClickState.value() }
            )
            .shadow(elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(bg)
            .border(width = theme.borderWidthDp.dp, color = borderClr, shape = shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick {
                    onClickState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            localPressed = true
                            val up = waitForUpOrCancellation()
                            localPressed = false
                            if (up != null && up.id == down.id && !up.isConsumed) {
                                onClickState.value()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )
        icon()
    }
}

/**
 * Repeatable Backspace Key: Single-click deletes instantly, hold auto-repeats, swipe-left deletes word
 */
@Composable
fun RowScope.RepeatableSpecialKeyButton(
    icon: @Composable () -> Unit,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    modifier: Modifier = Modifier,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 3,
    onClick: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    val shape = RoundedCornerShape(cornerRadius)
    val coroutineScope = rememberCoroutineScope()
    val onClickState = rememberUpdatedState(onClick)
    val onSwipeLeftState = rememberUpdatedState(onSwipeLeft)
    val swipeLeftCallback: (() -> Unit)? = if (onSwipeLeft != null) {
        { onSwipeLeftState.value?.invoke() }
    } else null

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onClickState.value() },
        onSwipeLeft = swipeLeftCallback
    )

    Box(
        modifier = modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = KeyHitCategory.BACKSPACE,
                rowIndex = surfaceRowIndex,
                onTap = { onClickState.value() },
                onSwipeLeft = swipeLeftCallback
            )
            .shadow(if (isPressed) 0.dp else elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(
                if (isPressed) Color(theme.pressedKeyBackgroundColor)
                else Color(theme.specialKeyBackgroundColor)
            )
            .border(
                width = theme.borderWidthDp.dp,
                color = Color(theme.specialKeyBorderColor),
                shape = shape
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick {
                    onClickState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            localPressed = true

                            // 1. Single tap executes IMMEDIATELY on touch down!
                            onClickState.value()

                            var dragX = 0f
                            var hasSwiped = false

                            // 2. Launch auto-repeat if held beyond threshold
                            val repeatJob = coroutineScope.launch {
                                delay(380)
                                while (isActive && !hasSwiped) {
                                    onClickState.value()
                                    delay(55)
                                }
                            }

                            // 3. Track horizontal swipe gesture
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    break
                                }
                                val delta = change.position.x - change.previousPosition.x
                                dragX += delta
                                if (dragX < -45f && onSwipeLeftState.value != null && !hasSwiped) {
                                    hasSwiped = true
                                    repeatJob.cancel()
                                    onSwipeLeftState.value?.invoke()
                                    dragX = 0f
                                }
                            }

                            repeatJob.cancel()
                            localPressed = false
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )
        icon()
    }
}

@Composable
private fun RowScope.SymbolDockTriggerTextButton(
    text: String,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 4,
    isBottomRow: Boolean = true,
    onTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onDragRoot: (Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    var buttonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(cornerRadius)
    val scope = rememberCoroutineScope()

    val onTapState = rememberUpdatedState(onTap)
    val onLongPressStartState = rememberUpdatedState(onLongPressStart)
    val onDragRootState = rememberUpdatedState(onDragRoot)
    val onReleaseState = rememberUpdatedState(onRelease)
    val onCancelState = rememberUpdatedState(onCancel)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onTapState.value() },
        onLongPress = { onLongPressStartState.value() }
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = KeyHitCategory.SYMBOL_MODE,
                rowIndex = surfaceRowIndex,
                isBottomRow = isBottomRow,
                onTap = { onTapState.value() },
                onLongPress = { onLongPressStartState.value() }
            )
            .shadow(if (isPressed) 0.dp else elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(
                if (isPressed) Color(theme.pressedKeyBackgroundColor)
                else Color(theme.specialKeyBackgroundColor)
            )
            .border(
                width = theme.borderWidthDp.dp,
                color = Color(theme.specialKeyBorderColor),
                shape = shape
            )
            .then(if (surfaceState == null) Modifier.onGloballyPositioned { buttonCoords = it } else Modifier)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Symbols. Tap for symbols or hold and drag for quick symbol dock."
                onClick {
                    onTapState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        val longPressMs = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val activePointerId = down.id
                            val downPos = down.position
                            localPressed = true

                            var dockOpened = false
                            var gestureCancelled = false
                            var movedBeforeLongPress = false

                            val longPressJob = scope.launch {
                                delay(longPressMs)
                                if (!gestureCancelled && !movedBeforeLongPress) {
                                    dockOpened = true
                                    onLongPressStartState.value()
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1) {
                                    gestureCancelled = true
                                    longPressJob.cancel()
                                    if (dockOpened) onCancelState.value()
                                    break
                                }
                                val change = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                if (!change.pressed) {
                                    longPressJob.cancel()
                                    if (dockOpened) {
                                        onReleaseState.value()
                                    } else if (!gestureCancelled && !movedBeforeLongPress) {
                                        onTapState.value()
                                    }
                                    break
                                }
                                val distFromDown = (change.position - downPos).getDistance()
                                if (!dockOpened) {
                                    if (distFromDown > slop) {
                                        movedBeforeLongPress = true
                                        longPressJob.cancel()
                                    }
                                } else {
                                    change.consume()
                                    val rootPos = buttonCoords?.let { coords ->
                                        if (coords.isAttached) coords.localToRoot(change.position) else null
                                    }
                                    if (rootPos != null) onDragRootState.value(rootPos)
                                }
                            }
                            longPressJob.cancel()
                            localPressed = false
                        }
                    }
                } else Modifier
            )
            .testTag("btn_symbol_key"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )
        Text(
            text = text,
            color = Color(theme.specialKeyTextColor),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = HindSiliguri
        )
    }
}

/**
 * Text-based Functional Key Button (?123, =\<, ABC)
 */
@Composable
fun RowScope.SpecialKeyTextButton(
    text: String,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    surfaceState: KeyboardSurfaceTouchState? = null,
    surfaceKeyId: String? = null,
    surfaceRowIndex: Int = 3,
    isBottomRow: Boolean = false,
    onClick: () -> Unit
) {
    var localPressed by remember { mutableStateOf(false) }
    val isPressed = if (surfaceState != null && surfaceKeyId != null) {
        surfaceState.isPressed(surfaceKeyId)
    } else {
        localPressed
    }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val onClickState = rememberUpdatedState(onClick)

    SurfaceKeyRegistration(
        surfaceState = surfaceState,
        keyId = surfaceKeyId,
        onTap = { onClickState.value() }
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .registerSurfaceBounds(
                surfaceState = surfaceState,
                keyId = surfaceKeyId,
                category = KeyHitCategory.SYMBOL_MODE,
                rowIndex = surfaceRowIndex,
                isBottomRow = isBottomRow,
                onTap = { onClickState.value() }
            )
            .shadow(elevation, shape)
            .clip(shape)
            .drawBehind {
                val rimHeight = 2.dp.toPx()
                drawRoundRect(
                    color = Color(theme.keyBottomRimColor),
                    topLeft = Offset(0f, size.height - rimHeight),
                    size = Size(size.width, rimHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .background(
                if (isPressed) Color(theme.pressedKeyBackgroundColor)
                else Color(theme.specialKeyBackgroundColor)
            )
            .border(
                width = theme.borderWidthDp.dp,
                color = Color(theme.specialKeyBorderColor),
                shape = shape
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick {
                    onClickState.value()
                    true
                }
            }
            .then(
                if (surfaceState == null) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            localPressed = true
                            val up = waitForUpOrCancellation()
                            localPressed = false
                            if (up != null && up.id == down.id && !up.isConsumed) {
                                onClickState.value()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.22f))
        )
        Text(
            text = text,
            color = Color(theme.specialKeyTextColor),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = HindSiliguri
        )
    }
}


