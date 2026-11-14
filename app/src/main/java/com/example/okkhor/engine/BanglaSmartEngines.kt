package com.example.okkhor.engine

import com.example.okkhor.data.model.InputMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ClipboardSmartHint(
    val value: String,
    val label: String,
    val isOtp: Boolean,
    val isUrl: Boolean
)

/**
 * Detects OTP verification codes and URLs in the latest clipboard item cleanly.
 */
object ClipboardSmartParser {
    private val OTP_CONTEXT_REGEX = Regex(
        """(?i)(?:otp|code|pin|passcode|verification|verify|কোড|ওটিপি)\D{0,14}(\d{4,8})\b"""
    )
    private val STANDALONE_OTP_REGEX = Regex("""^\d{4,8}$""")
    private val URL_REGEX = Regex("""(?i)\b((?:https?://|www\.)[^\s<>"]+)""")

    fun extractHint(clipText: String?): ClipboardSmartHint? {
        val trimmed = clipText?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > 512) return null

        val urlMatch = URL_REGEX.find(trimmed)
        if (urlMatch != null) {
            val url = urlMatch.groupValues[1]
            val display = if (url.length > 26) url.take(23) + "…" else url
            return ClipboardSmartHint(
                value = url,
                label = "Link: $display",
                isOtp = false,
                isUrl = true
            )
        }

        if (STANDALONE_OTP_REGEX.matches(trimmed)) {
            return ClipboardSmartHint(
                value = trimmed,
                label = "OTP: $trimmed",
                isOtp = true,
                isUrl = false
            )
        }

        val otpContextMatch = OTP_CONTEXT_REGEX.find(trimmed)
        if (otpContextMatch != null) {
            val code = otpContextMatch.groupValues[1]
            return ClipboardSmartHint(
                value = code,
                label = "OTP: $code",
                isOtp = true,
                isUrl = false
            )
        }

        return null
    }
}

/**
 * Provides smart Bangla spelling corrections, conjunct (যুক্তাক্ষর) completions,
 * and alternate key variants for Bangla and English keys.
 */
object BanglaConjunctHelper {

    private val COMMON_SPELLING_CORRECTIONS: Map<String, List<String>> = mapOf(
        "কিভাবে" to listOf("কীভাবে", "কিভাবে"),
        "কীভাবে" to listOf("কীভাবে", "কিভাবে"),
        "ভালো" to listOf("ভালো", "ভাল"),
        "ভাল" to listOf("ভালো", "ভাল"),
        "হয়েছে" to listOf("হয়েছে"),
        "হয়েছে" to listOf("হয়েছে"),
        "করছে" to listOf("করছে", "করছেন"),
        "বাংলা" to listOf("বাংলা", "বাংলাদেশ"),
        "অনেক" to listOf("অনেক", "অনেকে"),
        "ধন্যবাদ" to listOf("ধন্যবাদ", "ধন্যবাদান্তে"),
        "সুন্দর" to listOf("সুন্দর", "সৌন্দর্য"),
        "বিজ্ঞান" to listOf("বিজ্ঞান", "বৈজ্ঞানিক"),
        "সমস্যা" to listOf("সমস্যা", "সমস্যার"),
        "নিশ্চয়" to listOf("নিশ্চয়", "নিশ্চিত"),
        "সাহায্য" to listOf("সাহায্য", "সহযোগিতা"),
        "পরীক্ষা" to listOf("পরীক্ষা", "পরীক্ষায়"),
        "শিক্ষা" to listOf("শিক্ষা", "শিক্ষক"),
        "স্বাগতম" to listOf("স্বাগতম", "স্বাগত"),
        "অভিনন্দন" to listOf("অভিনন্দন"),
        "শুভেচ্ছা" to listOf("শুভেচ্ছা")
    )

