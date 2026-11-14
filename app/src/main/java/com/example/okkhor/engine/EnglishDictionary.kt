package com.example.okkhor.engine

/**
 * Offline English Vocabulary & Bigram Engine for dedicated English QWERTY mode.
 * Provides fast prefix autocomplete, contraction normalization, and next-word predictions.
 */
object EnglishDictionary {

    val NEXT_WORD_PREDICTIONS: Map<String, List<String>> = mapOf(
        "i" to listOf("am", "have", "will", "want", "think", "know", "can", "was", "love", "need"),
        "i'm" to listOf("good", "fine", "sorry", "not", "going", "here", "ready", "busy", "happy"),
        "you" to listOf("are", "can", "have", "want", "know", "need", "think", "will"),
        "how" to listOf("are", "is", "was", "did", "can", "do", "much", "many"),
        "are" to listOf("you", "we", "they", "going", "doing", "ready"),
        "good" to listOf("morning", "night", "evening", "afternoon", "luck", "job", "idea"),
        "thank" to listOf("you", "god"),
        "thanks" to listOf("a", "for", "so", "very"),
        "what" to listOf("is", "are", "do", "did", "about", "happened", "time"),
        "where" to listOf("are", "is", "did", "do", "were"),
        "when" to listOf("will", "are", "did", "is", "can"),
        "please" to listOf("help", "send", "check", "let", "wait", "call", "tell"),
        "see" to listOf("you", "it", "that", "what"),
        "have" to listOf("a", "to", "you", "been", "some"),
        "this" to listOf("is", "was", "will", "one", "morning"),
        "that" to listOf("is", "was", "sounds", "would", "looks"),
        "can" to listOf("you", "i", "we", "help", "do"),
        "will" to listOf("be", "do", "send", "call", "check", "go"),
        "let" to listOf("me", "us", "know", "go"),
        "do" to listOf("you", "not", "it", "this", "that"),
        "don't" to listOf("know", "worry", "think", "want", "forget"),
        "it" to listOf("is", "was", "will", "looks", "sounds"),
        "it's" to listOf("okay", "fine", "good", "not", "great"),
        "on" to listOf("the", "my", "time", "your"),
        "in" to listOf("the", "my", "this", "a"),
        "for" to listOf("you", "the", "me", "this", "sure"),
        "happy" to listOf("birthday", "new", "to"),
        "take" to listOf("care", "it", "a", "your")
    )

