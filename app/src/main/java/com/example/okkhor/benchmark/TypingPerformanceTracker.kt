package com.example.okkhor.benchmark

import android.content.Context
import android.content.SharedPreferences
import com.example.okkhor.data.model.InputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Aggregated, strictly anonymized typing telemetry snapshot.
 * Never contains typed characters, words, passwords, or editor text.
 */
data class TypingMetricsSummary(
    val timestampMs: Long = System.currentTimeMillis(),
    val layoutMode: String = InputMode.BANGLA_PHONETIC.name,
    val label: String = "Live Session",
    val standardWpm: Double = 0.0,
    val banglaGraphemeWpm: Double = 0.0,
    val banglaGraphemesPerMin: Double = 0.0,
    val characterErrorRate: Double = 0.0,
    val accuracyPercent: Double = 100.0,
    val totalKeystrokes: Int = 0,
    val backspaceCount: Int = 0,
    val backspaceFrequencyPercent: Double = 0.0,
    val correctionRatePercent: Double = 0.0,
    val missedTouchEvents: Int = 0,
    val accidentalNeighborActivations: Int = 0,
    val keypressP50Ms: Double = 0.0,
    val keypressP95Ms: Double = 0.0,
    val banglaConversionP50Us: Double = 0.0,
    val banglaConversionP95Us: Double = 0.0,
    val suggestionSelections: Int = 0,
    val suggestionSelectionPercent: Double = 0.0,
    val suggestionCorrectionTimeMs: Long = 0L,
    val measuredFrames: Int = 0,
    val slowFramesOver16Ms: Int = 0,
    val averageFrameMs: Double = 0.0,
    val elapsedSeconds: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestampMs", timestampMs)
        put("layoutMode", layoutMode)
        put("label", label)
        put("standardWpm", standardWpm)
        put("banglaGraphemeWpm", banglaGraphemeWpm)
        put("banglaGraphemesPerMin", banglaGraphemesPerMin)
        put("characterErrorRate", characterErrorRate)
        put("accuracyPercent", accuracyPercent)
        put("totalKeystrokes", totalKeystrokes)
        put("backspaceCount", backspaceCount)
        put("backspaceFrequencyPercent", backspaceFrequencyPercent)
        put("correctionRatePercent", correctionRatePercent)
        put("missedTouchEvents", missedTouchEvents)
        put("accidentalNeighborActivations", accidentalNeighborActivations)
        put("keypressP50Ms", keypressP50Ms)
        put("keypressP95Ms", keypressP95Ms)
        put("banglaConversionP50Us", banglaConversionP50Us)
        put("banglaConversionP95Us", banglaConversionP95Us)
        put("suggestionSelections", suggestionSelections)
        put("suggestionSelectionPercent", suggestionSelectionPercent)
        put("suggestionCorrectionTimeMs", suggestionCorrectionTimeMs)
        put("measuredFrames", measuredFrames)
        put("slowFramesOver16Ms", slowFramesOver16Ms)
        put("averageFrameMs", averageFrameMs)
        put("elapsedSeconds", elapsedSeconds)
    }

    companion object {
        fun fromJson(obj: JSONObject): TypingMetricsSummary = TypingMetricsSummary(
            timestampMs = obj.optLong("timestampMs", 0L),
            layoutMode = obj.optString("layoutMode", InputMode.BANGLA_PHONETIC.name),
            label = obj.optString("label", "Session"),
            standardWpm = obj.optDouble("standardWpm", 0.0),
            banglaGraphemeWpm = obj.optDouble("banglaGraphemeWpm", 0.0),
            banglaGraphemesPerMin = obj.optDouble("banglaGraphemesPerMin", 0.0),
            characterErrorRate = obj.optDouble("characterErrorRate", 0.0),
            accuracyPercent = obj.optDouble("accuracyPercent", 100.0),
            totalKeystrokes = obj.optInt("totalKeystrokes", 0),
            backspaceCount = obj.optInt("backspaceCount", 0),
            backspaceFrequencyPercent = obj.optDouble("backspaceFrequencyPercent", 0.0),
            correctionRatePercent = obj.optDouble("correctionRatePercent", 0.0),
            missedTouchEvents = obj.optInt("missedTouchEvents", 0),
            accidentalNeighborActivations = obj.optInt("accidentalNeighborActivations", 0),
            keypressP50Ms = obj.optDouble("keypressP50Ms", 0.0),
            keypressP95Ms = obj.optDouble("keypressP95Ms", 0.0),
            banglaConversionP50Us = obj.optDouble("banglaConversionP50Us", 0.0),
            banglaConversionP95Us = obj.optDouble("banglaConversionP95Us", 0.0),
            suggestionSelections = obj.optInt("suggestionSelections", 0),
            suggestionSelectionPercent = obj.optDouble("suggestionSelectionPercent", 0.0),
            suggestionCorrectionTimeMs = obj.optLong("suggestionCorrectionTimeMs", 0L),
            measuredFrames = obj.optInt("measuredFrames", 0),
            slowFramesOver16Ms = obj.optInt("slowFramesOver16Ms", 0),
            averageFrameMs = obj.optDouble("averageFrameMs", 0.0),
            elapsedSeconds = obj.optDouble("elapsedSeconds", 0.0)
        )
    }
}

