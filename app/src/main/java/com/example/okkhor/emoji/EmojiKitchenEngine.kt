package com.example.okkhor.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Original Okkhor Emoji Mashup / Emoji Kitchen Engine.
 *
 * Generates original 256x256 graphical PNG sticker combinations from curated compatible
 * base emojis on an Android Canvas. Does NOT scrape external proprietary assets and
 * explicitly exposes only supported compatible pairings.
 */
object EmojiKitchenEngine {

    data class MashupPalette(
        val primaryColor: Int,
        val secondaryColor: Int,
        val accentColor: Int,
        val badgeLabel: String,
        val ornament: String,
        val keywords: List<String>,
        val titleBn: String,
        val titleEn: String
    )

    private val BASE_THEMES: Map<String, MashupPalette> = mapOf(
        "😀" to MashupPalette(0xFFFFCA28.toInt(), 0xFFFF8F00.toInt(), 0xFFFFECB3.toInt(), "JOY", "✨", listOf("happy", "smile", "grin", "খুশি", "হাসি"), "আনন্দ", "Joyful"),
        "😂" to MashupPalette(0xFF42A5F5.toInt(), 0xFFFFCA28.toInt(), 0xFFE3F2FD.toInt(), "LOL", "💧", listOf("laugh", "funny", "lol", "হাসি", "মজা"), "হাস্যরস", "Laugh"),
        "😍" to MashupPalette(0xFFEC407A.toInt(), 0xFFFF80AB.toInt(), 0xFFFCE4EC.toInt(), "LOVE", "❤️", listOf("love", "crush", "heart", "ভালোবাসা", "প্রেম"), "ভালোবাসা", "Love"),
        "🥰" to MashupPalette(0xFFF06292.toInt(), 0xFFFFAB91.toInt(), 0xFFFFF0F5.toInt(), "ADORE", "💕", listOf("adore", "warm", "love", "স্নেহ", "ভালোবাসা"), "আদর", "Adore"),
        "😎" to MashupPalette(0xFF263238.toInt(), 0xFF00ACC1.toInt(), 0xFF80DEEA.toInt(), "COOL", "⚡", listOf("cool", "swag", "shades", "দারুণ", "স্টাইল"), "কুল", "Cool"),
        "🥳" to MashupPalette(0xFFAB47BC.toInt(), 0xFFFFB300.toInt(), 0xFFF3E5F5.toInt(), "PARTY", "🎉", listOf("party", "celebrate", "birthday", "উৎসব", "পার্টি"), "উৎসব", "Party"),
        "🥺" to MashupPalette(0xFF5C6BC0.toInt(), 0xFF90CAF9.toInt(), 0xFFE8EAF6.toInt(), "CUTE", "💫", listOf("plead", "cute", "puppy", "মায়া", "আদর"), "মায়াবী", "Pleading"),
        "😭" to MashupPalette(0xFF1E88E5.toInt(), 0xFF81D4FA.toInt(), 0xFFE1F5FE.toInt(), "CRY", "💧", listOf("cry", "sad", "tears", "কান্না", "দুঃখ"), "কান্না", "Tears"),
        "😡" to MashupPalette(0xFFE53935.toInt(), 0xFFFF7043.toInt(), 0xFFFFCDD2.toInt(), "RAGE", "🔥", listOf("angry", "mad", "fire", "রাগ", "ক্রোধ"), "ক্রোধ", "Fiery"),
        "🤯" to MashupPalette(0xFFFF7043.toInt(), 0xFF7E57C2.toInt(), 0xFFFFE0B2.toInt(), "BOOM", "💥", listOf("mindblown", "wow", "shock", "বিস্ময়", "দারুণ"), "বিস্ময়", "Mindblown"),
        "😇" to MashupPalette(0xFF4DD0E1.toInt(), 0xFFFFF176.toInt(), 0xFFE0F7FA.toInt(), "PURE", "⭐", listOf("angel", "halo", "sweet", "পবিত্র", "ভালো"), "সৌম্য", "Angelic"),
        "🤔" to MashupPalette(0xFF8D6E63.toInt(), 0xFFFFCA28.toInt(), 0xFFEFEBE9.toInt(), "THINK", "💭", listOf("think", "wonder", "idea", "চিন্তা", "ভাবনা"), "ভাবনা", "Thinking"),
        "😴" to MashupPalette(0xFF3949AB.toInt(), 0xFF7986CB.toInt(), 0xFFC5CAE9.toInt(), "ZZZ", "🌙", listOf("sleep", "tired", "night", "ঘুম", "রাত"), "নিদ্রা", "Sleepy"),
        "🤩" to MashupPalette(0xFFFFB300.toInt(), 0xFFE91E63.toInt(), 0xFFFFF8E1.toInt(), "STAR", "⭐", listOf("star", "amazing", "fan", "তারা", "চমৎকার"), "তারকা", "Starry"),
        "😱" to MashupPalette(0xFF3F51B5.toInt(), 0xFF00BCD4.toInt(), 0xFFE0F2F1.toInt(), "OMG", "⚡", listOf("shock", "omg", "scared", "ভয়", "অবাক"), "চমক", "Shock"),
        "❤️" to MashupPalette(0xFFE53935.toInt(), 0xFFF06292.toInt(), 0xFFFFEBEE.toInt(), "HEART", "❤️", listOf("heart", "love", "romance", "হৃদয়", "ভালোবাসা"), "হৃদয়", "Heart"),
        "🔥" to MashupPalette(0xFFFF5722.toInt(), 0xFFFFC107.toInt(), 0xFFFFF3E0.toInt(), "FIRE", "🔥", listOf("fire", "lit", "hot", "আগুন", "গরম"), "অগ্নি", "Blaze"),
        "⭐" to MashupPalette(0xFFFFC107.toInt(), 0xFFFFEE58.toInt(), 0xFFFFFDE7.toInt(), "GOLD", "✨", listOf("star", "shine", "gold", "তারা", "আলো"), "তারা", "Star"),
        "✨" to MashupPalette(0xFFAB47BC.toInt(), 0xFFFFD54F.toInt(), 0xFFF3E5F5.toInt(), "SPARK", "✨", listOf("sparkle", "magic", "shine", "জাদু", "ঝলক"), "ঝলক", "Sparkle"),
        "🎉" to MashupPalette(0xFFEC407A.toInt(), 0xFF29B6F6.toInt(), 0xFFFFF9C4.toInt(), "FEST", "🎊", listOf("party", "congrats", "celebrate", "উৎসব", "অভিনন্দন"), "উদযাপন", "Festive"),
        "🌈" to MashupPalette(0xFF26A69A.toInt(), 0xFFEC407A.toInt(), 0xFFE0F2F1.toInt(), "PRISM", "🌈", listOf("rainbow", "color", "hope", "রংধনু", "রঙিন"), "রংধনু", "Rainbow"),
        "☀️" to MashupPalette(0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFFDE7.toInt(), "SUN", "☀️", listOf("sun", "sunny", "bright", "সূর্য", "রোদ"), "রোদেলা", "Sunny"),
        "🌧️" to MashupPalette(0xFF0288D1.toInt(), 0xFF78909C.toInt(), 0xFFE1F5FE.toInt(), "RAIN", "💧", listOf("rain", "weather", "monsoon", "বৃষ্টি", "বর্ষা"), "বর্ষণ", "Rainy"),
        "❄️" to MashupPalette(0xFF00ACC1.toInt(), 0xFFB2EBF2.toInt(), 0xFFE0F7FA.toInt(), "ICE", "❄️", listOf("snow", "cold", "winter", "বরফ", "শীত"), "তুষার", "Frost"),
        "⚡" to MashupPalette(0xFFFBC02D.toInt(), 0xFF1E88E5.toInt(), 0xFFFFFDE7.toInt(), "VOLT", "⚡", listOf("lightning", "fast", "energy", "বিদ্যুৎ", "শক্তি"), "বজ্র", "Electric"),
        "🌸" to MashupPalette(0xFFF48FB1.toInt(), 0xFFFCE4EC.toInt(), 0xFFFFF0F5.toInt(), "BLOOM", "🌸", listOf("flower", "spring", "sakura", "ফুল", "বসন্ত"), "পুষ্প", "Blossom"),
        "☕" to MashupPalette(0xFF6D4C41.toInt(), 0xFFA1887F.toInt(), 0xFFEFEBE9.toInt(), "CAFE", "☕", listOf("coffee", "tea", "chai", "কফি", "চা"), "কফি", "Coffee"),
        "🐱" to MashupPalette(0xFFFFA726.toInt(), 0xFFFFE082.toInt(), 0xFFFFF8E1.toInt(), "MEOW", "🐾", listOf("cat", "kitty", "cute", "বিড়াল", "পোষা"), "বিড়াল", "Kitty"),
        "🐶" to MashupPalette(0xFF8D6E63.toInt(), 0xFFFFCC80.toInt(), 0xFFFFF3E0.toInt(), "WOOF", "🐾", listOf("dog", "puppy", "pet", "কুকুর"), "পাপ্পি", "Puppy"),
        "🦋" to MashupPalette(0xFF29B6F6.toInt(), 0xFFAB47BC.toInt(), 0xFFE1F5FE.toInt(), "FLUTTER", "✨", listOf("butterfly", "wing", "nature", "প্রজাপতি"), "প্রজাপতি", "Butterfly"),
        "👻" to MashupPalette(0xFF7E57C2.toInt(), 0xFFB39DDB.toInt(), 0xFFEDE7F6.toInt(), "BOO", "👻", listOf("ghost", "spooky", "fun", "ভূত"), "ভৌতিক", "Spooky"),
        "🤖" to MashupPalette(0xFF455A64.toInt(), 0xFF26C6DA.toInt(), 0xFFECEFF1.toInt(), "BOT", "⚙️", listOf("robot", "tech", "ai", "রোবট", "প্রযুক্তি"), "রোবট", "Cyber"),
        "👑" to MashupPalette(0xFFFFB300.toInt(), 0xFF8E24AA.toInt(), 0xFFFFF8E1.toInt(), "ROYAL", "👑", listOf("king", "queen", "crown", "রাজা", "মুকুট"), "রাজকীয়", "Royal"),
        "🎵" to MashupPalette(0xFFEC407A.toInt(), 0xFF5C6BC0.toInt(), 0xFFFCE4EC.toInt(), "MUSIC", "🎶", listOf("music", "song", "melody", "গান", "সুর"), "সঙ্গীত", "Melody"),
        "🚀" to MashupPalette(0xFF1565C0.toInt(), 0xFFFF7043.toInt(), 0xFFE3F2FD.toInt(), "LAUNCH", "🚀", listOf("rocket", "space", "fast", "রকেট", "মহাকাশ"), "রকেট", "Rocket")
    )

