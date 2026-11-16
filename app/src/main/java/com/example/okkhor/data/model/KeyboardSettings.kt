package com.example.okkhor.data.model

enum class OneHandedMode {
    OFF,
    LEFT,
    RIGHT
}

enum class ClipboardRetentionOption(
    val hours: Int,
    val labelEn: String,
    val labelBn: String
) {
    ONE_HOUR(1, "1 hour", "১ ঘণ্টা"),
    TWENTY_FOUR_HOURS(24, "24 hours", "২৪ ঘণ্টা"),
    SEVEN_DAYS(168, "7 days", "৭ দিন"),
    THIRTY_DAYS(720, "30 days", "৩০ দিন"),
    NEVER(-1, "Never (Permanent)", "কখনোই নয়");

    companion object {
        fun fromHours(hours: Int): ClipboardRetentionOption {
            return entries.find { it.hours == hours } ?: TWENTY_FOUR_HOURS
        }
    }
}

enum class ErgonomicsPreset(
    val id: String,
    val label: String,
    val description: String,
    val heightPercent: Int,
    val keySpacingDp: Float
) {
    COMPACT_SPEED("compact_speed", "Compact Speed", "94% height • 3.5dp gap for fast thumb reach", 94, 3.5f),
    BALANCED("balanced", "Balanced Standard", "100% height • 4.5dp gap for daily 40+ WPM accuracy", 100, 4.5f),
    SPACIOUS_COMFORT("spacious_comfort", "Spacious Comfort", "110% height • 5.5dp gap to prevent adjacent typos", 110, 5.5f)
}

data class KeyboardSettings(
    val selectedThemeId: String = KeyboardTheme.OkkhorEmerald.id,
    val defaultInputMode: InputMode = InputMode.BANGLA_PHONETIC,
    val showNumberRow: Boolean = true,
    val showSuggestions: Boolean = true,
    val autoCorrect: Boolean = false,
    val nextWordPredictionEnabled: Boolean = true,
    val keySpacingDpOverride: Float = 4.5f,
    val hapticFeedback: Boolean = true,
    val soundFeedback: Boolean = true,
    val keyPopupPreview: Boolean = true,
    val spacebarCursorSwipe: Boolean = true,
    val backspaceSwipeDelete: Boolean = true,
    val keyboardHeightPercent: Int = 100, // 75% to 140%
    val keyboardWidthPercent: Int = 100,  // 75% to 100%
    val clipboardHistoryEnabled: Boolean = true,
    val clipboardRetentionHours: Int = 24, // 1, 24, 168 (7d), 720 (30d), -1 (Never)
    val showUndoButton: Boolean = true,
    val voiceTypingEnabled: Boolean = true,
    val gifSearchEnabled: Boolean = true,
    val roundedPillKeys: Boolean = false,
    val oneHandedMode: OneHandedMode = OneHandedMode.OFF,
    val longPressSymbolsEnabled: Boolean = true,
    val smartDoubleSpacePeriod: Boolean = true,
    val recentEmojiDockCount: Int = 8,
    val spacebarLanguageSwipe: Boolean = true,
    val hapticKeypress: Boolean = true,
    val hapticLongPress: Boolean = true,
    val hapticGestureComplete: Boolean = true,
    val hapticDockOpen: Boolean = true,
    val hapticLanguageSwitch: Boolean = true,
    val hapticIntensityPercent: Int = 70,
    val glideTypingEnabled: Boolean = true,
    val floatingKeyboardMode: Boolean = false,
    val floatingOffsetX: Float = 0f,
    val floatingOffsetY: Float = 0f,
    val customTranslationUrl: String = "",
    val customThemeJson: String = "",
    val shortcutGesturesEnabled: Boolean = true,
    val shortcutCopyEnabled: Boolean = true,
    val shortcutPasteEnabled: Boolean = true,
    val shortcutCutEnabled: Boolean = true,
    val shortcutSelectAllEnabled: Boolean = true,
    val shortcutUndoEnabled: Boolean = true,
    val shortcutRedoEnabled: Boolean = true,
    val shortcutSelectWordEnabled: Boolean = true,
    val shortcutDeleteWordEnabled: Boolean = true,
    val shortcutPrevWordEnabled: Boolean = true,
    val shortcutNextWordEnabled: Boolean = true,
    val shortcutLineStartEnabled: Boolean = true,
    val shortcutLineEndEnabled: Boolean = true,
    val shortcutClipboardDockEnabled: Boolean = true,
    val shortcutLongPressDurationMs: Long = 1500L,
    val shortcutHapticEnabled: Boolean = true
)

data class ShortcutItem(
    val trigger: String,
    val expansion: String
)

data class ClipboardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