/**
 * Privacy-preserving local performance tracker for Okkhor Typing Performance Lab.
 * Stores ONLY numeric latencies and anonymized aggregate counters in memory and local preferences.
 */
object TypingPerformanceTracker {

    private const val PREFS_NAME = "okkhor_typing_perf_lab"
    private const val KEY_HISTORY = "perf_session_history_json"
    private const val KEY_IME_SNAPSHOT = "perf_live_ime_snapshot_json"
    private const val MAX_SAMPLES = 512
    private const val MAX_HISTORY = 24

    private val lock = Any()
    private val keypressLatenciesNs = ArrayDeque<Long>(MAX_SAMPLES)
    private val banglaConversionLatenciesNs = ArrayDeque<Long>(MAX_SAMPLES)
    private val frameDurationsMs = ArrayDeque<Double>(MAX_SAMPLES)

    private var sessionStartMs: Long = 0L
    private var lastActivityMs: Long = 0L
    private var totalKeystrokes: Int = 0
    private var enteredCharCount: Int = 0
    private var enteredGraphemeCount: Int = 0
    private var backspaceCount: Int = 0
    private var missedTouchCount: Int = 0
    private var accidentalNeighborCount: Int = 0
    private var suggestionSelectionCount: Int = 0
    private var wordCommitCount: Int = 0
    private var suggestionCorrectionTimeMs: Long = 0L
    private var lastSuggestionCommitMs: Long = 0L
    private var slowFrameCount: Int = 0
    private var activeLayoutMode: String = InputMode.BANGLA_PHONETIC.name

    // Track ONLY QWERTY grid column/row index internally for neighbor-correction heuristic (never char text)
    private var prevTapMs: Long = 0L
    private var prevTapRow: Int = -1
    private var prevTapCol: Int = -1
    private var backspaceAfterTapMs: Long = 0L
    private var suspectNeighborTapRow: Int = -1
    private var suspectNeighborTapCol: Int = -1

    private val _liveSummary = MutableStateFlow(TypingMetricsSummary())
    val liveSummary: StateFlow<TypingMetricsSummary> = _liveSummary.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<TypingMetricsSummary>>(emptyList())
    val sessionHistory: StateFlow<List<TypingMetricsSummary>> = _sessionHistory.asStateFlow()

    @Volatile
    private var appPrefs: SharedPreferences? = null

    fun init(context: Context) {
        if (appPrefs != null) return
        synchronized(lock) {
            if (appPrefs == null) {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                appPrefs = prefs
                loadHistoryLocked(prefs)
            }
        }
    }

    fun startNewSession(mode: InputMode) {
        synchronized(lock) {
            resetCountersLocked(mode.name)
            publishLocked()
        }
    }

