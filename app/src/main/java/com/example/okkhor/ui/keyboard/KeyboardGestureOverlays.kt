package com.example.okkhor.ui.keyboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okkhor.data.model.ClipboardItem
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.engine.ClipboardSmartParser
import com.example.okkhor.engine.DateTimeFormatHelper
import com.example.okkhor.engine.TranslationController
import com.example.okkhor.engine.TranslationDirection
import com.example.okkhor.engine.TranslationResult
import com.example.ui.theme.HindSiliguri
import kotlinx.coroutines.launch
import kotlin.math.abs

data class LanguageDisplayInfo(
    val mode: InputMode,
    val primaryName: String,
    val shortBadge: String
)

object KeyboardGestureHelper {

    val LANGUAGE_ORDER: List<InputMode> = listOf(
        InputMode.BANGLA_PHONETIC,
        InputMode.BANGLA_FIXED,
        InputMode.ENGLISH
    )

    val QUICK_SYMBOLS: List<String> = listOf(
        "@", "#", "৳", "$", "%", "&", "*", "-", "+", "/", "(", ")", "?", "!", ":", ";", "\"", "'"
    )

    fun displayInfo(mode: InputMode): LanguageDisplayInfo {
        return when (mode) {
            InputMode.BANGLA_PHONETIC -> LanguageDisplayInfo(
                mode = InputMode.BANGLA_PHONETIC,
                primaryName = "Avro Phonetic",
                shortBadge = "ফোনেটিক"
            )
            InputMode.BANGLA_FIXED -> LanguageDisplayInfo(
                mode = InputMode.BANGLA_FIXED,
                primaryName = "প্রভাত",
                shortBadge = "বাংলা"
            )
            InputMode.ENGLISH -> LanguageDisplayInfo(
                mode = InputMode.ENGLISH,
                primaryName = "English (ENG)",
                shortBadge = "ENG"
            )
            else -> displayInfo(InputMode.BANGLA_PHONETIC)
        }
    }

    fun stepLanguageMode(baseMode: InputMode, step: Int): InputMode {
        val safeBase = if (baseMode in LANGUAGE_ORDER) baseMode else InputMode.BANGLA_PHONETIC
        val idx = LANGUAGE_ORDER.indexOf(safeBase).coerceAtLeast(0)
        val n = LANGUAGE_ORDER.size
        val nextIdx = ((idx + step) % n + n) % n
        return LANGUAGE_ORDER[nextIdx]
    }

    fun resolveDockEmojiHit(
        pointerRoot: Offset,
        viewportBounds: Rect?,
        emojiBounds: Map<String, Rect>,
        verticalTolerancePx: Float,
        horizontalTolerancePx: Float
    ): String? = resolveBoundsHit(
        pointerRoot = pointerRoot,
        viewportBounds = viewportBounds,
        itemBounds = emojiBounds,
        verticalTolerancePx = verticalTolerancePx,
        horizontalTolerancePx = horizontalTolerancePx
    )

    fun resolveBoundsHit(
        pointerRoot: Offset,
        viewportBounds: Rect?,
        itemBounds: Map<String, Rect>,
        verticalTolerancePx: Float,
        horizontalTolerancePx: Float
    ): String? {
        if (itemBounds.isEmpty()) return null
        if (viewportBounds != null) {
            val expandedViewport = Rect(
                left = viewportBounds.left - horizontalTolerancePx,
                top = viewportBounds.top - verticalTolerancePx,
                right = viewportBounds.right + horizontalTolerancePx,
                bottom = viewportBounds.bottom + verticalTolerancePx
            )
            if (!expandedViewport.contains(pointerRoot)) {
                return null
            }
        }

        var bestMatch: String? = null
        var bestDist = Float.MAX_VALUE
        for ((key, rect) in itemBounds) {
            if (viewportBounds != null) {
                val visibleLeft = maxOf(rect.left, viewportBounds.left)
                val visibleRight = minOf(rect.right, viewportBounds.right)
                if (visibleRight <= visibleLeft) continue
            }
            val expandedRect = Rect(
                left = rect.left - horizontalTolerancePx,
                top = rect.top - verticalTolerancePx,
                right = rect.right + horizontalTolerancePx,
                bottom = rect.bottom + verticalTolerancePx
            )
            if (expandedRect.contains(pointerRoot)) {
                val dist = abs(pointerRoot.x - rect.center.x) + abs(pointerRoot.y - rect.center.y) * 0.5f
                if (dist < bestDist) {
                    bestDist = dist
                    bestMatch = key
                }
            }
        }
        return bestMatch
    }
}

