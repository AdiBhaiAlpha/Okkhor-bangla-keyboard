package com.example.okkhor.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.okkhor.benchmark.MicroBenchmarkResult
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypingPerformanceLabSection(
    settings: KeyboardSettings,
    activeTheme: KeyboardTheme,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    remember(context) {
        TypingPerformanceTracker.init(context)
        true
    }

    val liveSummary by TypingPerformanceTracker.liveSummary.collectAsState()
    val history by TypingPerformanceTracker.sessionHistory.collectAsState()

    val phoneticEngine = remember { BanglaPhoneticEngine() }
    val suggestionEngine = remember(phoneticEngine) { SuggestionEngine(phoneticEngine) }

    var selectedPromptIndex by remember { mutableIntStateOf(0) }
    val activePrompt = TypingBenchmarkEngine.BENCHMARK_PROMPTS[selectedPromptIndex]
    var posture by remember { mutableStateOf("2-Thumb") }
    var benchmarkRunning by remember { mutableStateOf(false) }
    var promptTypedText by remember { mutableStateOf("") }
    var promptComposingRoman by remember { mutableStateOf("") }
    var promptStartMs by remember { mutableLongStateOf(0L) }
    var promptBackspaces by remember { mutableIntStateOf(0) }
    var promptCorrections by remember { mutableIntStateOf(0) }
    var promptKeystrokes by remember { mutableIntStateOf(0) }
    var sandboxMode by remember(activePrompt.id) { mutableStateOf(activePrompt.mode) }
    var sandboxShifted by remember { mutableStateOf(false) }
    var lastPromptResult by remember { mutableStateOf<TypingMetricsSummary?>(null) }

    var microBenchResult by remember { mutableStateOf<MicroBenchmarkResult?>(null) }

    val promptCandidates = remember(promptComposingRoman, sandboxMode, settings.showSuggestions, settings.nextWordPredictionEnabled) {
        if (!settings.showSuggestions) {
            emptyList()
        } else if (promptComposingRoman.isNotEmpty()) {
            val translit = if (sandboxMode == InputMode.BANGLA_PHONETIC) {
                phoneticEngine.transliterate(promptComposingRoman)
            } else null
            suggestionEngine.getSuggestions(
                input = promptComposingRoman,
                mode = sandboxMode,
                maxCandidates = 6,
                precomputedTransliterated = translit,
                nextWordPredictionEnabled = settings.nextWordPredictionEnabled
            )
        } else if (settings.nextWordPredictionEnabled && promptTypedText.isNotBlank()) {
            val lastWord = promptTypedText.trimEnd().substringAfterLast(' ')
            suggestionEngine.getSuggestions(
                input = "",
                mode = sandboxMode,
                maxCandidates = 6,
                lastCommittedWord = lastWord,
                nextWordPredictionEnabled = true
            )
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header + Privacy Notice
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("btn_perf_lab_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back from Performance Lab"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.perf_lab_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.perf_lab_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("perf_lab_privacy_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Protection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Local & Privacy-Preserving: Zero typed characters, words, or password contents are ever stored or uploaded. Only numeric latencies and anonymized counters are recorded.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Card 1: Live IME Telemetry
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("perf_lab_live_telemetry_card"),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Live Telemetry",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live IME Telemetry (${liveSummary.layoutMode})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    PerfMetricTile("Standard WPM", formatPerfDecimal(liveSummary.standardWpm), Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile(
                        "Bangla Grapheme WPM",
                        "${formatPerfDecimal(liveSummary.banglaGraphemeWpm)} (${formatPerfDecimal(liveSummary.banglaGraphemesPerMin)} gr/m)",
                        Modifier.fillMaxWidth(0.48f)
                    )
                    PerfMetricTile("CER / Accuracy", "${formatPerfDecimal(liveSummary.characterErrorRate * 100.0)}% / ${formatPerfDecimal(liveSummary.accuracyPercent)}%", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Backspace / Corr.", "${formatPerfDecimal(liveSummary.backspaceFrequencyPercent)}% BS • ${formatPerfDecimal(liveSummary.correctionRatePercent)}% corr", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Missed / Neighbor", "${liveSummary.missedTouchEvents} missed • ${liveSummary.accidentalNeighborActivations} adj", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Keypress Latency", "P50 ${formatPerfDecimal(liveSummary.keypressP50Ms)}ms • P95 ${formatPerfDecimal(liveSummary.keypressP95Ms)}ms", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Bangla Engine Latency", "P50 ${formatPerfDecimal(liveSummary.banglaConversionP50Us)}µs • P95 ${formatPerfDecimal(liveSummary.banglaConversionP95Us)}µs", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Suggestions & Corr.", "${liveSummary.suggestionSelections} (${formatPerfDecimal(liveSummary.suggestionSelectionPercent)}%) • ${liveSummary.suggestionCorrectionTimeMs}ms", Modifier.fillMaxWidth(0.48f))
                    PerfMetricTile("Compose Frame Render", "${formatPerfDecimal(liveSummary.averageFrameMs)}ms avg • ${liveSummary.slowFramesOver16Ms} slow (>16ms)", Modifier.fillMaxWidth(0.98f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { TypingPerformanceTracker.startNewSession(settings.defaultInputMode) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_perf_start_fresh")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Fresh", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { TypingPerformanceTracker.saveCurrentSessionSnapshot("Manual Live Snapshot") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_perf_save_live")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Snapshot", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { TypingPerformanceTracker.clearAllMetrics() },
                        modifier = Modifier.testTag("btn_perf_reset")
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }
            }
        }

        // Card 2: Interactive Prompt Benchmark Runner (All 6 Standardized Tests)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("perf_lab_prompt_runner_card"),
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
                Text(
                    text = "Timed Typing Benchmark Runner (6 Standardized Tests)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypingBenchmarkEngine.BENCHMARK_PROMPTS.forEachIndexed { index, prompt ->
                        FilterChip(
                            selected = selectedPromptIndex == index,
                            onClick = {
                                selectedPromptIndex = index
                                sandboxMode = prompt.mode
                                benchmarkRunning = false
                                promptTypedText = ""
                                promptComposingRoman = ""
                            },
                            label = { Text(prompt.category, fontSize = 11.sp) },
                            modifier = Modifier.testTag("bench_prompt_${prompt.id}")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("2-Thumb", "1-Thumb").forEach { modeLabel ->
                        FilterChip(
                            selected = posture == modeLabel,
                            onClick = { posture = modeLabel },
                            label = { Text(modeLabel, fontSize = 11.5.sp) },
                            modifier = Modifier.testTag("bench_posture_${modeLabel.lowercase().replace("-", "_")}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(activePrompt.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activePrompt.targetText, fontSize = 13.5.sp)
                    }
                }

                OutlinedTextField(
                    value = promptTypedText + if (promptComposingRoman.isNotEmpty() && sandboxMode == InputMode.BANGLA_PHONETIC) {
                        phoneticEngine.transliterate(promptComposingRoman)
                    } else promptComposingRoman,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Typed Output (use embedded Okkhor keyboard below)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bench_typed_preview")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            TypingPerformanceTracker.startNewSession(activePrompt.mode)
                            sandboxMode = activePrompt.mode
                            promptTypedText = ""
                            promptComposingRoman = ""
                            promptBackspaces = 0
                            promptCorrections = 0
                            promptKeystrokes = 0
                            promptStartMs = System.currentTimeMillis()
                            lastPromptResult = null
                            benchmarkRunning = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_bench_start")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (benchmarkRunning) "Restart" else "Start Run", fontSize = 12.5.sp)
                    }

                    Button(
                        enabled = benchmarkRunning,
                        onClick = {
                            val finalized = if (promptComposingRoman.isNotEmpty()) {
                                val tail = if (sandboxMode == InputMode.BANGLA_PHONETIC) {
                                    phoneticEngine.transliterate(promptComposingRoman)
                                } else promptComposingRoman
                                promptTypedText + tail
                            } else promptTypedText

                            val elapsed = (System.currentTimeMillis() - promptStartMs).coerceAtLeast(300L)
                            val summary = TypingBenchmarkEngine.computePromptSummary(
                                prompt = activePrompt,
                                typedText = finalized,
                                elapsedMs = elapsed,
                                backspaceCount = promptBackspaces,
                                totalKeystrokes = promptKeystrokes,
                                posture = posture,
                                suggestionCorrections = promptCorrections
                            )
                            lastPromptResult = summary
                            TypingPerformanceTracker.saveCurrentSessionSnapshot(
                                label = summary.label,
                                cerOverride = summary.characterErrorRate,
                                accuracyOverride = summary.accuracyPercent,
                                wpmOverride = summary.standardWpm,
                                graphemeWpmOverride = summary.banglaGraphemeWpm
                            )
                            benchmarkRunning = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_bench_finish")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish & Save", fontSize = 12.5.sp)
                    }
                }

                lastPromptResult?.let { res ->
                    Text(
                        text = "Result: ${formatPerfDecimal(res.standardWpm)} WPM • Bangla Grapheme: ${formatPerfDecimal(res.banglaGraphemeWpm)} WPM • Acc: ${formatPerfDecimal(res.accuracyPercent)}% • BS: ${formatPerfDecimal(res.backspaceFrequencyPercent)}% • Corr: ${formatPerfDecimal(res.correctionRatePercent)}% • Missed: ${res.missedTouchEvents} • ${formatPerfDecimal(res.elapsedSeconds)}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (benchmarkRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        KeyboardContainer(
                            settings = settings,
                            theme = activeTheme,
                            currentMode = sandboxMode,
                            lastTextMode = sandboxMode.takeIf { it.isTextMode } ?: activePrompt.mode,
                            isShifted = sandboxShifted,
                            candidates = promptCandidates,
                            onKeyPress = { key ->
                                val t0 = System.nanoTime()
                                promptKeystrokes++
                                val isPhoneticAscii = sandboxMode == InputMode.BANGLA_PHONETIC &&
                                    key.length == 1 && (key[0] in 'a'..'z' || key[0] in 'A'..'Z' || key in listOf("^", ":", "`"))
                                if (isPhoneticAscii) {
                                    promptComposingRoman += key
                                } else {
                                    if (promptComposingRoman.isNotEmpty()) {
                                        val flushed = if (sandboxMode == InputMode.BANGLA_PHONETIC) {
                                            phoneticEngine.transliterate(promptComposingRoman)
                                        } else promptComposingRoman
                                        promptTypedText += flushed
                                        promptComposingRoman = ""
                                    }
                                    promptTypedText += key
                                }
                                if (sandboxShifted) sandboxShifted = false
                                TypingPerformanceTracker.recordKeyCommit(
                                    mode = sandboxMode,
                                    latencyNs = System.nanoTime() - t0,
                                    enteredChars = key.length,
                                    enteredGraphemes = BanglaUnicodeHelper.countGraphemeClusters(key),
                                    isSensitiveField = false
                                )
                            },
                            onActionKey = { code ->
                                val t0 = System.nanoTime()
                                when (code) {
                                    KeyboardLayouts.CODE_SHIFT -> sandboxShifted = !sandboxShifted
                                    KeyboardLayouts.CODE_DELETE -> {
                                        promptKeystrokes++
                                        promptBackspaces++
                                        if (promptComposingRoman.isNotEmpty()) {
                                            promptComposingRoman = promptComposingRoman.dropLast(1)
                                        } else if (promptTypedText.isNotEmpty()) {
                                            val delLen = BanglaUnicodeHelper.calculateBackspaceLength(promptTypedText)
                                            promptTypedText = promptTypedText.dropLast(delLen.coerceAtLeast(1))
                                        }
                                        TypingPerformanceTracker.recordBackspace(sandboxMode, System.nanoTime() - t0, false)
                                    }
                                    KeyboardLayouts.CODE_SPACE -> {
                                        promptKeystrokes++
                                        if (promptComposingRoman.isNotEmpty()) {
                                            val commit = suggestionEngine.resolveCommitText(promptComposingRoman, sandboxMode, settings)
                                            promptTypedText += "$commit "
                                            promptComposingRoman = ""
                                        } else {
                                            promptTypedText += " "
                                        }
                                        TypingPerformanceTracker.recordKeyCommit(sandboxMode, System.nanoTime() - t0, 1, 1, false)
                                    }
                                    KeyboardLayouts.CODE_ENTER -> {
                                        if (promptComposingRoman.isNotEmpty()) {
                                            val commit = suggestionEngine.resolveCommitText(promptComposingRoman, sandboxMode, settings)
                                            promptTypedText += "$commit\n"
                                            promptComposingRoman = ""
                                        } else {
                                            promptTypedText += "\n"
                                        }
                                    }
                                }
                            },
                            onCandidateClick = { candidate ->
                                promptCorrections++
                                promptTypedText += "$candidate "
                                promptComposingRoman = ""
                                TypingPerformanceTracker.recordWordCommitted(viaSuggestion = true, isSensitiveField = false)
                            },
                            onModeSwitch = { sandboxMode = it },
                            onCursorMove = {},
                            onOpenSettings = {},
                            onOpenClipboard = {}
                        )
                    }
                }
            }
        }

        // Card 3: Long-Session Comfort Mode (5, 10, 15 minutes continuous typing)
        LongSessionComfortCard(
            settings = settings,
            activeTheme = activeTheme,
            phoneticEngine = phoneticEngine,
            suggestionEngine = suggestionEngine,
            onSaveLongSessionSummary = { summary ->
                TypingPerformanceTracker.saveCurrentSessionSnapshot(
                    label = summary.label,
                    accuracyOverride = summary.accuracyPercent,
                    wpmOverride = summary.standardWpm,
                    graphemeWpmOverride = summary.banglaGraphemeWpm
                )
            }
        )

        // Card 4: Deterministic Engine Micro-Benchmark
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("perf_lab_micro_bench_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Engine Latency Micro-Benchmark (Avro & Dictionary)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { microBenchResult = TypingBenchmarkEngine.runEngineMicroBenchmark(iterations = 500) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_run_micro_bench")
                ) {
                    Text("Run 500-Iteration Engine Benchmark")
                }
                microBenchResult?.let { mb ->
                    Text(
                        text = "Avro Transliteration: P50 ${mb.transliterateP50Us}µs • P95 ${mb.transliterateP95Us}µs (Mean ${mb.transliterateMeanUs}µs)\n" +
                            "Bangla Suggestions: P50 ${mb.banglaSuggestionP50Us}µs • P95 ${mb.banglaSuggestionP95Us}µs\n" +
                            "English Suggestions: P50 ${mb.englishSuggestionP50Us}µs • P95 ${mb.englishSuggestionP95Us}µs\n" +
                            "Dictionary Bucket Lookup: P50 ${mb.dictionaryScanP50Us}µs • P95 ${mb.dictionaryScanP95Us}µs",
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("micro_bench_output")
                    )
                }
            }
        }

        // Card 5: Reproducible Controlled A/B Comparison Protocol
        ComparisonProtocolCard(history = history)

        // Card 6: Saved History
        if (history.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("perf_lab_history_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Saved Benchmark History (${history.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    history.take(8).forEachIndexed { idx, item ->
                        if (idx > 0) HorizontalDivider()
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "WPM ${formatPerfDecimal(item.standardWpm)} • Grapheme WPM ${formatPerfDecimal(item.banglaGraphemeWpm)} • Acc ${formatPerfDecimal(item.accuracyPercent)}% • P50 ${formatPerfDecimal(item.keypressP50Ms)}ms",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
