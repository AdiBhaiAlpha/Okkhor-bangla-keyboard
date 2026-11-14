package com.example.okkhor.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings

@Composable
fun InputSettingsSection(
    settings: KeyboardSettings,
    onUpdateSettings: ((KeyboardSettings) -> KeyboardSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            onUpdateSettings { it.copy(voiceTypingEnabled = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.input_settings_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Default Input Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pref_default_mode),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                InputModeRadioItem(
                    title = stringResource(R.string.mode_phonetic),
                    selected = settings.defaultInputMode == InputMode.BANGLA_PHONETIC,
                    onClick = {
                        onUpdateSettings { it.copy(defaultInputMode = InputMode.BANGLA_PHONETIC) }
                    }
                )

                InputModeRadioItem(
                    title = stringResource(R.string.mode_fixed),
                    selected = settings.defaultInputMode == InputMode.BANGLA_FIXED,
                    onClick = {
                        onUpdateSettings { it.copy(defaultInputMode = InputMode.BANGLA_FIXED) }
                    }
                )

                InputModeRadioItem(
                    title = stringResource(R.string.mode_english),
                    selected = settings.defaultInputMode == InputMode.ENGLISH,
                    onClick = {
                        onUpdateSettings { it.copy(defaultInputMode = InputMode.ENGLISH) }
                    }
                )
            }
        }

        // Layout & Behavior Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Layout & Typing Behavior",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Number row toggle
                SettingToggleItem(
                    title = stringResource(R.string.pref_show_number_row),
                    description = stringResource(R.string.pref_show_number_row_desc),
                    checked = settings.showNumberRow,
                    tag = "switch_number_row",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(showNumberRow = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Suggestions toggle
                SettingToggleItem(
                    title = stringResource(R.string.pref_show_suggestions),
                    description = stringResource(R.string.pref_show_suggestions_desc),
                    checked = settings.showSuggestions,
                    tag = "switch_suggestions",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(showSuggestions = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Autocorrect toggle
                SettingToggleItem(
                    title = stringResource(R.string.pref_autocorrect),
                    description = stringResource(R.string.pref_autocorrect_desc),
                    checked = settings.autoCorrect,
                    tag = "switch_autocorrect",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(autoCorrect = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Spacebar language switcher swipe
                SettingToggleItem(
                    title = "Spacebar Language Switcher",
                    description = "Swipe left or right on the spacebar to preview and switch between Avro Phonetic, প্রভাত, and English",
                    checked = settings.spacebarLanguageSwipe,
                    tag = "switch_spacebar_language_swipe",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(spacebarLanguageSwipe = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Spacebar cursor swipe
                SettingToggleItem(
                    title = stringResource(R.string.pref_space_swipe),
                    description = if (settings.spacebarLanguageSwipe) {
                        "Hold the spacebar briefly then drag left or right to move the cursor"
                    } else {
                        stringResource(R.string.pref_space_swipe_desc)
                    },
                    checked = settings.spacebarCursorSwipe,
                    tag = "switch_space_swipe",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(spacebarCursorSwipe = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Configurable Recent Emoji Dock count
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recent Emoji Dock Capacity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Number of recent emojis shown when holding the Emoji key (${settings.recentEmojiDockCount})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(4, 6, 8, 10, 12).forEach { count ->
                            FilterChip(
                                selected = settings.recentEmojiDockCount == count,
                                onClick = {
                                    onUpdateSettings { it.copy(recentEmojiDockCount = count) }
                                },
                                label = { Text("$count") },
                                modifier = Modifier.testTag("dock_emoji_count_$count")
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Key popup
                SettingToggleItem(
                    title = stringResource(R.string.pref_key_popup),
                    description = stringResource(R.string.pref_key_popup_desc),
                    checked = settings.keyPopupPreview,
                    tag = "switch_key_popup",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(keyPopupPreview = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Long-press for symbols & numbers
                SettingToggleItem(
                    title = "Long Press for Symbols & Numbers",
                    description = "Hold any key to quickly insert secondary numbers or punctuation",
                    checked = settings.longPressSymbolsEnabled,
                    tag = "switch_long_press_symbols",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(longPressSymbolsEnabled = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Smart Double-space for Dari / Period
                SettingToggleItem(
                    title = "Double Space for Period / Dari (।)",
                    description = "Double-tap spacebar to quickly insert a Dari in Bangla or period in English",
                    checked = settings.smartDoubleSpacePeriod,
                    tag = "switch_double_space_dari",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(smartDoubleSpacePeriod = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Backspace swipe to delete word
                SettingToggleItem(
                    title = "Backspace Swipe to Delete",
                    description = "Swipe left from the backspace key to delete whole words or syllables quickly",
                    checked = settings.backspaceSwipeDelete,
                    tag = "switch_backspace_swipe",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(backspaceSwipeDelete = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Glide Typing
                SettingToggleItem(
                    title = "Glide Typing (স্লাইড টাইপিং)",
                    description = "Slide across QWERTY letter keys continuously to form English or phonetic Bangla words",
                    checked = settings.glideTypingEnabled,
                    tag = "switch_glide_typing",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(glideTypingEnabled = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Floating Keyboard Mode
                SettingToggleItem(
                    title = "Floating Keyboard Mode",
                    description = "Detach keyboard as a movable floating window you can drag freely across the screen",
                    checked = settings.floatingKeyboardMode,
                    tag = "switch_floating_keyboard",
                    onCheckedChange = { checked ->
                        onUpdateSettings {
                            it.copy(
                                floatingKeyboardMode = checked,
                                oneHandedMode = if (checked) com.example.okkhor.data.model.OneHandedMode.OFF else it.oneHandedMode
                            )
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Undo button in suggestion strip
                SettingToggleItem(
                    title = "Suggestion Strip Undo",
                    description = "Show a quick Undo chip after corrections, expansions, or deletions",
                    checked = settings.showUndoButton,
                    tag = "switch_undo_button",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(showUndoButton = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Rounded pill keycaps
                SettingToggleItem(
                    title = "Rounded Pill Keycaps",
                    description = "Use rounded pill-shaped keycaps across the keyboard",
                    checked = settings.roundedPillKeys,
                    tag = "switch_rounded_keys",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(roundedPillKeys = checked) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Voice typing
                SettingToggleItem(
                    title = "Voice Typing",
                    description = "Dictate in Bangla (bn-BD) or English (en-US) from the toolbar microphone",
                    checked = settings.voiceTypingEnabled,
                    tag = "switch_voice_typing",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(voiceTypingEnabled = checked) }
                        if (checked && !hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                if (settings.voiceTypingEnabled && !hasMicPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_grant_mic_permission")
                    ) {
                        Text(
                            text = "Grant Microphone Permission for Voice Typing",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingToggleItem(
                    title = "Online GIF Search & Previews",
                    description = "Allow optional network requests when searching or browsing GIFs. Typed text is never uploaded.",
                    checked = settings.gifSearchEnabled,
                    tag = "switch_gif_search",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(gifSearchEnabled = checked) }
                    }
                )
            }
        }

        // Hold, Drag & Release Text Editing Gestures Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hold, Drag & Release Text Editing Gestures",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Perform advanced text editing shortcuts (Copy, Paste, Cut, Select, Word delete, Line navigation) by holding letter keys (C, V, X, A, Z, R, S, D, B, F, H, L, P) for 1.5 seconds.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SettingToggleItem(
                    title = "Enable Shortcut Gestures",
                    description = "Long-press letter keys to trigger text editing actions",
                    checked = settings.shortcutGesturesEnabled,
                    tag = "switch_shortcut_gestures",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(shortcutGesturesEnabled = checked) }
                    }
                )

                if (settings.shortcutGesturesEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingToggleItem(
                        title = "Copy (C) & Paste (V) Shortcuts",
                        description = "Hold C for Copy, V for Paste",
                        checked = settings.shortcutCopyEnabled && settings.shortcutPasteEnabled,
                        tag = "switch_shortcut_copy_paste",
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(shortcutCopyEnabled = checked, shortcutPasteEnabled = checked) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleItem(
                        title = "Select Word (S) & Delete Word (D)",
                        description = "Hold S to select word at cursor, D to delete word",
                        checked = settings.shortcutSelectWordEnabled && settings.shortcutDeleteWordEnabled,
                        tag = "switch_shortcut_word_actions",
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(shortcutSelectWordEnabled = checked, shortcutDeleteWordEnabled = checked) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleItem(
                        title = "Line Start (H) & Line End (L) Shortcuts",
                        description = "Hold H to jump to start of line, L to jump to end of line",
                        checked = settings.shortcutLineStartEnabled && settings.shortcutLineEndEnabled,
                        tag = "switch_shortcut_line_actions",
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(shortcutLineStartEnabled = checked, shortcutLineEndEnabled = checked) }
                        }
                    )
                }
            }
        }

        // One-Handed Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "One-Handed Mode (এক হাতে টাইপিং)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                InputModeRadioItem(
                    title = "Off (Full Width Keyboard)",
                    selected = settings.oneHandedMode == com.example.okkhor.data.model.OneHandedMode.OFF,
                    onClick = {
                        onUpdateSettings { it.copy(oneHandedMode = com.example.okkhor.data.model.OneHandedMode.OFF) }
                    }
                )

                InputModeRadioItem(
                    title = "Right-Handed Mode (ডানহাতে সুবিধাজনক)",
                    selected = settings.oneHandedMode == com.example.okkhor.data.model.OneHandedMode.RIGHT,
                    onClick = {
                        onUpdateSettings { it.copy(oneHandedMode = com.example.okkhor.data.model.OneHandedMode.RIGHT) }
                    }
                )

                InputModeRadioItem(
                    title = "Left-Handed Mode (বামহাতে সুবিধাজনক)",
                    selected = settings.oneHandedMode == com.example.okkhor.data.model.OneHandedMode.LEFT,
                    onClick = {
                        onUpdateSettings { it.copy(oneHandedMode = com.example.okkhor.data.model.OneHandedMode.LEFT) }
                    }
                )
            }
        }

        // Feedback & Haptics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sound & Haptic Feedback",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Vibration
                SettingToggleItem(
                    title = stringResource(R.string.pref_vibration),
                    description = stringResource(R.string.pref_vibration_desc),
                    checked = settings.hapticFeedback,
                    tag = "switch_haptic",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(hapticFeedback = checked) }
                    }
                )

                if (settings.hapticFeedback) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Haptic Strength (${settings.hapticIntensityPercent}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(30, 50, 70, 100).forEach { pct ->
                            FilterChip(
                                selected = settings.hapticIntensityPercent == pct,
                                onClick = {
                                    onUpdateSettings { it.copy(hapticIntensityPercent = pct) }
                                },
                                label = { Text("$pct%") },
                                modifier = Modifier.testTag("haptic_intensity_$pct")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleItem(
                        title = "Keypress Haptics",
                        description = "Subtle vibration on regular key taps",
                        checked = settings.hapticKeypress,
                        tag = "switch_haptic_keypress",
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(hapticKeypress = checked) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleItem(
                        title = "Long-Press & Dock Haptics",
                        description = "Feedback when opening emoji, clipboard, or symbol floating docks",
                        checked = settings.hapticDockOpen,
                        tag = "switch_haptic_dock_open",
                        onCheckedChange = { checked ->
                            onUpdateSettings {
                                it.copy(hapticDockOpen = checked, hapticLongPress = checked)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleItem(
                        title = "Gesture & Language Switch Haptics",
                        description = "Tactile ticks when previewing languages or completing swipes",
                        checked = settings.hapticGestureComplete,
                        tag = "switch_haptic_gesture",
                        onCheckedChange = { checked ->
                            onUpdateSettings {
                                it.copy(hapticGestureComplete = checked, hapticLanguageSwitch = checked)
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Sound
                SettingToggleItem(
                    title = stringResource(R.string.pref_sound),
                    description = stringResource(R.string.pref_sound_desc),
                    checked = settings.soundFeedback,
                    tag = "switch_sound",
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(soundFeedback = checked) }
                    }
                )
            }
        }
    }
}

@Composable
private fun InputModeRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