    val COMPATIBLE_BASE_EMOJIS: List<String> = BASE_THEMES.keys.toList()

    private val FACES = setOf("😀", "😂", "😍", "🥰", "😎", "🥳", "🥺", "😭", "😡", "🤯", "😇", "🤔", "😴", "🤩", "😱")
    private val MODIFIERS = setOf("❤️", "🔥", "⭐", "✨", "🎉", "🌈", "☀️", "🌧️", "❄️", "⚡", "🌸", "☕", "👑", "🎵", "🚀")
    private val CHARACTERS = setOf("🐱", "🐶", "🦋", "👻", "🤖")

    fun getCompatibleSecondEmojis(firstEmoji: String): List<String> {
        if (!BASE_THEMES.containsKey(firstEmoji)) return emptyList()
        return when {
            firstEmoji in FACES -> COMPATIBLE_BASE_EMOJIS.filter { it != firstEmoji }
            firstEmoji in CHARACTERS -> COMPATIBLE_BASE_EMOJIS.filter { it != firstEmoji }
            firstEmoji in MODIFIERS -> (FACES + CHARACTERS + MODIFIERS).filter { it != firstEmoji }
            else -> emptyList()
        }
    }

    fun isCompatiblePair(firstEmoji: String, secondEmoji: String): Boolean {
        if (firstEmoji == secondEmoji) return false
        return BASE_THEMES.containsKey(firstEmoji) && BASE_THEMES.containsKey(secondEmoji)
    }

