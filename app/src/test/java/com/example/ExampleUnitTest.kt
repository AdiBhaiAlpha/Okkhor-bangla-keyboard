package com.example

import com.example.okkhor.data.model.InputMode
import com.example.okkhor.data.model.KeyboardTheme
import com.example.okkhor.engine.BanglaDictionary
import com.example.okkhor.engine.BanglaPhoneticEngine
import com.example.okkhor.engine.BanglaUnicodeHelper
import com.example.okkhor.engine.SuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun testDictionaryLookups() {
        val matches = BanglaDictionary.findWordSuggestions("shonar")
        assertTrue(matches.contains("সোনার"))

        val dhakaMatches = BanglaDictionary.findWordSuggestions("dhaka")
        assertTrue(dhakaMatches.contains("ঢাকা"))
    }

    @Test
    fun testSuggestionEngineRanking() {
        val suggestions = suggestionEngine.getSuggestions("ami", InputMode.BANGLA_PHONETIC)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("আমি", suggestions.first())
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