    private val CONJUNCT_MAP: Map<String, List<String>> = mapOf(
        "ক" to listOf("ক্ষ", "ক্ত", "ক্র", "ক্ল", "ক্ষ্ম"),
        "গ" to listOf("জ্ঞ", "গ্র", "গ্ল", "গ্ধ"),
        "চ" to listOf("চ্ছ", "চ্চ", "চ্য"),
        "জ" to listOf("জ্ঞ", "জ্জ", "জ্ব", "জ্র"),
        "ত" to listOf("ত্ত", "ত্র", "ত্ম", "ত্য", "ত্থ"),
        "দ" to listOf("দ্দ", "দ্ধ", "দ্ব", "দ্র", "দ্ভ"),
        "ন" to listOf("ন্ত", "ন্দ", "ন্ধ", "ন্ন", "ন্ম"),
        "প" to listOf("প্ত", "প্র", "প্ল", "প্প"),
        "ব" to listOf("ব্দ", "ব্ধ", "ব্র", "ব্ব"),
        "ম" to listOf("ম্প", "ম্ব", "ম্ভ", "ম্ম", "ম্ল"),
        "শ" to listOf("শ্র", "শ্চ", "শ্ব", "শ্ম"),
        "ষ" to listOf("ষ্ট", "ষ্ঠ", "ষ্প", "ষ্ণ"),
        "স" to listOf("স্ত", "স্থ", "স্প", "স্ব", "স্ক"),
        "হ" to listOf("হ্ম", "হ্ণ", "হ্ল", "হ্ব")
    )

    private val PHONETIC_CONJUNCT_HINTS: Map<String, List<String>> = mapOf(
        "kkh" to listOf("ক্ষ", "ক্ষণ", "ক্ষমা"),
        "gg" to listOf("জ্ঞ", "জ্ঞান", "বিজ্ঞান"),
        "tr" to listOf("ত্র", "মাত্র", "পাত্র"),
        "shr" to listOf("শ্র", "শ্রম", "শ্রেষ্ঠ"),
        "nd" to listOf("ন্দ", "সুন্দর", "আনন্দ"),
        "nt" to listOf("ন্ত", "শান্ত", "কিন্তু"),
        "st" to listOf("স্ত", "ব্যস্ত", "সস্তা"),
        "sth" to listOf("স্থ", "অবস্থা", "সুস্থ"),
        "shw" to listOf("শ্ব", "বিশ্ব", "ঈশ্বর"),
        "kt" to listOf("ক্ত", "রক্ত", "শক্ত"),
        "mp" to listOf("ম্প", "সম্পদ", "কম্পন"),
        "hm" to listOf("হ্ম", "ব্রাহ্মণ")
    )

    private val ALTERNATE_CHAR_MAP: Map<String, List<String>> = mapOf(
        "a" to listOf("@", "á", "à", "â", "ä", "æ", "ã", "å"),
        "e" to listOf("3", "é", "è", "ê", "ë", "€"),
        "i" to listOf("8", "í", "ì", "î", "ï"),
        "o" to listOf("9", "ó", "ò", "ô", "ö", "õ", "ø"),
        "u" to listOf("7", "ú", "ù", "û", "ü"),
        "s" to listOf("#", "ß", "ś", "š", "$"),
        "c" to listOf("'", "ç", "ć", "č", "©"),
        "n" to listOf("!", "ñ", "ń"),
        "y" to listOf("6", "ý", "ÿ", "¥"),
        "d" to listOf("৳", "$", "ð"),
        "q" to listOf("1", "!", "¹"),
        "w" to listOf("2", "@", "²"),
        "r" to listOf("4", "®"),
        "t" to listOf("5", "™"),
        "p" to listOf("0", "°"),
        "." to listOf(",", "?", "!", "…", ":", ";", "।"),
        "।" to listOf(".", "?", "!", ",", "…", ";"),
        "ক" to listOf("খ", "ক্ষ", "ক্ত", "ক্র"),
        "জ" to listOf("ঝ", "জ্ঞ", "জ্জ", "জ্ব"),
        "ত" to listOf("থ", "ৎ", "ত্র", "ত্ত"),
        "দ" to listOf("ধ", "দ্ধ", "দ্ব", "দ্র"),
        "ন" to listOf("ণ", "ন্ত", "ন্দ", "ন্ধ"),
        "শ" to listOf("ষ", "স", "শ্র", "শ্চ"),
        "স" to listOf("শ", "ষ", "স্ত", "স্থ")
    )