    fun buildPairMetadata(firstEmoji: String, secondEmoji: String): EmojiMashupPair? {
        val t1 = BASE_THEMES[firstEmoji] ?: return null
        val t2 = BASE_THEMES[secondEmoji] ?: return null
        if (!isCompatiblePair(firstEmoji, secondEmoji)) return null

        val combinedKeywords = (t1.keywords + t2.keywords).distinct()
        return EmojiMashupPair(
            first = firstEmoji,
            second = secondEmoji,
            titleEn = "${t1.titleEn} + ${t2.titleEn}",
            titleBn = "${t1.titleBn} + ${t2.titleBn}",
            keywords = combinedKeywords
        )
    }

    fun searchCompatiblePairs(
        query: String,
        selectedFirst: String? = null,
        limit: Int = 24
    ): List<EmojiMashupPair> {
        val q = query.trim().lowercase()
        val firstList = if (selectedFirst != null && BASE_THEMES.containsKey(selectedFirst)) {
            listOf(selectedFirst)
        } else {
            COMPATIBLE_BASE_EMOJIS
        }

        val pairs = ArrayList<EmojiMashupPair>()
        for (first in firstList) {
            for (second in getCompatibleSecondEmojis(first)) {
                val meta = buildPairMetadata(first, second) ?: continue
                if (q.isEmpty() ||
                    meta.titleEn.lowercase().contains(q) ||
                    meta.titleBn.lowercase().contains(q) ||
                    meta.first == q ||
                    meta.second == q ||
                    meta.keywords.any { it.lowercase().contains(q) }
                ) {
                    pairs.add(meta)
                    if (pairs.size >= limit) return pairs
                }
            }
        }
        return pairs
    }