/**
 * Floating Glassmorphic Recent Emoji Quick Access Dock triggered by long-pressing the Emoji key.
 */
@Composable
fun EmojiQuickAccessDock(
    visible: Boolean,
    emojis: List<String>,
    highlightedEmoji: String?,
    theme: KeyboardTheme,
    onViewportBoundsChanged: (Rect) -> Unit,
    onEmojiBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 8.dp,
        testTag = "emoji_quick_dock",
        modifier = modifier
    ) {
        val scrollState = rememberScrollState()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = null,
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "সাম্প্রতিক ইমোজি",
                        color = Color(theme.popupTextColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguri
                    )
                }

                val statusText = if (highlightedEmoji != null) {
                    "ছেড়ে দিন: $highlightedEmoji"
                } else {
                    "আঙুল টেনে ছাড়ুন"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (highlightedEmoji != null) {
                                Color(theme.accentColor).copy(alpha = 0.22f)
                            } else {
                                Color(theme.keyBackgroundColor).copy(alpha = 0.55f)
                            }
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = if (highlightedEmoji != null) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                        fontSize = 10.5.sp,
                        fontWeight = if (highlightedEmoji != null) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = HindSiliguri,
                        modifier = Modifier.testTag("emoji_dock_status")
                    )
                }
            }

            if (emojis.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(theme.keyBackgroundColor).copy(alpha = 0.45f))
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) onViewportBoundsChanged(coords.boundsInRoot())
                        }
                        .testTag("emoji_dock_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো সাম্প্রতিক ইমোজি নেই • ট্যাপ করে বেছে নিন",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguri,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                onViewportBoundsChanged(coords.boundsInRoot())
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    emojis.forEachIndexed { index, emoji ->
                        val isTargeted = emoji == highlightedEmoji
                        val itemScale by animateFloatAsState(
                            targetValue = if (isTargeted) 1.28f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dock_emoji_scale_$index"
                        )
                        val slotShape = RoundedCornerShape(14.dp)

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(itemScale)
                                .clip(slotShape)
                                .background(
                                    if (isTargeted) {
                                        Color(theme.accentColor).copy(alpha = 0.28f)
                                    } else {
                                        Color(theme.keyBackgroundColor).copy(alpha = 0.42f)
                                    }
                                )
                                .then(
                                    if (isTargeted) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = Color(theme.accentColor),
                                            shape = slotShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .onGloballyPositioned { coords ->
                                    if (coords.isAttached) {
                                        onEmojiBoundsChanged(emoji, coords.boundsInRoot())
                                    }
                                }
                                .semantics {
                                    contentDescription = "Recent emoji $emoji"
                                }
                                .testTag("dock_emoji_$index"),
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
            }
        }
    }
}

/**
 * Floating Language Switcher Overlay anchored above the spacebar during horizontal swipe gestures.
 */
