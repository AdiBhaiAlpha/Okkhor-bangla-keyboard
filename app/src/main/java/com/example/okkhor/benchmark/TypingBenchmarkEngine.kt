package com.example.okkhor.benchmark

import com.example.okkhor.data.model.InputMode
import com.example.okkhor.engine.BanglaDictionary
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.EnglishDictionary
import com.example.okkhor.engine.SuggestionEngine
import kotlin.math.roundToInt

data class MicroBenchmarkResult(
    val transliterateP50Us: Double,
    val transliterateP95Us: Double,
    val transliterateMeanUs: Double,
    val banglaSuggestionP50Us: Double,
    val banglaSuggestionP95Us: Double,
    val englishSuggestionP50Us: Double,
    val englishSuggestionP95Us: Double,
    val dictionaryScanP50Us: Double,
    val dictionaryScanP95Us: Double,
    val iterations: Int
)

data class BenchmarkPrompt(
    val id: String,
    val title: String,
    val category: String,
    val mode: InputMode,
    val isLongPassage: Boolean,
    val targetText: String
)

data class ControlledProtocolEntry(
    val keyboardName: String,
    val scenario: String,
    val posture: String,
    val standardWpm: Double,
    val banglaGraphemeWpm: Double,
    val accuracyPercent: Double,
    val cerPercent: Double,
    val notes: String
)

data class LongSessionBucketInput(
    val minuteIndex: Int,
    val charsTyped: Int,
    val graphemesTyped: Int,
    val backspaces: Int,
    val keystrokes: Int,
    val elapsedMsInBucket: Long = 60_000L
)

data class LongSessionSegmentMetric(
    val minuteIndex: Int,
    val wpm: Double,
    val graphemeWpm: Double,
    val backspaceCount: Int,
    val keystrokeCount: Int
)

data class LongSessionAnalysis(
    val targetDurationMinutes: Int,
    val completedDurationSeconds: Double,
    val averageWpm: Double,
    val averageGraphemeWpm: Double,
    val accuracyPercent: Double,
    val backspaceFrequencyPercent: Double,
    val correctionRatePercent: Double,
    val missedTouches: Int,
    val keypressP50Ms: Double,
    val keypressP95Ms: Double,
    val wpmConsistencyScorePercent: Double,
    val speedDriftPercent: Double,
    val segments: List<LongSessionSegmentMetric>
)

/**
 * Deterministic benchmark algorithms, prompt tasks, and reproducible A/B protocol helpers
 * for Okkhor Typing Performance Lab.
 */
object TypingBenchmarkEngine {

    val BENCHMARK_PROMPTS = listOf(
        BenchmarkPrompt(
            id = "en_short",
            title = "English Conversational (Short)",
            category = "English Short",
            mode = InputMode.ENGLISH,
            isLongPassage = false,
            targetText = "Good morning, please send me the updated document when you are ready."
        ),
        BenchmarkPrompt(
            id = "en_long",
            title = "English Speed Paragraph (Long)",
            category = "English Long",
            mode = InputMode.ENGLISH,
            isLongPassage = true,
            targetText = "Fast mobile typing depends on clean key spacing, low touch latency, and reliable spacebar accuracy across every sentence."
        ),
        BenchmarkPrompt(
            id = "bn_avro_short",
            title = "Bangla Avro Phonetic (Short)",
            category = "Avro Short",
            mode = InputMode.BANGLA_PHONETIC,
            isLongPassage = false,
            targetText = "আমি বাংলায় গান গাই, শুভ সকাল সবাইকে।"
        ),
        BenchmarkPrompt(
            id = "bn_avro_long",
            title = "Bangla Avro Phonetic (Long Paragraph)",
            category = "Avro Long",
            mode = InputMode.BANGLA_PHONETIC,
            isLongPassage = true,
            targetText = "প্রতিদিন আমাদের কাজের মাঝে বাংলা ও ইংরেজি দ্রুত লেখার প্রয়োজন হয়, তাই কিবোর্ডের বাটন চাপ এবং অক্ষরের সঠিক অবস্থান খুব গুরুত্বপূর্ণ।"
        ),
        BenchmarkPrompt(
            id = "bn_probhat_short",
            title = "Bangla Probhat Fixed (Short)",
            category = "Probhat",
            mode = InputMode.BANGLA_FIXED,
            isLongPassage = false,
            targetText = "বাংলাদেশের বিজ্ঞান ও সংস্কৃতি এগিয়ে যাচ্ছে।"
        ),
        BenchmarkPrompt(
            id = "mixed_long",
            title = "Mixed Bangla + English Conversation",
            category = "Mixed",
            mode = InputMode.BANGLA_PHONETIC,
            isLongPassage = true,
            targetText = "আজকে meeting শেষ করে আমরা Dhaka ফিরে যাব, please send the link on time."
        )
    )

