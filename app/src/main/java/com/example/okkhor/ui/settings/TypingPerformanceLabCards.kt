package com.example.okkhor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okkhor.benchmark.LongSessionAnalysis
import com.example.okkhor.benchmark.LongSessionBucketInput
import com.example.okkhor.benchmark.TypingBenchmarkEngine
import com.example.okkhor.benchmark.TypingMetricsSummary
import com.example.okkhor.benchmark.TypingPerformanceTracker
import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardSettings
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.SuggestionEngine
import com.example.okkhor.ui.keyboard.KeyboardContainer
import com.example.okkhor.ui.keyboard.KeyboardLayouts
import kotlinx.coroutines.delay
import java.util.Locale

data class ExternalComparatorInput(
    val wpm: String = "",
    val accuracy: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LongSessionComfortCard(
    settings: KeyboardSettings,
    activeTheme: KeyboardTheme,
    phoneticEngine: BanglaPhoneticEngine,
    suggestionEngine: SuggestionEngine,
    onSaveLongSessionSummary: (TypingMetricsSummary) -> Unit
) {
    var targetMinutes by remember { mutableIntStateOf(5) }
    var sessionMode by remember { mutableStateOf(InputMode.BANGLA_PHONETIC) }
    var isRunning by remember { mutableStateOf(false) }
    var isShifted by remember { mutableStateOf(false) }
    var sessionStartMs by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    // Anonymized numeric counters only — preview is transient in RAM and cleared on stop
    var transientPreviewTail by remember { mutableStateOf("") }
    var transientComposing by remember { mutableStateOf("") }
    var totalChars by remember { mutableIntStateOf(0) }
    var totalGraphemes by remember { mutableIntStateOf(0) }
    var totalBackspaces by remember { mutableIntStateOf(0) }
    var totalCorrections by remember { mutableIntStateOf(0) }
    var totalKeystrokes by remember { mutableIntStateOf(0) }
    val minuteBuckets = remember { mutableStateListOf<LongSessionBucketInput>() }
    var analysisResult by remember { mutableStateOf<LongSessionAnalysis?>(null) }

    fun ensureBucketIndex(index: Int) {
        while (minuteBuckets.size <= index) {
            minuteBuckets.add(
                LongSessionBucketInput(
                    minuteIndex = minuteBuckets.size + 1,
                    charsTyped = 0,
                    graphemesTyped = 0,
                    backspaces = 0,
                    keystrokes = 0,
                    elapsedMsInBucket = 60_000L
                )
            )
        }
    }

    fun finishSession() {
        val safeElapsed = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(1_000L)
        elapsedMs = safeElapsed
        val activeMinutes = (((safeElapsed - 1L) / 60_000L).toInt() + 1).coerceAtLeast(1)
        ensureBucketIndex(activeMinutes - 1)
        val adjustedBuckets = minuteBuckets.take(activeMinutes).mapIndexed { idx, bucket ->
            if (idx == activeMinutes - 1) {
                val remainder = safeElapsed - idx * 60_000L
                bucket.copy(elapsedMsInBucket = remainder.coerceAtLeast(1_000L))
            } else {
                bucket.copy(elapsedMsInBucket = 60_000L)
            }
        }
        val live = TypingPerformanceTracker.liveSummary.value
        val result = TypingBenchmarkEngine.computeLongSessionAnalysis(
            targetDurationMinutes = targetMinutes,
            totalElapsedMs = safeElapsed,
            buckets = adjustedBuckets,
            totalChars = totalChars,
            totalGraphemes = totalGraphemes,
            totalBackspaces = totalBackspaces,
            totalSuggestionCorrections = totalCorrections,
            totalKeystrokes = totalKeystrokes,
            missedTouches = live.missedTouchEvents,
            p50Ms = live.keypressP50Ms,
            p95Ms = live.keypressP95Ms
        )
        analysisResult = result
        isRunning = false
        transientPreviewTail = ""
        transientComposing = ""
        onSaveLongSessionSummary(
            TypingMetricsSummary(
                timestampMs = System.currentTimeMillis(),
                layoutMode = sessionMode.name,
                label = "Long-Session Comfort (${targetMinutes}m ${sessionMode.name})",
                standardWpm = result.averageWpm,
                banglaGraphemeWpm = result.averageGraphemeWpm,
                accuracyPercent = result.accuracyPercent,
                totalKeystrokes = totalKeystrokes,
                backspaceCount = totalBackspaces,
                backspaceFrequencyPercent = result.backspaceFrequencyPercent,
                correctionRatePercent = result.correctionRatePercent,
                missedTouchEvents = result.missedTouches,
                keypressP50Ms = result.keypressP50Ms,
                keypressP95Ms = result.keypressP95Ms,
                elapsedSeconds = result.completedDurationSeconds
            )
        )
    }

    LaunchedEffect(isRunning, sessionStartMs) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            elapsedMs = System.currentTimeMillis() - sessionStartMs
            if (elapsedMs >= targetMinutes * 60_000L) {
                finishSession()
                break
            }
            delay(500L)
        }
    }

    val liveCandidates = remember(transientComposing, sessionMode, settings.showSuggestions) {
        if (!settings.showSuggestions || transientComposing.isEmpty()) emptyList()
        else {
            val translit = if (sessionMode == InputMode.BANGLA_PHONETIC) {
                phoneticEngine.transliterate(transientComposing)
            } else null
            suggestionEngine.getSuggestions(
                input = transientComposing,
                mode = sessionMode,
                maxCandidates = 5,
                precomputedTransliterated = translit,
                nextWordPredictionEnabled = settings.nextWordPredictionEnabled
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perf_lab_long_session_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "Long Session Comfort", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Long-Session Comfort Mode (Consistency & Fatigue)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Measure typing comfort, WPM consistency, and fatigue drift over 5, 10, or 15 continuous minutes. Typed text stays volatile and is cleared without saving.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { mins ->
                    FilterChip(
                        selected = targetMinutes == mins,
                        enabled = !isRunning,
                        onClick = { targetMinutes = mins },
                        label = { Text("${mins} min", fontSize = 11.5.sp) },
                        modifier = Modifier.testTag("long_session_dur_$mins")
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    InputMode.BANGLA_PHONETIC to "Avro Bangla",
                    InputMode.ENGLISH to "English",
                    InputMode.BANGLA_FIXED to "Probhat"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = sessionMode == mode,
                        onClick = { sessionMode = mode },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("long_session_mode_${mode.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        TypingPerformanceTracker.startNewSession(sessionMode)
                        sessionStartMs = System.currentTimeMillis()
                        elapsedMs = 0L
                        totalChars = 0
                        totalGraphemes = 0
                        totalBackspaces = 0
                        totalCorrections = 0
                        totalKeystrokes = 0
                        transientPreviewTail = ""
                        transientComposing = ""
                        minuteBuckets.clear()
                        ensureBucketIndex(0)
                        analysisResult = null
                        isRunning = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_long_session_start")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "Restart (${targetMinutes}m)" else "Start ${targetMinutes}m Session", fontSize = 12.sp)
                }

                Button(
                    enabled = isRunning,
                    onClick = { finishSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_long_session_finish")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish & Analyze", fontSize = 12.sp)
                }
            }

            if (isRunning) {
                val totalSec = (elapsedMs / 1000L).toInt()
                val remSec = ((targetMinutes * 60) - totalSec).coerceAtLeast(0)
                val liveWpm = TypingPerformanceTracker.computeStandardWpm(totalChars, elapsedMs.coerceAtLeast(1000L))
                Text(
                    text = "Elapsed ${totalSec / 60}m ${totalSec % 60}s • Remaining ${remSec / 60}m ${remSec % 60}s • Live Avg ${formatPerfDecimal(liveWpm)} WPM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                val displayTail = (transientPreviewTail + if (transientComposing.isNotEmpty() && sessionMode == InputMode.BANGLA_PHONETIC) {
                    phoneticEngine.transliterate(transientComposing)
                } else transientComposing).takeLast(90)

                OutlinedTextField(
                    value = displayTail,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Volatile Live Buffer (cleared on stop)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_session_preview")
                )

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
                    KeyboardContainer(
                        settings = settings,
                        theme = activeTheme,
                        currentMode = sessionMode,
                        lastTextMode = sessionMode.takeIf { it.isTextMode } ?: InputMode.BANGLA_PHONETIC,
                        isShifted = isShifted,
                        candidates = liveCandidates,
                        onKeyPress = { key ->
                            val t0 = System.nanoTime()
                            val bucketIdx = ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt().coerceAtLeast(0)
                            ensureBucketIndex(bucketIdx)
                            totalKeystrokes++
                            val graphemes = BanglaUnicodeHelper.countGraphemeClusters(key).coerceAtLeast(1)
                            totalChars += key.length
                            totalGraphemes += graphemes
                            val b = minuteBuckets[bucketIdx]
                            minuteBuckets[bucketIdx] = b.copy(
                                charsTyped = b.charsTyped + key.length,
                                graphemesTyped = b.graphemesTyped + graphemes,
                                keystrokes = b.keystrokes + 1
                            )
                            val isPhonetic = sessionMode == InputMode.BANGLA_PHONETIC &&
                                key.length == 1 && (key[0] in 'a'..'z' || key[0] in 'A'..'Z' || key in listOf("^", ":", "`"))
                            if (isPhonetic) {
                                transientComposing += key
                            } else {
                                if (transientComposing.isNotEmpty()) {
                                    transientPreviewTail = (transientPreviewTail + phoneticEngine.transliterate(transientComposing)).takeLast(120)
                                    transientComposing = ""
                                }
                                transientPreviewTail = (transientPreviewTail + key).takeLast(120)
                            }
                            if (isShifted) isShifted = false
                            TypingPerformanceTracker.recordKeyCommit(sessionMode, System.nanoTime() - t0, key.length, graphemes, false)
                        },
                        onActionKey = { code ->
                            val t0 = System.nanoTime()
                            val bucketIdx = ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt().coerceAtLeast(0)
                            ensureBucketIndex(bucketIdx)
                            when (code) {
                                KeyboardLayouts.CODE_SHIFT -> isShifted = !isShifted
                                KeyboardLayouts.CODE_DELETE -> {
                                    totalKeystrokes++
                                    totalBackspaces++
                                    val b = minuteBuckets[bucketIdx]
                                    minuteBuckets[bucketIdx] = b.copy(backspaces = b.backspaces + 1, keystrokes = b.keystrokes + 1)
                                    if (transientComposing.isNotEmpty()) {
                                        transientComposing = transientComposing.dropLast(1)
                                    } else if (transientPreviewTail.isNotEmpty()) {
                                        val delLen = BanglaUnicodeHelper.calculateBackspaceLength(transientPreviewTail)
                                        transientPreviewTail = transientPreviewTail.dropLast(delLen.coerceAtLeast(1))
                                    }
                                    TypingPerformanceTracker.recordBackspace(sessionMode, System.nanoTime() - t0, false)
                                }
                                KeyboardLayouts.CODE_SPACE, KeyboardLayouts.CODE_ENTER -> {
                                    totalKeystrokes++
                                    totalChars++
                                    totalGraphemes++
                                    val sep = if (code == KeyboardLayouts.CODE_ENTER) "\n" else " "
                                    val b = minuteBuckets[bucketIdx]
                                    minuteBuckets[bucketIdx] = b.copy(
                                        charsTyped = b.charsTyped + 1,
                                        graphemesTyped = b.graphemesTyped + 1,
                                        keystrokes = b.keystrokes + 1
                                    )
                                    if (transientComposing.isNotEmpty()) {
                                        val committed = suggestionEngine.resolveCommitText(transientComposing, sessionMode, settings)
                                        transientPreviewTail = (transientPreviewTail + committed + sep).takeLast(120)
                                        transientComposing = ""
                                    } else {
                                        transientPreviewTail = (transientPreviewTail + sep).takeLast(120)
                                    }
                                    TypingPerformanceTracker.recordKeyCommit(sessionMode, System.nanoTime() - t0, 1, 1, false)
                                }
                            }
                        },
                        onCandidateClick = { candidate ->
                            totalCorrections++
                            transientPreviewTail = (transientPreviewTail + candidate + " ").takeLast(120)
                            transientComposing = ""
                            TypingPerformanceTracker.recordWordCommitted(viaSuggestion = true, isSensitiveField = false)
                        },
                        onModeSwitch = { sessionMode = it },
                        onCursorMove = {},
                        onOpenSettings = {},
                        onOpenClipboard = {}
                    )
                }
            }

            analysisResult?.let { res ->
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_session_analysis_output"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    PerfMetricTile("Avg Speed / Grapheme", "${formatPerfDecimal(res.averageWpm)} / ${formatPerfDecimal(res.averageGraphemeWpm)} WPM", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Consistency / Drift", "${formatPerfDecimal(res.wpmConsistencyScorePercent)}% • ${formatSigned(res.speedDriftPercent)}%", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Accuracy / Backspace", "${formatPerfDecimal(res.accuracyPercent)}% • ${formatPerfDecimal(res.backspaceFrequencyPercent)}% BS", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Corrections / Missed", "${formatPerfDecimal(res.correctionRatePercent)}% • ${res.missedTouches} missed", Modifier.fillMaxWidth(0.48f))
                }
                if (res.segments.isNotEmpty()) {
                    Text(
                        text = "Minute WPM Splits: " + res.segments.joinToString(" | ") { "M${it.minuteIndex}: ${formatPerfDecimal(it.wpm)}" },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonProtocolCard(
    history: List<TypingMetricsSummary>
) {
    val externalGboard = remember { mutableStateMapOf<Int, ExternalComparatorInput>() }
    val externalRidmik = remember { mutableStateMapOf<Int, ExternalComparatorInput>() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perf_lab_comparison_protocol_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Controlled Comparison Protocol (Okkhor vs Gboard vs Ridmik)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Same device, all 6 standardized prompts, randomized keyboard order. Okkhor metrics come directly from your runs above; Gboard and Ridmik remain '—' until you record real external measurements.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TypingBenchmarkEngine.COMPARISON_SCENARIOS.forEachIndexed { idx, scenario ->
                val prompt = TypingBenchmarkEngine.BENCHMARK_PROMPTS.getOrNull(idx)
                val matchingOkkhor = if (prompt != null) {
                    history.firstOrNull { it.label.contains(prompt.title, ignoreCase = true) }
                } else null
                val okkhorText = if (matchingOkkhor != null) {
                    "${formatPerfDecimal(matchingOkkhor.standardWpm)} WPM / ${formatPerfDecimal(matchingOkkhor.accuracyPercent)}%"
                } else "—"
                val gb = externalGboard[idx] ?: ExternalComparatorInput()
                val rd = externalRidmik[idx] ?: ExternalComparatorInput()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${idx + 1}. $scenario", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Okkhor (Measured): $okkhorText", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gb.wpm,
                            onValueChange = { externalGboard[idx] = gb.copy(wpm = it) },
                            label = { Text("Gboard WPM", fontSize = 10.sp) },
                            placeholder = { Text("—") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ab_gboard_wpm_$idx")
                        )
                        OutlinedTextField(
                            value = rd.wpm,
                            onValueChange = { externalRidmik[idx] = rd.copy(wpm = it) },
                            label = { Text("Ridmik WPM", fontSize = 10.sp) },
                            placeholder = { Text("—") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ab_ridmik_wpm_$idx")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerfMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatPerfDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatSigned(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return prefix + String.format(Locale.US, "%.1f", value)
}