@Composable
fun FloatingLanguageSwitcherOverlay(
    visible: Boolean,
    baseMode: InputMode,
    previewStep: Int,
    dragProgress: Float,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier
) {
    val prevMode = KeyboardGestureHelper.stepLanguageMode(baseMode, -1)
    val nextMode = KeyboardGestureHelper.stepLanguageMode(baseMode, +1)
    val orderedModes = listOf(prevMode, baseMode, nextMode)
    val previewMode = KeyboardGestureHelper.stepLanguageMode(baseMode, previewStep)
    val previewInfo = KeyboardGestureHelper.displayInfo(previewMode)

    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 10.dp,
        testTag = "floating_language_switcher",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = previewInfo.primaryName,
                        color = Color(theme.accentColor),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("language_switcher_preview_label")
                    )
                }

                val hintLabel = when {
                    previewStep < 0 -> "← পূর্ববর্তী"
                    previewStep > 0 -> "পরবর্তী →"
                    else -> "← বামে / ডানে →"
                }
                Text(
                    text = hintLabel,
                    color = Color(theme.keySecondaryTextColor),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = HindSiliguri
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(theme.keyBackgroundColor).copy(alpha = 0.55f))
                    .padding(3.dp)
            ) {
                val slotWidth = maxWidth / 3f
                val targetIdx = (previewStep + 1).coerceIn(0, 2)
                val nudgeDp = if (previewStep == 0) {
                    slotWidth * (dragProgress.coerceIn(-0.28f, 0.28f))
                } else {
                    0.dp
                }
                val indicatorOffsetX by animateDpAsState(
                    targetValue = (slotWidth * targetIdx) + nudgeDp,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "language_indicator_offset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(slotWidth)
                        .height(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(theme.accentColor))
                        .testTag("language_active_indicator")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    orderedModes.forEachIndexed { idx, mode ->
                        val info = KeyboardGestureHelper.displayInfo(mode)
                        val isSelected = idx == targetIdx
                        val itemScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 0.96f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "lang_item_scale_$idx"
                        )

                        Box(
                            modifier = Modifier
                                .width(slotWidth)
                                .height(42.dp)
                                .scale(itemScale)
                                .padding(horizontal = 4.dp)
                                .testTag("lang_option_${mode.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = info.primaryName,
                                    color = if (isSelected) {
                                        Color(theme.accentTextColor)
                                    } else {
                                        Color(theme.keyTextColor).copy(alpha = 0.82f)
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = HindSiliguri,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                if (mode != InputMode.ENGLISH) {
                                    Text(
                                        text = info.shortBadge,
                                        color = if (isSelected) {
                                            Color(theme.accentTextColor).copy(alpha = 0.85f)
                                        } else {
                                            Color(theme.keySecondaryTextColor).copy(alpha = 0.72f)
                                        },
                                        fontSize = 9.sp,
                                        fontFamily = HindSiliguri,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

/**
 * Floating Clipboard Quick Dock: long-press clipboard icon to view/tap/pin/delete/clear snippets quickly.
 */
@Composable
fun ClipboardQuickDock(
    visible: Boolean,
    items: List<ClipboardItem>,
    theme: KeyboardTheme,
    onInsertClip: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 8.dp,
        testTag = "clipboard_quick_dock",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ক্লিপবোর্ড কুইক ডক",
                        color = Color(theme.popupTextColor),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (items.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clipboard_dock_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Clipboard",
                                tint = Color(theme.keySecondaryTextColor),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clipboard_dock_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Clipboard Dock",
                            tint = Color(theme.accentColor),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(theme.keyBackgroundColor).copy(alpha = 0.45f))
                        .testTag("clipboard_dock_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ক্লিপবোর্ড খালি আছে",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguri
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.take(8).forEachIndexed { idx, item ->
                        val smartHint = remember(item.text) { ClipboardSmartParser.extractHint(item.text) }
                        val cardShape = RoundedCornerShape(14.dp)
                        Column(
                            modifier = Modifier
                                .widthIn(min = 128.dp, max = 196.dp)
                                .clip(cardShape)
                                .background(Color(theme.keyBackgroundColor).copy(alpha = 0.72f))
                                .border(
                                    width = 1.dp,
                                    color = if (item.isPinned) Color(theme.accentColor) else Color(theme.keyBorderColor),
                                    shape = cardShape
                                )
                                .clickable { onInsertClip(item.text) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("clipboard_dock_item_$idx"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        smartHint?.isOtp == true -> "OTP"
                                        smartHint?.isUrl == true -> "LINK"
                                        item.isPinned -> "PINNED"
                                        else -> "CLIP"
                                    },
                                    color = Color(theme.accentColor),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .clickable { onTogglePin(item.id) }
                                            .testTag("clipboard_dock_pin_$idx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pin clip",
                                            tint = if (item.isPinned) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .clickable { onDeleteItem(item.id) }
                                            .testTag("clipboard_dock_delete_$idx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete clip",
                                            tint = Color(theme.keySecondaryTextColor),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = item.text,
                                color = Color(theme.keyTextColor),
                                fontSize = 12.sp,
                                fontFamily = HindSiliguri,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating Quick Symbol Dock triggered by long-pressing ?123; supports drag-to-insert or tap.
 */
@Composable
fun SymbolQuickDock(
    visible: Boolean,
    symbols: List<String>,
    highlightedSymbol: String?,
    theme: KeyboardTheme,
    onViewportBoundsChanged: (Rect) -> Unit,
    onSymbolBoundsChanged: (String, Rect) -> Unit,
    onSymbolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 8.dp,
        testTag = "symbol_quick_dock",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কুইক সিম্বল",
                    color = Color(theme.popupTextColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )
                Text(
                    text = highlightedSymbol?.let { "ছেড়ে দিন: $it" } ?: "টেনে বেছে নিন",
                    color = if (highlightedSymbol != null) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguri
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) onViewportBoundsChanged(coords.boundsInRoot())
                    },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                symbols.forEachIndexed { idx, sym ->
                    val isTargeted = sym == highlightedSymbol
                    val slotShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(if (isTargeted) 1.18f else 1.0f)
                            .clip(slotShape)
                            .background(
                                if (isTargeted) Color(theme.accentColor).copy(alpha = 0.28f)
                                else Color(theme.keyBackgroundColor).copy(alpha = 0.65f)
                            )
                            .border(
                                width = if (isTargeted) 1.5.dp else 1.dp,
                                color = if (isTargeted) Color(theme.accentColor) else Color(theme.keyBorderColor),
                                shape = slotShape
                            )
                            .clickable { onSymbolClick(sym) }
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) onSymbolBoundsChanged(sym, coords.boundsInRoot())
                            }
                            .testTag("symbol_dock_item_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sym,
                            color = Color(theme.keyTextColor),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Alternate Character Selector triggered when holding a key with alternates.
 */
@Composable
fun AlternateCharDock(
    visible: Boolean,
    baseKey: String,
    alternates: List<String>,
    highlightedChar: String?,
    theme: KeyboardTheme,
    onSelectChar: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingDockContainer(
        visible = visible && alternates.isNotEmpty(),
        theme = theme,
        horizontalPadding = 12.dp,
        testTag = "alternate_char_dock",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বিকল্প অক্ষর • $baseKey",
                    color = Color(theme.popupTextColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguri
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close alternate chars",
                    tint = Color(theme.keySecondaryTextColor),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onClose() }
                        .testTag("btn_close_alternates")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                alternates.forEachIndexed { idx, alt ->
                    val selected = alt == highlightedChar || (highlightedChar == null && idx == 0)
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(shape)
                            .background(
                                if (selected) Color(theme.accentColor).copy(alpha = 0.28f)
                                else Color(theme.keyBackgroundColor).copy(alpha = 0.72f)
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) Color(theme.accentColor) else Color(theme.keyBorderColor),
                                shape = shape
                            )
                            .clickable { onSelectChar(alt) }
                            .testTag("alt_char_option_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alt,
                            color = Color(theme.keyTextColor),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Date & Time Quick Insertion Dock.
 */
@Composable
fun DateTimeQuickDock(
    visible: Boolean,
    theme: KeyboardTheme,
    onInsertValue: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(visible) { DateTimeFormatHelper.getQuickOptions() }

    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 8.dp,
        testTag = "datetime_quick_dock",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "তারিখ ও সময় যুক্ত করুন",
                        color = Color(theme.popupTextColor),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("datetime_dock_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Date Time Dock",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                options.forEach { opt ->
                    val chipShape = RoundedCornerShape(13.dp)
                    Column(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(Color(theme.keyBackgroundColor).copy(alpha = 0.78f))
                            .border(1.dp, Color(theme.keyBorderColor), chipShape)
                            .clickable { onInsertValue(opt.value) }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                            .testTag("datetime_option_${opt.id}"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = opt.label,
                            color = Color(theme.accentColor),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri
                        )
                        Text(
                            text = opt.value,
                            color = Color(theme.keyTextColor),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Bangla <-> English Translation Dock (offline bilingual dictionary + optional endpoint).
 */
@Composable
fun TranslationQuickDock(
    visible: Boolean,
    theme: KeyboardTheme,
    customEndpointUrl: String,
    initialClipText: String,
    onInsertTranslation: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val controller = remember { TranslationController() }
    var query by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(TranslationDirection.EN_TO_BN) }
    var result by remember { mutableStateOf<TranslationResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    FloatingDockContainer(
        visible = visible,
        theme = theme,
        horizontalPadding = 8.dp,
        testTag = "translation_quick_dock",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "অনুবাদ ডক",
                        color = Color(theme.popupTextColor),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(theme.accentColor).copy(alpha = 0.16f))
                            .clickable {
                                direction = if (direction == TranslationDirection.EN_TO_BN) {
                                    TranslationDirection.BN_TO_EN
                                } else {
                                    TranslationDirection.EN_TO_BN
                                }
                                result = null
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("btn_translate_swap"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch translation direction",
                            tint = Color(theme.accentColor),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = direction.label,
                            color = Color(theme.accentColor),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguri
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("translation_dock_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Translation Dock",
                        tint = Color(theme.accentColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(theme.keyBackgroundColor).copy(alpha = 0.78f))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "যেমন: good morning / ধন্যবাদ",
                            color = Color(theme.keySecondaryTextColor).copy(alpha = 0.7f),
                            fontSize = 11.5.sp,
                            fontFamily = HindSiliguri,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(theme.keyTextColor),
                            fontSize = 12.5.sp,
                            fontFamily = HindSiliguri
                        ),
                        cursorBrush = SolidColor(Color(theme.accentColor)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translation_query_input")
                    )
                }

                if (initialClipText.isNotBlank() && query.isBlank()) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(theme.specialKeyBackgroundColor))
                            .clickable { query = initialClipText.take(80) }
                            .padding(horizontal = 8.dp)
                            .testTag("btn_translate_paste_clip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Paste",
                            color = Color(theme.accentColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguri
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(theme.accentColor))
                        .clickable(enabled = !isLoading) {
                            scope.launch {
                                isLoading = true
                                result = controller.translate(query, direction, customEndpointUrl)
                                isLoading = false
                            }
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("btn_translate_run"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "..." else "অনুবাদ",
                        color = Color(theme.accentTextColor),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguri
                    )
                }
            }

            val currentResult = result
            if (currentResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(theme.keyBackgroundColor).copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (currentResult.translatedText.isNotBlank()) {
                            Text(
                                text = currentResult.translatedText,
                                color = Color(theme.keyTextColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguri,
                                modifier = Modifier.testTag("translation_result_text")
                            )
                        }
                        Text(
                            text = currentResult.note,
                            color = Color(theme.keySecondaryTextColor),
                            fontSize = 10.sp,
                            fontFamily = HindSiliguri
                        )
                    }

                    if (currentResult.translatedText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(theme.accentColor))
                                .clickable { onInsertTranslation(currentResult.translatedText) }
                                .padding(horizontal = 10.dp)
                                .testTag("btn_insert_translation"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Insert",
                                color = Color(theme.accentTextColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