    val COMPARISON_SCENARIOS = listOf(
        "English Short Message (2-Thumb)",
        "English Long Paragraph (1-Thumb / 2-Thumb)",
        "Bangla Avro Short Message (2-Thumb)",
        "Bangla Avro Long Paragraph (2-Thumb)",
        "Bangla Probhat Fixed Layout (1-Thumb / 2-Thumb)",
        "Mixed Bangla-English Conversation (2-Thumb)"
    )

    /**
     * Computes Character Error Rate (Levenshtein edit distance / max(reference.length, 1)).
     */
    fun computeCharacterErrorRate(reference: String, actual: String): Double {
        if (reference.isEmpty() && actual.isEmpty()) return 0.0
        if (reference.isEmpty()) return 1.0
        val distance = levenshteinDistance(reference, actual)
        val cer = (distance.toDouble() / reference.length.toDouble()).coerceIn(0.0, 1.0)
        return (cer * 1000.0).roundToInt() / 1000.0
    }

    fun computeAccuracyPercent(reference: String, actual: String): Double {
        val cer = computeCharacterErrorRate(reference, actual)
        return ((1.0 - cer) * 1000.0).roundToInt() / 10.0
    }

    fun computePromptSummary(
        prompt: BenchmarkPrompt,
        typedText: String,
        elapsedMs: Long,
        backspaceCount: Int,
        totalKeystrokes: Int,
        posture: String,
        suggestionCorrections: Int = 0
    ): TypingMetricsSummary {
        val safeElapsedMs = elapsedMs.coerceAtLeast(250L)
        val chars = typedText.length
        val graphemes = BanglaUnicodeHelper.countGraphemeClusters(typedText)
        val standardWpm = TypingPerformanceTracker.computeStandardWpm(chars, safeElapsedMs)
        val graphemeWpm = TypingPerformanceTracker.computeBanglaGraphemeWpm(graphemes, safeElapsedMs)
        val elapsedMin = safeElapsedMs / 60_000.0
        val graphemesPerMin = if (elapsedMin > 0.0) ((graphemes / elapsedMin) * 10.0).roundToInt() / 10.0 else 0.0
        val cer = computeCharacterErrorRate(prompt.targetText, typedText)
        val acc = computeAccuracyPercent(prompt.targetText, typedText)
        val bsPct = if (totalKeystrokes > 0) ((backspaceCount * 100.0 / totalKeystrokes) * 10.0).roundToInt() / 10.0 else 0.0
        val correctionEvents = backspaceCount + suggestionCorrections
        val corrPct = if (totalKeystrokes > 0) {
            ((correctionEvents * 100.0 / totalKeystrokes).coerceIn(0.0, 100.0) * 10.0).roundToInt() / 10.0
        } else 0.0

        val live = TypingPerformanceTracker.liveSummary.value
        return TypingMetricsSummary(
            timestampMs = System.currentTimeMillis(),
            layoutMode = prompt.mode.name,
            label = "${prompt.title} ($posture)",
            standardWpm = standardWpm,
            banglaGraphemeWpm = graphemeWpm,
            banglaGraphemesPerMin = graphemesPerMin,
            characterErrorRate = cer,
            accuracyPercent = acc,
            totalKeystrokes = totalKeystrokes,
            backspaceCount = backspaceCount,
            backspaceFrequencyPercent = bsPct,
            correctionRatePercent = corrPct,
            missedTouchEvents = live.missedTouchEvents,
            accidentalNeighborActivations = live.accidentalNeighborActivations,
            keypressP50Ms = live.keypressP50Ms,
            keypressP95Ms = live.keypressP95Ms,
            banglaConversionP50Us = live.banglaConversionP50Us,
            banglaConversionP95Us = live.banglaConversionP95Us,
            suggestionSelections = live.suggestionSelections,
            suggestionSelectionPercent = live.suggestionSelectionPercent,
            suggestionCorrectionTimeMs = live.suggestionCorrectionTimeMs,
            measuredFrames = live.measuredFrames,
            slowFramesOver16Ms = live.slowFramesOver16Ms,
            averageFrameMs = live.averageFrameMs,
            elapsedSeconds = (safeElapsedMs / 100.0).roundToInt() / 10.0
        )
    }

