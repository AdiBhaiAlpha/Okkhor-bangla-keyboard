package com.example.okkhor.emoji

enum class EmojiCategory(
    val id: Int,
    val labelEn: String,
    val labelBn: String,
    val iconUnicode: String
) {
    RECENTS(-3, "Recents", "সাম্প্রতিক", "🕒"),
    FREQUENT(-2, "Frequent", "বহুল ব্যবহৃত", "🔥"),
    FAVORITES(-1, "Favorites", "প্রিয়", "⭐"),
    SMILEYS(0, "Smileys", "অনুভূতি", "😀"),
    PEOPLE(1, "People", "মানুষ", "👋"),
    ANIMALS(2, "Nature", "প্রকৃতি", "🐻"),
    FOOD(3, "Food", "খাবার", "🍔"),
    TRAVEL(4, "Travel", "ভ্রমণ", "✈️"),
    ACTIVITIES(5, "Activity", "খেলা", "⚽"),
    OBJECTS(6, "Objects", "বস্তু", "💡"),
    SYMBOLS(7, "Symbols", "চিহ্ন", "❤️"),
    FLAGS(8, "Flags", "পতাকা", "🇧🇩");

    companion object {
        val STANDARD_CATEGORIES = listOf(
            SMILEYS, PEOPLE, ANIMALS, FOOD, TRAVEL, ACTIVITIES, OBJECTS, SYMBOLS, FLAGS
        )
    }
}

data class EmojiEntry(
    val unicode: String,
    val category: Int,
    val name: String,
    val keywords: List<String>,
    val variants: List<String> = emptyList()
)

data class GifItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val gifUrl: String,
    val width: Int = 200,
    val height: Int = 200
)

data class GifCategoryItem(
    val name: String,
    val searchQuery: String
)

sealed interface GifSearchUiState {
    data object Idle : GifSearchUiState
    data object Loading : GifSearchUiState
    data class Success(
        val items: List<GifItem>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : GifSearchUiState
    data object Unconfigured : GifSearchUiState
    data object Disabled : GifSearchUiState
    data object Offline : GifSearchUiState
    data class Error(val message: String) : GifSearchUiState
}

enum class EmojiMashupStyle(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    FUSION_BLEND("fusion", "Fusion", "মিশ্রণ"),
    AURA_HALO("halo", "Aura", "আভা"),
    DUO_BADGE("duo", "Duo", "যুগল"),
    STAMP_CARD("stamp", "Sticker", "স্টিকার")
}

data class EmojiMashupPair(
    val first: String,
    val second: String,
    val titleEn: String,
    val titleBn: String,
    val keywords: List<String>
) {
    val pairId: String get() = "${first}_${second}"
}

data class EmojiMashupRecord(
    val first: String,
    val second: String,
    val styleId: String,
    val label: String
) {
    val id: String get() = "${first}_${second}_${styleId}"
}
