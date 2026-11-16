package com.example.okkhor.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.okkhor.ui.keyboard.KeyboardLayouts
import com.example.ui.theme.HindSiliguri

/**
 * Okkhor Home & Setup Screen
 */
@Composable
fun OnboardingSection(
    settings: KeyboardSettings,
    activeTheme: KeyboardTheme,
    onNavigateToTab: (Int) -> Unit = {},
    onOpenPerfLab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(checkImeEnabled(context)) }
    var isSelected by remember { mutableStateOf(checkImeSelected(context)) }
    var testText by remember { mutableStateOf("") }
    var showInteractivePreview by remember { mutableStateOf(false) }
    var showAvroGuide by remember { mutableStateOf(false) }
    var sandboxMode by remember { mutableStateOf(settings.defaultInputMode) }
    var sandboxLastTextMode by remember { mutableStateOf(settings.defaultInputMode) }
    var sandboxShifted by remember { mutableStateOf(false) }

    // Recheck IME status when user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = checkImeEnabled(context)
                isSelected = checkImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Okkhor Header Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161920)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E333F)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Okkhor Matra Keys Brand Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_okkhor_brand_icon),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("home_brand_icon")
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color(0xFFF9FAFB),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                    Text(
                        text = "অক্ষর বাংলা কিবোর্ড • Bangla Phonetic, Probhat & English",
                        color = Color(0xFFE5A96A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = HindSiliguri
                    )
                    Text(
                        text = if (isEnabled && isSelected) "সক্রিয় রয়েছে (Active & Ready)" else "সেটআপ সম্পন্ন করুন",
                        color = if (isEnabled && isSelected) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // --- 2. Dashboard Action Cards ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Subscription / Theme Studio
            DashboardActionCard(
                icon = Icons.Default.Palette,
                title = "থিম স্টুডিও",
                subtitle = "Themes & Colors",
                accentColor = Color(0xFFE5A96A),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(1) }
            )

            // Card 2: Avro Rules / Support
            DashboardActionCard(
                icon = Icons.Default.MenuBook,
                title = "অভ্র গাইড",
                subtitle = "Avro Rules",
                accentColor = Color(0xFFE5A96A),
                modifier = Modifier.weight(1f),
                onClick = { showAvroGuide = !showAvroGuide }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 3: Shortcuts & Clipboard
            DashboardActionCard(
                icon = Icons.Default.ShortText,
                title = "ক্লিপবোর্ড ও শর্টকাট",
                subtitle = "Snippets & Keys",
                accentColor = Color(0xFF00A884),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(3) }
            )

            // Card 4: Security & Privacy
            DashboardActionCard(
                icon = Icons.Default.Security,
                title = "প্রাইভেসি ও পলিসি",
                subtitle = "Zero Internet • Local Data",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(4) }
            )
        }

        // Performance Lab Launch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenPerfLab() }
                .testTag("btn_open_perf_lab"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A96A).copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = stringResource(R.string.perf_lab_title),
                    tint = Color(0xFFE5A96A),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.perf_lab_title),
                        color = Color(0xFFF9FAFB),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.perf_lab_subtitle),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // --- 3. Avro Rules & Cheatsheet Expandable Panel ---
        AnimatedVisibility(visible = showAvroGuide) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B1E26)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A96A).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📖 অভ্র ফোনেটিক টাইপিং চিটশিট (Avro Rules)",
                        color = Color(0xFFE5A96A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )

                    val cheatsheet = listOf(
                        "koto" to "কত (স্বরবর্ণ ও ভাঙা)",
                        "amra" to "আমরা",
                        "kkh" to "ক্ষ (যুক্তবর্ণ)",
                        "jny" to "জ্ঞ",
                        "tr" to "ত্র",
                        "shro" to "শ্র",
                        "t`" to "ৎ (খন্ড-ত)",
                        "ng" to "ং (অনুস্বার)",
                        ":`" to "ঃ (বিসর্গ)",
                        "^`" to "ঁ (চন্দ্রবিন্দু)",
                        "bng" to "বঙ্গ",
                        "k...o" to "ক... (যুক্তবর্ণ ভাঙতে o চাপুন)"
                    )

                    cheatsheet.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { (eng, bng) ->
                                Text(
                                    text = "$eng  ➜  $bng",
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.5.sp,
                                    fontFamily = HindSiliguri,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Activation Steps Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161920)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E333F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    fontFamily = HindSiliguri
                )

                // Step 1: Enable in Settings
                StepRow(
                    stepNumber = "1",
                    title = stringResource(R.string.step_1_title),
                    description = stringResource(R.string.step_1_desc),
                    isDone = isEnabled,
                    buttonText = stringResource(R.string.step_1_button),
                    onButtonClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                // Step 2: Select Keyboard
                StepRow(
                    stepNumber = "2",
                    title = stringResource(R.string.step_2_title),
                    description = stringResource(R.string.step_2_desc),
                    isDone = isSelected,
                    buttonText = stringResource(R.string.step_2_button),
                    enabled = isEnabled,
                    onButtonClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    }
                )

                if (isEnabled && isSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.step_done),
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }
        }

        // --- 5. Action Button: "Test Your Keyboard" ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE5A96A))
                .clickable { showInteractivePreview = !showInteractivePreview }
                .testTag("test_keyboard_btn"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showInteractivePreview) "কিবোর্ড প্রিভিউ লুকান (Hide Preview)" else "কিবোর্ড পরীক্ষা করুন (Test Your Keyboard)",
                color = Color(0xFF121418),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguri
            )
        }

        // --- 6. Live Interactive Keyboard Sandbox ---
        AnimatedVisibility(visible = showInteractivePreview) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161920)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E333F))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "টাইপিং পরীক্ষা (Typing Sandbox):",
                        color = Color(0xFFE5A96A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )

                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("এখানে বাংলা বা ইংরেজিতে টাইপ করে দেখুন...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_typing_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5A96A),
                            unfocusedBorderColor = Color(0xFF2E333F),
                            focusedContainerColor = Color(0xFF111317),
                            unfocusedContainerColor = Color(0xFF111317),
                            focusedTextColor = Color(0xFFF9FAFB),
                            unfocusedTextColor = Color(0xFFF9FAFB)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    ) {
                        KeyboardContainer(
                            settings = settings,
                            theme = activeTheme,
                            currentMode = sandboxMode,
                            lastTextMode = sandboxLastTextMode,
                            isShifted = sandboxShifted,
                            candidates = if (sandboxMode == InputMode.ENGLISH) {
                                listOf("Hello", "Good", "Thanks", "Okay")
                            } else {
                                listOf("আমি", "বাংলা", "ধন্যবাদ", "শুভ", "সকাল")
                            },
                            onKeyPress = { key ->
                                if (sandboxMode == InputMode.BANGLA_PHONETIC && key.length == 1 && (key[0] in 'a'..'z' || key[0] in 'A'..'Z')) {
                                    testText = BanglaPhoneticEngine.transliterate(testText + key)
                                } else {
                                    testText += key
                                }
                                if (sandboxShifted) sandboxShifted = false
                            },
                            onActionKey = { code ->
                                when (code) {
                                    KeyboardLayouts.CODE_SHIFT -> sandboxShifted = !sandboxShifted
                                    KeyboardLayouts.CODE_DELETE -> if (testText.isNotEmpty()) testText = testText.dropLast(1)
                                    KeyboardLayouts.CODE_SPACE -> testText += " "
                                    KeyboardLayouts.CODE_ENTER -> testText += "\n"
                                }
                            },
                            onCandidateClick = { candidate -> testText += "$candidate " },
                            onModeSwitch = { newMode ->
                                if (newMode.isTextMode) sandboxLastTextMode = newMode
                                sandboxMode = newMode
                            },
                            onCursorMove = {},
                            onOpenSettings = {},
                            onOpenClipboard = {}
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dashboard Action Card
 */
@Composable
private fun DashboardActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1D24)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E333F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )

            Column {
                Text(
                    text = title,
                    color = Color(0xFFF9FAFB),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    stepNumber: String,
    title: String,
    description: String,
    isDone: Boolean,
    buttonText: String,
    enabled: Boolean = true,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) Color(0xFF10B981) else Color(0xFF2A2E38)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE5A96A)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = Color(0xFFF9FAFB),
                fontFamily = HindSiliguri
            )
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                fontFamily = HindSiliguri
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isDone) {
            Button(
                onClick = onButtonClick,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5A96A),
                    contentColor = Color(0xFF121418)
                )
            ) {
                Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = HindSiliguri)
            }
        }
    }
}

private fun checkImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    val list = imm.enabledInputMethodList
    return list.any { it.packageName == context.packageName }
}

private fun checkImeSelected(context: Context): Boolean {
    val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return current?.contains(context.packageName) == true
}
