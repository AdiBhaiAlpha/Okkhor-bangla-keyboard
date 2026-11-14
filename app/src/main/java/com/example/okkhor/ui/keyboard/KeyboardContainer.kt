package com.example.okkhor.ui.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.ClipboardRetentionOption
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.data.model.OneHandedMode
import com.example.okkhor.ime.VoiceInputState
import com.example.ui.theme.HindSiliguri
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * AAA Premium Okkhor Bangla Keyboard Container
 *
 * Implements:
 * 1. Immediate-response backspace with rapid auto-repeat on hold
 * 2. Smart CapsLock: single-tap capitalizes next letter, double-tap locks Caps Lock
 * 3. Compact true One-Handed mode (74% keyboard with enclosed candidate bar + 26% utility side rail)
 * 4. Glassmorphism rounded pill candidate bar & keys (Image 1)
 * 5. Gboard-style 4x3 Quick Tools Grid Drawer (Image 2)
 * 6. Text Editing Controller with D-Pad & clipboard actions
 * 7. In-keyboard clipboard history snippet panel
 * 8. Quick height resize toolbar
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
    voiceState: VoiceInputState = VoiceInputState.Idle,
    onUndoClick: () -> Unit = {},
    onToggleVoice: () -> Unit = {},
    onStopVoice: () -> Unit = {},
    onDismissVoice: () -> Unit = {},
    onGrantMicPermission: () -> Unit = {},
    enterActionLabel: String = "Enter",
    clipboardItems: List<ClipboardItem> = emptyList(),
    onTogglePinClipboard: (String) -> Unit = {},
    onRemoveClipboardItem: (String) -> Unit = {},
    onClearClipboard: () -> Unit = {},
    onUpdateOneHandedMode: (OneHandedMode) -> Unit = {},
    onUpdateHeightPercent: (Int) -> Unit = {},
    onCycleTheme: () -> Unit = {},
    onUpdateSettings: ((KeyboardSettings) -> KeyboardSettings) -> Unit = {},
    onTextEditAction: (String) -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    val isOneHanded = settings.oneHandedMode != OneHandedMode.OFF
    val scaleMultiplier = if (isOneHanded) 0.90f else 1.0f
    val baseKeyHeight = (54 * (settings.keyboardHeightPercent / 100f) * scaleMultiplier).dp
    val cornerRadius = if (settings.roundedPillKeys) {
        (baseKeyHeight * 0.40f).coerceAtLeast(18.dp)
    } else {
        theme.keyCornerRadiusDp.dp
    }
    val elevation = theme.keyElevationDp.dp
    val spacing = if (isOneHanded) 2.5.dp else theme.keySpacingDp.dp

    // Sub-panel states
    var showToolsDrawer by remember { mutableStateOf(false) }
    var showClipboardPanel by remember { mutableStateOf(false) }
    var showTextEditingPanel by remember { mutableStateOf(false) }
    var showResizeBar by remember { mutableStateOf(false) }

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

    LaunchedEffect(inputSessionKey) {
        showToolsDrawer = false
        showClipboardPanel = false
        showTextEditingPanel = false
        showResizeBar = false
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
        if (settings.hapticFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (settings.soundFeedback) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.7f)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun playSpecialFeedback() {
        if (settings.hapticFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        if (settings.soundFeedback) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.85f)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    // Content lambda that renders Candidate Bar + Keys/Panels
    @Composable
    fun KeyboardMainBlock(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            // --- 1. Candidate Suggestion Strip & Quick Toolbar (Pill Styling - Image 1) ---
            CandidateToolbar(
                theme = theme,
                currentMode = currentMode,
                candidates = candidates,
                canUndo = canUndo,
                showMicButton = settings.voiceTypingEnabled,
                isVoiceListening = voiceState is VoiceInputState.Listening,
                onCandidateClick = { candidate ->
                    playFeedback()
                    onCandidateClick(candidate)
                },
                onUndoClick = {
                    playSpecialFeedback()
                    onUndoClick()
                },
                onToggleVoice = {
                    playSpecialFeedback()
                    onToggleVoice()
                },
                onModeSwitch = {
                    playFeedback()
                    val next = if (!currentMode.isTextMode) {
                        rememberedTextMode
                    } else {
                        nextLanguageMode()
                    }
                    onModeSwitch(next)
                },
                onOpenTools = {
                    playFeedback()
                    showToolsDrawer = !showToolsDrawer
                    showClipboardPanel = false
                    showTextEditingPanel = false
                },
                onOpenClipboard = {
                    playFeedback()
                    showClipboardPanel = !showClipboardPanel
                    showToolsDrawer = false
                    showTextEditingPanel = false
                    if (showClipboardPanel) {
                        onOpenClipboard()
                    }
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
                    // Gboard-Style 4x3 Quick Tools Grid Drawer (Image 2)
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
                            showResizeBar = !showResizeBar
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
                    // Text Editing Controller Panel with D-Pad
                    TextEditingPanel(
                        theme = theme,
                        height = (baseKeyHeight * 4.35f),
                        onClose = { showTextEditingPanel = false },
                        onCursorMove = { delta ->
                            playFeedback()
                            onCursorMove(delta)
                        },
                        onActionKey = { code ->
                            playFeedback()
                            onActionKey(code)
                        },
                        onTextEditAction = { action ->
                            playSpecialFeedback()
                            onTextEditAction(action)
                        }
                    )
                }

                else -> {
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
                        playFeedback = { playFeedback() },
                        playSpecialFeedback = { playSpecialFeedback() },
                        onKeyPress = onKeyPress,
                        onActionKey = onActionKey,
                        onModeSwitch = onModeSwitch,
                        onCursorMove = onCursorMove
                    )
                }
            }
        }
    }

    // Outer Layout with Responsive Rounded Top Corners & Proper Clipping
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val topCornerRadius = (maxWidth * 0.065f).coerceIn(22.dp, 28.dp)
        val containerShape = RoundedCornerShape(
            topStart = topCornerRadius,
            topEnd = topCornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        val oneHandedInnerShape = RoundedCornerShape(
            topStart = (topCornerRadius - 4.dp).coerceAtLeast(18.dp),
            topEnd = (topCornerRadius - 4.dp).coerceAtLeast(18.dp),
            bottomStart = 14.dp,
            bottomEnd = 14.dp
        )
        val outerBorderColor = Color(theme.keyBorderColor).copy(alpha = 0.45f)

        when (settings.oneHandedMode) {
            OneHandedMode.OFF -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Color(theme.backgroundColor))
                        .border(1.dp, outerBorderColor, containerShape)
                        .navigationBarsPadding()
                        .testTag("keyboard_container")
                ) {
                    KeyboardMainBlock()
                }
            }

            OneHandedMode.LEFT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Color(theme.backgroundColor))
                        .border(1.dp, outerBorderColor, containerShape)
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("keyboard_container_left"),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.74f)
                            .clip(oneHandedInnerShape)
                            .background(Color(theme.backgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor).copy(alpha = 0.55f), oneHandedInnerShape)
                    ) {
                        KeyboardMainBlock()
                    }

                    OneHandedSideRail(
                        theme = theme,
                        isLeft = true,
                        onExpand = { onUpdateOneHandedMode(OneHandedMode.OFF) },
                        onSwitchSide = { onUpdateOneHandedMode(OneHandedMode.RIGHT) },
                        onResize = { showResizeBar = !showResizeBar },
                        modifier = Modifier.weight(0.26f)
                    )
                }
            }

            OneHandedMode.RIGHT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Color(theme.backgroundColor))
                        .border(1.dp, outerBorderColor, containerShape)
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
                        onResize = { showResizeBar = !showResizeBar },
                        modifier = Modifier.weight(0.26f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(0.74f)
                            .clip(oneHandedInnerShape)
                            .background(Color(theme.backgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor).copy(alpha = 0.55f), oneHandedInnerShape)
                    ) {
                        KeyboardMainBlock()
                    }
                }
            }
        }
    }
}