    fun renderMashupBitmap(
        firstEmoji: String,
        secondEmoji: String,
        style: EmojiMashupStyle,
        sizePx: Int = 256
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val t1 = BASE_THEMES[firstEmoji] ?: BASE_THEMES.values.first()
        val t2 = BASE_THEMES[secondEmoji] ?: BASE_THEMES.values.last()

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = sizePx * 0.44f

        // White sticker outer border
        val stickerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.style = Paint.Style.FILL
            setShadowLayer(sizePx * 0.03f, 0f, sizePx * 0.015f, 0x38000000)
        }

        when (style) {
            EmojiMashupStyle.FUSION_BLEND -> {
                canvas.drawCircle(cx, cy, radius + sizePx * 0.035f, stickerBorderPaint)
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cx - radius * 0.25f,
                        cy - radius * 0.25f,
                        radius * 1.25f,
                        intArrayOf(t2.accentColor, t1.primaryColor, t2.secondaryColor),
                        floatArrayOf(0f, 0.58f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, radius, bgPaint)

                // Glossy top arc highlight
                val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x35FFFFFF
                }
                canvas.drawOval(
                    RectF(cx - radius * 0.65f, cy - radius * 0.85f, cx + radius * 0.65f, cy - radius * 0.10f),
                    highlightPaint
                )

                // Primary hero emoji centered + fused top/corner accent emoji
                drawEmojiCentered(canvas, firstEmoji, cx, cy + sizePx * 0.03f, sizePx * 0.46f)
                drawMiniStickerBadge(canvas, secondEmoji, cx + radius * 0.60f, cy - radius * 0.56f, sizePx * 0.22f, t2.accentColor)
                drawEmojiCentered(canvas, t2.ornament, cx - radius * 0.66f, cy - radius * 0.48f, sizePx * 0.12f)
            }

            EmojiMashupStyle.AURA_HALO -> {
                canvas.drawCircle(cx, cy, radius + sizePx * 0.035f, stickerBorderPaint)
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                        intArrayOf(t1.secondaryColor, t2.primaryColor),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, radius, haloPaint)

