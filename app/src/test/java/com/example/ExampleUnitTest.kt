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
}
