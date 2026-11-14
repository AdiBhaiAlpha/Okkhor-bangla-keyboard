package com.example.okkhor.emoji

import android.content.Context
import android.graphics.Paint
import androidx.core.graphics.PaintCompat
import com.example.okkhor.engine.BanglaPhoneticEngine
import org.json.JSONArray

/**
 * Loads the complete Unicode Emoji dataset from assets/unicode_emojis.json,
 * filters glyphs unsupported by the host Android font when running on device,
 * and provides instant English, Bangla, and phonetic Bangla keyword search.
 */
class UnicodeEmojiRepository(private val context: Context) {

    private val phoneticEngine = BanglaPhoneticEngine()

    @Volatile
    private var cachedEntries: List<EmojiEntry>? = null

    @Volatile
    private var cachedByCategory: Map<Int, List<EmojiEntry>>? = null

    @Volatile
    private var cachedByUnicode: Map<String, EmojiEntry>? = null

    fun getAllEmojis(): List<EmojiEntry> {
        cachedEntries?.let { return it }
        synchronized(this) {
            cachedEntries?.let { return it }
            val loaded = loadFromAssets()
            cachedEntries = loaded
            cachedByCategory = loaded.groupBy { it.category }
            val byCode = HashMap<String, EmojiEntry>(loaded.size * 2)
            for (entry in loaded) {
                byCode[entry.unicode] = entry
                for (variant in entry.variants) {
                    byCode[variant] = entry
                }
            }
            cachedByUnicode = byCode
            return loaded
        }
    }

    fun getByCategory(categoryId: Int): List<EmojiEntry> {
        if (cachedByCategory == null) {
            getAllEmojis()
        }
        return cachedByCategory?.get(categoryId).orEmpty()
    }

    fun findByUnicode(unicode: String): EmojiEntry? {
        if (cachedByUnicode == null) {
            getAllEmojis()
        }
        return cachedByUnicode?.get(unicode)
    }

    fun search(query: String, limit: Int = 180): List<EmojiEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val all = getAllEmojis()
        val qLower = trimmed.lowercase()

        // Also support phonetic Bangla queries typed with Latin keys (e.g. "brishti", "khushi", "bhalobasha")
        val phoneticQuery = if (trimmed.all { it.code < 128 && (it.isLetter() || it == ' ') }) {
            phoneticEngine.transliterate(trimmed).trim().lowercase().takeIf { it.isNotEmpty() && it != qLower }
        } else {
            null
        }

        data class Scored(val entry: EmojiEntry, val score: Int)
        val matches = ArrayList<Scored>(64)

        for (entry in all) {
            val nameLower = entry.name.lowercase()
            var score = 0

            if (nameLower == qLower) {
                score = 100
            } else if (nameLower.startsWith(qLower)) {
                score = 80
            } else if (nameLower.contains(qLower)) {
                score = 60
            }

            for (kw in entry.keywords) {
                val kwLower = kw.lowercase()
                when {
                    kwLower == qLower -> score = maxOf(score, 95)
                    kwLower.startsWith(qLower) -> score = maxOf(score, 70)
                    kwLower.contains(qLower) -> score = maxOf(score, 50)
                    phoneticQuery != null && kwLower == phoneticQuery -> score = maxOf(score, 85)
                    phoneticQuery != null && kwLower.contains(phoneticQuery) -> score = maxOf(score, 45)
                }
            }

            if (score > 0) {
                matches.add(Scored(entry, score))
            }
        }

