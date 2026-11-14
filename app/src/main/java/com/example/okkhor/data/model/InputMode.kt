package com.example.okkhor.data.model

enum class InputMode(val title: String, val shortLabel: String) {
    BANGLA_PHONETIC("Bangla Phonetic", "ফোনেটিক"),
    BANGLA_FIXED("Bangla Fixed", "বাংলা"),
    ENGLISH("English", "ENG"),
    NUMBERS_SYMBOLS("Numbers & Symbols", "?123"),
    SECONDARY_SYMBOLS("More Symbols", "=\\<"),
    EMOJI("Emoji", "\uD83D\uDE00");

    val isBangla: Boolean
        get() = this == BANGLA_PHONETIC || this == BANGLA_FIXED

    val isTextMode: Boolean
        get() = this == BANGLA_PHONETIC || this == BANGLA_FIXED || this == ENGLISH
}