/**
 * Top Candidate Toolbar with Glassmorphic Pill styling (Image 1)
 */
@Composable
private fun CandidateToolbar(
    theme: KeyboardTheme,
    currentMode: InputMode,
    candidates: List<String>,
    canUndo: Boolean,
    showMicButton: Boolean,
    isVoiceListening: Boolean,
    onCandidateClick: (String) -> Unit,
    onUndoClick: () -> Unit,
    onToggleVoice: () -> Unit,
    onModeSwitch: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit
) {
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
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick Tools Launcher Icon (Gboard 4-box / Grid icon - Image 2)
        IconButton(
            onClick = onOpenTools,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Quick Tools",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Mode Switch Pill
        val modeChipLabel = when (currentMode) {
            InputMode.BANGLA_PHONETIC -> "ফোনেটিক"
            InputMode.BANGLA_FIXED -> "প্রভাত"
            InputMode.ENGLISH -> "ENG"
            InputMode.NUMBERS_SYMBOLS -> "?123"
            InputMode.SECONDARY_SYMBOLS -> "=\\<"
            InputMode.EMOJI -> "Emoji"
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(theme.accentColor).copy(alpha = 0.16f))
                .border(1.dp, Color(theme.accentColor).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clickable { onModeSwitch() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = modeChipLabel,
                color = Color(theme.accentColor),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguri
            )
        }

        // Proactive 1-Tap Undo Pill (inspired by PixelBoard suggestion strip undo)
        if (canUndo) {
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(theme.accentColor).copy(alpha = 0.2f))
                    .border(1.dp, Color(theme.accentColor).copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                    .clickable { onUndoClick() }
                    .padding(horizontal = 8.dp)
                    .testTag("btn_suggestion_undo"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = Color(theme.accentColor),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Undo",
                    color = Color(theme.accentColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Candidate suggestions list with Glassmorphic Pill styling (Image 1)
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (candidates.isEmpty()) {
                item {
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
                itemsIndexed(candidates) { index, candidate ->
                    val isFirst = index == 0
                    val pillShape = RoundedCornerShape(20.dp)

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(pillShape)
                            .background(
                                if (isFirst) Color(theme.candidateHighlightColor)
                                else Color(theme.keyBackgroundColor).copy(alpha = 0.88f)
                            )
                            .border(
                                width = if (isFirst) 0.dp else 1.dp,
                                color = Color(theme.keyBorderColor).copy(alpha = 0.65f),
                                shape = pillShape
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isVoiceListening) Color(theme.accentColor).copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("btn_voice_mic")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Typing",
                    tint = if (isVoiceListening) Color(theme.accentColor) else Color(theme.candidateBarTextColor).copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Quick Clipboard snippet button
        IconButton(
            onClick = onOpenClipboard,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "Clipboard",
                tint = Color(theme.candidateBarTextColor).copy(alpha = 0.82f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Quick Theme switcher button
        IconButton(
            onClick = onOpenThemes,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Themes",
                tint = Color(theme.candidateBarTextColor).copy(alpha = 0.82f),
                modifier = Modifier.size(17.dp)
            )
        }

        // Companion App Settings button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(30.dp)
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

/**
 * Gboard-Style 4x3 Quick Tools Grid Drawer (Direct benchmark from Image 2)
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
    onOpenSettings: () -> Unit
) {
    data class ToolItem(
        val titleBn: String,
        val subtitleEn: String,
        val icon: ImageVector,
        val isActive: Boolean = false,
        val onClick: () -> Unit
    )

    val tools = listOf(
        ToolItem("ক্লিপবোর্ড", "Clipboard", Icons.AutoMirrored.Filled.Assignment, onClick = onOpenClipboard),
        ToolItem("ভয়েস টাইপ", "Voice Typing", Icons.Default.Mic, isActive = settings.voiceTypingEnabled, onClick = onStartVoice),
        ToolItem("থিম স্টুডিও", "Themes", Icons.Default.Palette, onClick = onOpenTheme),
        ToolItem("গোল কী", "Rounded Keys", Icons.Default.RoundedCorner, isActive = settings.roundedPillKeys, onClick = onToggleRoundedKeys),
        ToolItem("আকার", "Resize", Icons.Default.AspectRatio, onClick = onToggleResize),
        ToolItem("এক হাত", "One-handed", Icons.Default.StayCurrentPortrait, isActive = settings.oneHandedMode != OneHandedMode.OFF, onClick = onToggleOneHanded),
        ToolItem("টেক্সট এডিট", "Text editing", Icons.Default.TextFields, onClick = onOpenTextEditing),
        ToolItem("লেআউট", "Layout", Icons.Default.Language, onClick = onSwitchLayout),
        ToolItem("সংখ্যা সারি", "Number Row", Icons.Default.Tune, isActive = settings.showNumberRow, onClick = onToggleNumberRow),
        ToolItem("শব্দ ও ভাইব", "Sound & Haptic", Icons.AutoMirrored.Filled.VolumeUp, isActive = settings.soundFeedback, onClick = onToggleSound),
        ToolItem("স্মার্ট প্রেডিকশন", "Suggestions", Icons.Default.Search, isActive = settings.showSuggestions, onClick = onToggleSuggestions),
        ToolItem("অ্যাপ সেটিংস", "Settings", Icons.Default.Settings, onClick = onOpenSettings)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(theme.backgroundColor))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Drawer Header with Return arrow & quick actions (Image 2)
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
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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
    onClose: () -> Unit,
    onCursorMove: (Int) -> Unit,
    onActionKey: (Int) -> Unit,
    onTextEditAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(theme.backgroundColor))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "টেক্সট এডিটিং কন্ট্রোলার • Text Editing",
                    color = Color(theme.keyTextColor),
                    fontSize = 13.5.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.Bold
                )
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

        // Quick Clipboard Actions Row (Select All, Cut, Copy, Paste)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val actions = listOf(
                "সব নির্বাচন" to "SELECT_ALL",
                "কাট" to "CUT",
                "কপি" to "COPY",
                "পেস্ট" to "PASTE"
            )

            actions.forEach { (label, actionCode) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(8.dp))
                        .clickable { onTextEditAction(actionCode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color(theme.keyTextColor),
                        fontSize = 12.sp,
                        fontFamily = HindSiliguri,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Ergonomic D-Pad Cursor Navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .clickable { onTextEditAction("CURSOR_UP") },
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
                    // LEFT
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onCursorMove(-1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Left",
                            tint = Color(theme.accentColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Backspace in center
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.specialKeyBackgroundColor))
                            .border(1.dp, Color(theme.specialKeyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onActionKey(KeyboardLayouts.CODE_DELETE) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color(theme.specialKeyTextColor),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // RIGHT
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                            .clickable { onCursorMove(1) },
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
                        .clickable { onTextEditAction("CURSOR_DOWN") },
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
    playFeedback: () -> Unit,
    playSpecialFeedback: () -> Unit,
    onKeyPress: (String) -> Unit,
    onActionKey: (Int) -> Unit,
    onModeSwitch: (InputMode) -> Unit,
    onCursorMove: (Int) -> Unit
) {
    Column {
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

                // Row 1 (Q W E R T Y U I O P)
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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
                            }
                        )
                    }
                }

                // Row 2 (A S D F G H J K L)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
                            }
                        )
                    }
                }

                // Row 3 (Shift + Z X C V B N M + Backspace)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Button with CapsLock support
                    ShiftKeyButton(
                        isShifted = isShifted,
                        isCapsLock = isCapsLock,
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
                            }
                        )
                    }

                    // Backspace with immediate single-click delete
                    RepeatableSpecialKeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = Color(theme.specialKeyTextColor),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        },
                        onSwipeLeft = {
                            playSpecialFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        }
                    )
                }
            }

            InputMode.ENGLISH -> {
                val r1 = KeyboardLayouts.ENGLISH_ROW_1
                val r2 = KeyboardLayouts.ENGLISH_ROW_2
                val r3 = KeyboardLayouts.ENGLISH_ROW_3

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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
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
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
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
                            onClick = {
                                playFeedback()
                                onKeyPress(char)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                if (secondary.isNotEmpty()) onKeyPress(secondary)
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
                        weight = 1.35f,
                        height = baseKeyHeight,
                        theme = theme,
                        cornerRadius = cornerRadius,
                        elevation = elevation,
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        },
                        onSwipeLeft = {
                            playSpecialFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        }
                    )
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
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                onKeyPress(secondary)
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
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                onKeyPress(secondary)
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
                        onClick = {
                            playSpecialFeedback()
                            onActionKey(KeyboardLayouts.CODE_SHIFT)
                        }
                    )

                    for (idx in r3Norm.indices) {
                        val normChar = r3Norm[idx]
                        val shiftChar = r3Shift.getOrElse(idx) { "" }
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
                            onClick = {
                                playFeedback()
                                onKeyPress(primary)
                            },
                            onLongClick = {
                                playSpecialFeedback()
                                onKeyPress(secondary)
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
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        },
                        onSwipeLeft = {
                            playSpecialFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
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
                    KeyboardLayouts.SYMBOLS_ROW_2.forEach { sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
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
                        onClick = {
                            playSpecialFeedback()
                            onModeSwitch(InputMode.SECONDARY_SYMBOLS)
                        }
                    )

                    KeyboardLayouts.SYMBOLS_ROW_3.forEach { sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
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
                    KeyboardLayouts.MORE_SYMBOLS_ROW_1.forEach { sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
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
                    KeyboardLayouts.MORE_SYMBOLS_ROW_2.forEach { sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
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
                        onClick = {
                            playSpecialFeedback()
                            onModeSwitch(InputMode.NUMBERS_SYMBOLS)
                        }
                    )

                    KeyboardLayouts.MORE_SYMBOLS_ROW_3.forEach { sym ->
                        KeyButton(
                            primary = sym,
                            weight = 1f,
                            height = baseKeyHeight,
                            theme = theme,
                            cornerRadius = cornerRadius,
                            elevation = elevation,
                            showPopupPreview = settings.keyPopupPreview,
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
                        onClick = {
                            playFeedback()
                            onActionKey(KeyboardLayouts.CODE_DELETE)
                        }
                    )
                }
            }

            InputMode.EMOJI -> {
                EmojiPanel(
                    emojis = KeyboardLayouts.EMOJI_LIST,
                    height = (baseKeyHeight * 4.25f),
                    theme = theme,
                    lastTextMode = lastTextMode,
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    onEmojiClick = { emoji ->
                        playFeedback()
                        onKeyPress(emoji)
                    },
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
                cornerRadius = cornerRadius,
                elevation = elevation,
                enterActionLabel = enterActionLabel,
                spacebarSwipeEnabled = settings.spacebarCursorSwipe,
                onSymbolToggle = {
                    playSpecialFeedback()
                    if (currentMode == InputMode.NUMBERS_SYMBOLS || currentMode == InputMode.SECONDARY_SYMBOLS) {
                        onModeSwitch(lastTextMode)
                    } else {
                        onModeSwitch(InputMode.NUMBERS_SYMBOLS)
                    }
                },
                onEmojiClick = {
                    playSpecialFeedback()
                    onModeSwitch(InputMode.EMOJI)
                },
                onLanguageToggle = {
                    playSpecialFeedback()
                    val base = if (currentMode.isTextMode) currentMode else lastTextMode
                    val next = when (base) {
                        InputMode.BANGLA_PHONETIC -> InputMode.BANGLA_FIXED
                        InputMode.BANGLA_FIXED -> InputMode.ENGLISH
                        InputMode.ENGLISH -> InputMode.BANGLA_PHONETIC
                        else -> lastTextMode
                    }
                    onModeSwitch(next)
                },
                onSpaceClick = {
                    playFeedback()
                    onActionKey(KeyboardLayouts.CODE_SPACE)
                },
                onCursorSwipe = { delta ->
                    onCursorMove(delta)
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
}

/**
 * Shift Keycap with Single-Tap Temporary Upper and Double-Tap CapsLock Lock
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
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)

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
            .clickable { onClick() },
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

/**
 * Bottom Control Row with ?123, Emoji, Language, Spacebar, Dot/Dari, and Action Key
 */
@Composable
private fun BottomControlRow(
    settings: KeyboardSettings,
    theme: KeyboardTheme,
    currentMode: InputMode,
    baseHeight: Dp,
    cornerRadius: Dp,
    elevation: Dp,
    enterActionLabel: String,
    spacebarSwipeEnabled: Boolean,
    onSymbolToggle: () -> Unit,
    onEmojiClick: () -> Unit,
    onLanguageToggle: () -> Unit,
    onSpaceClick: () -> Unit,
    onCursorSwipe: (Int) -> Unit,
    onDotClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var isSpacePressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(theme.keySpacingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val symbolLabel = if (currentMode == InputMode.NUMBERS_SYMBOLS || currentMode == InputMode.SECONDARY_SYMBOLS) "ABC" else "?123"
        SpecialKeyTextButton(
            text = symbolLabel,
            weight = 1.35f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            onClick = onSymbolToggle
        )

        SpecialKeyButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = "Emoji",
                    tint = Color(theme.specialKeyTextColor),
                    modifier = Modifier.size(20.dp)
                )
            },
            weight = 1.05f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            onClick = onEmojiClick
        )

        SpecialKeyButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = Color(theme.accentColor),
                    modifier = Modifier.size(20.dp)
                )
            },
            weight = 1.05f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            onClick = onLanguageToggle
        )

        val spaceLabel = when (currentMode) {
            InputMode.BANGLA_PHONETIC -> "বাংলা ফোনেটিক"
            InputMode.BANGLA_FIXED -> "বাংলা প্রভাত"
            InputMode.ENGLISH -> "English"
            else -> "Space"
        }

        val spaceShape = RoundedCornerShape(cornerRadius)

        // Elongated Capsule Spacebar (Image 1)
        Box(
            modifier = Modifier
                .weight(4.0f)
                .height(baseHeight)
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
                    if (isSpacePressed) Color(theme.pressedKeyBackgroundColor)
                    else Color(theme.keyBackgroundColor)
                )
                .border(
                    width = theme.borderWidthDp.dp,
                    color = Color(theme.keyBorderColor),
                    shape = spaceShape
                )
                .pointerInput(spacebarSwipeEnabled) {
                    detectDragGestures(
                        onDragStart = { isSpacePressed = true },
                        onDragEnd = {
                            isSpacePressed = false
                            accumulatedDrag = 0f
                        },
                        onDragCancel = {
                            isSpacePressed = false
                            accumulatedDrag = 0f
                        },
                        onDrag = { change, dragAmount ->
                            if (spacebarSwipeEnabled) {
                                change.consume()
                                accumulatedDrag += dragAmount.x
                                val threshold = 32f
                                if (abs(accumulatedDrag) >= threshold) {
                                    val direction = if (accumulatedDrag > 0) 1 else -1
                                    onCursorSwipe(direction)
                                    accumulatedDrag = 0f
                                }
                            }
                        }
                    )
                }
                .clickable { onSpaceClick() },
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
                if (spacebarSwipeEnabled) {
                    Text(
                        text = "‹",
                        color = Color(theme.keySecondaryTextColor).copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = spaceLabel,
                    color = Color(theme.keySecondaryTextColor),
                    fontSize = 13.sp,
                    fontFamily = HindSiliguri,
                    fontWeight = FontWeight.Medium
                )
                if (spacebarSwipeEnabled) {
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
            weight = 1.05f,
            height = baseHeight,
            theme = theme,
            cornerRadius = cornerRadius,
            elevation = elevation,
            showPopupPreview = settings.keyPopupPreview,
            onClick = onDotClick
        )

        val enterShape = RoundedCornerShape(cornerRadius)
        var isEnterPressed by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .weight(1.45f)
                .height(baseHeight)
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
                .clickable { onEnterClick() },
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
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        label = "key_scale"
    )

    val bg = when {
        isPressed -> Color(theme.pressedKeyBackgroundColor)
        isSpecial -> Color(theme.specialKeyBackgroundColor)
        else -> Color(theme.keyBackgroundColor)
    }

    val borderClr = when {
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
            .scale(scale)
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
            .pointerInput(primary, secondary) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        if (onLongClick != null) {
                            onLongClick()
                        } else {
                            onClick()
                        }
                    }
                )
            }
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
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(theme.popupBackgroundColor))
                    .border(1.2.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp)),
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
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)

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
        modifier = Modifier
            .weight(weight)
            .height(height)
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .clickable { onClick() },
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
    onClick: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true

                    // 1. Single tap executes IMMEDIATELY on touch down!
                    onClick()

                    var dragX = 0f
                    var hasSwiped = false

                    // 2. Launch auto-repeat if held beyond threshold
                    val repeatJob = coroutineScope.launch {
                        delay(380)
                        while (isActive && !hasSwiped) {
                            onClick()
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
                        if (dragX < -45f && onSwipeLeft != null && !hasSwiped) {
                            hasSwiped = true
                            repeatJob.cancel()
                            onSwipeLeft()
                            dragX = 0f
                        }
                    }

                    repeatJob.cancel()
                    isPressed = false
                }
            },
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
 * Text-based Functional Key Button (?123, =\\<, ABC)
 */