    fun getAlternateChars(primaryKey: String, secondaryHint: String = ""): List<String> {
        val lower = primaryKey.lowercase(Locale.US)
        val baseList = ALTERNATE_CHAR_MAP[lower] ?: ALTERNATE_CHAR_MAP[primaryKey]
        val result = LinkedHashSet<String>()
        if (secondaryHint.isNotBlank()) result.add(secondaryHint)
        baseList?.forEach { result.add(it) }
        return result.toList()
    }

    fun getEnhancements(rawInput: String, transliterated: String): List<String> {
        val out = LinkedHashSet<String>()
        val cleanRaw = rawInput.trim().lowercase(Locale.US)
        val cleanBn = transliterated.trim()

        COMMON_SPELLING_CORRECTIONS[cleanBn]?.forEach { out.add(it) }

        if (cleanRaw.isNotEmpty()) {
            PHONETIC_CONJUNCT_HINTS.entries
                .filter { (key, _) -> cleanRaw.endsWith(key) || cleanRaw == key }
                .forEach { (_, list) -> out.addAll(list) }
        }

        if (cleanBn.endsWith("্") && cleanBn.length >= 2) {
            val baseChar = cleanBn[cleanBn.length - 2].toString()
            val prefix = cleanBn.dropLast(2)
            CONJUNCT_MAP[baseChar]?.forEach { conjunct ->
                out.add(prefix + conjunct)
            }
        } else if (cleanBn.length == 1 && CONJUNCT_MAP.containsKey(cleanBn)) {
            CONJUNCT_MAP[cleanBn]?.forEach { out.add(it) }
        }

        return out.toList().take(4)
    }
}

/**
 * Matches a glide path of visited QWERTY keys against English or Phonetic Bangla dictionary words.
 */
object GestureTypingEngine {
    fun matchPathToWords(
        visitedKeys: List<String>,
        mode: InputMode,
        maxResults: Int = 5
    ): List<String> {
        if (visitedKeys.size < 2) return emptyList()
        val deduped = buildString {
            for (k in visitedKeys) {
                val c = k.lowercase(Locale.US).firstOrNull() ?: continue
                if (c in 'a'..'z' && (isEmpty() || last() != c)) {
                    append(c)
                }
            }
        }
        if (deduped.length < 2) return emptyList()

        val firstChar = deduped.first()
        val lastChar = deduped.last()

        if (mode == InputMode.ENGLISH) {
            val pool = EnglishDictionary.findWordSuggestions(firstChar.toString(), maxResults = 80)
            val ranked = pool.mapNotNull { candidate ->
                val lower = candidate.lowercase(Locale.US)
                if (lower.length < 2 || lower.first() != firstChar) return@mapNotNull null
                val score = scoreSubsequence(deduped, lower, lastChar)
                if (score > 0) candidate to score else null
            }.sortedByDescending { it.second }
                .map { it.first }
                .distinct()
                .take(maxResults)

            return if (ranked.isNotEmpty()) ranked else listOf(deduped)
        }

        if (mode == InputMode.BANGLA_PHONETIC) {
            val phoneticEngine = BanglaPhoneticEngine()
            val phoneticWord = phoneticEngine.transliterate(deduped)
            val dictMatches = BanglaDictionary.findWordSuggestions(deduped.take(3), maxResults = 6)
            return (listOf(phoneticWord) + dictMatches)
                .filter { it.isNotBlank() }
                .distinct()
                .take(maxResults)
        }

        return emptyList()
    }

    private fun scoreSubsequence(path: String, word: String, lastChar: Char): Int {
        var wi = 0
        for (p in path) {
            if (wi < word.length && word[wi] == p) {
                wi++
            }
        }
        if (wi < 2) return 0
        var score = wi * 12
        if (word.last() == lastChar) score += 18
        if (path == word) score += 50
        score -= kotlin.math.abs(word.length - path.length) * 3
        return score
    }
}

data class DateTimeOption(
    val id: String,
    val label: String,
    val value: String
)

