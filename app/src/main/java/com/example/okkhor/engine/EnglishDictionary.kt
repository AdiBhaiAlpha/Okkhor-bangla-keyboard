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
        // Articles, Determiners, Pronouns & Core
        "the", "a", "an", "I", "I'm", "I'll", "I've", "I'd", "you", "you're", "your", "yours", "we", "we're", "our",
        "they", "they're", "their", "them", "he", "she", "it", "it's", "its", "this", "that", "these", "those",
        "here", "there", "then",
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
        "depend", "depends", "update", "updated", "updating",
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
        "and", "or", "but", "so", "because", "if", "while", "with", "without", "for", "from", "to", "of", "as",
        "in", "on", "at", "by", "about", "after", "before", "under", "over", "between", "into", "through", "across",
        // Nouns & Common Words
        "time", "day", "week", "month", "year", "home", "house", "room", "office", "school", "college",
        "university", "friend", "friends", "family", "mother", "father", "brother", "sister", "brotherhood",
        "people", "person", "man", "woman", "boy", "girl", "child", "name", "number", "phone", "mobile",
        "message", "text", "email", "address", "password", "account", "photo", "video", "music", "game",
        "document", "link", "file", "report", "sentence", "spacing", "touch", "latency", "accuracy",
        "food", "water", "tea", "coffee", "money", "price", "market", "shop", "car", "bus", "train",
        "country", "city", "road", "world", "life", "love", "heart", "mind", "hand", "way", "thing", "things",
        "problem", "solution", "question", "answer", "news", "version", "keyboard", "english", "bangla",
        "happy", "sad", "angry", "tired", "busy", "free", "ready", "safe", "fast", "slow", "easy", "hard",
        "clean", "reliable", "new", "old", "big", "small", "long", "short", "high", "low", "right", "wrong", "true", "false",
        "very", "much", "many", "more", "most", "some", "any", "all", "every", "each", "other", "another",
        "first", "last", "next", "same", "different", "important", "special", "beautiful", "congratulations",
        "birthday", "anniversary", "Eid", "Bangladesh", "Dhaka"
    )

    private val HIGH_FREQUENCY_ORDER: Map<String, Int> = listOf(
        "the", "to", "and", "of", "a", "in", "is", "that", "it", "for", "you", "with", "on", "are", "was",
        "be", "this", "have", "from", "not", "we", "he", "she", "as", "they", "but", "can", "will", "do",
        "what", "so", "by", "or", "an", "my", "your", "their", "them", "there", "then", "here", "when",
        "where", "who", "how", "all", "if", "about", "me", "please", "good", "send", "time", "ready",
        "updated", "document", "link", "meeting", "morning", "thanks", "thank", "okay", "sorry"
    ).withIndex().associate { it.value to it.index }

    private val COMMON_WORDS_LOWER_MAP: Map<String, String> = COMMON_WORDS.associateBy { it.lowercase() }

    private val WORDS_BY_FIRST_CHAR: Map<Char, List<Pair<String, String>>> =
        COMMON_WORDS
            .distinctBy { it.lowercase() }
            .map { it.lowercase() to it }
            .groupBy { it.first[0] }
            .mapValues { (_, entries) ->
                entries.sortedWith(
                    compareBy<Pair<String, String>>(
                        { HIGH_FREQUENCY_ORDER[it.first] ?: Int.MAX_VALUE },
                        { it.first.length }
                    )
                )
            }

    /**
     * Find English word completions matching [prefix], preserving user capitalization when helpful.
     */
    fun findWordSuggestions(prefix: String, maxResults: Int = 5): List<String> {
        if (prefix.isBlank() || maxResults <= 0) return emptyList()
        val lower = prefix.lowercase()
        val results = LinkedHashSet<String>(maxResults * 2)

        // Exact case-insensitive match first (e.g. "im" / "i'm" -> "I'm", "dhaka" -> "Dhaka")
        if (lower == "im" || lower == "i'm") results.add("I'm")
        if (lower == "dont" || lower == "don't") results.add("don't")
        if (lower == "cant" || lower == "can't") results.add("can't")
        if (lower == "its" || lower == "it's") results.add("it's")

        val exact = COMMON_WORDS_LOWER_MAP[lower]
        if (exact != null) {
            results.add(applyPrefixCase(prefix, exact))
        }
        if (results.size >= maxResults) return results.take(maxResults)

        val bucket = WORDS_BY_FIRST_CHAR[lower[0]]
        if (bucket != null) {
            for ((wordLower, wordCased) in bucket) {
                if (wordLower.length > lower.length && wordLower.startsWith(lower)) {
                    results.add(applyPrefixCase(prefix, wordCased))
                    if (results.size >= maxResults) break
                }
            }
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
