package com.example.okkhor.engine

/**
 * Authentic Avro Phonetic Transliteration Engine.
 * Follows OmicronLab Avro Phonetic rules:
 * - Vowels: Independent vs Dependent (Kar)
 * - Inherent 'o' vowel breaker to prevent unwanted conjuncts (e.g. koto -> কত vs kto -> ক্ত)
 * - Automatic and explicit conjuncts (যুক্তবর্ণ via Hasanta/Virama)
 * - Reph (র্): 'r' followed by consonant preceded by vowel or word-start
 * - Ra-phala (্র): consonant followed by 'r'
 * - Ya-phala (্য): consonant followed by 'y' or 'Z'
 * - Ba-phala (্ব): consonant followed by 'w'
 * - Khanda-Ta (ৎ): 't`' or 't''
 * - Anusvara (ং): 'ng', Chandrabindu (ঁ): '^', Bisarga (ঃ): ':'
 * - Hasanta explicit virama: ',,'
 * - Dari: '.' or '|'
 */
class BanglaPhoneticEngine {

    companion object {
        // Independent vowels (at word start or following a vowel)
        val INDEPENDENT_VOWELS = listOf(
            "rri" to "ঋ",
            "ri" to "ঋ",
            "aa" to "আ",
            "a" to "আ",
            "A" to "আ",
            "ee" to "ঈ",
            "ii" to "ঈ",
            "i" to "ই",
            "I" to "ঈ",
            "oo" to "ঊ",
            "uu" to "ঊ",
            "u" to "উ",
            "U" to "ঊ",
            "oi" to "ঐ",
            "ou" to "ঔ",
            "e" to "এ",
            "E" to "এ",
            "O" to "ও",
            "o" to "অ"
        )

        // Dependent vowel signs (কার) attached to consonants
        val VOWEL_SIGNS = listOf(
            "rri" to "ৃ",
            "ri" to "ৃ",
            "aa" to "া",
            "a" to "া",
            "A" to "া",
            "ee" to "ী",
            "ii" to "ী",
            "i" to "ি",
            "I" to "ী",
            "oo" to "ূ",
            "uu" to "ূ",
            "u" to "ু",
            "U" to "ূ",
            "oi" to "ৈ",
            "ou" to "ৌ",
            "e" to "ে",
            "E" to "্যা", // Avro feature: consonant + E = ্যা (e.g. kE -> ক্যা, bE -> ব্যা)
            "O" to "ো",
            "o" to ""     // Inherent vowel breaker: preserves base consonant without kar
        )

        // Multi-character consonant & ligature mappings (longest first)
        val SPECIAL_CONJUNCTS = listOf(
            "kkhN" to "ক্ষ্ণ",
            "kkhm" to "ক্ষ্ম",
            "kkh" to "ক্ষ",
            "ksh" to "ক্ষ",
            "kSh" to "ক্ষ",
            "shch" to "শ্ছ",
            "nggh" to "ঙ্ঘ",
            "ngkh" to "ঙ্খ",
            "ngg" to "ঙ্গ",
            "ngk" to "ঙ্ক",
            "ShTh" to "ষ্ঠ",
            "sTh" to "ষ্ঠ",
            "ShT" to "ষ্ট",
            "sT" to "স্ট",
            "ShN" to "ষ্ণ",
            "NTh" to "ণ্ঠ",
            "NDh" to "ণ্ঢ",
            "tth" to "ত্থ",
            "ndh" to "ন্ধ",
            "nth" to "ন্থ",
            "mph" to "ম্ফ",
            "mbh" to "ম্ভ",
            "sth" to "স্থ",
            "sph" to "স্ফ",
            "jjh" to "জ্ঝ",
            "jnj" to "জ্ঞ",
            "jG" to "জ্ঞ",
            "gy" to "জ্ঞ",
            "shc" to "শ্চ",
            "shn" to "শ্ন",
            "shr" to "শ্র",
            "shl" to "শ্ল",
            "shm" to "শ্ম",
            "Shp" to "ষ্প",
            "Shph" to "ষ্ফ",
            "Shm" to "ষ্ম",
            "Shk" to "ষ্ক",
            "ntr" to "ন্ত্র",
            "ndr" to "ন্দ্র",
            "ndhr" to "ন্ধ্র",
            "str" to "স্ত্র",
            "mpr" to "ম্প্র",
            "mbr" to "ম্ব্র",
            "kt" to "ক্ত",
            "kr" to "ক্র",
            "kl" to "ক্ল",
            "kw" to "ক্ব",
            "cc" to "চ্চ",
            "cch" to "চ্ছ",
            "jj" to "জ্জ",
            "TT" to "ট্ট",
            "DD" to "ড্ড",
            "NT" to "ণ্ট",
            "ND" to "ণ্ড",
            "NN" to "ণ্ণ",
            "tt" to "ত্ত",
            "tr" to "ত্র",
            "tm" to "ত্ম",
            "nd" to "ন্দ",
            "nt" to "ন্ত",
            "nn" to "ন্ন",
            "bd" to "ব্দ",
            "bb" to "ব্ব",
            "br" to "ব্র",
            "bl" to "ব্ল",
            "mp" to "ম্প",
            "mb" to "ম্ব",
            "mm" to "ম্ম",
            "st" to "স্ত",
            "sp" to "স্প",
            "sn" to "স্ন",
            "sm" to "স্ম",
            "sk" to "স্ক",
            "skh" to "স্খ",
            "sl" to "স্ল",
            "kk" to "ক্ক",
            "dd" to "দ্দ",
            "ddh" to "দ্ধ",
            "gg" to "গ্গ",
            "gdh" to "গ্ধ",
            "gn" to "গ্ন",
            "gm" to "গ্ম",
            "gl" to "গ্ল",
            "pt" to "প্ত",
            "pn" to "প্ন",
            "pp" to "প্প",
            "pl" to "প্ল",
            "ps" to "প্স",
            "mn" to "ম্ন",
            "ml" to "ম্ল",
            "ll" to "ল্ল",
            "lk" to "ল্ক",
            "lg" to "ল্গ",
            "ld" to "ল্দ",
            "lp" to "ল্প",
            "lb" to "ল্ব",
            "lm" to "ল্ম"
        )

        // Single and digraph consonants
        val CONSONANTS = listOf(
            "kh" to "খ",
            "gh" to "ঘ",
            "Ng" to "ঙ",
            "ch" to "ছ",
            "jh" to "ঝ",
            "NG" to "ঞ",
            "Th" to "ঠ",
            "Dh" to "ঢ",
            "th" to "থ",
            "dh" to "ধ",
            "ph" to "ফ",
            "bh" to "ভ",
            "shh" to "ষ",
            "sh" to "শ",
            "Sh" to "ষ",
            "Rh" to "ঢ়",
            "k" to "ক",
            "K" to "ক",
            "g" to "গ",
            "G" to "গ",
            "c" to "চ",
            "C" to "চ",
            "j" to "জ",
            "J" to "জ",
            "T" to "ট",
            "D" to "ড",
            "N" to "ণ",
            "t" to "ত",
            "d" to "দ",
            "n" to "ন",
            "p" to "প",
            "P" to "প",
            "f" to "ফ",
            "F" to "ফ",
            "b" to "ব",
            "B" to "ব",
            "v" to "ভ",
            "V" to "ভ",
            "m" to "ম",
            "M" to "ম",
            "z" to "য",
            "Z" to "্য",
            "y" to "য়",
            "Y" to "য়",
            "r" to "র",
            "l" to "ল",
            "L" to "ল",
            "s" to "স",
            "S" to "ষ",
            "h" to "হ",
            "H" to "হ",
            "R" to "ড়",
            "w" to "ওয়",
            "W" to "ওয়"
        )

        fun transliterate(input: String): String {
            return BanglaPhoneticEngine().transliterate(input)
        }
    }