        return matches
            .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.entry.name.length })
            .take(limit)
            .map { it.entry }
    }

    fun getRelatedEmojis(unicode: String, limit: Int = 12): List<EmojiEntry> {
        val target = findByUnicode(unicode) ?: return emptyList()
        val targetKeywords = target.keywords.map { it.lowercase() }.toHashSet()
        val all = getAllEmojis()

        return all
            .asSequence()
            .filter { it.unicode != target.unicode }
            .map { candidate ->
                var overlap = 0
                if (candidate.category == target.category) overlap += 2
                for (kw in candidate.keywords) {
                    if (kw.lowercase() in targetKeywords) overlap += 3
                }
                candidate to overlap
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    fun getSuggestedEmojis(
        recentEmojis: List<String>,
        frequentCounts: Map<String, Int>,
        limit: Int = 12
    ): List<EmojiEntry> {
        val all = getAllEmojis()
        if (all.isEmpty()) return emptyList()

        val result = LinkedHashMap<String, EmojiEntry>()
        // 1. Most frequently used emojis
        frequentCounts.entries
            .sortedByDescending { it.value }
            .forEach { (unicode, _) ->
                if (result.size >= limit) return@forEach
                findByUnicode(unicode)?.let {
                    result[unicode] = it.copy(unicode = unicode)
                }
            }

        // 2. Recent emojis
        for (unicode in recentEmojis) {
            if (result.size >= limit) break
            if (!result.containsKey(unicode)) {
                findByUnicode(unicode)?.let {
                    result[unicode] = it.copy(unicode = unicode)
                }
            }
        }

        // 3. Related emojis from most recent emoji
        recentEmojis.firstOrNull()?.let { latest ->
            for (rel in getRelatedEmojis(latest, limit = 6)) {
                if (result.size >= limit) break
                if (!result.containsKey(rel.unicode)) {
                    result[rel.unicode] = rel
                }
            }
        }

        // 4. Expressive defaults if user history is still sparse
        val popularDefaults = listOf("😀", "😂", "❤️", "🥰", "👍", "🙏", "🎉", "🔥", "😭", "😎", "🇧🇩", "✨")
        for (def in popularDefaults) {
            if (result.size >= limit) break
            if (!result.containsKey(def)) {
                findByUnicode(def)?.let { result[def] = it }
            }
        }

        return result.values.take(limit)
    }

    private fun loadFromAssets(): List<EmojiEntry> {
        val paint = Paint()
        // Robolectric shadow Paint returns false for hasGlyph; only filter when system font reports baseline support.
        val canFilterGlyphs = runCatching { PaintCompat.hasGlyph(paint, "😀") }.getOrDefault(false)

        val jsonText = context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(jsonText)
        val list = ArrayList<EmojiEntry>(array.length())

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val unicode = obj.getString("u")
            if (canFilterGlyphs && !isRenderable(paint, unicode)) {
                continue
            }

            val category = obj.getInt("c")
            val name = obj.optString("n", "")
            val kwArray = obj.optJSONArray("k")
            val keywords = if (kwArray != null) {
                List(kwArray.length()) { idx -> kwArray.getString(idx) }
            } else {
                emptyList()
            }

            val variantsArray = obj.optJSONArray("s")
            val variants = if (variantsArray != null) {
                buildList(variantsArray.length()) {
                    for (v in 0 until variantsArray.length()) {
                        val vStr = variantsArray.getString(v)
                        if (!canFilterGlyphs || isRenderable(paint, vStr)) {
                            add(vStr)
                        }
                    }
                }
            } else {
                emptyList()
            }

            list.add(
                EmojiEntry(
                    unicode = unicode,
                    category = category,
                    name = name,
                    keywords = keywords,
                    variants = variants
                )
            )
        }
        return list
    }

    private fun isRenderable(paint: Paint, emoji: String): Boolean {
        return runCatching { PaintCompat.hasGlyph(paint, emoji) }.getOrDefault(true)
    }

    companion object {
        private const val ASSET_FILE_NAME = "unicode_emojis.json"

        @Volatile
        private var INSTANCE: UnicodeEmojiRepository? = null

        fun getInstance(context: Context): UnicodeEmojiRepository {
            val appCtx = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnicodeEmojiRepository(appCtx).also { INSTANCE = it }
            }
        }
    }
}