    /**
     * Computes consistency-over-time and fatigue drift analysis across minute buckets
     * for 5, 10, and 15 minute continuous typing sessions (strictly anonymized numeric inputs).
     */
    fun computeLongSessionAnalysis(
        targetDurationMinutes: Int,
        totalElapsedMs: Long,
        buckets: List<LongSessionBucketInput>,
        totalChars: Int,
        totalGraphemes: Int,
        totalBackspaces: Int,
        totalSuggestionCorrections: Int,
        totalKeystrokes: Int,
        missedTouches: Int,
        p50Ms: Double,
        p95Ms: Double
    ): LongSessionAnalysis {
        val safeElapsedMs = totalElapsedMs.coerceAtLeast(1_000L)
        val avgWpm = TypingPerformanceTracker.computeStandardWpm(totalChars, safeElapsedMs)
        val avgGraphemeWpm = TypingPerformanceTracker.computeBanglaGraphemeWpm(totalGraphemes, safeElapsedMs)
        val approxCer = if (totalChars + totalBackspaces > 0) {
            (totalBackspaces.toDouble() / (totalChars + totalBackspaces)).coerceIn(0.0, 1.0)
        } else 0.0
        val accPct = ((1.0 - approxCer) * 1000.0).roundToInt() / 10.0
        val bsPct = if (totalKeystrokes > 0) ((totalBackspaces * 100.0 / totalKeystrokes) * 10.0).roundToInt() / 10.0 else 0.0
        val corrPct = if (totalKeystrokes > 0) {
            (((totalBackspaces + totalSuggestionCorrections) * 100.0 / totalKeystrokes).coerceIn(0.0, 100.0) * 10.0).roundToInt() / 10.0
        } else 0.0

        val segments = buckets.map { b ->
            val bucketMs = b.elapsedMsInBucket.coerceAtLeast(1_000L)
            LongSessionSegmentMetric(
                minuteIndex = b.minuteIndex,
                wpm = TypingPerformanceTracker.computeStandardWpm(b.charsTyped, bucketMs),
                graphemeWpm = TypingPerformanceTracker.computeBanglaGraphemeWpm(b.graphemesTyped, bucketMs),
                backspaceCount = b.backspaces,
                keystrokeCount = b.keystrokes
            )
        }

        val activeWpms = segments.filter { it.keystrokeCount > 0 }.map { it.wpm }
        val consistencyScore = if (activeWpms.size >= 2) {
            val mean = activeWpms.average()
            if (mean > 0.0) {
                val variance = activeWpms.sumOf { (it - mean) * (it - mean) } / activeWpms.size
                val stdDev = kotlin.math.sqrt(variance)
                ((100.0 - (stdDev / mean) * 100.0).coerceIn(0.0, 100.0) * 10.0).roundToInt() / 10.0
            } else 0.0
        } else if (activeWpms.isNotEmpty()) {
            100.0
        } else 0.0

        val speedDrift = if (activeWpms.size >= 2) {
            val mid = activeWpms.size / 2
            val firstHalfAvg = activeWpms.subList(0, mid).average()
            val secondHalfAvg = activeWpms.subList(mid, activeWpms.size).average()
            if (firstHalfAvg > 0.0) {
                (((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0 * 10.0).roundToInt() / 10.0
            } else 0.0
        } else 0.0

        return LongSessionAnalysis(
            targetDurationMinutes = targetDurationMinutes,
            completedDurationSeconds = (safeElapsedMs / 100.0).roundToInt() / 10.0,
            averageWpm = avgWpm,
            averageGraphemeWpm = avgGraphemeWpm,
            accuracyPercent = accPct,
            backspaceFrequencyPercent = bsPct,
            correctionRatePercent = corrPct,
            missedTouches = missedTouches,
            keypressP50Ms = p50Ms,
            keypressP95Ms = p95Ms,
            wpmConsistencyScorePercent = consistencyScore,
            speedDriftPercent = speedDrift,
            segments = segments
        )
    }

    /**
     * Runs a reproducible hot-path micro-benchmark across transliteration, dictionary prefix lookups,
     * and full SuggestionEngine queries so Before/After engine speedups are objectively measurable.
     */
    fun runEngineMicroBenchmark(
        iterations: Int = 400,
        phoneticEngine: BanglaPhoneticEngine = BanglaPhoneticEngine(),
        suggestionEngine: SuggestionEngine = SuggestionEngine(phoneticEngine)
    ): MicroBenchmarkResult {
        val avroInputs = listOf(
            "a", "am", "ami", "amra", "bangla", "bangladesh", "dhonnobad",
            "koto", "kintu", "biggyan", "shondha", "kkh", "str", "t`", "kE"
        )
        val englishInputs = listOf("h", "he", "hel", "hell", "hello", "th", "tha", "than", "tomo", "morn")

        // Warmup JIT
        repeat(40) { idx ->
            val a = avroInputs[idx % avroInputs.size]
            val e = englishInputs[idx % englishInputs.size]
            phoneticEngine.transliterate(a)
            suggestionEngine.getSuggestions(a, InputMode.BANGLA_PHONETIC)
            suggestionEngine.getSuggestions(e, InputMode.ENGLISH)
            BanglaDictionary.findWordSuggestions(a, 5)
            EnglishDictionary.findWordSuggestions(e, 5)
        }

        val translitSamples = LongArray(iterations)
        val bnSugSamples = LongArray(iterations)
        val enSugSamples = LongArray(iterations)
        val dictSamples = LongArray(iterations)

        for (i in 0 until iterations) {
            val avroWord = avroInputs[i % avroInputs.size]
            val enWord = englishInputs[i % englishInputs.size]

            val t0 = System.nanoTime()
            phoneticEngine.transliterate(avroWord)
            translitSamples[i] = System.nanoTime() - t0

            val t1 = System.nanoTime()
            suggestionEngine.getSuggestions(avroWord, InputMode.BANGLA_PHONETIC)
            bnSugSamples[i] = System.nanoTime() - t1

            val t2 = System.nanoTime()
            suggestionEngine.getSuggestions(enWord, InputMode.ENGLISH)
            enSugSamples[i] = System.nanoTime() - t2

            val t3 = System.nanoTime()
            BanglaDictionary.findWordSuggestions(avroWord, 5)
            EnglishDictionary.findWordSuggestions(enWord, 5)
            dictSamples[i] = System.nanoTime() - t3
        }

        return MicroBenchmarkResult(
            transliterateP50Us = percentileUs(translitSamples, 0.50),
            transliterateP95Us = percentileUs(translitSamples, 0.95),
            transliterateMeanUs = ((translitSamples.average() / 1_000.0) * 10.0).roundToInt() / 10.0,
            banglaSuggestionP50Us = percentileUs(bnSugSamples, 0.50),
            banglaSuggestionP95Us = percentileUs(bnSugSamples, 0.95),
            englishSuggestionP50Us = percentileUs(enSugSamples, 0.50),
            englishSuggestionP95Us = percentileUs(enSugSamples, 0.95),
            dictionaryScanP50Us = percentileUs(dictSamples, 0.50),
            dictionaryScanP95Us = percentileUs(dictSamples, 0.95),
            iterations = iterations
        )
    }

    private fun percentileUs(samples: LongArray, p: Double): Double {
        if (samples.isEmpty()) return 0.0
        val sorted = samples.sortedArray()
        val idx = ((sorted.size - 1) * p).roundToInt().coerceIn(0, sorted.lastIndex)
        return ((sorted[idx] / 1_000.0) * 10.0).roundToInt() / 10.0
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            val ca = a[i - 1]
            for (j in 1..b.length) {
                val cost = if (ca == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }
}