    fun recordKeyCommit(
        mode: InputMode,
        latencyNs: Long,
        enteredChars: Int,
        enteredGraphemes: Int,
        isSensitiveField: Boolean,
        qwertyRow: Int = -1,
        qwertyCol: Int = -1
    ) {
        if (isSensitiveField) return
        val nowMs = System.currentTimeMillis()
        synchronized(lock) {
            ensureSessionStartedLocked(nowMs, mode.name)
            activeLayoutMode = mode.name
            totalKeystrokes++
            enteredCharCount += enteredChars.coerceAtLeast(1)
            enteredGraphemeCount += enteredGraphemes.coerceAtLeast(1)
            pushBounded(keypressLatenciesNs, latencyNs.coerceAtLeast(0L))

            // Detect accidental neighbor activation: tap A -> backspace -> tap adjacent key B within 750ms
            if (backspaceAfterTapMs > 0L && (nowMs - backspaceAfterTapMs) <= 750L &&
                suspectNeighborTapRow >= 0 && qwertyRow >= 0 &&
                kotlin.math.abs(suspectNeighborTapRow - qwertyRow) <= 1 &&
                kotlin.math.abs(suspectNeighborTapCol - qwertyCol) <= 1
            ) {
                accidentalNeighborCount++
            }
            backspaceAfterTapMs = 0L
            prevTapMs = nowMs
            prevTapRow = qwertyRow
            prevTapCol = qwertyCol
            lastActivityMs = nowMs
            publishLocked()
        }
    }

    fun recordBackspace(
        mode: InputMode,
        latencyNs: Long,
        isSensitiveField: Boolean
    ) {
        if (isSensitiveField) return
        val nowMs = System.currentTimeMillis()
        synchronized(lock) {
            ensureSessionStartedLocked(nowMs, mode.name)
            activeLayoutMode = mode.name
            totalKeystrokes++
            backspaceCount++
            pushBounded(keypressLatenciesNs, latencyNs.coerceAtLeast(0L))

            if (prevTapMs > 0L && (nowMs - prevTapMs) <= 650L) {
                backspaceAfterTapMs = nowMs
                suspectNeighborTapRow = prevTapRow
                suspectNeighborTapCol = prevTapCol
            }
            if (lastSuggestionCommitMs > 0L && (nowMs - lastSuggestionCommitMs) <= 2200L) {
                suggestionCorrectionTimeMs += (nowMs - lastSuggestionCommitMs)
                lastSuggestionCommitMs = 0L
            }
            lastActivityMs = nowMs
            publishLocked()
        }
    }

    fun recordMissedTouch() {
        synchronized(lock) {
            if (sessionStartMs == 0L) sessionStartMs = System.currentTimeMillis()
            missedTouchCount++
            lastActivityMs = System.currentTimeMillis()
            publishLocked()
        }
    }

    fun recordBanglaConversion(durationNs: Long, isSensitiveField: Boolean) {
        if (isSensitiveField) return
        synchronized(lock) {
            pushBounded(banglaConversionLatenciesNs, durationNs.coerceAtLeast(0L))
            publishLocked()
        }
    }

    fun recordWordCommitted(viaSuggestion: Boolean, isSensitiveField: Boolean) {
        if (isSensitiveField) return
        val nowMs = System.currentTimeMillis()
        synchronized(lock) {
            ensureSessionStartedLocked(nowMs, activeLayoutMode)
            wordCommitCount++
            if (viaSuggestion) {
                suggestionSelectionCount++
                lastSuggestionCommitMs = nowMs
            } else {
                lastSuggestionCommitMs = 0L
            }
            lastActivityMs = nowMs
            publishLocked()
        }
    }

    fun recordFrameDurationMs(frameMs: Double) {
        if (frameMs <= 0.0 || frameMs > 500.0) return
        synchronized(lock) {
            pushBoundedDouble(frameDurationsMs, frameMs)
            if (frameMs > 16.67) {
                slowFrameCount++
            }
            publishLocked()
        }
    }

    fun saveCurrentSessionSnapshot(
        label: String,
        cerOverride: Double? = null,
        accuracyOverride: Double? = null,
        wpmOverride: Double? = null,
        graphemeWpmOverride: Double? = null
    ): TypingMetricsSummary {
        return synchronized(lock) {
            val base = buildSummaryLocked(label)
            val finalSummary = base.copy(
                label = label,
                characterErrorRate = cerOverride ?: base.characterErrorRate,
                accuracyPercent = accuracyOverride ?: base.accuracyPercent,
                standardWpm = wpmOverride ?: base.standardWpm,
                banglaGraphemeWpm = graphemeWpmOverride ?: base.banglaGraphemeWpm
            )
            val updated = (listOf(finalSummary) + _sessionHistory.value).take(MAX_HISTORY)
            _sessionHistory.value = updated
            appPrefs?.let { saveHistoryLocked(it, updated) }
            finalSummary
        }
    }