    val COMMON_WORDS: List<String> = listOf(
        // Pronouns & Core
        "I", "I'm", "I'll", "I've", "I'd", "you", "you're", "your", "yours", "we", "we're", "our",
        "they", "they're", "their", "he", "she", "it", "it's", "its", "this", "that", "these", "those",
        // Auxiliaries & Verbs
        "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did",
        "don't", "doesn't", "didn't", "can", "can't", "cannot", "could", "couldn't", "will", "won't",
        "would", "wouldn't", "should", "shouldn't", "may", "might", "must",
        // Everyday Verbs
        "go", "going", "gone", "went", "come", "coming", "came", "get", "getting", "got",
        "make", "making", "made", "take", "taking", "took", "give", "giving", "gave",
        "know", "knowing", "knew", "think", "thinking", "thought", "want", "wanting", "wanted",
        "need", "needing", "needed", "like", "love", "hate", "feel", "feeling", "felt",
        "see", "seeing", "saw", "look", "looking", "looked", "watch", "watching",
        "say", "saying", "said", "tell", "telling", "told", "ask", "asking", "asked",
        "talk", "talking", "speak", "speaking", "call", "calling", "called",
        "send", "sending", "sent", "share", "sharing", "check", "checking", "checked",
        "help", "helping", "helped", "wait", "waiting", "waited", "work", "working", "worked",
        "play", "playing", "read", "reading", "write", "writing", "wrote", "learn", "learning",
        "buy", "buying", "start", "starting", "started", "stop", "stopping", "finish", "finished",
        "try", "trying", "tried", "use", "using", "used", "find", "finding", "found",
        "keep", "keeping", "kept", "leave", "leaving", "left", "meet", "meeting", "met",
        // Greetings & Conversational
        "hello", "hey", "hi", "bye", "goodbye", "okay", "ok", "yes", "yeah", "yep", "no", "nope",
        "please", "thanks", "thank", "welcome", "sorry", "excuse", "alright", "sure", "fine",
        "good", "great", "awesome", "nice", "amazing", "perfect", "cool", "best", "better", "bad", "worst",
        "morning", "afternoon", "evening", "night", "today", "tomorrow", "yesterday", "tonight", "now", "later",
        "soon", "always", "never", "sometimes", "maybe", "probably", "really", "actually", "literally",
        // Question words
        "what", "when", "where", "why", "who", "whom", "which", "whose", "how",
        "what's", "where's", "who's", "how's",
        // Prepositions & Conjunctions
        "and", "or", "but", "so", "because", "if", "while", "with", "without", "for", "from", "to",
        "in", "on", "at", "by", "about", "after", "before", "under", "over", "between", "into", "through",
        // Nouns & Common Words
        "time", "day", "week", "month", "year", "home", "house", "room", "office", "school", "college",
        "university", "friend", "friends", "family", "mother", "father", "brother", "sister", "brotherhood",
        "people", "person", "man", "woman", "boy", "girl", "child", "name", "number", "phone", "mobile",
        "message", "text", "email", "address", "password", "account", "photo", "video", "music", "game",
        "food", "water", "tea", "coffee", "money", "price", "market", "shop", "car", "bus", "train",
        "country", "city", "road", "world", "life", "love", "heart", "mind", "hand", "way", "thing", "things",
        "problem", "solution", "question", "answer", "news", "update", "version", "keyboard", "english", "bangla",
        "happy", "sad", "angry", "tired", "busy", "free", "ready", "safe", "fast", "slow", "easy", "hard",
        "new", "old", "big", "small", "long", "short", "high", "low", "right", "wrong", "true", "false",
        "very", "much", "many", "more", "most", "some", "any", "all", "every", "each", "other", "another",
        "first", "last", "next", "same", "different", "important", "special", "beautiful", "congratulations",
        "birthday", "anniversary", "Eid", "Bangladesh", "Dhaka"
    )

    private val COMMON_WORDS_LOWER_MAP: Map<String, String> = COMMON_WORDS.associateBy { it.lowercase() }

    /**
     * Find English word completions matching [prefix], preserving user capitalization when helpful.
     */
    fun findWordSuggestions(prefix: String, maxResults: Int = 5): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        val results = mutableListOf<String>()

        // Exact case-insensitive match first (e.g. "im" / "i'm" -> "I'm", "dhaka" -> "Dhaka")
        if (lower == "im" || lower == "i'm") results.add("I'm")
        if (lower == "dont" || lower == "don't") results.add("don't")
        if (lower == "cant" || lower == "can't") results.add("can't")
        if (lower == "its" || lower == "it's") results.add("it's")

        val exact = COMMON_WORDS_LOWER_MAP[lower]
        if (exact != null) {
            results.add(applyPrefixCase(prefix, exact))
        }

        // Prefix matches sorted by length (shorter common completions first)
        val prefixMatches = COMMON_WORDS
            .filter { it.lowercase().startsWith(lower) && it.lowercase() != lower }
            .sortedBy { it.length }

        for (match in prefixMatches) {
            val cased = applyPrefixCase(prefix, match)
            if (!results.contains(cased)) {
                results.add(cased)
            }
            if (results.size >= maxResults) break
        }

        return results.take(maxResults)
    }

    fun getNextWordPredictions(previousWord: String, maxResults: Int = 6): List<String> {
        val clean = previousWord.trim().lowercase()
        if (clean.isEmpty()) return emptyList()
        return NEXT_WORD_PREDICTIONS[clean]?.take(maxResults) ?: emptyList()
    }

    private fun applyPrefixCase(typedPrefix: String, dictWord: String): String {
        return when {
            typedPrefix.length >= 2 && typedPrefix.all { it.isUpperCase() } -> dictWord.uppercase()
            typedPrefix.firstOrNull()?.isUpperCase() == true && dictWord.firstOrNull()?.isLowerCase() == true ->
                dictWord.replaceFirstChar { it.uppercase() }
            else -> dictWord
        }
    }
}