                // Inner glowing ring
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = sizePx * 0.025f
                    color = t2.accentColor
                }
                canvas.drawCircle(cx, cy, radius * 0.78f, ringPaint)

                // 6 orbiting mini second emojis around perimeter
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60 - 90).toDouble())
                    val ox = cx + (radius * 0.78f * cos(angle)).toFloat()
                    val oy = cy + (radius * 0.78f * sin(angle)).toFloat()
                    drawEmojiCentered(canvas, secondEmoji, ox, oy, sizePx * 0.13f)
                }

                drawEmojiCentered(canvas, firstEmoji, cx, cy + sizePx * 0.01f, sizePx * 0.42f)
            }

            EmojiMashupStyle.DUO_BADGE -> {
                val rect = RectF(sizePx * 0.06f, sizePx * 0.16f, sizePx * 0.94f, sizePx * 0.84f)
                val corner = sizePx * 0.22f
                val outerRect = RectF(rect.left - sizePx * 0.025f, rect.top - sizePx * 0.025f, rect.right + sizePx * 0.025f, rect.bottom + sizePx * 0.025f)
                canvas.drawRoundRect(outerRect, corner + 6f, corner + 6f, stickerBorderPaint)

                val duoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        rect.left, rect.top, rect.right, rect.bottom,
                        intArrayOf(t1.primaryColor, t2.primaryColor),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, corner, corner, duoPaint)

                drawEmojiCentered(canvas, firstEmoji, sizePx * 0.31f, cy, sizePx * 0.34f)
                drawEmojiCentered(canvas, secondEmoji, sizePx * 0.69f, cy, sizePx * 0.34f)
                drawEmojiCentered(canvas, t2.ornament, cx, cy - sizePx * 0.16f, sizePx * 0.14f)
            }

            EmojiMashupStyle.STAMP_CARD -> {
                val cardRect = RectF(sizePx * 0.10f, sizePx * 0.10f, sizePx * 0.90f, sizePx * 0.90f)
                val outerCard = RectF(cardRect.left - sizePx * 0.025f, cardRect.top - sizePx * 0.025f, cardRect.right + sizePx * 0.025f, cardRect.bottom + sizePx * 0.025f)
                canvas.drawRoundRect(outerCard, sizePx * 0.18f, sizePx * 0.18f, stickerBorderPaint)

                val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, cardRect.top, 0f, cardRect.bottom,
                        intArrayOf(t1.accentColor, t2.secondaryColor),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, sizePx * 0.16f, sizePx * 0.16f, cardPaint)

                drawEmojiCentered(canvas, firstEmoji, cx - sizePx * 0.09f, cy - sizePx * 0.06f, sizePx * 0.38f)
                drawEmojiCentered(canvas, secondEmoji, cx + sizePx * 0.14f, cy + sizePx * 0.09f, sizePx * 0.31f)

                // Crisp bottom caption pill
                val pillRect = RectF(cx - sizePx * 0.28f, cardRect.bottom - sizePx * 0.15f, cx + sizePx * 0.28f, cardRect.bottom - sizePx * 0.03f)
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC111827.toInt() }
                canvas.drawRoundRect(pillRect, sizePx * 0.06f, sizePx * 0.06f, pillPaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = sizePx * 0.068f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val caption = "${t1.badgeLabel} × ${t2.badgeLabel}"
                val fm = textPaint.fontMetrics
                val textY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(caption, pillRect.centerX(), textY, textPaint)
            }
        }

        return bitmap
    }

    fun saveMashupPng(
        context: Context,
        firstEmoji: String,
        secondEmoji: String,
        style: EmojiMashupStyle
    ): File {
        val dir = File(context.cacheDir, "shared_media/mashups").apply { mkdirs() }
        val key = "${firstEmoji.codePoints().toArray().joinToString("-")}_${secondEmoji.codePoints().toArray().joinToString("-")}_${style.id}"
        val outFile = File(dir, "mashup_$key.png")
        if (outFile.exists() && outFile.length() > 100L) {
            return outFile
        }
        val bitmap = renderMashupBitmap(firstEmoji, secondEmoji, style)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }

    private fun drawMiniStickerBadge(
        canvas: Canvas,
        emoji: String,
        cx: Float,
        cy: Float,
        diameter: Float,
        fillColor: Int
    ) {
        val r = diameter / 2f
        val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawCircle(cx, cy, r + diameter * 0.12f, whiteRing)
        canvas.drawCircle(cx, cy, r, fillPaint)
        drawEmojiCentered(canvas, emoji, cx, cy, diameter * 0.72f)
    }

    private fun drawEmojiCentered(
        canvas: Canvas,
        emoji: String,
        cx: Float,
        cy: Float,
        textSizePx: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }
        val fm = paint.fontMetrics
        val y = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(emoji, cx, y, paint)
    }
}
