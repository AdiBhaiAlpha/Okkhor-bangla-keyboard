package com.example.okkhor.engine

import com.example.okkhor.data.model.InputMode

class SuggestionEngine(
    private val phoneticEngine: BanglaPhoneticEngine = BanglaPhoneticEngine(),
    private val smartMemory: SmartWordMemory? = null
) {

    fun getSuggestions(
        input: String,
        mode: InputMode,
        maxCandidates: Int = 6,
        lastCommittedWord: String = ""
    ): List<String> {
        val candidates = mutableListOf<String>()

        // Case 1: Input is empty - provide smart next-word contextual predictions
        if (input.isBlank()) {
            if (lastCommittedWord.isNotBlank()) {
                if (mode.isBangla) {
                    // 1. First priority: Words the user personally typed after this word
                    smartMemory?.let { memory ->
                        val learnedNext = memory.getLearnedNextWords(lastCommittedWord, maxResults = maxCandidates)
                        candidates.addAll(learnedNext)
                    }

                    // 2. Second priority: Standard dictionary bigram next-word predictions
                    val dictNext = BanglaDictionary.getNextWordPredictions(lastCommittedWord, maxResults = maxCandidates)
                    for (word in dictNext) {
                        if (!candidates.contains(word)) {
                            candidates.add(word)
                        }
                    }
                } else if (mode == InputMode.ENGLISH) {
                    smartMemory?.let { memory ->
                        val learnedNext = memory.getLearnedNextWords(lastCommittedWord, maxResults = maxCandidates)
                        for (word in learnedNext) {
                            if (word.any { it in 'a'..'z' || it in 'A'..'Z' } && !candidates.contains(word)) {
                                candidates.add(word)
                            }
                        }
                    }

                    val dictNext = EnglishDictionary.getNextWordPredictions(lastCommittedWord, maxResults = maxCandidates)
                    for (word in dictNext) {
                        if (!candidates.contains(word)) {
                            candidates.add(word)
                        }
                    }
                }

                if (candidates.isNotEmpty()) {
                    return candidates.take(maxCandidates)
                }
            }
            return emptyList()
        }

        when (mode) {
            InputMode.BANGLA_PHONETIC -> {
                // If input is uppercase acronym (e.g. "OK", "USA", "SMS"), prioritize the literal uppercase word
                if (input.length >= 2 && input.all { it.isUpperCase() && it in 'A'..'Z' }) {
                    candidates.add(input)
                }

                // 1. First Priority: User's previously selected corrections for this exact input!
                smartMemory?.let { memory ->
                    val userCorrections = memory.getLearnedCorrections(input)
                    for (c in userCorrections) {
                        if (!candidates.contains(c)) candidates.add(c)
                    }
                }

                // 2. Exact phonetic match from dictionary
                val dictMatches = BanglaDictionary.findWordSuggestions(input, maxResults = 3)

                // 3. Transliterated candidate from Avro engine
                val transliterated = phoneticEngine.transliterate(input)

                if (dictMatches.isNotEmpty()) {
                    for (m in dictMatches) {
                        if (!candidates.contains(m)) candidates.add(m)
                    }
                    if (transliterated.isNotBlank() && !candidates.contains(transliterated)) {
                        candidates.add(1.coerceAtMost(candidates.size), transliterated)
                    }
                } else if (transliterated.isNotBlank() && !candidates.contains(transliterated)) {
                    candidates.add(transliterated)
                }

                // 4. User lexicon search matching prefix
                smartMemory?.let { memory ->
                    val userCustomWords = memory.searchUserLexicon(transliterated)
                    for (w in userCustomWords) {
                        if (!candidates.contains(w)) candidates.add(w)
                    }
                }

                // 5. Prefix completions based on transliterated text
                if (transliterated.isNotBlank()) {
                    val prefixMatches = BanglaDictionary.findWordSuggestions(transliterated, maxResults = 3)
                    for (match in prefixMatches) {
                        if (!candidates.contains(match)) {
                            candidates.add(match)
                        }
                    }
                }

                // 6. Raw Latin input as a fallback candidate so the user can easily keep what was typed
                if (!candidates.contains(input)) {
                    candidates.add(input)
                }
            }

            InputMode.BANGLA_FIXED -> {
                // 1. User custom words first
                smartMemory?.let { memory ->
                    val userWords = memory.searchUserLexicon(input)
                    candidates.addAll(userWords)
                }

                // 2. Bengali prefix completions
                val matches = BanglaDictionary.findWordSuggestions(input, maxResults = maxCandidates)
                for (m in matches) {
                    if (!candidates.contains(m)) candidates.add(m)
                }
            }

            InputMode.ENGLISH -> {
                // Always preserve what user typed first so space doesn't unexpectedly overwrite intentional English
                candidates.add(input)

                // User's previously selected English words/corrections
                smartMemory?.let { memory ->
                    val learned = memory.getLearnedCorrections(input)
                    for (w in learned) {
                        if (w.any { it in 'a'..'z' || it in 'A'..'Z' } && !candidates.contains(w)) {
                            candidates.add(w)
                        }
                    }
                    val lexicon = memory.searchUserLexicon(input)
                    for (w in lexicon) {
                        if (w.any { it in 'a'..'z' || it in 'A'..'Z' } && !candidates.contains(w)) {
                            candidates.add(w)
                        }
                    }
                }

                // Offline English dictionary completions & contractions
                val dictMatches = EnglishDictionary.findWordSuggestions(input, maxResults = maxCandidates)
                for (m in dictMatches) {
                    if (!candidates.contains(m)) candidates.add(m)
                }

                if (input.isNotEmpty() && input[0].isLowerCase()) {
                    val capitalized = input.replaceFirstChar { it.uppercase() }
                    if (!candidates.contains(capitalized)) {
                        candidates.add(capitalized)
                    }
                }
            }

            else -> {
                // Symbols / Emoji
            }
        }

        return candidates.distinct().take(maxCandidates)
    }
}
