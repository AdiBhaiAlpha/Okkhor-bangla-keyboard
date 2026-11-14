package com.example.okkhor.ui.keyboard

data class KeyModel(
    val primaryLabel: String,
    val secondaryLabel: String = "",
    val code: Int = 0,
    val isSpecial: Boolean = false,
    val weight: Float = 1.0f
)

object KeyboardLayouts {

    // Action key codes
    const val CODE_SHIFT = -1
    const val CODE_DELETE = -2
    const val CODE_SWITCH_MODE = -3
    const val CODE_ENTER = -4
    const val CODE_SPACE = 32
    const val CODE_SYMBOLS = -5
    const val CODE_ABC = -6
    const val CODE_EMOJI = -7
    const val CODE_MORE_SYMBOLS = -8
    const val CODE_SETTINGS = -9
    const val CODE_CLIPBOARD = -10
    const val CODE_ONE_HANDED = -11
    const val CODE_RESIZE = -12

    val NUMBER_ROW_ENGLISH = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val NUMBER_ROW_BANGLA = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")

    val ENGLISH_ROW_1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val ENGLISH_ROW_2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val ENGLISH_ROW_3 = listOf("z", "x", "c", "v", "b", "n", "m")

    val BANGLA_FIXED_ROW_1_NORMAL = listOf("ঙ", "ত", "থ", "দ", "ধ", "ট", "ঠ", "ড", "ঢ", "ণ")
    val BANGLA_FIXED_ROW_1_SHIFT = listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও")

    val BANGLA_FIXED_ROW_2_NORMAL = listOf("ক", "খ", "গ", "ঘ", "ড়", "চ", "ছ", "জ", "ঝ", "ঢ়")
    val BANGLA_FIXED_ROW_2_SHIFT = listOf("ঔ", "া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো")

    val BANGLA_FIXED_ROW_3_NORMAL = listOf("প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ", "ষ")
    val BANGLA_FIXED_ROW_3_SHIFT = listOf("ৌ", "্", "ৎ", "ং", "ঃ", "ঁ", "য়", "।", "স", "হ")

    // Prabhat layout aliases
    val PRABHAT_ROW_1_NORMAL = BANGLA_FIXED_ROW_1_NORMAL
    val PRABHAT_ROW_1_SHIFT = BANGLA_FIXED_ROW_1_SHIFT
    val PRABHAT_ROW_2_NORMAL = BANGLA_FIXED_ROW_2_NORMAL
    val PRABHAT_ROW_2_SHIFT = BANGLA_FIXED_ROW_2_SHIFT
    val PRABHAT_ROW_3_NORMAL = BANGLA_FIXED_ROW_3_NORMAL
    val PRABHAT_ROW_3_SHIFT = BANGLA_FIXED_ROW_3_SHIFT

    val BANGLA_FIXED_BOTTOM_EXTRA = listOf("স", "হ", "্", "।")

    val SYMBOLS_ROW_1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val SYMBOLS_ROW_2 = listOf("@", "#", "৳", "$", "%", "&", "-", "+", "(", ")")
    val SYMBOLS_ROW_3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    val MORE_SYMBOLS_ROW_1 = listOf("~", "`", "|", "^", "_", "=", "/", "\\", "[", "]")
    val MORE_SYMBOLS_ROW_2 = listOf("{", "}", "<", ">", "€", "£", "¥", "₹", "©", "®")
    val MORE_SYMBOLS_ROW_3 = listOf("…", "°", "§", "•", "।", "¿", "¡")

    val BANGLA_DIGITS_ROW = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")

    val EMOJI_LIST = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
        "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😋", "😛", "😜",
        "🤪", "😝", "🤑", "🤗", "🫢", "🤫", "🤔", "🫡", "🤐", "🤨",
        "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "😮‍💨", "🤥", "😌",
        "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧",
        "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "😎", "🤓", "🧐",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
        "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "👍", "👎",
        "👏", "🙌", "👐", "🤲", "🤝", "✌️", "🤞", "🤟", "🤘", "🤙",
        "👈", "👉", "👆", "👇", "☝️", "👋", "🤚", "🖐️", "✋", "🖖",
        "🇧🇩", "🇮🇳", "🇺🇸", "🇬🇧", "🇯🇵", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇧🇷",
        "☀️", "🌙", "⭐", "🌟", "✨", "⚡", "🔥", "💧", "🎉", "🏆"
    )

    // Phonetic Bengali hint mappings shown in the upper-right corner during Phonetic typing
    val PHONETIC_HINTS: Map<String, String> = mapOf(
        "q" to "ঙ", "w" to "ও", "e" to "এ", "r" to "র", "t" to "ট",
        "y" to "য়", "u" to "উ", "i" to "ই", "o" to "ও", "p" to "প",
        "a" to "আ", "s" to "স", "d" to "ড", "f" to "ফ", "g" to "গ",
        "h" to "হ", "j" to "জ", "k" to "ক", "l" to "ল",
        "z" to "য", "x" to "ক্স", "c" to "চ", "v" to "ভ", "b" to "ব",
        "n" to "ন", "m" to "ম"
    )

    // Long-press symbols and numbers for each QWERTY key
    val LONG_PRESS_SYMBOLS: Map<String, String> = mapOf(
        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
        "a" to "@", "s" to "#", "d" to "৳", "f" to "$", "g" to "%",
        "h" to "&", "j" to "-", "k" to "+", "l" to "(",
        "z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";",
        "n" to "!", "m" to "?",
        "." to "!", "," to "?"
    )

    // Long-press Bengali numbers for Phonetic mode
    val LONG_PRESS_BANGLA_DIGITS: Map<String, String> = mapOf(
        "q" to "১", "w" to "২", "e" to "৩", "r" to "৪", "t" to "৫",
        "y" to "৬", "u" to "৭", "i" to "৮", "o" to "৯", "p" to "০",
        "a" to "@", "s" to "#", "d" to "৳", "f" to "$", "g" to "%",
        "h" to "&", "j" to "-", "k" to "+", "l" to "(",
        "z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";",
        "n" to "!", "m" to "?",
        "।" to "!", "." to "।"
    )
}