    fun persistLiveImeSnapshot() {
        synchronized(lock) {
            val prefs = appPrefs ?: return
            val snapshot = buildSummaryLocked("IME Live Session")
            prefs.edit().putString(KEY_IME_SNAPSHOT, snapshot.toJson().toString()).apply()
        }
    }

    fun clearAllMetrics() {
        synchronized(lock) {
            resetCountersLocked(activeLayoutMode)
            _sessionHistory.value = emptyList()
            appPrefs?.edit()?.clear()?.apply()
            publishLocked()
        }
    }

    /**
     * Pure helper for computing standard WPM: (enteredChars / 5.0) / elapsedMinutes.
     */
    fun computeStandardWpm(enteredChars: Int, elapsedMs: Long): Double {
        if (enteredChars <= 0 || elapsedMs <= 0L) return 0.0
        val elapsedMin = elapsedMs / 60_000.0
        if (elapsedMin <= 0.0) return 0.0
        return ((enteredChars / 5.0) / elapsedMin * 10.0).roundToInt() / 10.0
    }

    /**
     * Pure helper for Bangla Unicode grapheme cluster WPM (kept strictly distinct from standard WPM).
     */
    fun computeBanglaGraphemeWpm(graphemeClusters: Int, elapsedMs: Long): Double {
        if (graphemeClusters <= 0 || elapsedMs <= 0L) return 0.0
        val elapsedMin = elapsedMs / 60_000.0
        if (elapsedMin <= 0.0) return 0.0
        return ((graphemeClusters / 5.0) / elapsedMin * 10.0).roundToInt() / 10.0
    }

    private fun ensureSessionStartedLocked(nowMs: Long, modeName: String) {
        if (sessionStartMs == 0L || (lastActivityMs > 0L && nowMs - lastActivityMs > 90_000L)) {
            resetCountersLocked(modeName)
            sessionStartMs = nowMs
        }
    }

    private fun resetCountersLocked(modeName: String) {
        sessionStartMs = System.currentTimeMillis()
        lastActivityMs = sessionStartMs
        activeLayoutMode = modeName
        keypressLatenciesNs.clear()
        banglaConversionLatenciesNs.clear()
        frameDurationsMs.clear()
        totalKeystrokes = 0
        enteredCharCount = 0
        enteredGraphemeCount = 0
        backspaceCount = 0
        missedTouchCount = 0
        accidentalNeighborCount = 0
        suggestionSelectionCount = 0
        wordCommitCount = 0
        suggestionCorrectionTimeMs = 0L
        lastSuggestionCommitMs = 0L
        slowFrameCount = 0
        prevTapMs = 0L
        prevTapRow = -1
        prevTapCol = -1
        backspaceAfterTapMs = 0L
        suspectNeighborTapRow = -1
        suspectNeighborTapCol = -1
    }

    private fun publishLocked() {
        _liveSummary.value = buildSummaryLocked("Live IME Session")
    }