@Composable
fun RowScope.SpecialKeyTextButton(
    text: String,
    weight: Float,
    height: Dp,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    elevation: Dp,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .clickable { onClick() },
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
 * High-fidelity Scrollable Emoji Panel with bottom ABC (lastTextMode) & Backspace controls
 */
@Composable
private fun EmojiPanel(
    emojis: List<String>,
    height: Dp,
    theme: KeyboardTheme,
    lastTextMode: InputMode,
    cornerRadius: Dp,
    elevation: Dp,
    onEmojiClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToText: () -> Unit
) {
    val backLabel = when (lastTextMode) {
        InputMode.ENGLISH -> "ABC (ENG)"
        InputMode.BANGLA_FIXED -> "কখগ (প্রভাত)"
        else -> "কখগ (ফোনেটিক)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ইমোজি • Emojis",
                color = Color(theme.keyTextColor),
                fontSize = 13.sp,
                fontFamily = HindSiliguri,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ফিরুন: ${lastTextMode.shortLabel}",
                color = Color(theme.accentColor),
                fontSize = 12.5.sp,
                fontFamily = HindSiliguri,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBackToText() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 23.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecialKeyTextButton(
                text = backLabel,
                weight = 2.2f,
                height = 42.dp,
                theme = theme,
                cornerRadius = cornerRadius,
                elevation = elevation,
                onClick = onBackToText
            )

            Spacer(modifier = Modifier.weight(1.8f))

            RepeatableSpecialKeyButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color(theme.specialKeyTextColor),
                        modifier = Modifier.size(20.dp)
                    )
                },
                weight = 1.2f,
                height = 42.dp,
                theme = theme,
                cornerRadius = cornerRadius,
                elevation = elevation,
                onClick = onBackspace
            )
        }
    }
}

