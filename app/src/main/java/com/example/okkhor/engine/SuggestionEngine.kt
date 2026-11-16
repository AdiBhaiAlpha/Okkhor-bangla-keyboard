package com.example.okkhor.engine

import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings

class SuggestionEngine(
    private val phoneticEngine: BanglaPhoneticEngine = BanglaPhoneticEngine(),
    private val smartMemory: SmartWordMemory? = null
) {

    fun getSuggestions(
        input: String,
        mode: InputMode,
        maxCandidates: Int = 6,
        lastCommittedWord: String = "",
        precomputedTransliterated: String? = null,
        nextWordPredictionEnabled: Boolean = true
    ): List<String> {
        val candidates = LinkedHashSet<String>(maxCandidates * 2)

        // Case 1: Input is empty - provide smart next-word contextual predictions if enabled
        if (input.isBlank()) {
            if (!nextWordPredictionEnabled || lastCommittedWord.isBlank()) {
                return emptyList()
            }
            if (mode.isBangla) {
                smartMemory?.getLearnedNextWords(lastCommittedWord, maxResults = maxCandidates)?.let {
                    candidates.addAll(it)
                }
                if (candidates.size < maxCandidates) {
                    val dictNext = BanglaDictionary.getNextWordPredictions(lastCommittedWord, maxResults = maxCandidates)
                    for (word in dictNext) {
                        candidates.add(word)
                        if (candidates.size >= maxCandidates) break
                    }
                }
            } else if (mode == InputMode.ENGLISH) {
                smartMemory?.getLearnedNextWords(lastCommittedWord, maxResults = maxCandidates)?.let { learnedNext ->
                    for (word in learnedNext) {
                        if (word.any { it in 'a'..'z' || it in 'A'..'Z' }) {
                            candidates.add(word)
                            if (candidates.size >= maxCandidates) break
                        }
                    }
                }
                if (candidates.size < maxCandidates) {
                    val dictNext = EnglishDictionary.getNextWordPredictions(lastCommittedWord, maxResults = maxCandidates)
                    for (word in dictNext) {
                        candidates.add(word)
                        if (candidates.size >= maxCandidates) break
                    }
                }
            }
            return candidates.take(maxCandidates)
        }

        when (mode) {
            InputMode.BANGLA_PHONETIC -> {
                // Acronym typed explicitly in uppercase (e.g. "OK", "USA", "SMS")
                if (input.length >= 2 && isAllAsciiUpper(input)) {
                    candidates.add(input)
                }

                // 1. User's previously selected corrections for this exact Roman input
                smartMemory?.getLearnedCorrections(input)?.let { userCorrections ->
                    for (c in userCorrections) {
                        candidates.add(c)
                    }
                }

                // 2. Transliteration computed once (reuses service transliteration when available)
                val transliterated = precomputedTransliterated ?: phoneticEngine.transliterate(input)
                val exactDictMatches = BanglaDictionary.COMMON_WORD_MAP[input.lowercase()]

                // Exact phonetic dictionary match OR literal Avro transliteration ALWAYS claims primary slot
                // so prefix completions (e.g. "আমি" for input "am") never displace "আম"!
                if (exactDictMatches != null) {
                    for (m in exactDictMatches) {
                        candidates.add(m)
                    }
                }
                if (transliterated.isNotBlank()) {
                    candidates.add(transliterated)
                }

                // 3. Dictionary prefix completions fill slots 1..N (single query covers both Roman prefix and Bangla prefix)
                if (candidates.size < maxCandidates) {
                    val dictSuggestions = BanglaDictionary.findWordSuggestions(input, maxResults = maxCandidates)
                    for (match in dictSuggestions) {
                        candidates.add(match)
                        if (candidates.size >= maxCandidates) break
                    }
                }

                // 4. User personal lexicon completions
                if (candidates.size < maxCandidates && transliterated.isNotBlank()) {
                    smartMemory?.searchUserLexicon(transliterated, maxResults = 3)?.let { userCustomWords ->
                        for (w in userCustomWords) {
                            candidates.add(w)
                            if (candidates.size >= maxCandidates) break
                        }
                    }
                }

                // 5. Smart conjunct (যুক্তাক্ষর) & common Bangla spelling enhancements
                if (candidates.size < maxCandidates && transliterated.isNotBlank()) {
                    val conjunctEnhancements = BanglaConjunctHelper.getEnhancements(input, transliterated)
                    for (enh in conjunctEnhancements) {
                        candidates.add(enh)
                        if (candidates.size >= maxCandidates) break
                    }
                }

                // 6. Raw Latin input fallback for mixed Bangla-English convenience
                if (candidates.size < maxCandidates) {
                    candidates.add(input)
                }
            }

            InputMode.BANGLA_FIXED -> {
                smartMemory?.searchUserLexicon(input, maxResults = maxCandidates)?.let {
                    candidates.addAll(it)
                }
                if (candidates.size < maxCandidates) {
                    val matches = BanglaDictionary.findWordSuggestions(input, maxResults = maxCandidates)
                    for (m in matches) {
                        candidates.add(m)
                        if (candidates.size >= maxCandidates) break
                    }
                }
                if (candidates.size < maxCandidates) {
                    val enhancements = BanglaConjunctHelper.getEnhancements("", input)
                    for (enh in enhancements) {
                        candidates.add(enh)
                        if (candidates.size >= maxCandidates) break
                    }
                }
            }

            InputMode.ENGLISH -> {
                // Slot 0 ALWAYS preserves user's exact typed word so spacebar never unexpectedly overwrites it
                candidates.add(input)

                smartMemory?.let { memory ->
                    for (w in memory.getLearnedCorrections(input)) {
                        if (w.any { it in 'a'..'z' || it in 'A'..'Z' }) {
                            candidates.add(w)
                        }
                    }
                    if (candidates.size < maxCandidates) {
                        for (w in memory.searchUserLexicon(input, maxResults = maxCandidates)) {
                            if (w.any { it in 'a'..'z' || it in 'A'..'Z' }) {
                                candidates.add(w)
                                if (candidates.size >= maxCandidates) break
                            }
                        }
                    }
                }

                if (candidates.size < maxCandidates) {
                    val dictMatches = EnglishDictionary.findWordSuggestions(input, maxResults = maxCandidates)
                    for (m in dictMatches) {
                        candidates.add(m)
                        if (candidates.size >= maxCandidates) break
                    }
                }

                if (candidates.size < maxCandidates && input.isNotEmpty() && input[0].isLowerCase()) {
                    candidates.add(input.replaceFirstChar { it.uppercase() })
                }
            }

            else -> {
                // Symbols / Emoji
            }
        }

        return candidates.take(maxCandidates)
    }

    /**
     * Conservative commit-time resolver for Spacebar / Enter / Punctuation:
     * - Preserves the user's exact word by default (non-intrusive).
     * - When autoCorrect is enabled, applies only high-confidence replacements (repeatedly user-chosen
     *   corrections or exact dictionary contractions/orthography), never replacing a short word with
     *   a longer prefix completion or unknown guess.
     */
    fun resolveCommitText(
        rawInput: String,
        mode: InputMode,
        settings: KeyboardSettings,
        precomputedTransliterated: String? = null
    ): String {
        if (rawInput.isEmpty()) return ""
        return when (mode) {
            InputMode.BANGLA_PHONETIC -> {
                val transliterated = precomputedTransliterated ?: phoneticEngine.transliterate(rawInput)
                if (!settings.autoCorrect) return transliterated
                val learned = smartMemory?.getTopLearnedCorrectionWithCount(rawInput)
                if (learned != null && learned.second >= 2) {
                    return learned.first
                }
                BanglaDictionary.COMMON_WORD_MAP[rawInput.lowercase()]?.firstOrNull() ?: transliterated
            }
            InputMode.ENGLISH -> {
                if (!settings.autoCorrect) return rawInput
                val learned = smartMemory?.getTopLearnedCorrectionWithCount(rawInput)
                if (learned != null && learned.second >= 2) {
                    return learned.first
                }
                when (rawInput.lowercase()) {
                    "im" -> "I'm"
                    "dont" -> "don't"
                    "cant" -> "can't"
                    "its" -> rawInput // ambiguous ("its" vs "it's") -> preserve user text!
                    "i" -> "I"
                    else -> rawInput
                }
            }
            else -> rawInput
        }
    }

    private fun isAllAsciiUpper(s: String): Boolean {
        for (i in s.indices) {
            if (s[i] !in 'A'..'Z') return false
        }
        return true
    }
}
