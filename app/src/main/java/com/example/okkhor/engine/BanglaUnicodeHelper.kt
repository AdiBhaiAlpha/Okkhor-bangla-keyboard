package com.example.okkhor.engine

object BanglaUnicodeHelper {

    const val HASANTA = '\u09CD'
    const val ANUSVARA = '\u0982'
    const val BISARGA = '\u0983'
    const val CHANDRABINDU = '\u0981'
    const val NUKTA = '\u09BC'
    const val KHANDA_TA = '\u09CE'
    const val BENGALI_DANDA = '\u0964' // ।
    private const val VARIATION_SELECTOR_TEXT = '\uFE0E'
    private const val VARIATION_SELECTOR_EMOJI = '\uFE0F'

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

    private const val ZWJ = '\u200D'
    private const val KEYCAP_COMBINING = '\u20E3'

    private fun isRegionalIndicator(codePoint: Int): Boolean =
        codePoint in 0x1F1E6..0x1F1FF

    private fun isSkinToneModifier(codePoint: Int): Boolean =
        codePoint in 0x1F3FB..0x1F3FF

    /**
     * Calculates the number of UTF-16 Chars to delete for a single-step backspace when no selection exists.
     * Safely removes full ZWJ emoji clusters, skin-tone modifiers, regional indicator flags, keycaps,
     * and variation-selector emoji sequences while stepping cleanly through Bangla vowel signs/characters.
     */
    fun calculateBackspaceLength(textBeforeCursor: CharSequence): Int {
        if (textBeforeCursor.isEmpty()) return 0
        val len = textBeforeCursor.length

        // Check Bangla combining marks first (fast 1-char step per vowel sign/hasanta)
        val lastChar = textBeforeCursor[len - 1]
        if (lastChar == HASANTA || isVowelSign(lastChar) || lastChar == CHANDRABINDU ||
            lastChar == NUKTA || lastChar == ANUSVARA || lastChar == BISARGA
        ) {
            return 1
        }

        // Handle keycap emoji sequences (e.g. 1️⃣ = '1' + U+FE0F + U+20E3 or '1' + U+20E3)
        if (lastChar == KEYCAP_COMBINING && len >= 2) {
            return if (len >= 3 && textBeforeCursor[len - 2] == VARIATION_SELECTOR_EMOJI) 3 else 2
        }

        // Handle Regional Indicator flag pairs (4 chars = 2 code points)
        val lastCp = Character.codePointBefore(textBeforeCursor, len)
        val lastCpCount = Character.charCount(lastCp)
        if (isRegionalIndicator(lastCp) && len - lastCpCount > 0) {
            val prevCp = Character.codePointBefore(textBeforeCursor, len - lastCpCount)
            if (isRegionalIndicator(prevCp)) {
                return lastCpCount + Character.charCount(prevCp)
            }
        }

        // Walk backwards across an emoji cluster joined by ZWJ (U+200D), variation selectors, or skin-tone modifiers
        var deleteCount = 0
        var idx = len
        while (idx > 0) {
            val cp = Character.codePointBefore(textBeforeCursor, idx)
            val cpChars = Character.charCount(cp)
            deleteCount += cpChars
            idx -= cpChars

            if (cp == VARIATION_SELECTOR_EMOJI.code || cp == VARIATION_SELECTOR_TEXT.code || isSkinToneModifier(cp)) {
                if (idx > 0) {
                    val baseCp = Character.codePointBefore(textBeforeCursor, idx)
                    val baseChars = Character.charCount(baseCp)
                    deleteCount += baseChars
                    idx -= baseChars
                }
            }

            // If preceded by ZWJ, consume ZWJ and continue merging the preceding emoji unit
            if (idx > 0 && textBeforeCursor[idx - 1] == ZWJ) {
                deleteCount += 1
                idx -= 1
                continue
            }
            break
        }

        return deleteCount.coerceAtLeast(1)
    }

    /**
     * Advances [index] by [deltaCodePoints] within [text], never splitting a UTF-16 surrogate pair.
     */
    fun moveCodePointOffset(text: CharSequence, index: Int, deltaCodePoints: Int): Int {
        if (text.isEmpty()) return 0
        var pos = index.coerceIn(0, text.length)
        if (pos in 1 until text.length && Character.isLowSurrogate(text[pos]) && Character.isHighSurrogate(text[pos - 1])) {
            pos -= 1
        }
        if (deltaCodePoints == 0) return pos

        var remaining = deltaCodePoints
        if (remaining > 0) {
            while (remaining > 0 && pos < text.length) {
                val cp = Character.codePointAt(text, pos)
                pos = (pos + Character.charCount(cp)).coerceAtMost(text.length)
                remaining--
            }
        } else {
            while (remaining < 0 && pos > 0) {
                val cp = Character.codePointBefore(text, pos)
                pos = (pos - Character.charCount(cp)).coerceAtLeast(0)
                remaining++
            }
        }
        return pos.coerceIn(0, text.length)
    }

    /**
     * Computes how many UTF-16 Chars before cursor make up the preceding word (plus any trailing spaces),
     * cleanly stepping across surrogate pairs and Bangla conjuncts/matras without splitting code points.
     */
    fun calculateWordDeleteLength(textBeforeCursor: CharSequence): Int {
        if (textBeforeCursor.isEmpty()) return 0
        var idx = textBeforeCursor.length

        // 1. Skip any immediate trailing horizontal whitespace / punctuation run before cursor
        while (idx > 0) {
            val cp = Character.codePointBefore(textBeforeCursor, idx)
            val isSpace = Character.isWhitespace(cp)
            if (isSpace) {
                idx -= Character.charCount(cp)
            } else {
                break
            }
        }

        // If the entire prefix was whitespace, delete the whitespace run
        if (idx == 0) return textBeforeCursor.length

        // 2. If cursor is right after punctuation (not part of a Bangla word/hasanta), remove punctuation then check
        val stopChars = setOf(' ', '\n', '\t', '\r', '।', '.', ',', '!', '?', ';', ':', '/', '\\', '(', ')', '[', ']', '{', '}', '"', '\'')
        var consumedNonSpace = 0
        while (idx > 0) {
            val cp = Character.codePointBefore(textBeforeCursor, idx)
            val ch = cp.toChar()
            if (Character.isWhitespace(cp) || (cp < 0x10000 && stopChars.contains(ch))) {
                if (consumedNonSpace == 0 && cp < 0x10000 && stopChars.contains(ch)) {
                    // Delete single punctuation token at end if swipe occurred immediately after punctuation
                    idx -= Character.charCount(cp)
                    consumedNonSpace++
                }
                break
            }
            idx -= Character.charCount(cp)
            consumedNonSpace++
        }

        return (textBeforeCursor.length - idx).coerceAtLeast(1)
    }
}
