package com.example.okkhor.engine

object BanglaUnicodeHelper {

    const val HASANTA = '\u09CD'
    const val ANUSVARA = '\u0982'
    const val BISARGA = '\u0983'
    const val CHANDRABINDU = '\u0981'
    const val NUKTA = '\u09BC'
    const val KHANDA_TA = '\u09CE'
    const val BENGALI_DANDA = '\u0964' // ।

    private val VOWEL_SIGNS = setOf(
        '\u09BE', // া
        '\u09BF', // ি
        '\u09C0', // ী
        '\u09C1', // ু
        '\u09C2', // ূ
        '\u09C3', // ৃ
        '\u09C7', // ে
        '\u09C8', // ৈ
        '\u09CB', // ো
        '\u09CC'  // ৌ
    )

    fun isVowelSign(c: Char): Boolean = VOWEL_SIGNS.contains(c)

    fun isBanglaChar(c: Char): Boolean = c in '\u0980'..'\u09FF'

    fun isBanglaText(text: CharSequence): Boolean = text.any { isBanglaChar(it) }

    /**
     * Calculates the number of characters to delete for a logical grapheme backspace.
     * Prevents leaving orphaned diacritics or corrupted combining sequences.
     */
    fun calculateBackspaceLength(textBeforeCursor: CharSequence): Int {
        if (textBeforeCursor.isEmpty()) return 0
        val len = textBeforeCursor.length
        val lastChar = textBeforeCursor[len - 1]

        // If last char is a surrogate pair (e.g. emoji)
        if (Character.isSurrogate(lastChar)) {
            if (len >= 2 && Character.isSurrogatePair(textBeforeCursor[len - 2], lastChar)) {
                return 2
            }
            return 1
        }

        // If ends with a combining mark (vowel sign, nukta, virama, chandrabindu)
        if (lastChar == HASANTA || isVowelSign(lastChar) || lastChar == CHANDRABINDU || lastChar == NUKTA || lastChar == ANUSVARA || lastChar == BISARGA) {
            // Delete just the combining mark
            return 1
        }

        // If previous character was virama, deleting the consonant may unravel a conjunct;
        // removing 1 is safe in standard editor composition
        return 1
    }
}
