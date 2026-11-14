package com.example.okkhor.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.ui.theme.HindSiliguri

@Composable
fun ThemeStudioSection(
    settings: KeyboardSettings,
    activeTheme: KeyboardTheme,
    onSelectThemeId: (String) -> Unit,
    onUpdateTheme: (KeyboardTheme) -> Unit,
    onUpdateHeightPercent: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var previewMode by remember { mutableStateOf(settings.defaultInputMode) }
    var previewText by remember { mutableStateOf("") }

    val accentPalette = listOf(
        0xFF10B981L, // Okkhor Emerald
        0xFF0D9488L, // Deep Teal
        0xFFF5B041L, // Artisan Gold
        0xFF06B6D4L, // Cyan
        0xFF2563EBL, // Royal Blue
        0xFFEA580CL, // Sunset Amber
        0xFFFFD600L, // Canary Yellow
        0xFFEC4899L, // Vivid Pink
        0xFF8B5CF6L  // Indigo Purple
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.theme_studio_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize colors, key shapes, and layout spacing",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { onSelectThemeId(KeyboardTheme.OkkhorEmerald.id) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Theme", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Theme", fontSize = 12.sp)
            }
        }

        // --- 1. Live Interactive Keyboard Theme Preview ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(activeTheme.accentColor))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Preview: ${activeTheme.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (previewText.isNotEmpty()) {
                        Text(
                            text = "\"$previewText\"",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = HindSiliguri,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    KeyboardContainer(
                        settings = settings,
                        theme = activeTheme,
                        currentMode = previewMode,
                        isShifted = false,
                        candidates = listOf("অক্ষর", "বাংলা", "কিবোর্ড", "অভ্র", "সোনার"),
                        onKeyPress = { key -> previewText += key },
                        onActionKey = { code ->
                            if (code == com.example.okkhor.ui.keyboard.KeyboardLayouts.CODE_DELETE && previewText.isNotEmpty()) {
                                previewText = previewText.dropLast(1)
                            } else if (code == com.example.okkhor.ui.keyboard.KeyboardLayouts.CODE_SPACE) {
                                previewText += " "
                            } else if (code == com.example.okkhor.ui.keyboard.KeyboardLayouts.CODE_ENTER) {
                                previewText += "\n"
                            }
                        },
                        onCandidateClick = { candidate -> previewText += "$candidate " },
                        onModeSwitch = { newMode -> previewMode = newMode },
                        onCursorMove = {},
                        onOpenSettings = {},
                        onOpenClipboard = {}
                    )
                }
            }
        }

        // --- 2. Theme Presets Carousel ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_presets),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(KeyboardTheme.ALL_PRESETS) { preset ->
                        val isSelected = activeTheme.id == preset.id
                        ThemePresetCard(
                            preset = preset,
                            isSelected = isSelected,
                            isFlagship = preset.id == KeyboardTheme.OkkhorEmerald.id,
                            onClick = { onSelectThemeId(preset.id) }
                        )
                    }
                }
            }
        }

        // --- 3. Instant Accent Palette Swatches ---
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Accent Color",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )
                Text(
                    text = "Adjust highlights, Enter key, and active indicator colors",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accentPalette.forEach { colorVal ->
                        val isPicked = activeTheme.accentColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isPicked) 2.5.dp else 1.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val updated = activeTheme.copy(
                                        accentColor = colorVal,
                                        candidateHighlightColor = colorVal
                                    )
                                    onUpdateTheme(updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (colorVal == 0xFFFFD600L || colorVal == 0xFFF5B041L) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Dimensions & Tactile Keycap Sliders ---
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Keycap Dimensions & Spacing",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )

                // Keyboard Height
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pref_keyboard_height),
                            fontSize = 13.sp,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = "${settings.keyboardHeightPercent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.keyboardHeightPercent.toFloat(),
                        onValueChange = { onUpdateHeightPercent(it.toInt()) },
                        valueRange = 80f..130f,
                        steps = 5,
                        modifier = Modifier.testTag("slider_keyboard_height")
                    )
                }

                // Key Corner Rounding
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pref_key_corner_radius),
                            fontSize = 13.sp,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = "${activeTheme.keyCornerRadiusDp} dp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = activeTheme.keyCornerRadiusDp.toFloat(),
                        onValueChange = { radius ->
                            val updated = activeTheme.copy(keyCornerRadiusDp = radius.toInt())
                            onUpdateTheme(updated)
                        },
                        valueRange = 0f..20f,
                        steps = 10,
                        modifier = Modifier.testTag("slider_corner_radius")
                    )
                }

                // Elevation & Tactile Depth
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Key Elevation & Shadow",
                            fontSize = 13.sp,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = "${activeTheme.keyElevationDp} dp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = activeTheme.keyElevationDp.toFloat(),
                        onValueChange = { elev ->
                            val updated = activeTheme.copy(keyElevationDp = elev.toInt())
                            onUpdateTheme(updated)
                        },
                        valueRange = 0f..6f,
                        steps = 6,
                        modifier = Modifier.testTag("slider_elevation")
                    )
                }

                // Key Spacing
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Key Spacing",
                            fontSize = 13.sp,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = "${"%.1f".format(activeTheme.keySpacingDp)} dp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = activeTheme.keySpacingDp,
                        onValueChange = { spacing ->
                            val updated = activeTheme.copy(keySpacingDp = spacing)
                            onUpdateTheme(updated)
                        },
                        valueRange = 1.5f..6.0f,
                        steps = 9,
                        modifier = Modifier.testTag("slider_key_spacing")
                    )
                }

                // Key Border Width
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Outline Border Thickness",
                            fontSize = 13.sp,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = "${"%.1f".format(activeTheme.borderWidthDp)} dp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = activeTheme.borderWidthDp,
                        onValueChange = { width ->
                            val updated = activeTheme.copy(borderWidthDp = width)
                            onUpdateTheme(updated)
                        },
                        valueRange = 0.5f..2.5f,
                        steps = 4,
                        modifier = Modifier.testTag("slider_border_width")
                    )
                }
            }
        }

        // --- 5. Theme Export and Import ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val json = activeTheme.toJson()
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Okkhor Keyboard Theme", json))
                    Toast.makeText(context, "Theme JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = "Export")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON")
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Import")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import JSON")
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Custom Theme JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your theme JSON configuration below:")
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = KeyboardTheme.fromJson(importJsonText)
                        if (parsed != null) {
                            onUpdateTheme(parsed.copy(id = "custom"))
                            Toast.makeText(context, "Theme imported successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Invalid Theme JSON format", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Apply Theme")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemePresetCard(
    preset: KeyboardTheme,
    isSelected: Boolean,
    isFlagship: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(preset.backgroundColor)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(preset.accentColor))
                 else androidx.compose.foundation.BorderStroke(1.dp, Color(preset.keyBorderColor).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Swatches preview
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(Color(preset.keyBackgroundColor))
                            .border(0.5.dp, Color(preset.keyBorderColor), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(Color(preset.accentColor))
                    )
                }

                if (isFlagship) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(preset.accentColor).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Default",
                            color = Color(preset.accentColor),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(preset.accentColor),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = preset.name,
                color = Color(preset.keyTextColor),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HindSiliguri,
                maxLines = 1
            )
        }
    }
}
