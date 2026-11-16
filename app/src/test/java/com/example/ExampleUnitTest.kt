package com.example

import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.engine.BanglaConjunctHelper
import com.example.okkhor.engine.BanglaDictionary
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.ClipboardSmartParser
import com.example.okkhor.engine.DateTimeFormatHelper
import com.example.okkhor.engine.GestureTypingEngine
import com.example.okkhor.engine.SuggestionEngine
import com.example.okkhor.engine.TranslationController
import com.example.okkhor.engine.TranslationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val engine = BanglaPhoneticEngine()
    private val suggestionEngine = SuggestionEngine(engine)

    @Test
    fun testPhoneticBasicVowelsAndConsonants() {
        assertEquals("আমি", engine.transliterate("ami"))
        assertEquals("তুমি", engine.transliterate("tumi"))
        assertEquals("আমরা", engine.transliterate("amra"))
        assertEquals("বাংলা", engine.transliterate("bangla"))
    }

    @Test
    fun testPhoneticConjuncts() {
        assertEquals("ক্ষ", engine.transliterate("kkh"))
        assertEquals("ত্র", engine.transliterate("tr"))
        assertEquals("স্ত", engine.transliterate("st"))
        assertEquals("ধন্যবাদ", engine.transliterate("dhonyobad"))
        assertEquals("ধন্যবাদ", suggestionEngine.getSuggestions("dhonnobad", InputMode.BANGLA_PHONETIC).first())
    }

    @Test
    fun testRephAndSpecialChars() {
        // Reph
        assertTrue(engine.transliterate("rk").contains("র্ক"))
        // Khanda Ta
        assertEquals("ৎ", engine.transliterate("t`"))
        // Chandrabindu
        assertEquals("ঁ", engine.transliterate("^"))
        // Bisarga
        assertEquals("ঃ", engine.transliterate(":"))
    }

    @Test
    fun testUnicodeHelperGraphemeBackspace() {
        assertEquals(0, BanglaUnicodeHelper.calculateBackspaceLength(""))
        assertEquals(1, BanglaUnicodeHelper.calculateBackspaceLength("ক"))
        assertEquals(1, BanglaUnicodeHelper.calculateBackspaceLength("কি"))
        assertEquals(1, BanglaUnicodeHelper.calculateBackspaceLength("ক্ষ"))
    }

    @Test
    fun testWordDeleteLengthRespectsEnglishAndBanglaBoundaries() {
        assertEquals(0, BanglaUnicodeHelper.calculateWordDeleteLength(""))
        assertEquals("world".length, BanglaUnicodeHelper.calculateWordDeleteLength("Hello world"))
        assertEquals("world  ".length, BanglaUnicodeHelper.calculateWordDeleteLength("Hello world  "))
        assertEquals("বিজ্ঞান".length, BanglaUnicodeHelper.calculateWordDeleteLength("বাংলাদেশের বিজ্ঞান"))
        assertEquals("কর ".length, BanglaUnicodeHelper.calculateWordDeleteLength("ক্ষমা কর "))
    }

    @Test
    fun testDictionaryLookups() {
        val matches = BanglaDictionary.findWordSuggestions("shonar")
        assertTrue(matches.contains("সোনার"))

        val dhakaMatches = BanglaDictionary.findWordSuggestions("dhaka")
        assertTrue(dhakaMatches.contains("ঢাকা"))
    }

    @Test
    fun testSuggestionEngineRankingAndConjuncts() {
        val suggestions = suggestionEngine.getSuggestions("ami", InputMode.BANGLA_PHONETIC)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("আমি", suggestions.first())

        val conjuncts = BanglaConjunctHelper.getEnhancements("gg", "গ")
        assertTrue(conjuncts.contains("জ্ঞ"))
        assertTrue(BanglaConjunctHelper.getAlternateChars("a").contains("@"))
        assertTrue(BanglaConjunctHelper.getAlternateChars("ক").contains("ক্ষ"))

        val fixedSuggestions = suggestionEngine.getSuggestions("ভাল", InputMode.BANGLA_FIXED)
        assertTrue(fixedSuggestions.contains("ভালো"))
    }

    @Test
    fun testClipboardSmartParserOtpAndLinks() {
        val otpMsg = ClipboardSmartParser.extractHint("Your verification code is 482913 for login")
        assertNotNull(otpMsg)
        assertTrue(otpMsg!!.isOtp)
        assertEquals("482913", otpMsg.value)

        val standaloneOtp = ClipboardSmartParser.extractHint("904512")
        assertNotNull(standaloneOtp)
        assertTrue(standaloneOtp!!.isOtp)
        assertEquals("904512", standaloneOtp.value)

        val bnOtp = ClipboardSmartParser.extractHint("আপনার ওটিপি কোড: 731950")
        assertNotNull(bnOtp)
        assertTrue(bnOtp!!.isOtp)
        assertEquals("731950", bnOtp.value)

        val urlHint = ClipboardSmartParser.extractHint("Check https://example.com/docs?page=2 today")
        assertNotNull(urlHint)
        assertTrue(urlHint!!.isUrl)
        assertFalse(urlHint.isOtp)
        assertEquals("https://example.com/docs?page=2", urlHint.value)

        assertNull(ClipboardSmartParser.extractHint("Regular plain sentence without digits"))
    }

    @Test
    fun testGestureTypingDateTimeAndOfflineTranslation() {
        val enWords = GestureTypingEngine.matchPathToWords(
            listOf("h", "e", "l", "l", "o"),
            InputMode.ENGLISH
        )
        assertTrue(enWords.any { it.equals("hello", ignoreCase = true) })

        val bnWords = GestureTypingEngine.matchPathToWords(
            listOf("a", "m", "i"),
            InputMode.BANGLA_PHONETIC
        )
        assertTrue(bnWords.contains("আমি"))

        assertEquals("২০২৬", DateTimeFormatHelper.toBanglaDigits("2026"))
        val dtOptions = DateTimeFormatHelper.getQuickOptions(1700000000000L)
        assertEquals(5, dtOptions.size)
        assertTrue(dtOptions.any { it.id == "bn_date" && it.value.isNotBlank() })

        val translator = TranslationController()
        assertEquals("শুভ সকাল", translator.translateOffline("good morning", TranslationDirection.EN_TO_BN))
        assertEquals("Good morning", translator.translateOffline("শুভ সকাল", TranslationDirection.BN_TO_EN))
    }

    @Test
    fun testThemeJsonSerialization() {
        val original = KeyboardTheme.ClassicTealGold
        val json = original.toJson()
        val restored = KeyboardTheme.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.id, restored?.id)
        assertEquals(original.name, restored?.name)
        assertEquals(original.backgroundColor, restored?.backgroundColor)
        assertEquals(original.keyCornerRadiusDp, restored?.keyCornerRadiusDp)
    }

    @Test
    fun testEngineMicroBenchmarkAndMetrics() {
        val result = com.example.okkhor.benchmark.TypingBenchmarkEngine.runEngineMicroBenchmark(iterations = 1000)
        println(
            "ENGINE_BENCHMARK translitP50=${result.transliterateP50Us}us " +
                "translitP95=${result.transliterateP95Us}us " +
                "translitMean=${result.transliterateMeanUs}us " +
                "bnSugP50=${result.banglaSuggestionP50Us}us " +
                "bnSugP95=${result.banglaSuggestionP95Us}us " +
                "enSugP50=${result.englishSuggestionP50Us}us " +
                "enSugP95=${result.englishSuggestionP95Us}us " +
                "dictP50=${result.dictionaryScanP50Us}us " +
                "dictP95=${result.dictionaryScanP95Us}us"
        )
        assertTrue(result.transliterateP50Us >= 0.0)
        assertTrue(result.banglaSuggestionP50Us >= 0.0)
    }

    @Test
    fun testAvroConjunctEquivalenceAndLruCacheConsistency() {
        val cases = mapOf(
            "ami" to "আমি",
            "amra" to "আমরা",
            "bangladesh" to "বাংলাদেশ",
            "shro" to "শ্র",
            "str" to "স্ত্র",
            "kkh" to "ক্ষ",
            "jnj" to "জ্ঞ",
            "jny" to "জ্ন্য",
            "t`" to "ৎ",
            "bng" to "বং",
            "koi" to "কৈ"
        )
        // Call twice to exercise both cache miss and cache hit paths
        for ((input, expected) in cases) {
            assertEquals("Cache miss mismatch for $input", expected, engine.transliterate(input))
            assertEquals("Cache hit mismatch for $input", expected, engine.transliterate(input))
        }
    }

    @Test
    fun testConservativeCommitAndIndependentPredictionToggles() {
        val offAutoCorrect = com.example.okkhor.data.model.KeyboardSettings(autoCorrect = false, nextWordPredictionEnabled = true)
        val onAutoCorrect = com.example.okkhor.data.model.KeyboardSettings(autoCorrect = true, nextWordPredictionEnabled = true)

        // "am" must resolve to "আম" on Space, never silently expanding to "আমি"
        assertEquals("আম", suggestionEngine.resolveCommitText("am", InputMode.BANGLA_PHONETIC, offAutoCorrect))
        assertEquals("আম", suggestionEngine.resolveCommitText("am", InputMode.BANGLA_PHONETIC, onAutoCorrect))

        // Suggestion strip for "am" keeps "আম" first while still offering "আমি" as selectable completion
        val bnSuggestions = suggestionEngine.getSuggestions("am", InputMode.BANGLA_PHONETIC)
        assertTrue(bnSuggestions.isNotEmpty())
        assertEquals("আম", bnSuggestions.first())
        assertTrue(bnSuggestions.contains("আমি"))

        // English conservative autocorrect preserves "its" even when enabled, and "im" when disabled
        assertEquals("im", suggestionEngine.resolveCommitText("im", InputMode.ENGLISH, offAutoCorrect))
        assertEquals("I'm", suggestionEngine.resolveCommitText("im", InputMode.ENGLISH, onAutoCorrect))
        assertEquals("its", suggestionEngine.resolveCommitText("its", InputMode.ENGLISH, onAutoCorrect))

        // Independent nextWordPredictionEnabled toggle
        val nextEnabled = suggestionEngine.getSuggestions(
            input = "",
            mode = InputMode.ENGLISH,
            lastCommittedWord = "good",
            nextWordPredictionEnabled = true
        )
        assertTrue(nextEnabled.isNotEmpty())

        val nextDisabled = suggestionEngine.getSuggestions(
            input = "",
            mode = InputMode.ENGLISH,
            lastCommittedWord = "good",
            nextWordPredictionEnabled = false
        )
        assertTrue(nextDisabled.isEmpty())
    }

    @Test
    fun testPerformanceTrackerPrivacyAndDistinctBanglaGraphemeWpm() {
        val tracker = com.example.okkhor.benchmark.TypingPerformanceTracker
        tracker.clearAllMetrics()
        tracker.startNewSession(InputMode.BANGLA_PHONETIC)

        // Simulate sensitive password input -> must be completely ignored
        tracker.recordKeyCommit(InputMode.ENGLISH, 500_000L, 12, 12, isSensitiveField = true)
        assertEquals(0, tracker.liveSummary.value.totalKeystrokes)

        // Record normal Bangla typing
        tracker.recordKeyCommit(InputMode.BANGLA_PHONETIC, 1_200_000L, enteredChars = 10, enteredGraphemes = 4, isSensitiveField = false)
        val summaryStr = tracker.liveSummary.value.toString()
        assertFalse(summaryStr.contains("SecretPassword"))
        assertTrue(tracker.liveSummary.value.totalKeystrokes >= 1)

        // Distinct Bangla Grapheme WPM vs Standard WPM (10 UTF-16 code units vs 4 grapheme clusters in 12s)
        val stdWpm = tracker.computeStandardWpm(enteredChars = 10, elapsedMs = 12_000L)
        val graphemeWpm = tracker.computeBanglaGraphemeWpm(graphemeClusters = 4, elapsedMs = 12_000L)
        assertEquals(10.0, stdWpm, 0.01)
        assertEquals(4.0, graphemeWpm, 0.01)

        // CER & Accuracy math
        val cer = com.example.okkhor.benchmark.TypingBenchmarkEngine.computeCharacterErrorRate("hello", "helo")
        assertEquals(0.2, cer, 0.01)
        assertEquals(80.0, com.example.okkhor.benchmark.TypingBenchmarkEngine.computeAccuracyPercent("hello", "helo"), 0.1)
    }

    @Test
    fun testKeyHitTestResolverEdgesCornersGapsVoronoiAndBottomRow() {
        val density = 3.0f // xxhdpi (~1mm = 6.3dp = 18.9px)
        val oneMmPx = 6.3f * density
        val keyW = 96f
        val keyH = 144f
        val hGap = 12f // 4dp gap
        val vGap = 12f // 4dp vertical gap between rows

        // Build realistic Row 1 (q..p), staggered Row 2 (a..l), and Bottom Row (?123, Emoji, Lang, Space, Dot, Enter)
        val targets = mutableListOf<com.example.okkhor.ui.keyboard.KeyHitTarget>()
        val row1Keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        row1Keys.forEachIndexed { i, k ->
            val left = 9f + i * (keyW + hGap)
            targets.add(
                com.example.okkhor.ui.keyboard.KeyHitTarget(
                    id = "r1_$k",
                    category = com.example.okkhor.ui.keyboard.KeyHitCategory.LETTER,
                    rowIndex = 1,
                    bounds = androidx.compose.ui.geometry.Rect(left, 10f, left + keyW, 10f + keyH),
                    isGlideEligible = true
                )
            )
        }

        val row2Top = 10f + keyH + vGap
        val staggerLeft = 9f + keyW * 0.45f
        val row2Keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        row2Keys.forEachIndexed { i, k ->
            val left = staggerLeft + i * (keyW + hGap)
            targets.add(
                com.example.okkhor.ui.keyboard.KeyHitTarget(
                    id = "r2_$k",
                    category = com.example.okkhor.ui.keyboard.KeyHitCategory.LETTER,
                    rowIndex = 2,
                    bounds = androidx.compose.ui.geometry.Rect(left, row2Top, left + keyW, row2Top + keyH),
                    isGlideEligible = true
                )
            )
        }

        val bottomTop = row2Top + keyH + vGap
        // Simplified bottom row with exact proportional widths totaling ~1068px
        val symRect = androidx.compose.ui.geometry.Rect(9f, bottomTop, 130f, bottomTop + keyH)
        val emojiRect = androidx.compose.ui.geometry.Rect(142f, bottomTop, 238f, bottomTop + keyH)
        val langRect = androidx.compose.ui.geometry.Rect(250f, bottomTop, 346f, bottomTop + keyH)
        val spaceRect = androidx.compose.ui.geometry.Rect(358f, bottomTop, 800f, bottomTop + keyH)
        val dotRect = androidx.compose.ui.geometry.Rect(812f, bottomTop, 908f, bottomTop + keyH)
        val enterRect = androidx.compose.ui.geometry.Rect(920f, bottomTop, 1070f, bottomTop + keyH)

        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("btn_symbol_key", com.example.okkhor.ui.keyboard.KeyHitCategory.SYMBOL_MODE, 4, symRect, isBottomRow = true))
        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("btn_emoji", com.example.okkhor.ui.keyboard.KeyHitCategory.EMOJI, 4, emojiRect, isBottomRow = true))
        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("btn_language", com.example.okkhor.ui.keyboard.KeyHitCategory.LANGUAGE, 4, langRect, isBottomRow = true))
        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("spacebar", com.example.okkhor.ui.keyboard.KeyHitCategory.SPACEBAR, 4, spaceRect, isBottomRow = true))
        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("key_dot", com.example.okkhor.ui.keyboard.KeyHitCategory.NUMBER_OR_SYMBOL, 4, dotRect, isBottomRow = true))
        targets.add(com.example.okkhor.ui.keyboard.KeyHitTarget("btn_enter", com.example.okkhor.ui.keyboard.KeyHitCategory.ENTER, 4, enterRect, isBottomRow = true))

        val tolerances = com.example.okkhor.ui.keyboard.KeyHitToleranceConfig.fromDensity(density, keyW)
        val eTarget = targets.first { it.id == "r1_e" }
        val rTarget = targets.first { it.id == "r1_r" }

        // 1. Center & all 4 exact border edges & 4 corners must stay on 'e'
        val exactPts = listOf(
            eTarget.bounds.center,
            androidx.compose.ui.geometry.Offset(eTarget.bounds.left, eTarget.bounds.center.y),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.right, eTarget.bounds.center.y),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.center.x, eTarget.bounds.top),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.center.x, eTarget.bounds.bottom),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.left, eTarget.bounds.top),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.right, eTarget.bounds.top),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.left, eTarget.bounds.bottom),
            androidx.compose.ui.geometry.Offset(eTarget.bounds.right, eTarget.bounds.bottom)
        )
        for (pt in exactPts) {
            val res = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(pt, targets, tolerances)
            assertEquals("Expected exact edge/corner $pt to hit 'e'", "r1_e", res?.target?.id)
        }

        // 2. Inter-key gap Voronoi tie-breaker: left of midpoint hits 'e', right of midpoint hits 'r'
        val gapMidX = (eTarget.bounds.right + rTarget.bounds.left) / 2f
        val leftBias = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(gapMidX - 2f, eTarget.bounds.center.y),
            targets,
            tolerances
        )
        val rightBias = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(gapMidX + 2f, eTarget.bounds.center.y),
            targets,
            tolerances
        )
        assertEquals("r1_e", leftBias?.target?.id)
        assertEquals("r1_r", rightBias?.target?.id)

        // 3. Staggered Row-2 gutter beside 'a' and 'l' resolves accurately without missing
        val aTarget = targets.first { it.id == "r2_a" }
        val lTarget = targets.first { it.id == "r2_l" }
        val leftGutterHit = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(aTarget.bounds.left - oneMmPx * 0.5f, aTarget.bounds.center.y),
            targets,
            tolerances
        )
        assertEquals("r2_a", leftGutterHit?.target?.id)
        val rightGutterHit = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(lTarget.bounds.right + oneMmPx * 0.5f, lTarget.bounds.center.y),
            targets,
            tolerances
        )
        assertEquals("r2_l", rightGutterHit?.target?.id)

        // 4. Touch strictly inside visual bounds is NEVER stolen by neighbor expanded region
        val innerNearBorder = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(eTarget.bounds.right - 1f, eTarget.bounds.bottom - 1f),
            targets,
            tolerances
        )
        assertEquals("r1_e", innerNearBorder?.target?.id)
        assertTrue(innerNearBorder!!.hitInsideVisualBounds)

        // 5. ~1mm below bottom row Spacebar & Enter & Symbol buttons hits the bottom-row key reliably
        val spaceBottom1mm = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(spaceRect.left + 8f, spaceRect.bottom + oneMmPx),
            targets,
            tolerances
        )
        assertEquals("spacebar", spaceBottom1mm?.target?.id)

        // Outer edge of wide Spacebar must not be stolen by Language or Dot neighbors
        val spaceOuterLeftEdge = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(spaceRect.left + 1f, spaceRect.center.y),
            targets,
            tolerances
        )
        assertEquals("spacebar", spaceOuterLeftEdge?.target?.id)

        val enterBottom1mm = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(
            androidx.compose.ui.geometry.Offset(enterRect.center.x, enterRect.bottom + oneMmPx),
            targets,
            tolerances
        )
        assertEquals("btn_enter", enterBottom1mm?.target?.id)

        // 6. Repeatability check: 20 repeated calls at exact same boundary point always match identically
        val probe = androidx.compose.ui.geometry.Offset(eTarget.bounds.center.x, eTarget.bounds.bottom + 4f)
        val firstId = com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(probe, targets, tolerances)?.target?.id
        repeat(20) {
            assertEquals(firstId, com.example.okkhor.ui.keyboard.KeyHitTestResolver.resolveHit(probe, targets, tolerances)?.target?.id)
        }
    }

    @Test
    fun testImprovedSuggestionRankingAndSixBenchmarkLongSessionMetrics() {
        val enTh = com.example.okkhor.engine.EnglishDictionary.findWordSuggestions("th", 5)
        assertEquals("the", enTh.first())

        val bnBa = suggestionEngine.getSuggestions("ba", InputMode.BANGLA_PHONETIC, maxCandidates = 6)
        assertTrue(bnBa.indexOf("বাংলা") in 1..3)

        assertEquals(6, com.example.okkhor.benchmark.TypingBenchmarkEngine.BENCHMARK_PROMPTS.size)
        assertEquals(6, com.example.okkhor.benchmark.TypingBenchmarkEngine.COMPARISON_SCENARIOS.size)
        assertTrue(com.example.okkhor.benchmark.TypingBenchmarkEngine.BENCHMARK_PROMPTS.any { it.id == "bn_avro_long" })

        val longAnalysis = com.example.okkhor.benchmark.TypingBenchmarkEngine.computeLongSessionAnalysis(
            targetDurationMinutes = 5,
            totalElapsedMs = 120_000L,
            buckets = listOf(
                com.example.okkhor.benchmark.LongSessionBucketInput(1, charsTyped = 150, graphemesTyped = 120, backspaces = 3, keystrokes = 155),
                com.example.okkhor.benchmark.LongSessionBucketInput(2, charsTyped = 150, graphemesTyped = 120, backspaces = 3, keystrokes = 155)
            ),
            totalChars = 300,
            totalGraphemes = 240,
            totalBackspaces = 6,
            totalSuggestionCorrections = 0,
            totalKeystrokes = 310,
            missedTouches = 0,
            p50Ms = 1.2,
            p95Ms = 2.4
        )
        assertEquals(30.0, longAnalysis.averageWpm, 0.1)
        assertEquals(100.0, longAnalysis.wpmConsistencyScorePercent, 0.1)
        assertEquals(0.0, longAnalysis.speedDriftPercent, 0.1)
    }

    @Test
    fun testLiquidDropRippleGeometryAndStateCapping() {
        val bounds = androidx.compose.ui.geometry.Rect(10f, 20f, 110f, 140f)
        val clamped = com.example.okkhor.ui.keyboard.clampPointToKeyBounds(
            point = androidx.compose.ui.geometry.Offset(60f, 152f),
            bounds = bounds,
            insetPx = 4f
        )
        assertEquals(60f, clamped.x, 0.01f)
        assertEquals(136f, clamped.y, 0.01f)

        val spaceBounds = androidx.compose.ui.geometry.Rect(0f, 0f, 500f, 120f)
        val spaceMaxR = com.example.okkhor.ui.keyboard.computeLiquidMaxRadius(
            impact = androidx.compose.ui.geometry.Offset(250f, 60f),
            bounds = spaceBounds
        )
        assertTrue(spaceMaxR in 240f..290f)

        val state = com.example.okkhor.ui.keyboard.KeyboardSurfaceTouchState()
        repeat(18) { idx ->
            state.triggerLiquidDrop(
                keyId = "k_$idx",
                keyBounds = bounds,
                rawTouchPoint = androidx.compose.ui.geometry.Offset(40f + idx, 80f)
            )
        }
        assertEquals(
            com.example.okkhor.ui.keyboard.KeyboardSurfaceTouchState.MAX_ACTIVE_LIQUID_DROPS,
            state.liquidDrops.size
        )
        assertTrue(state.hasActiveLiquidDrops)
        assertEquals("k_17", state.liquidDrops.last().keyId)
    }
}
