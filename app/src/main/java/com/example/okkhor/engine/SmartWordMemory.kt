package com.example.okkhor.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Smart Persistent Memory for Auto-Correction, User Word Selection Learning, and Next-Word Predictions.
 *
 * Optimized for zero main-thread lock contention: reads hit lock-free ConcurrentHashMaps while writes
 * mark dirty and coalesce JSON serialization into debounced background IO flushes.
 */
class SmartWordMemory private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lock-free in-memory caches for zero-latency keystroke suggestions
    private val correctionsMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val bigramMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val userLexicon = ConcurrentHashMap<String, Int>()

    @Volatile
    private var isDirty = false
    private val persistScheduleLock = Any()
    private var pendingPersistJob: Job? = null

    init {
        loadMemory()
    }

    private fun loadMemory() {
        try {
            val correctionsJson = prefs.getString(KEY_CORRECTIONS, null)
            if (!correctionsJson.isNullOrEmpty()) {
                val json = JSONObject(correctionsJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val raw = keys.next()
                    val wordObj = json.getJSONObject(raw)
                    val freqMap = ConcurrentHashMap<String, Int>()
                    val wordKeys = wordObj.keys()
                    while (wordKeys.hasNext()) {
                        val word = wordKeys.next()
                        freqMap[word] = wordObj.optInt(word, 1)
                    }
                    correctionsMap[raw] = freqMap
                }
            }

            val bigramJson = prefs.getString(KEY_BIGRAMS, null)
            if (!bigramJson.isNullOrEmpty()) {
                val json = JSONObject(bigramJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val prev = keys.next()
                    val nextObj = json.getJSONObject(prev)
                    val freqMap = ConcurrentHashMap<String, Int>()
                    val nextKeys = nextObj.keys()
                    while (nextKeys.hasNext()) {
                        val next = nextKeys.next()
                        freqMap[next] = nextObj.optInt(next, 1)
                    }
                    bigramMap[prev] = freqMap
                }
            }

            val lexiconJson = prefs.getString(KEY_LEXICON, null)
            if (!lexiconJson.isNullOrEmpty()) {
                val json = JSONObject(lexiconJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val word = keys.next()
                    userLexicon[word] = json.optInt(word, 1)
                }
            }
        } catch (_: Exception) {
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

        val freqMap = correctionsMap.getOrPut(cleanRaw) { ConcurrentHashMap() }
        freqMap.merge(cleanWord, 1) { old, inc -> old + inc }
        userLexicon.merge(cleanWord, 1) { old, inc -> old + inc }
        isDirty = true
        schedulePersist()
    }

    /**
     * Record that [nextWord] followed [previousWord] in user typing.
     */
    fun recordNextWord(previousWord: String, nextWord: String) {
        val prev = previousWord.trim()
        val next = nextWord.trim()
        if (prev.isEmpty() || next.isEmpty() || prev == next) return

        val nextMap = bigramMap.getOrPut(prev) { ConcurrentHashMap() }
        nextMap.merge(next, 1) { old, inc -> old + inc }
        isDirty = true
        schedulePersist()
    }

    /**
     * Record a standalone word committed by user.
     */
    fun recordWordUsage(word: String) {
        val clean = word.trim()
        if (clean.length < 2) return

        userLexicon.merge(clean, 1) { old, inc -> old + inc }
        isDirty = true
        schedulePersist()
    }

    /**
     * Retrieve learned corrections for [rawInput] ranked by frequency.
     */
    fun getLearnedCorrections(rawInput: String): List<String> {
        val clean = rawInput.trim().lowercase()
        val freqMap = correctionsMap[clean] ?: return emptyList()
        if (freqMap.size == 1) return freqMap.keys.toList()
        return freqMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    /**
     * Highest-frequency learned correction along with its repetition count for conservative auto-replace checks.
     */
    fun getTopLearnedCorrectionWithCount(rawInput: String): Pair<String, Int>? {
        val clean = rawInput.trim().lowercase()
        val freqMap = correctionsMap[clean] ?: return null
        var bestKey: String? = null
        var bestCount = 0
        for ((k, v) in freqMap) {
            if (v > bestCount) {
                bestCount = v
                bestKey = k
            }
        }
        return bestKey?.let { it to bestCount }
    }

    /**
     * Retrieve user's learned next-words following [previousWord] ranked by frequency.
     */
    fun getLearnedNextWords(previousWord: String, maxResults: Int = 4): List<String> {
        val clean = previousWord.trim()
        val freqMap = bigramMap[clean] ?: return emptyList()
        return freqMap.entries
            .sortedByDescending { it.value }
            .take(maxResults)
            .map { it.key }
    }

    /**
     * Search user's personal lexicon for words matching [prefix].
     */
    fun searchUserLexicon(prefix: String, maxResults: Int = 3): List<String> {
        val clean = prefix.trim()
        if (clean.isEmpty()) return emptyList()
        val matches = mutableListOf<Map.Entry<String, Int>>()
        for (entry in userLexicon.entries) {
            val k = entry.key
            if (k.length > clean.length && k.startsWith(clean, ignoreCase = true)) {
                matches.add(entry)
            }
        }
        if (matches.isEmpty()) return emptyList()
        return matches
            .sortedByDescending { it.value }
            .take(maxResults)
            .map { it.key }
    }

    /**
     * Immediately flush any pending in-memory updates to SharedPreferences on a background thread.
     */
    fun flushPendingSaves() {
        if (!isDirty) return
        synchronized(persistScheduleLock) {
            pendingPersistJob?.cancel()
            pendingPersistJob = scope.launch {
                writeSnapshotToPrefs()
            }
        }
    }

    /**
     * Clear all learned memory (e.g. from Settings).
     */
    fun clearMemory() {
        synchronized(persistScheduleLock) {
            pendingPersistJob?.cancel()
        }
        correctionsMap.clear()
        bigramMap.clear()
        userLexicon.clear()
        isDirty = false
        prefs.edit().clear().apply()
    }

    private fun schedulePersist() {
        synchronized(persistScheduleLock) {
            pendingPersistJob?.cancel()
            pendingPersistJob = scope.launch {
                delay(1500L)
                writeSnapshotToPrefs()
            }
        }
    }

    private fun writeSnapshotToPrefs() {
        if (!isDirty) return
        try {
            isDirty = false
            val correctionsJson = JSONObject()
            val bigramJson = JSONObject()
            val lexiconJson = JSONObject()

            for ((raw, map) in correctionsMap) {
                val obj = JSONObject()
                for ((w, c) in map) obj.put(w, c)
                correctionsJson.put(raw, obj)
            }

            for ((prev, map) in bigramMap) {
                val obj = JSONObject()
                for ((next, c) in map) obj.put(next, c)
                bigramJson.put(prev, obj)
            }

            for ((w, c) in userLexicon) {
                lexiconJson.put(w, c)
            }

            prefs.edit().apply {
                putString(KEY_CORRECTIONS, correctionsJson.toString())
                putString(KEY_BIGRAMS, bigramJson.toString())
                putString(KEY_LEXICON, lexiconJson.toString())
                apply()
            }
        } catch (_: Exception) {
            // Ignore background persistence errors
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