object DateTimeFormatHelper {
    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: String): String = buildString(input.length) {
        for (c in input) {
            if (c in '0'..'9') append(BN_DIGITS[c - '0']) else append(c)
        }
    }

    fun getQuickOptions(nowMs: Long = System.currentTimeMillis()): List<DateTimeOption> {
        val date = Date(nowMs)
        val enLocale = Locale.US
        val bnMonths = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val day = SimpleDateFormat("d", enLocale).format(date)
        val monthIdx = SimpleDateFormat("M", enLocale).format(date).toIntOrNull()?.minus(1)?.coerceIn(0, 11) ?: 0
        val year = SimpleDateFormat("yyyy", enLocale).format(date)
        val bnFormattedDate = "${toBanglaDigits(day)} ${bnMonths[monthIdx]}, ${toBanglaDigits(year)}"

        val timeEn = SimpleDateFormat("hh:mm a", enLocale).format(date)
        val dateNumeric = SimpleDateFormat("dd/MM/yyyy", enLocale).format(date)
        val dateReadableEn = SimpleDateFormat("MMM d, yyyy", enLocale).format(date)
        val isoDate = SimpleDateFormat("yyyy-MM-dd", enLocale).format(date)

        return listOf(
            DateTimeOption("bn_date", "বাংলা তারিখ", bnFormattedDate),
            DateTimeOption("en_date", "Readable Date", dateReadableEn),
            DateTimeOption("time_12h", "Current Time", timeEn),
            DateTimeOption("slash_date", "DD/MM/YYYY", dateNumeric),
            DateTimeOption("iso_date", "ISO Date", isoDate)
        )
    }
}

enum class TranslationDirection(val label: String, val sourceCode: String, val targetCode: String) {
    BN_TO_EN("বাংলা → English", "bn", "en"),
    EN_TO_BN("English → বাংলা", "en", "bn")
}

data class TranslationResult(
    val translatedText: String,
    val isOfflineFallback: Boolean,
    val note: String
)

/**
 * Offline-first Bangla <-> English translator with optional user-configured endpoint.
 * Never uploads typed content unless the user explicitly taps Translate with an optional endpoint.
 */