    private fun buildSummaryLocked(label: String): TypingMetricsSummary {
        val elapsedMs = if (sessionStartMs > 0L && lastActivityMs > sessionStartMs) {
            (lastActivityMs - sessionStartMs).coerceAtLeast(1000L)
        } else {
            0L
        }
        val elapsedMin = elapsedMs / 60_000.0
        val wpm = computeStandardWpm(enteredCharCount, elapsedMs)
        val graphemeWpm = computeBanglaGraphemeWpm(enteredGraphemeCount, elapsedMs)
        val graphemesPerMin = if (elapsedMin > 0.0 && enteredGraphemeCount > 0) {
            ((enteredGraphemeCount / elapsedMin) * 10.0).roundToInt() / 10.0
        } else 0.0

        val bsFreqPct = if (totalKeystrokes > 0) {
            ((backspaceCount * 100.0 / totalKeystrokes) * 10.0).roundToInt() / 10.0
        } else 0.0
        val corrPct = if (totalKeystrokes > 0) {
            (((backspaceCount + accidentalNeighborCount) * 100.0 / totalKeystrokes).coerceIn(0.0, 100.0) * 10.0).roundToInt() / 10.0
        } else 0.0
        val approxCer = if (enteredCharCount > 0) {
            (backspaceCount.toDouble() / (enteredCharCount + backspaceCount)).coerceIn(0.0, 1.0)
        } else 0.0
        val accPct = ((1.0 - approxCer) * 1000.0).roundToInt() / 10.0
        val sugPct = if (wordCommitCount > 0) {
            ((suggestionSelectionCount * 100.0 / wordCommitCount) * 10.0).roundToInt() / 10.0
        } else 0.0

        val kpSorted = keypressLatenciesNs.sorted()
        val kpP50Ms = percentileNsToMs(kpSorted, 0.50)
        val kpP95Ms = percentileNsToMs(kpSorted, 0.95)

        val bnSorted = banglaConversionLatenciesNs.sorted()
        val bnP50Us = percentileNsToUs(bnSorted, 0.50)
        val bnP95Us = percentileNsToUs(bnSorted, 0.95)

        val avgFrame = if (frameDurationsMs.isNotEmpty()) {
            ((frameDurationsMs.average()) * 100.0).roundToInt() / 100.0
        } else 0.0

        return TypingMetricsSummary(
            timestampMs = System.currentTimeMillis(),
            layoutMode = activeLayoutMode,
            label = label,
            standardWpm = wpm,
            banglaGraphemeWpm = graphemeWpm,
            banglaGraphemesPerMin = graphemesPerMin,
            characterErrorRate = (approxCer * 1000.0).roundToInt() / 1000.0,
            accuracyPercent = accPct,
            totalKeystrokes = totalKeystrokes,
            backspaceCount = backspaceCount,
            backspaceFrequencyPercent = bsFreqPct,
            correctionRatePercent = corrPct,
            missedTouchEvents = missedTouchCount,
            accidentalNeighborActivations = accidentalNeighborCount,
            keypressP50Ms = kpP50Ms,
            keypressP95Ms = kpP95Ms,
            banglaConversionP50Us = bnP50Us,
            banglaConversionP95Us = bnP95Us,
            suggestionSelections = suggestionSelectionCount,
            suggestionSelectionPercent = sugPct,
            suggestionCorrectionTimeMs = suggestionCorrectionTimeMs,
            measuredFrames = frameDurationsMs.size,
            slowFramesOver16Ms = slowFrameCount,
            averageFrameMs = avgFrame,
            elapsedSeconds = (elapsedMs / 100.0).roundToInt() / 10.0
        )
    }

    private fun percentileNsToMs(sortedNs: List<Long>, p: Double): Double {
        if (sortedNs.isEmpty()) return 0.0
        val idx = ((sortedNs.size - 1) * p).roundToInt().coerceIn(0, sortedNs.lastIndex)
        return ((sortedNs[idx] / 1_000_000.0) * 1000.0).roundToInt() / 1000.0
    }

    private fun percentileNsToUs(sortedNs: List<Long>, p: Double): Double {
        if (sortedNs.isEmpty()) return 0.0
        val idx = ((sortedNs.size - 1) * p).roundToInt().coerceIn(0, sortedNs.lastIndex)
        return ((sortedNs[idx] / 1_000.0) * 10.0).roundToInt() / 10.0
    }

    private fun pushBounded(queue: ArrayDeque<Long>, value: Long) {
        if (queue.size >= MAX_SAMPLES) queue.removeFirst()
        queue.addLast(value)
    }

    private fun pushBoundedDouble(queue: ArrayDeque<Double>, value: Double) {
        if (queue.size >= MAX_SAMPLES) queue.removeFirst()
        queue.addLast(value)
    }

    private fun loadHistoryLocked(prefs: SharedPreferences) {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = List(arr.length()) { idx ->
                TypingMetricsSummary.fromJson(arr.getJSONObject(idx))
            }
            _sessionHistory.value = list
        }
    }

    private fun saveHistoryLocked(prefs: SharedPreferences, list: List<TypingMetricsSummary>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
