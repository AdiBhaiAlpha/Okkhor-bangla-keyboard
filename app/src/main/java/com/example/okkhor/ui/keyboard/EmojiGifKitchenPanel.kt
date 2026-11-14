package com.example.okkhor.ui.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.emoji.EmojiCategory
import com.example.okkhor.emoji.EmojiEntry
import com.example.okkhor.emoji.EmojiKitchenEngine
import com.example.okkhor.emoji.EmojiMashupRecord
import com.example.okkhor.emoji.EmojiMashupStyle
import com.example.okkhor.emoji.GifCategoryItem
import com.example.okkhor.emoji.GifItem
import com.example.okkhor.emoji.GifSearchUiState
import com.example.okkhor.emoji.GiphyRepository
import com.example.okkhor.emoji.UnicodeEmojiRepository
import com.example.ui.theme.HindSiliguri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MediaPanelTab {
    EMOJI,
    GIF,
    KITCHEN
}

private enum class GifSubTab {
    TRENDING,
    CATEGORIES,
    RECENT,
    FAVORITES
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiGifKitchenPanel(
    height: Dp,
    theme: KeyboardTheme,
    settings: KeyboardSettings,
    lastTextMode: InputMode,
    cornerRadius: Dp,
    elevation: Dp,
    emojiRepository: UnicodeEmojiRepository,
    giphyRepository: GiphyRepository,
    recentEmojis: List<String>,
    emojiFrequency: Map<String, Int>,
    favoriteEmojis: List<String>,
    emojiSearchHistory: List<String>,
    preferredVariants: Map<String, String>,
    recentGifs: List<GifItem>,
    favoriteGifs: List<GifItem>,
    gifSearchHistory: List<String>,
    recentMashups: List<EmojiMashupRecord>,
    favoriteMashups: List<EmojiMashupRecord>,
    supportsGifCommit: Boolean,
    supportsImageCommit: Boolean,
    mediaInsertNotice: String?,
    onDismissNotice: () -> Unit,
    onEmojiSelected: (baseUnicode: String, insertedUnicode: String) -> Unit,
    onSelectVariant: (baseUnicode: String, variantUnicode: String) -> Unit,
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
    onBackspace: () -> Unit,
    onBackToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MediaPanelTab.EMOJI) }
    var isSearchInputOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMiniKeyboardBangla by remember { mutableStateOf(false) }

    // Emoji tab state
    var selectedCategoryId by remember { mutableIntStateOf(EmojiCategory.SMILEYS.id) }
    var lastSelectedEmoji by remember { mutableStateOf<String?>(null) }
    var variantSheetEntry by remember { mutableStateOf<EmojiEntry?>(null) }

    // GIF tab state
    var gifSubTab by remember { mutableStateOf(GifSubTab.TRENDING) }
    var gifState by remember { mutableStateOf<GifSearchUiState>(GifSearchUiState.Idle) }
    var gifCategories by remember { mutableStateOf(GiphyRepository.DEFAULT_CATEGORIES) }
    var gifSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var gifOffset by remember { mutableIntStateOf(0) }
    var pendingFallbackGif by remember { mutableStateOf<GifItem?>(null) }

    // Emoji Kitchen state
    var kitchenFirst by remember { mutableStateOf("😀") }
    var kitchenSecond by remember { mutableStateOf("❤️") }
    var kitchenPickingSlot by remember { mutableIntStateOf(2) }
    var kitchenSubFilter by remember { mutableStateOf("all") } // all, recent, favorites
    var pendingFallbackMashup by remember { mutableStateOf<Triple<String, String, EmojiMashupStyle>?>(null) }

    val gifImageLoader = remember(context) {
        buildGifImageLoader(context)
    }

    val allEmojis = remember(emojiRepository) { emojiRepository.getAllEmojis() }

    // Load GIF trending / search reactively when GIF tab is active
    LaunchedEffect(activeTab, gifSubTab, searchQuery, settings.gifSearchEnabled) {
        if (activeTab != MediaPanelTab.GIF) return@LaunchedEffect
        if (!settings.gifSearchEnabled) {
            gifState = GifSearchUiState.Disabled
            return@LaunchedEffect
        }
        if (gifSubTab == GifSubTab.RECENT || gifSubTab == GifSubTab.FAVORITES) return@LaunchedEffect
        if (gifSubTab == GifSubTab.CATEGORIES && gifCategories == GiphyRepository.DEFAULT_CATEGORIES) {
            gifCategories = giphyRepository.fetchCategories()
        }
        val clean = searchQuery.trim()
        if (clean.isNotEmpty()) {
            delay(320)
            gifSuggestions = giphyRepository.fetchSearchSuggestions(clean)
            if (clean.length >= 2) {
                onRecordGifSearch(clean)
            }
            gifOffset = 0
            gifState = GifSearchUiState.Loading
            gifState = giphyRepository.searchGifs(clean, offset = 0)
        } else if (gifSubTab == GifSubTab.TRENDING) {
            gifSuggestions = emptyList()
            gifOffset = 0
            gifState = GifSearchUiState.Loading
            gifState = giphyRepository.fetchTrending(offset = 0)
        }
    }

    // Record emoji search history on pause
    LaunchedEffect(activeTab, searchQuery) {
        if (activeTab == MediaPanelTab.EMOJI && searchQuery.trim().length >= 2) {
            delay(600)
            onRecordEmojiSearch(searchQuery.trim())
        }
    }

    val backLabel = when (lastTextMode) {
        InputMode.ENGLISH -> "ABC"
        InputMode.BANGLA_FIXED -> "প্রভাত"
        else -> "অভ্র"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("emoji_gif_kitchen_panel")
    ) {
        // --- 1. Top Navigation Tabs: Emoji | GIF | Kitchen + Search Toggle ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PanelTabChip(
                label = "Emoji",
                icon = Icons.Default.Mood,
                selected = activeTab == MediaPanelTab.EMOJI,
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_emoji"),
                onClick = {
                    activeTab = MediaPanelTab.EMOJI
                    variantSheetEntry = null
                }
            )
            PanelTabChip(
                label = "GIF",
                icon = Icons.Default.GifBox,
                selected = activeTab == MediaPanelTab.GIF,
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_gif"),
                onClick = {
                    activeTab = MediaPanelTab.GIF
                    variantSheetEntry = null
                }
            )
            PanelTabChip(
                label = "Kitchen",
                icon = Icons.Default.AutoAwesome,
                selected = activeTab == MediaPanelTab.KITCHEN,
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_kitchen"),
                onClick = {
                    activeTab = MediaPanelTab.KITCHEN
                    variantSheetEntry = null
                }
            )
        }

        // --- 2. Search Bar (Shared across Emoji / GIF / Kitchen) ---
        val searchPlaceholder = when (activeTab) {
            MediaPanelTab.EMOJI -> "Search emojis (love, বৃষ্টি, খুশি...)"
            MediaPanelTab.GIF -> "Search GIFs on GIPHY..."
            MediaPanelTab.KITCHEN -> "Search mashups (joy, love, বৃষ্টি...)"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(theme.keyBackgroundColor))
                .border(
                    width = 1.dp,
                    color = if (isSearchInputOpen) Color(theme.accentColor) else Color(theme.keyBorderColor),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { isSearchInputOpen = !isSearchInputOpen }
                .padding(horizontal = 10.dp)
                .testTag("media_search_bar"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(theme.accentColor),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (searchQuery.isEmpty()) searchPlaceholder else searchQuery,
                color = if (searchQuery.isEmpty()) Color(theme.keySecondaryTextColor) else Color(theme.keyTextColor),
                fontSize = 12.5.sp,
                fontFamily = HindSiliguri,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("media_search_text")
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_clear_media_search")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search",
                        tint = Color(theme.keySecondaryTextColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (isSearchInputOpen) {
                Text(
                    text = "Done",
                    color = Color(theme.accentColor),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isSearchInputOpen = false }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("btn_close_search_pad")
                )
            }
        }

        // Honest notice banner if host app rejected commitContent
        if (!mediaInsertNotice.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(theme.accentColor).copy(alpha = 0.14f))
                    .border(1.dp, Color(theme.accentColor).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("media_insert_notice"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = mediaInsertNotice,
                    color = Color(theme.keyTextColor),
                    fontSize = 10.5.sp,
                    fontFamily = HindSiliguri,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                pendingFallbackGif?.let { gif ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(theme.accentColor))
                            .clickable { onShareGif(gif) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Share", color = Color(theme.accentTextColor), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .clickable { onCopyGifUrl(gif) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Link", color = Color(theme.keyTextColor), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                pendingFallbackMashup?.let { (f, s, st) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(theme.accentColor))
                            .clickable { onShareMashup(f, s, st) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Share", color = Color(theme.accentTextColor), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDismissNotice, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(theme.keyTextColor), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 3. Active Tab Content Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                MediaPanelTab.EMOJI -> {
                    EmojiTabContent(
                        theme = theme,
                        emojiRepository = emojiRepository,
                        allEmojis = allEmojis,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it },
                        recentEmojis = recentEmojis,
                        emojiFrequency = emojiFrequency,
                        favoriteEmojis = favoriteEmojis,
                        emojiSearchHistory = emojiSearchHistory,
                        preferredVariants = preferredVariants,
                        lastSelectedEmoji = lastSelectedEmoji,
                        variantSheetEntry = variantSheetEntry,
                        onPickSearchHistory = { searchQuery = it },
                        onClearSearchHistory = onClearEmojiSearchHistory,
                        onEmojiClick = { entry, displayUnicode ->
                            lastSelectedEmoji = displayUnicode
                            onEmojiSelected(entry.unicode, displayUnicode)
                        },
                        onEmojiLongClick = { entry ->
                            variantSheetEntry = entry
                        },
                        onSelectVariant = { base, variant ->
                            lastSelectedEmoji = variant
                            onSelectVariant(base, variant)
                            variantSheetEntry = null
                        },
                        onToggleFavorite = onToggleFavoriteEmoji,
                        onDismissVariantSheet = { variantSheetEntry = null }
                    )
                }

                MediaPanelTab.GIF -> {
                    GifTabContent(
                        theme = theme,
                        imageLoader = gifImageLoader,
                        giphyRepository = giphyRepository,
                        gifSubTab = gifSubTab,
                        onChangeSubTab = { gifSubTab = it },
                        searchQuery = searchQuery,
                        gifState = gifState,
                        gifCategories = gifCategories,
                        gifSuggestions = gifSuggestions,
                        recentGifs = recentGifs,
                        favoriteGifs = favoriteGifs,
                        gifSearchHistory = gifSearchHistory,
                        supportsGifCommit = supportsGifCommit,
                        onSelectQuery = { q ->
                            searchQuery = q
                            gifSubTab = GifSubTab.TRENDING
                        },
                        onClearSearchHistory = onClearGifSearchHistory,
                        onRetry = {
                            scope.launch {
                                gifOffset = 0
                                gifState = GifSearchUiState.Loading
                                gifState = if (searchQuery.isBlank()) {
                                    giphyRepository.fetchTrending(0)
                                } else {
                                    giphyRepository.searchGifs(searchQuery, 0)
                                }
                            }
                        },
                        onLoadMore = {
                            val current = gifState as? GifSearchUiState.Success ?: return@GifTabContent
                            if (current.isLoadingMore || !current.hasMore) return@GifTabContent
                            scope.launch {
                                gifState = current.copy(isLoadingMore = true)
                                val nextOffset = gifOffset + GiphyRepository.PAGE_SIZE
                                val nextRes = if (searchQuery.isBlank()) {
                                    giphyRepository.fetchTrending(nextOffset)
                                } else {
                                    giphyRepository.searchGifs(searchQuery, nextOffset)
                                }
                                if (nextRes is GifSearchUiState.Success) {
                                    gifOffset = nextOffset
                                    gifState = GifSearchUiState.Success(
                                        items = (current.items + nextRes.items).distinctBy { it.id },
                                        hasMore = nextRes.hasMore,
                                        isLoadingMore = false
                                    )
                                } else {
                                    gifState = current.copy(isLoadingMore = false)
                                }
                            }
                        },
                        onGifClick = { gif ->
                            pendingFallbackGif = gif
                            pendingFallbackMashup = null
                            onInsertGif(gif)
                        },
                        onShareGif = onShareGif,
                        onToggleFavoriteGif = onToggleFavoriteGif
                    )
                }

                MediaPanelTab.KITCHEN -> {
                    EmojiKitchenTabContent(
                        theme = theme,
                        searchQuery = searchQuery,
                        firstEmoji = kitchenFirst,
                        secondEmoji = kitchenSecond,
                        pickingSlot = kitchenPickingSlot,
                        subFilter = kitchenSubFilter,
                        recentMashups = recentMashups,
                        favoriteMashups = favoriteMashups,
                        supportsImageCommit = supportsImageCommit,
                        onChangeSlot = { kitchenPickingSlot = it },
                        onChangeSubFilter = { kitchenSubFilter = it },
                        onPickEmoji = { picked ->
                            if (kitchenPickingSlot == 1) {
                                kitchenFirst = picked
                                val compat = EmojiKitchenEngine.getCompatibleSecondEmojis(picked)
                                if (compat.isNotEmpty() && kitchenSecond !in compat) {
                                    kitchenSecond = compat.first()
                                }
                            } else {
                                kitchenSecond = picked
                            }
                        },
                        onPickPair = { f, s ->
                            kitchenFirst = f
                            kitchenSecond = s
                        },
                        onRandomize = {
                            val bases = EmojiKitchenEngine.COMPATIBLE_BASE_EMOJIS
                            val r1 = bases.random()
                            val r2 = EmojiKitchenEngine.getCompatibleSecondEmojis(r1).randomOrNull() ?: "❤️"
                            kitchenFirst = r1
                            kitchenSecond = r2
                        },
                        onInsertMashup = { f, s, style ->
                            pendingFallbackMashup = Triple(f, s, style)
                            pendingFallbackGif = null
                            onInsertMashup(f, s, style)
                        },
                        onShareMashup = onShareMashup,
                        onToggleFavoriteMashup = onToggleFavoriteMashup
                    )
                }
            }
        }

        // --- 4. Embedded Search Mini-Keyboard when Search Bar is Active ---
        if (isSearchInputOpen) {
            Spacer(modifier = Modifier.height(4.dp))
            InlineSearchKeyboard(
                theme = theme,
                isBanglaMode = searchMiniKeyboardBangla,
                onToggleLanguage = { searchMiniKeyboardBangla = !searchMiniKeyboardBangla },
                onChar = { ch -> searchQuery += ch },
                onBackspace = {
                    if (searchQuery.isNotEmpty()) {
                        searchQuery = searchQuery.dropLast(1)
                    }
                },
                onClose = { isSearchInputOpen = false }
            )
        } else {
            // --- 5. Bottom Return (ABC) & Backspace Bar ---
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
                    weight = 1.6f,
                    height = 38.dp,
                    theme = theme,
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    onClick = onBackToText
                )

                // Quick Category / Search info pill
                Box(
                    modifier = Modifier
                        .weight(2.4f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(Color(theme.keyBackgroundColor).copy(alpha = 0.7f))
                        .border(1.dp, Color(theme.keyBorderColor).copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                        .clickable { isSearchInputOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${allEmojis.size} Emojis • GIF • Mashup",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 11.sp,
                        fontFamily = HindSiliguri,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RepeatableSpecialKeyButton(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color(theme.specialKeyTextColor),
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    weight = 1.2f,
                    height = 38.dp,
                    theme = theme,
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    onClick = onBackspace
                )
            }
        }
    }
}

@Composable
private fun PanelTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Color(theme.accentColor)
                else Color(theme.keyBackgroundColor)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(theme.accentColor) else Color(theme.keyBorderColor),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(theme.accentTextColor) else Color(theme.keyTextColor),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (selected) Color(theme.accentTextColor) else Color(theme.keyTextColor),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = HindSiliguri,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiTabContent(
    theme: KeyboardTheme,
    emojiRepository: UnicodeEmojiRepository,
    allEmojis: List<EmojiEntry>,
    searchQuery: String,
    selectedCategoryId: Int,
    onSelectCategory: (Int) -> Unit,
    recentEmojis: List<String>,
    emojiFrequency: Map<String, Int>,
    favoriteEmojis: List<String>,
    emojiSearchHistory: List<String>,
    preferredVariants: Map<String, String>,
    lastSelectedEmoji: String?,
    variantSheetEntry: EmojiEntry?,
    onPickSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onEmojiClick: (EmojiEntry, String) -> Unit,
    onEmojiLongClick: (EmojiEntry) -> Unit,
    onSelectVariant: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismissVariantSheet: () -> Unit
) {
    val suggested = remember(recentEmojis, emojiFrequency) {
        emojiRepository.getSuggestedEmojis(recentEmojis, emojiFrequency, limit = 10)
    }
    val related = remember(lastSelectedEmoji) {
        lastSelectedEmoji?.let { emojiRepository.getRelatedEmojis(it, limit = 8) }.orEmpty()
    }
    val searchResults = remember(searchQuery, allEmojis) {
        if (searchQuery.isBlank()) emptyList() else emojiRepository.search(searchQuery)
    }

    val displayedEmojis = remember(
        searchQuery,
        searchResults,
        selectedCategoryId,
        recentEmojis,
        emojiFrequency,
        favoriteEmojis,
        allEmojis
    ) {
        if (searchQuery.isNotBlank()) {
            searchResults
        } else {
            when (selectedCategoryId) {
                EmojiCategory.RECENTS.id -> recentEmojis.mapNotNull { code ->
                    emojiRepository.findByUnicode(code)?.copy(unicode = code)
                        ?: EmojiEntry(code, 0, code, emptyList())
                }
                EmojiCategory.FREQUENT.id -> emojiFrequency.entries
                    .sortedByDescending { it.value }
                    .map { (code, _) ->
                        emojiRepository.findByUnicode(code)?.copy(unicode = code)
                            ?: EmojiEntry(code, 0, code, emptyList())
                    }
                EmojiCategory.FAVORITES.id -> favoriteEmojis.map { code ->
                    emojiRepository.findByUnicode(code)?.copy(unicode = code)
                        ?: EmojiEntry(code, 0, code, emptyList())
                }
                else -> emojiRepository.getByCategory(selectedCategoryId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (searchQuery.isBlank()) {
                // Category Strip
                val categories = remember {
                    listOf(
                        EmojiCategory.RECENTS,
                        EmojiCategory.FREQUENT,
                        EmojiCategory.FAVORITES
                    ) + EmojiCategory.STANDARD_CATEGORIES
                }
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = selectedCategoryId == cat.id
                        Row(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) Color(theme.accentColor).copy(alpha = 0.22f)
                                    else Color(theme.keyBackgroundColor).copy(alpha = 0.7f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) Color(theme.accentColor) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectCategory(cat.id) }
                                .padding(horizontal = 7.dp)
                                .testTag("emoji_cat_${cat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = cat.iconUnicode, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cat.labelEn,
                                color = if (isSel) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Suggested / Related strip + Search history quick chips
                val stripItems = if (related.isNotEmpty()) related else suggested
                if (stripItems.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = if (related.isNotEmpty()) "Related:" else "Suggested:",
                                color = Color(theme.accentColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        items(stripItems) { entry ->
                            val display = preferredVariants[entry.unicode] ?: entry.unicode
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(theme.keyBackgroundColor).copy(alpha = 0.65f))
                                    .clickable { onEmojiClick(entry, display) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = display, fontSize = 18.sp)
                            }
                        }
                        lastSelectedEmoji?.let { sel ->
                            item {
                                val isFav = sel in favoriteEmojis
                                IconButton(
                                    onClick = { onToggleFavorite(sel) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite Emoji",
                                        tint = if (isFav) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (emojiSearchHistory.isNotEmpty() && searchResults.isEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(emojiSearchHistory) { term ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(theme.keyBackgroundColor))
                                .clickable { onPickSearchHistory(term) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(term, color = Color(theme.keyTextColor), fontSize = 11.sp)
                        }
                    }
                    item {
                        Text(
                            text = "Clear",
                            color = Color(theme.accentColor),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onClearSearchHistory() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (displayedEmojis.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMsg = when {
                        searchQuery.isNotBlank() -> "No emojis matching \"$searchQuery\""
                        selectedCategoryId == EmojiCategory.RECENTS.id -> "No recent emojis yet. Tap any emoji to start!"
                        selectedCategoryId == EmojiCategory.FREQUENT.id -> "Frequently used emojis appear here as you type."
                        selectedCategoryId == EmojiCategory.FAVORITES.id -> "Long-press any emoji to add it to Favorites."
                        else -> "No emojis in this category."
                    }
                    Text(
                        text = emptyMsg,
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 12.sp,
                        fontFamily = HindSiliguri,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("emoji_grid"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(displayedEmojis, key = { "${selectedCategoryId}_${it.unicode}" }) { entry ->
                        val displayUnicode = preferredVariants[entry.unicode] ?: entry.unicode
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { onEmojiClick(entry, displayUnicode) },
                                    onLongClick = { onEmojiLongClick(entry) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayUnicode,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center
                            )
                            if (entry.variants.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.accentColor).copy(alpha = 0.65f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-press Variant & Favorite Overlay
        variantSheetEntry?.let { entry ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(theme.popupBackgroundColor))
                    .border(1.2.dp, Color(theme.accentColor), RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .testTag("emoji_variant_sheet")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.name.replaceFirstChar { it.uppercase() },
                            color = Color(theme.popupTextColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val isFav = entry.unicode in favoriteEmojis
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onToggleFavorite(entry.unicode) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (isFav) Color(theme.accentColor) else Color(theme.popupTextColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismissVariantSheet,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(theme.popupTextColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    val options = listOf(entry.unicode) + entry.variants
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(options) { variant ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(theme.keyBackgroundColor))
                                    .clickable { onSelectVariant(entry.unicode, variant) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = variant, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifTabContent(
    theme: KeyboardTheme,
    imageLoader: ImageLoader,
    giphyRepository: GiphyRepository,
    gifSubTab: GifSubTab,
    onChangeSubTab: (GifSubTab) -> Unit,
    searchQuery: String,
    gifState: GifSearchUiState,
    gifCategories: List<GifCategoryItem>,
    gifSuggestions: List<String>,
    recentGifs: List<GifItem>,
    favoriteGifs: List<GifItem>,
    gifSearchHistory: List<String>,
    supportsGifCommit: Boolean,
    onSelectQuery: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onGifClick: (GifItem) -> Unit,
    onShareGif: (GifItem) -> Unit,
    onToggleFavoriteGif: (GifItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(gifState) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && gifState is GifSearchUiState.Success && gifState.hasMore && !gifState.isLoadingMore) {
            onLoadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // GIF Sub-Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val tabs = listOf(
                GifSubTab.TRENDING to "Trending",
                GifSubTab.CATEGORIES to "Categories",
                GifSubTab.RECENT to "Recent",
                GifSubTab.FAVORITES to "Favorites"
            )
            items(tabs) { (tab, label) ->
                val selected = gifSubTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Color(theme.accentColor).copy(alpha = 0.22f)
                            else Color(theme.keyBackgroundColor).copy(alpha = 0.7f)
                        )
                        .border(
                            1.dp,
                            if (selected) Color(theme.accentColor) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onChangeSubTab(tab) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Autocomplete Suggestions or Search History
        if (gifSuggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(gifSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .clickable { onSelectQuery(suggestion) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(suggestion, color = Color(theme.accentColor), fontSize = 11.sp)
                    }
                }
            }
        } else if (searchQuery.isBlank() && gifSearchHistory.isNotEmpty() && gifSubTab == GifSubTab.TRENDING) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(gifSearchHistory) { term ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .clickable { onSelectQuery(term) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(term, color = Color(theme.keyTextColor), fontSize = 11.sp)
                    }
                }
                item {
                    Text(
                        text = "Clear",
                        color = Color(theme.accentColor),
                        fontSize = 10.5.sp,
                        modifier = Modifier
                            .clickable { onClearSearchHistory() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (!supportsGifCommit && (gifSubTab == GifSubTab.RECENT || gifSubTab == GifSubTab.FAVORITES || gifState is GifSearchUiState.Success)) {
            Text(
                text = "Note: Active text field doesn't support direct GIF insertion; tap Share on any GIF.",
                color = Color(theme.keySecondaryTextColor),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        when {
            gifSubTab == GifSubTab.RECENT -> {
                GifGrid(
                    items = recentGifs,
                    emptyMessage = "No recently used GIFs yet.",
                    favoriteIds = favoriteGifs.map { it.id }.toSet(),
                    theme = theme,
                    imageLoader = imageLoader,
                    gridState = gridState,
                    isLoadingMore = false,
                    onGifClick = onGifClick,
                    onShareGif = onShareGif,
                    onToggleFavorite = onToggleFavoriteGif
                )
            }

            gifSubTab == GifSubTab.FAVORITES -> {
                GifGrid(
                    items = favoriteGifs,
                    emptyMessage = "Tap ★ on any GIF to save it to Favorites.",
                    favoriteIds = favoriteGifs.map { it.id }.toSet(),
                    theme = theme,
                    imageLoader = imageLoader,
                    gridState = gridState,
                    isLoadingMore = false,
                    onGifClick = onGifClick,
                    onShareGif = onShareGif,
                    onToggleFavorite = onToggleFavoriteGif
                )
            }

            gifSubTab == GifSubTab.CATEGORIES && searchQuery.isBlank() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(gifCategories) { cat ->
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(theme.keyBackgroundColor))
                                .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                                .clickable { onSelectQuery(cat.searchQuery) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.name,
                                color = Color(theme.keyTextColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            gifState is GifSearchUiState.Disabled -> {
                GifStatusBox(
                    theme = theme,
                    title = "Online GIF Search is Disabled",
                    body = "Enable 'Online GIF Search & Previews' in Okkhor Input Settings to browse GIFs. Local emojis and Emoji Kitchen continue working offline."
                )
            }

            gifState is GifSearchUiState.Unconfigured -> {
                GifStatusBox(
                    theme = theme,
                    title = "GIPHY API Key Not Configured",
                    body = "Configure GIPHY_API_KEY in AI Studio Secrets to enable live trending and keyword GIF search. All Unicode emojis and Emoji Mashups remain available offline."
                )
            }

            gifState is GifSearchUiState.Offline -> {
                GifStatusBox(
                    theme = theme,
                    title = "No Internet Connection",
                    body = "GIF search requires an active internet connection. You can still browse Recent/Favorite GIFs or use offline emojis and Mashups.",
                    showRetry = true,
                    onRetry = onRetry
                )
            }

            gifState is GifSearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(theme.accentColor), modifier = Modifier.size(28.dp))
                }
            }

            gifState is GifSearchUiState.Error -> {
                GifStatusBox(
                    theme = theme,
                    title = "Unable to Load GIFs",
                    body = gifState.message,
                    showRetry = true,
                    onRetry = onRetry
                )
            }

            gifState is GifSearchUiState.Success -> {
                GifGrid(
                    items = gifState.items,
                    emptyMessage = if (searchQuery.isBlank()) "No trending GIFs found." else "No GIFs found for \"$searchQuery\".",
                    favoriteIds = favoriteGifs.map { it.id }.toSet(),
                    theme = theme,
                    imageLoader = imageLoader,
                    gridState = gridState,
                    isLoadingMore = gifState.isLoadingMore,
                    onGifClick = onGifClick,
                    onShareGif = onShareGif,
                    onToggleFavorite = onToggleFavoriteGif
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun GifGrid(
    items: List<GifItem>,
    emptyMessage: String,
    favoriteIds: Set<String>,
    theme: KeyboardTheme,
    imageLoader: ImageLoader,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    isLoadingMore: Boolean,
    onGifClick: (GifItem) -> Unit,
    onShareGif: (GifItem) -> Unit,
    onToggleFavorite: (GifItem) -> Unit
) {
    val context = LocalContext.current
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = emptyMessage,
                color = Color(theme.keySecondaryTextColor),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("gif_grid"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 4.dp)
    ) {
        items(items, key = { it.id }) { gif ->
            val isFav = gif.id in favoriteIds
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(theme.keyBackgroundColor))
                    .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(10.dp))
                    .clickable { onGifClick(gif) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(gif.previewUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = gif.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite GIF",
                        tint = if (isFav) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onToggleFavorite(gif) }
                    )
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share GIF",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onShareGif(gif) }
                    )
                }
            }
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(theme.accentColor), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun GifStatusBox(
    theme: KeyboardTheme,
    title: String,
    body: String,
    showRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = title,
                tint = Color(theme.accentColor),
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = title,
                color = Color(theme.keyTextColor),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                color = Color(theme.keySecondaryTextColor),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
            if (showRetry) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(theme.accentColor))
                        .clickable { onRetry() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(theme.accentTextColor), modifier = Modifier.size(14.dp))
                    Text("Retry", color = Color(theme.accentTextColor), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmojiKitchenTabContent(
    theme: KeyboardTheme,
    searchQuery: String,
    firstEmoji: String,
    secondEmoji: String,
    pickingSlot: Int,
    subFilter: String,
    recentMashups: List<EmojiMashupRecord>,
    favoriteMashups: List<EmojiMashupRecord>,
    supportsImageCommit: Boolean,
    onChangeSlot: (Int) -> Unit,
    onChangeSubFilter: (String) -> Unit,
    onPickEmoji: (String) -> Unit,
    onPickPair: (String, String) -> Unit,
    onRandomize: () -> Unit,
    onInsertMashup: (String, String, EmojiMashupStyle) -> Unit,
    onShareMashup: (String, String, EmojiMashupStyle) -> Unit,
    onToggleFavoriteMashup: (EmojiMashupRecord) -> Unit
) {
    val pairMeta = remember(firstEmoji, secondEmoji) {
        EmojiKitchenEngine.buildPairMetadata(firstEmoji, secondEmoji)
    }
    val compatibleChoices = remember(pickingSlot, firstEmoji) {
        if (pickingSlot == 1) EmojiKitchenEngine.COMPATIBLE_BASE_EMOJIS
        else EmojiKitchenEngine.getCompatibleSecondEmojis(firstEmoji)
    }
    val searchPairs = remember(searchQuery, firstEmoji) {
        if (searchQuery.isBlank()) emptyList()
        else EmojiKitchenEngine.searchCompatiblePairs(searchQuery, selectedFirst = null, limit = 16)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Kitchen Builder Row: [Emoji 1] + [Emoji 2] | Random | Recents/Favorites filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (pickingSlot == 1) Color(theme.accentColor).copy(alpha = 0.25f)
                        else Color(theme.keyBackgroundColor)
                    )
                    .border(
                        1.5.dp,
                        if (pickingSlot == 1) Color(theme.accentColor) else Color(theme.keyBorderColor),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onChangeSubFilter("all")
                        onChangeSlot(1)
                    }
                    .testTag("kitchen_slot_1"),
                contentAlignment = Alignment.Center
            ) {
                Text(firstEmoji, fontSize = 21.sp)
            }

            Text("+", color = Color(theme.accentColor), fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (pickingSlot == 2) Color(theme.accentColor).copy(alpha = 0.25f)
                        else Color(theme.keyBackgroundColor)
                    )
                    .border(
                        1.5.dp,
                        if (pickingSlot == 2) Color(theme.accentColor) else Color(theme.keyBorderColor),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onChangeSubFilter("all")
                        onChangeSlot(2)
                    }
                    .testTag("kitchen_slot_2"),
                contentAlignment = Alignment.Center
            ) {
                Text(secondEmoji, fontSize = 21.sp)
            }

            IconButton(
                onClick = {
                    onChangeSubFilter("all")
                    onRandomize()
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(theme.keyBackgroundColor))
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = "Random Mashup", tint = Color(theme.accentColor), modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            listOf("all" to "Mix", "recent" to "Recent", "favorites" to "Fav").forEach { (id, lbl) ->
                val active = subFilter == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(theme.accentColor) else Color(theme.keyBackgroundColor))
                        .clickable { onChangeSubFilter(id) }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lbl,
                        color = if (active) Color(theme.accentTextColor) else Color(theme.keyTextColor),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            if (searchPairs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No compatible mashup recipes match \"$searchQuery\".",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(searchPairs, key = { it.pairId }) { pair ->
                        MashupPreviewCard(
                            first = pair.first,
                            second = pair.second,
                            style = EmojiMashupStyle.FUSION_BLEND,
                            caption = pair.titleEn,
                            isFavorite = favoriteMashups.any {
                                it.first == pair.first && it.second == pair.second && it.styleId == EmojiMashupStyle.FUSION_BLEND.id
                            },
                            theme = theme,
                            onInsert = {
                                onPickPair(pair.first, pair.second)
                                onInsertMashup(pair.first, pair.second, EmojiMashupStyle.FUSION_BLEND)
                            },
                            onShare = { onShareMashup(pair.first, pair.second, EmojiMashupStyle.FUSION_BLEND) },
                            onToggleFavorite = {
                                onToggleFavoriteMashup(
                                    EmojiMashupRecord(pair.first, pair.second, EmojiMashupStyle.FUSION_BLEND.id, pair.titleEn)
                                )
                            }
                        )
                    }
                }
            }
            return@Column
        }

        if (subFilter == "recent" || subFilter == "favorites") {
            val records = if (subFilter == "recent") recentMashups else favoriteMashups
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (subFilter == "recent") "No recent mashups yet." else "Tap ★ on a mashup sticker to favorite it.",
                        color = Color(theme.keySecondaryTextColor),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(records, key = { it.id }) { rec ->
                        val style = EmojiMashupStyle.entries.find { it.id == rec.styleId } ?: EmojiMashupStyle.FUSION_BLEND
                        MashupPreviewCard(
                            first = rec.first,
                            second = rec.second,
                            style = style,
                            caption = style.labelEn,
                            isFavorite = favoriteMashups.any { it.id == rec.id },
                            theme = theme,
                            onInsert = { onInsertMashup(rec.first, rec.second, style) },
                            onShare = { onShareMashup(rec.first, rec.second, style) },
                            onToggleFavorite = { onToggleFavoriteMashup(rec) }
                        )
                    }
                }
            }
            return@Column
        }

        // Compatible Emoji Picker Strip for active Slot 1 or Slot 2
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(compatibleChoices) { choice ->
                val isSelected = (pickingSlot == 1 && choice == firstEmoji) || (pickingSlot == 2 && choice == secondEmoji)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(theme.accentColor).copy(alpha = 0.25f)
                            else Color(theme.keyBackgroundColor)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(theme.accentColor) else Color(theme.keyBorderColor),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onPickEmoji(choice) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(choice, fontSize = 19.sp)
                }
            }
        }

        if (pairMeta == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Select two different compatible emojis to generate mashup stickers.",
                    color = Color(theme.keySecondaryTextColor),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "${pairMeta.titleEn} • ${pairMeta.titleBn}" + if (!supportsImageCommit) " (Tap Share if app blocks sticker paste)" else "",
                color = Color(theme.keySecondaryTextColor),
                fontSize = 10.5.sp,
                fontFamily = HindSiliguri,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("kitchen_results_grid"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(EmojiMashupStyle.entries) { style ->
                    val rec = EmojiMashupRecord(
                        first = firstEmoji,
                        second = secondEmoji,
                        styleId = style.id,
                        label = "${pairMeta.titleEn} (${style.labelEn})"
                    )
                    MashupPreviewCard(
                        first = firstEmoji,
                        second = secondEmoji,
                        style = style,
                        caption = style.labelEn,
                        isFavorite = favoriteMashups.any { it.id == rec.id },
                        theme = theme,
                        onInsert = { onInsertMashup(firstEmoji, secondEmoji, style) },
                        onShare = { onShareMashup(firstEmoji, secondEmoji, style) },
                        onToggleFavorite = { onToggleFavoriteMashup(rec) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MashupPreviewCard(
    first: String,
    second: String,
    style: EmojiMashupStyle,
    caption: String,
    isFavorite: Boolean,
    theme: KeyboardTheme,
    onInsert: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val bmp: Bitmap = remember(first, second, style) {
        EmojiKitchenEngine.renderMashupBitmap(first, second, style, sizePx = 192)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(theme.keyBackgroundColor))
            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(12.dp))
            .clickable { onInsert() }
            .padding(4.dp)
            .testTag("mashup_card_${style.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "$first + $second $caption mashup",
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite Mashup",
                tint = if (isFavorite) Color(theme.accentColor) else Color(theme.keySecondaryTextColor),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onToggleFavorite() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = caption,
                color = Color(theme.keyTextColor),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Mashup",
                tint = Color(theme.accentColor),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onShare() }
            )
        }
    }
}

@Composable
private fun InlineSearchKeyboard(
    theme: KeyboardTheme,
    isBanglaMode: Boolean,
    onToggleLanguage: () -> Unit,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit
) {
    val row1 = if (isBanglaMode) listOf("অ", "আ", "ই", "উ", "এ", "ও", "ক", "খ", "গ", "ঘ") else KeyboardLayouts.ENGLISH_ROW_1
    val row2 = if (isBanglaMode) listOf("চ", "জ", "ট", "ড", "ত", "দ", "ন", "প", "ব") else KeyboardLayouts.ENGLISH_ROW_2
    val row3 = if (isBanglaMode) listOf("ম", "য", "র", "ল", "শ", "স", "হ", "া", "ি", "ু", "ে", "ো") else KeyboardLayouts.ENGLISH_ROW_3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inline_search_keyboard"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        listOf(row1, row2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(7.dp))
                            .clickable { onChar(key) }
                            .testTag("search_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = Color(theme.keyTextColor), fontSize = 14.sp, fontFamily = HindSiliguri)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(theme.specialKeyBackgroundColor))
                    .clickable { onToggleLanguage() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isBanglaMode) "EN" else "বাং", color = Color(theme.accentColor), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            row3.forEach { key ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(theme.keyBackgroundColor))
                        .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(7.dp))
                        .clickable { onChar(key) }
                        .testTag("search_key_$key"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, color = Color(theme.keyTextColor), fontSize = 13.sp, fontFamily = HindSiliguri)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(theme.specialKeyBackgroundColor))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Color(theme.specialKeyTextColor), modifier = Modifier.size(16.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(theme.specialKeyBackgroundColor))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to grid", tint = Color(theme.specialKeyTextColor), modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .weight(4f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(theme.keyBackgroundColor))
                    .border(1.dp, Color(theme.keyBorderColor), RoundedCornerShape(7.dp))
                    .clickable { onChar(" ") },
                contentAlignment = Alignment.Center
            ) {
                Text("Space", color = Color(theme.keySecondaryTextColor), fontSize = 11.5.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(theme.accentColor))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("Search", color = Color(theme.accentTextColor), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildGifImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
