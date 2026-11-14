package com.example.okkhor.ui.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.ime.VoiceInputState
import com.example.ui.theme.HindSiliguri

/**
 * Compact minimizable Voice Typing banner integrated cleanly above the keyboard matrix,
 * inspired by PixelBoard's ergonomic voice dictation bar.
 */
@Composable
fun VoiceInputBar(
    voiceState: VoiceInputState,
    theme: KeyboardTheme,
    onStop: () -> Unit,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (voiceState is VoiceInputState.Idle) return

    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(theme.accentColor).copy(alpha = 0.14f))
            .border(1.dp, Color(theme.accentColor).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("voice_input_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (voiceState) {
            is VoiceInputState.Listening -> {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(theme.accentColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = Color(theme.accentTextColor),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val langName = if (voiceState.languageTag.startsWith("bn")) "বাংলা (bn-BD)" else "English (en-US)"
                        Text(
                            text = "শুনছি • Listening ($langName)",
                            color = Color(theme.accentColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri
                        )
                        if (voiceState.partialText.isNotEmpty()) {
                            Text(
                                text = voiceState.partialText,
                                color = Color(theme.keyTextColor),
                                fontSize = 13.sp,
                                fontFamily = HindSiliguri,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "স্বাভাবিকভাবে কথা বলুন...",
                                color = Color(theme.keySecondaryTextColor),
                                fontSize = 11.5.sp,
                                fontFamily = HindSiliguri
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                        .clickable { onStop() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_voice_stop"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Voice Input",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "থামান",
                            color = Color(theme.keyTextColor),
                            fontSize = 12.sp,
                            fontFamily = HindSiliguri,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            is VoiceInputState.PermissionRequired -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "মাইক্রোফোন অনুমতি প্রয়োজন (Microphone Permission)",
                        color = Color(theme.keyTextColor),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                    Text(
                        text = "ভয়েস টাইপিং চালু করতে অডিও অনুমতি দিন",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 11.sp,
                        fontFamily = HindSiliguri
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.accentColor))
                            .clickable { onGrantPermission() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("btn_voice_grant_perm")
                    ) {
                        Text(
                            text = "অনুমতি দিন",
                            color = Color(theme.accentTextColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(theme.keySecondaryTextColor),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            is VoiceInputState.Error -> {
                Text(
                    text = voiceState.message,
                    color = Color(theme.keyTextColor),
                    fontSize = 12.sp,
                    fontFamily = HindSiliguri,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(theme.keySecondaryTextColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            is VoiceInputState.Idle -> Unit
        }
    }
}
