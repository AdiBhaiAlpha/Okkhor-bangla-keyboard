package com.example.okkhor.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Smart Persistent Memory for Auto-Correction, User Word Selection Learning, and Next-Word Predictions.
 *
 * Capabilities:
 * 1. User Correction Learning: Remembers which candidate word the user selects for any typed input (e.g. "kalke" -> "কালকে").
 * 2. Bigram Word Sequencing: Remembers which word the user types after another word (e.g. "আমি" -> "যাব", "আজকে" -> "দেখা").
 * 3. User Lexicon: Remembers custom Bengali words/names typed by the user.
 * 4. Frequency-based ranking so repeatedly chosen words automatically get promoted to the #1 prediction spot.
 */
class SmartWordMemory private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory caches for zero-latency keystroke suggestions
    // Map: rawInput (lowercase) -> Map of (chosenWord to frequency)
    private val correctionsMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Map: previousWord -> Map of (nextWord to frequency)
    private val bigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    // User's custom lexicon: word -> frequency
    private val userLexicon = mutableMapOf<String, Int>()

    init {
        loadMemory()
    }

    @Synchronized
    private fun loadMemory() {
        try {
            // 1. Load corrections
            val correctionsJson = prefs.getString(KEY_CORRECTIONS, null)
            if (!correctionsJson.isNullOrEmpty()) {
                val json = JSONObject(correctionsJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val raw = keys.next()
                    val wordObj = json.getJSONObject(raw)
                    val freqMap = mutableMapOf<String, Int>()
                    val wordKeys = wordObj.keys()
                    while (wordKeys.hasNext()) {
                        val word = wordKeys.next()
                        freqMap[word] = wordObj.optInt(word, 1)
                    }
                    correctionsMap[raw] = freqMap
                }
            }

            // 2. Load bigrams
            val bigramJson = prefs.getString(KEY_BIGRAMS, null)
            if (!bigramJson.isNullOrEmpty()) {
                val json = JSONObject(bigramJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val prev = keys.next()
                    val nextObj = json.getJSONObject(prev)
                    val freqMap = mutableMapOf<String, Int>()
                    val nextKeys = nextObj.keys()
                    while (nextKeys.hasNext()) {
                        val next = nextKeys.next()
                        freqMap[next] = nextObj.optInt(next, 1)
                    }
                    bigramMap[prev] = freqMap
                }
            }

            // 3. Load user lexicon
            val lexiconJson = prefs.getString(KEY_LEXICON, null)
            if (!lexiconJson.isNullOrEmpty()) {
                val json = JSONObject(lexiconJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val word = keys.next()
                    userLexicon[word] = json.optInt(word, 1)
                }
            }
        } catch (e: Exception) {
            // Graceful fallback on corrupt memory
        }
    }

    /**
     * Record that the user selected [chosenWord] for [rawInput].
     */
    fun recordCorrection(rawInput: String, chosenWord: String) {
        val cleanRaw = rawInput.trim().lowercase()
        val cleanWord = chosenWord.trim()
        if (cleanRaw.isEmpty() || cleanWord.isEmpty()) return

        synchronized(this) {
            val freqMap = correctionsMap.getOrPut(cleanRaw) { mutableMapOf() }
            val currentCount = freqMap[cleanWord] ?: 0
            freqMap[cleanWord] = currentCount + 1

            // Keep user lexicon updated
            val wordCount = userLexicon[cleanWord] ?: 0
            userLexicon[cleanWord] = wordCount + 1
        }
        persistAsync()
    }

    /**
     * Record that [nextWord] followed [previousWord] in user typing.
     */
    fun recordNextWord(previousWord: String, nextWord: String) {
        val prev = previousWord.trim()
        val next = nextWord.trim()
        if (prev.isEmpty() || next.isEmpty() || prev == next) return

        synchronized(this) {
            val nextMap = bigramMap.getOrPut(prev) { mutableMapOf() }
            val currentCount = nextMap[next] ?: 0
            nextMap[next] = currentCount + 1
        }
        persistAsync()
    }

    /**
     * Record a standalone word committed by user.
     */
    fun recordWordUsage(word: String) {
        val clean = word.trim()
        if (clean.length < 2) return

        synchronized(this) {
            val current = userLexicon[clean] ?: 0
            userLexicon[clean] = current + 1
        }
        persistAsync()
    }

    /**
     * Retrieve learned corrections for [rawInput] ranked by frequency.
     */
    @Synchronized
    fun getLearnedCorrections(rawInput: String): List<String> {
        val clean = rawInput.trim().lowercase()
        val freqMap = correctionsMap[clean] ?: return emptyList()
        return freqMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    /**
     * Retrieve user's learned next-words following [previousWord] ranked by frequency.
     */
    @Synchronized
    fun getLearnedNextWords(previousWord: String, maxResults: Int = 4): List<String> {
        val clean = previousWord.trim()
        val freqMap = bigramMap[clean] ?: return emptyList()
        return freqMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(maxResults)
    }

    /**
     * Search user's personal lexicon for words matching [prefix].
     */
    @Synchronized
    fun searchUserLexicon(prefix: String, maxResults: Int = 3): List<String> {
        val clean = prefix.trim()
        if (clean.isEmpty()) return emptyList()
        return userLexicon.entries
            .filter { it.key.startsWith(clean, ignoreCase = true) && it.key != clean }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(maxResults)
    }

    /**
     * Clear all learned memory (e.g. from Settings).
     */
    @Synchronized
    fun clearMemory() {
        correctionsMap.clear()
        bigramMap.clear()
        userLexicon.clear()
        prefs.edit().clear().apply()
    }

    private fun persistAsync() {
        scope.launch {
            try {
                val correctionsJson = JSONObject()
                val bigramJson = JSONObject()
                val lexiconJson = JSONObject()

                synchronized(this@SmartWordMemory) {
                    correctionsMap.forEach { (raw, map) ->
                        val obj = JSONObject()
                        map.forEach { (w, c) -> obj.put(w, c) }
                        correctionsJson.put(raw, obj)
                    }

                    bigramMap.forEach { (prev, map) ->
                        val obj = JSONObject()
                        map.forEach { (next, c) -> obj.put(next, c) }
                        bigramJson.put(prev, obj)
                    }

                    userLexicon.forEach { (w, c) ->
                        lexiconJson.put(w, c)
                    }
                }

                prefs.edit().apply {
                    putString(KEY_CORRECTIONS, correctionsJson.toString())
                    putString(KEY_BIGRAMS, bigramJson.toString())
                    putString(KEY_LEXICON, lexiconJson.toString())
                    apply()
                }
            } catch (e: Exception) {
                // Ignore background persistence errors
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "okkhor_smart_word_memory"
        private const val KEY_CORRECTIONS = "smart_learned_corrections"
        private const val KEY_BIGRAMS = "smart_learned_bigrams"
        private const val KEY_LEXICON = "smart_user_lexicon"

        @Volatile
        private var instance: SmartWordMemory? = null

        fun getInstance(context: Context): SmartWordMemory {
            return instance ?: synchronized(this) {
                instance ?: SmartWordMemory(context.applicationContext).also { instance = it }
            }
        }
    }
}
