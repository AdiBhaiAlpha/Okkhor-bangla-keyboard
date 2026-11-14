package com.example.okkhor.ui.keyboard.gesture

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings

enum class FloatingDockType {
    NONE,
    EMOJI_QUICK_DOCK,
    CLIPBOARD_QUICK_DOCK,
    SYMBOL_QUICK_DOCK,
    LANGUAGE_SWITCHER,
    ALTERNATE_CHARS_DOCK,
    TRANSLATION_DOCK,
    DATE_TIME_DOCK
}

enum class GesturePriority(val rank: Int) {
    IDLE(0),
    TAP(1),
    GLIDE_TYPING(2),
    SHIFT_DRAG(3),
    BACKSPACE_SWIPE(4),
    SPACEBAR_CURSOR(5),
    SPACEBAR_LANGUAGE(6),
    DOCK_DRAG(7),
    RESIZE_OR_FLOAT_DRAG(8)
}

enum class GestureMode {
    IDLE,
    EMOJI_DOCK_DRAG,
    CLIPBOARD_DOCK_DRAG,
    SYMBOL_DOCK_DRAG,
    SHIFT_KEY_DRAG,
    SPACEBAR_LANGUAGE_SWIPE,
    SPACEBAR_CURSOR_SWIPE,
    BACKSPACE_WORD_SWIPE,
    GLIDE_TYPING,
    FLOATING_KEYBOARD_DRAG,
    CANCELLED
}

data class GestureState(
    val mode: GestureMode = GestureMode.IDLE,
    val activePointerId: Long = -1L,
    val startPosition: Offset = Offset.Zero,
    val currentPosition: Offset = Offset.Zero,
    val priority: GesturePriority = GesturePriority.IDLE
)

sealed interface GestureAction {
    data class InsertText(val text: String) : GestureAction
    data class InsertEmoji(val emoji: String) : GestureAction
    data class PreviewLanguage(val mode: InputMode, val step: Int) : GestureAction
    data class CommitLanguage(val mode: InputMode) : GestureAction
    data class MoveCursor(val delta: Int) : GestureAction
    data object DeleteWord : GestureAction
    data class OpenDock(val dockType: FloatingDockType) : GestureAction
    data object DismissDock : GestureAction
    data object Cancel : GestureAction
}

enum class HapticEventType {
    KEYPRESS,
    LONG_PRESS,
    GESTURE_COMPLETE,
    DOCK_OPEN,
    LANGUAGE_SWITCH
}

/**
 * Granular haptic feedback controller respecting individual event toggles & intensity.
 */
class HapticFeedbackController(context: Context) {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun trigger(view: View, settings: KeyboardSettings, event: HapticEventType) {
        if (!settings.hapticFeedback) return
        val enabled = when (event) {
            HapticEventType.KEYPRESS -> settings.hapticKeypress
            HapticEventType.LONG_PRESS -> settings.hapticLongPress
            HapticEventType.GESTURE_COMPLETE -> settings.hapticGestureComplete
            HapticEventType.DOCK_OPEN -> settings.hapticDockOpen
            HapticEventType.LANGUAGE_SWITCH -> settings.hapticLanguageSwitch
        }
        if (!enabled) return

        val intensity = settings.hapticIntensityPercent.coerceIn(10, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                val baseDuration = when (event) {
                    HapticEventType.KEYPRESS -> 10L
                    HapticEventType.LONG_PRESS -> 20L
                    HapticEventType.GESTURE_COMPLETE -> 16L
                    HapticEventType.DOCK_OPEN -> 18L
                    HapticEventType.LANGUAGE_SWITCH -> 15L
                }
                val amplitude = ((intensity / 100f) * 200f).toInt().coerceIn(15, 255)
                val emitted = runCatching {
                    vib.vibrate(VibrationEffect.createOneShot(baseDuration, amplitude))
                    true
                }.getOrDefault(false)
                if (emitted) return
            }
        }

