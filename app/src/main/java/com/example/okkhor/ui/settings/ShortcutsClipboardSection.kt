package com.example.okkhor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.ClipboardRetentionOption
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.ShortcutItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutsClipboardSection(
    settings: KeyboardSettings,
    shortcuts: List<ShortcutItem>,
    clipboardItems: List<ClipboardItem>,
    onToggleClipboardEnabled: (Boolean) -> Unit,
    onUpdateRetentionHours: (Int) -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onTogglePinClipboard: (String) -> Unit,
    onRemoveClipboardItem: (String) -> Unit,
    onClearClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var triggerInput by remember { mutableStateOf("") }
    var expansionInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Text Shortcuts Section ---
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shortcuts_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.shortcuts_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddShortcutDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_add_shortcut")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_shortcut), fontSize = 12.sp)
                    }
                }

                if (shortcuts.isEmpty()) {
                    Text(
                        text = "No shortcuts configured yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    shortcuts.forEach { shortcut ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortcut.trigger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = shortcut.expansion,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { onRemoveShortcut(shortcut.trigger) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Shortcut",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Clipboard Section ---
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.clipboard_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                SettingToggleItem(
                    title = stringResource(R.string.clipboard_opt_in),
                    description = stringResource(R.string.clipboard_opt_in_desc),
                    checked = settings.clipboardHistoryEnabled,
                    tag = "switch_clipboard_opt_in",
                    onCheckedChange = onToggleClipboardEnabled
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clipboard_privacy_notice),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (settings.clipboardHistoryEnabled) {
                    val activeRetention = ClipboardRetentionOption.fromHours(settings.clipboardRetentionHours)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Clipboard History Retention (${activeRetention.labelEn})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeRetention == ClipboardRetentionOption.NEVER) {
                                "Unpinned clips stay until deleted manually. Up to 100 items stored locally."
                            } else {
                                "Unpinned clips are kept for ${activeRetention.labelEn}. Pinned items never expire."
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClipboardRetentionOption.entries.forEach { option ->
                                val isSelected = settings.clipboardRetentionHours == option.hours
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onUpdateRetentionHours(option.hours) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("retention_chip_${option.hours}")
                                ) {
                                    Text(
                                        text = option.labelEn,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stored Snippets (${clipboardItems.size}/100)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (clipboardItems.isNotEmpty()) {
                            TextButton(onClick = onClearClipboard) {
                                Text(stringResource(R.string.clipboard_clear_all), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (clipboardItems.isEmpty()) {
                        Text(
                            text = stringResource(R.string.clipboard_empty),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        clipboardItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(onClick = { onTogglePinClipboard(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pin",
                                            tint = if (item.isPinned) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { onRemoveClipboardItem(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showAddShortcutDialog = false },
            title = { Text(stringResource(R.string.add_shortcut)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = triggerInput,
                        onValueChange = { triggerInput = it },
                        label = { Text(stringResource(R.string.shortcut_trigger)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expansionInput,
                        onValueChange = { expansionInput = it },
                        label = { Text(stringResource(R.string.shortcut_expansion)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (triggerInput.isNotBlank() && expansionInput.isNotBlank()) {
                            onAddShortcut(triggerInput.trim(), expansionInput.trim())
                            triggerInput = ""
                            expansionInput = ""
                            showAddShortcutDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShortcutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