    /**
     * Transliterates a Romanized string into Bengali following Avro rules.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Preserve uppercase acronyms typed with Caps Lock or Shift (e.g. "OK", "USA", "SMS", "AI", "PIN")
        if (input.length >= 2 && input.all { it.isUpperCase() && it in 'A'..'Z' }) {
            if (input == "OK") return "OK"
            val isAvroConjunct = SPECIAL_CONJUNCTS.any { it.first == input }
            if (!isAvroConjunct) {
                val dictWord = BanglaDictionary.COMMON_WORD_MAP[input.lowercase()]?.firstOrNull()
                return dictWord ?: input
            }
        }

        // Direct dictionary match for authentic Avro phonetic vocabulary
        val dictWord = BanglaDictionary.COMMON_WORD_MAP[input.lowercase()]?.firstOrNull()
        if (dictWord != null) {
            return dictWord
        }

        val sb = StringBuilder()
        var i = 0
        val len = input.length
        var prevWasConsonant = false
        var lastConsonantGlyph = ""

        while (i < len) {
            val remaining = input.substring(i)

            // 1. Explicit Hasanta / Virama: ",,"
            if (remaining.startsWith(",,")) {
                sb.append(BanglaUnicodeHelper.HASANTA)
                i += 2
                prevWasConsonant = true
                continue
            }

            // 2. Khanda-Ta: "t`" or "t'"
            if (remaining.startsWith("t`") || remaining.startsWith("t'")) {
                sb.append("ৎ")
                i += 2
                prevWasConsonant = false
                continue
            }

            // 3. Anusvara: "ng" (when at end of word or before non-vowels)
            if (remaining.startsWith("ng") && (remaining.length == 2 || remaining[2] !in "aeiouAEIOU")) {
                sb.append("ং")
                i += 2
                prevWasConsonant = false
                continue
            }

            // 4. Chandrabindu: "^"
            if (remaining.startsWith("^")) {
                sb.append(BanglaUnicodeHelper.CHANDRABINDU)
                i += 1
                continue
            }

            // 5. Bisarga: ":"
            if (remaining.startsWith(":")) {
                sb.append(BanglaUnicodeHelper.BISARGA)
                i += 1
                prevWasConsonant = false
                continue
            }

            // 6. Bengali Dari / Danda: "|"
            if (remaining.startsWith("|")) {
                sb.append(BanglaUnicodeHelper.BENGALI_DANDA)
                i += 1
                prevWasConsonant = false
                continue
            }

            // 7. Reph (র্): 'r' followed by a consonant (only if preceded by vowel or word start)
            if (remaining.startsWith("r") && remaining.length >= 2 && !prevWasConsonant) {
                val nextChar = remaining[1]
                if (isConsonantChar(nextChar) && nextChar != 'r') {
                    val sub = remaining.substring(1)
                    val matchCons = matchConsonantOrConjunct(sub)
                    if (matchCons != null) {
                        sb.append("র্").append(matchCons.second)
                        i += 1 + matchCons.first
                        prevWasConsonant = true
                        lastConsonantGlyph = matchCons.second
                        continue
                    }
                }
            }

            // 8. If previous was consonant, check for attached Kar, Ya-phala, Ra-phala, or Ba-phala
            if (prevWasConsonant) {
                // Ya-phala: 'y' or 'Z'
                if (remaining.startsWith("y") || remaining.startsWith("Z")) {
                    sb.append(BanglaUnicodeHelper.HASANTA).append("য")
                    i += 1
                    prevWasConsonant = true
                    continue
                }

                // Ra-phala: 'r' immediately after a consonant (e.g. k + r -> ক্র, b + r -> ব্র, t + r -> ত্র)
                if (remaining.startsWith("r")) {
                    sb.append(BanglaUnicodeHelper.HASANTA).append("র")
                    i += 1
                    prevWasConsonant = true
                    continue
                }

                // Ba-phala: 'w' immediately after a consonant (e.g. d + w -> দ্ব, s + w -> স্ব)
                if (remaining.startsWith("w")) {
                    sb.append(BanglaUnicodeHelper.HASANTA).append("ব")
                    i += 1
                    prevWasConsonant = true
                    continue
                }

                // Vowel sign (কার) attached to consonant
                val matchedVowel = matchVowelSign(remaining)
                if (matchedVowel != null) {
                    sb.append(matchedVowel.second)
                    i += matchedVowel.first
                    prevWasConsonant = false
                    continue
                }
            }

            // 9. Check for Multi-character Conjuncts (e.g. kkh, shch, ntr, tr, nd, etc.)
            val matchedConjunct = matchSpecialConjunct(remaining)
            if (matchedConjunct != null) {
                if (prevWasConsonant) {
                    sb.append(BanglaUnicodeHelper.HASANTA)
                }
                sb.append(matchedConjunct.second)
                i += matchedConjunct.first
                prevWasConsonant = true
                lastConsonantGlyph = matchedConjunct.second
                continue
            }

            // 10. Check for Single or Digraph Consonants (e.g. k, kh, g, gh, etc.)
            val matchedConsonant = matchConsonant(remaining)
            if (matchedConsonant != null) {
                if (prevWasConsonant) {
                    // Automatic conjunct joiner for consecutive consonants!
                    sb.append(BanglaUnicodeHelper.HASANTA)
                }
                sb.append(matchedConsonant.second)
                i += matchedConsonant.first
                prevWasConsonant = true
                lastConsonantGlyph = matchedConsonant.second
                continue
            }

            // 11. Independent Vowel (at word-start or after a vowel)
            val matchedIndVowel = matchIndependentVowel(remaining)
            if (matchedIndVowel != null) {
                sb.append(matchedIndVowel.second)
                i += matchedIndVowel.first
                prevWasConsonant = false
                continue
            }

            // Fallback for non-alphabetic or untransliterated characters (e.g. digits, punctuation)
            sb.append(input[i])
            prevWasConsonant = false
            i++
        }

        return sb.toString()
    }

    private fun matchSpecialConjunct(s: String): Pair<Int, String>? {
        for ((trigger, glyph) in SPECIAL_CONJUNCTS) {
            if (s.startsWith(trigger)) {
                return Pair(trigger.length, glyph)
            }
        }
        return null
    }

    private fun matchConsonant(s: String): Pair<Int, String>? {
        for ((trigger, glyph) in CONSONANTS) {
            if (s.startsWith(trigger)) {
                return Pair(trigger.length, glyph)
            }
        }
        return null
    }

    private fun matchConsonantOrConjunct(s: String): Pair<Int, String>? {
        return matchSpecialConjunct(s) ?: matchConsonant(s)
    }

    private fun matchVowelSign(s: String): Pair<Int, String>? {
        for ((k, sign) in VOWEL_SIGNS) {
            if (s.startsWith(k)) {
                return Pair(k.length, sign)
            }
        }
        return null
    }

    private fun matchIndependentVowel(s: String): Pair<Int, String>? {
        for ((k, vowel) in INDEPENDENT_VOWELS) {
            if (s.startsWith(k)) {
                return Pair(k.length, vowel)
            }
        }
        return null
    }

    private fun isConsonantChar(c: Char): Boolean {
        return (c in 'a'..'z' || c in 'A'..'Z') && c !in "aeiouAEIOU"
    }
}