        val constant = when (event) {
            HapticEventType.KEYPRESS -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticEventType.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
            HapticEventType.GESTURE_COMPLETE -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticEventType.DOCK_OPEN -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticEventType.LANGUAGE_SWITCH -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }
}

/**
 * Coordinates mutually exclusive gestures and active floating docks across the keyboard surface.
 */
@Stable
class KeyboardGestureManager {
    var gestureState by mutableStateOf(GestureState())
        private set

    var activeDock by mutableStateOf(FloatingDockType.NONE)
        private set

    var highlightedDockTarget by mutableStateOf<String?>(null)
        private set

    var languageBaseMode by mutableStateOf(InputMode.BANGLA_PHONETIC)
        private set

    var languagePreviewStep by mutableIntStateOf(0)
        private set

    var languageDragProgress by mutableFloatStateOf(0f)
        private set

    var isCursorHoldActive by mutableStateOf(false)
        private set

    var activeAlternateKey by mutableStateOf("")
        private set

    var alternateOptions by mutableStateOf<List<String>>(emptyList())
        private set

    val isBackdropBlurActive: Boolean
        get() = activeDock != FloatingDockType.NONE

    fun canBeginGesture(requestedPriority: GesturePriority): Boolean {
        val current = gestureState.priority
        return gestureState.mode == GestureMode.IDLE || requestedPriority.rank >= current.rank
    }

    fun beginGesture(
        mode: GestureMode,
        priority: GesturePriority,
        pointerId: Long = -1L,
        startPos: Offset = Offset.Zero
    ): Boolean {
        if (!canBeginGesture(priority)) return false
        gestureState = GestureState(
            mode = mode,
            activePointerId = pointerId,
            startPosition = startPos,
            currentPosition = startPos,
            priority = priority
        )
        return true
    }

    fun updatePointer(pos: Offset) {
        gestureState = gestureState.copy(currentPosition = pos)
    }

    fun openDock(dockType: FloatingDockType, initialHighlight: String? = null) {
        activeDock = dockType
        highlightedDockTarget = initialHighlight
    }

    fun openAlternateCharsDock(baseKey: String, options: List<String>) {
        if (options.isEmpty()) return
        activeAlternateKey = baseKey
        alternateOptions = options
        openDock(FloatingDockType.ALTERNATE_CHARS_DOCK, options.firstOrNull())
    }

    fun updateDockHighlight(target: String?) {
        highlightedDockTarget = target
    }

    fun startLanguagePreview(baseMode: InputMode, step: Int, progress: Float) {
        languageBaseMode = baseMode
        languagePreviewStep = step
        languageDragProgress = progress.coerceIn(-1f, 1f)
        activeDock = FloatingDockType.LANGUAGE_SWITCHER
        beginGesture(GestureMode.SPACEBAR_LANGUAGE_SWIPE, GesturePriority.SPACEBAR_LANGUAGE)
    }

    fun updateLanguagePreview(step: Int, progress: Float) {
        languagePreviewStep = step
        languageDragProgress = progress.coerceIn(-1f, 1f)
    }

    fun setCursorHold(active: Boolean) {
        isCursorHoldActive = active
        if (active) {
            beginGesture(GestureMode.SPACEBAR_CURSOR_SWIPE, GesturePriority.SPACEBAR_CURSOR)
        } else if (gestureState.mode == GestureMode.SPACEBAR_CURSOR_SWIPE) {
            finishGesture()
        }
    }

    fun dismissDock() {
        activeDock = FloatingDockType.NONE
        highlightedDockTarget = null
        languagePreviewStep = 0
        languageDragProgress = 0f
        activeAlternateKey = ""
        alternateOptions = emptyList()
    }

    fun finishGesture() {
        gestureState = GestureState()
        isCursorHoldActive = false
    }

    fun cancelAll() {
        dismissDock()
        finishGesture()
    }
}