class TranslationController(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    suspend fun translate(
        query: String,
        direction: TranslationDirection,
        customEndpointUrl: String = ""
    ): TranslationResult {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return TranslationResult("", isOfflineFallback = true, note = "অনুবাদ করতে শব্দ বা বাক্য লিখুন")
        }

        // Optional external endpoint if configured by user (never hardcoded)
        val endpoint = customEndpointUrl.trim()
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            val online = runCatching {
                withContext(Dispatchers.IO) {
                    val payload = JSONObject().apply {
                        put("q", clean)
                        put("source", direction.sourceCode)
                        put("target", direction.targetCode)
                        put("format", "text")
                    }.toString()
                    val req = Request.Builder()
                        .url(endpoint)
                        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        val body = resp.body?.string().orEmpty()
                        JSONObject(body).optString("translatedText").takeIf { it.isNotBlank() }
                    }
                }
            }.getOrNull()

            if (!online.isNullOrBlank()) {
                return TranslationResult(online, isOfflineFallback = false, note = "Online translation")
            }
        }

        val offline = translateOffline(clean, direction)
        return if (offline != null) {
            TranslationResult(offline, isOfflineFallback = true, note = "অফলাইন অভিধান অনুবাদ")
        } else {
            TranslationResult(
                translatedText = "",
                isOfflineFallback = true,
                note = "অফলাইন শব্দভান্ডারে সরাসরি মেলেনি — সাধারণ শব্দ বা বাক্য চেষ্টা করুন"
            )
        }
    }

    fun translateOffline(query: String, direction: TranslationDirection): String? {
        val clean = query.trim()
        if (clean.isEmpty()) return null
        val lower = clean.lowercase(Locale.US)

        return when (direction) {
            TranslationDirection.EN_TO_BN -> {
                EN_TO_BN_PHRASES[lower] ?: run {
                    val tokens = lower.split(Regex("\\s+"))
                    val mapped = tokens.map { tok ->
                        val stripped = tok.trim(',', '.', '!', '?')
                        EN_TO_BN_WORDS[stripped] ?: return@run null
                    }
                    mapped.joinToString(" ")
                }
            }
            TranslationDirection.BN_TO_EN -> {
                BN_TO_EN_PHRASES[clean] ?: run {
                    val tokens = clean.split(Regex("\\s+"))
                    val mapped = tokens.map { tok ->
                        val stripped = tok.trim('।', ',', '!', '?')
                        BN_TO_EN_WORDS[stripped] ?: return@run null
                    }
                    mapped.joinToString(" ")
                }
            }
        }
    }

    companion object {
        private val EN_TO_BN_PHRASES = mapOf(
            "good morning" to "শুভ সকাল",
            "good afternoon" to "শুভ অপরাহ্ন",
            "good evening" to "শুভ সন্ধ্যা",
            "good night" to "শুভ রাত্রি",
            "how are you" to "আপনি কেমন আছেন?",
            "i am fine" to "আমি ভালো আছি",
            "thank you" to "ধন্যবাদ",
            "thank you very much" to "অনেক ধন্যবাদ",
            "please" to "দয়া করে",
            "sorry" to "দুঃখিত",
            "excuse me" to "মাফ করবেন",
            "see you soon" to "শীঘ্রই দেখা হবে",
            "what is your name" to "আপনার নাম কী?",
            "where are you" to "আপনি কোথায়?",
            "i love bangladesh" to "আমি বাংলাদেশকে ভালোবাসি",
            "on my way" to "আমি আসার পথে আছি",
            "happy birthday" to "শুভ জন্মদিন",
            "congratulations" to "অভিনন্দন",
            "welcome" to "স্বাগতম",
            "no problem" to "কোনো সমস্যা নেই"
        )

        private val BN_TO_EN_PHRASES = EN_TO_BN_PHRASES.entries.associate { (en, bn) ->
            bn.trimEnd('?', '।') to en.replaceFirstChar { it.uppercase() }
        } + mapOf(
            "আপনি কেমন আছেন?" to "How are you?",
            "আপনি কেমন আছেন" to "How are you?",
            "আমি ভালো আছি" to "I am fine",
            "কেমন আছেন" to "How are you?",
            "শুভ সকাল" to "Good morning",
            "শুভ রাত্রি" to "Good night",
            "ধন্যবাদ" to "Thank you",
            "অনেক ধন্যবাদ" to "Thank you very much",
            "আমি আসছি" to "I am coming",
            "কোনো সমস্যা নেই" to "No problem"
        )

        private val EN_TO_BN_WORDS = mapOf(
            "hello" to "হ্যালো", "hi" to "হাই", "yes" to "হ্যাঁ", "no" to "না",
            "ok" to "ঠিক আছে", "okay" to "ঠিক আছে", "good" to "ভালো", "fine" to "ভালো",
            "bad" to "খারাপ", "love" to "ভালোবাসা", "friend" to "বন্ধু", "family" to "পরিবার",
            "home" to "বাড়ি", "house" to "বাড়ি", "school" to "বিদ্যালয়", "work" to "কাজ",
            "time" to "সময়", "today" to "আজ", "tomorrow" to "আগামীকাল", "yesterday" to "গতকাল",
            "morning" to "সকাল", "night" to "রাত", "water" to "পানি", "food" to "খাবার",
            "book" to "বই", "phone" to "ফোন", "message" to "বার্তা", "bangla" to "বাংলা",
            "english" to "ইংরেজি", "bangladesh" to "বাংলাদেশ", "i" to "আমি", "you" to "তুমি",
            "we" to "আমরা", "they" to "তারা", "here" to "এখানে", "there" to "সেখানে",
            "come" to "আসো", "go" to "যাও", "eat" to "খাওয়া", "help" to "সাহায্য",
            "happy" to "খুশি", "sad" to "দুঃখিত", "beautiful" to "সুন্দর", "thanks" to "ধন্যবাদ"
        )

        private val BN_TO_EN_WORDS = EN_TO_BN_WORDS.entries.associate { (en, bn) -> bn to en }
    }
}
