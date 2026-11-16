package com.example.okkhor.engine

object BanglaDictionary {

    // Contextual Next-Word Predictions (Bigram predictions for everyday Bengali typing)
    val NEXT_WORD_PREDICTIONS: Map<String, List<String>> = mapOf(
        "আমি" to listOf("ভালো", "আছি", "তোমাকে", "যাব", "করছি", "বলছি", "জানি", "চাই", "মনে"),
        "তুমি" to listOf("কেমন", "আছো", "কোথায়", "কি", "যাবে", "করছো", "বলবে", "জানবে"),
        "আপনি" to listOf("কেমন", "আছেন", "কোথায়", "কি", "বলুন", "আসুন", "ধন্যবাদ"),
        "আমরা" to listOf("সবাই", "ভালো", "যাব", "করছি", "একসাথে", "চাই", "বাংলাদেশী"),
        "তোমরা" to listOf("সবাই", "কেমন", "আছো", "কোথায়", "যাবে"),
        "তারা" to listOf("সবাই", "কোথায়", "গেছে", "আসবে", "করছে"),
        "শুভ" to listOf("সকাল", "রাত্রি", "কামনা", "নববর্ষ", "সন্ধ্যা", "বিকেল", "জন্মদিন"),
        "অনেক" to listOf("ধন্যবাদ", "ভালো", "সুন্দর", "দিন", "কথা", "কষ্ট", "ভালোবাসা"),
        "ধন্যবাদ" to listOf("আপনাকে", "তোমাকে", "অনেক", "ভাই", "প্রিয়", "সবাইকে"),
        "কেমন" to listOf("আছেন", "আছো", "আছিস", "হলো", "লাগছে"),
        "কি" to listOf("খবর", "করছো", "হয়েছে", "অবস্থা", "ব্যাপার", "বলছেন"),
        "আমার" to listOf("সোনার", "দেশ", "বন্ধু", "নাম", "বাড়ি", "মা", "বাবা", "মন"),
        "তোমার" to listOf("নাম", "বাড়ি", "খবর", "কথা", "জন্য", "সাথে"),
        "আপনার" to listOf("নাম", "দয়া", "জন্য", "সাথে", "কথা"),
        "বাংলাদেশ" to listOf("আমার", "সুন্দর", "চিরজীবী", "একটি", "দেশ"),
        "সোনার" to listOf("বাংলা", "মেডেল", "খাঁচা", "হরিণ"),
        "ভালো" to listOf("আছি", "থাকবেন", "থেকো", "লাগে", "হয়েছে", "মানুষ"),
        "ভাল" to listOf("আছি", "থেকো", "থাকবেন"),
        "কাজ" to listOf("করছি", "শেষ", "হয়েছে", "করব", "আছে"),
        "দেখা" to listOf("হবে", "করব", "হয়েছে", "হলে"),
        "ইনশাল্লাহ" to listOf("ভালো", "হবে", "দেখা", "যাব"),
        "সবাইকে" to listOf("ধন্যবাদ", "স্বাগতম", "শুভ", "সালাম"),
        "সালাম" to listOf("নিবেন", "জানাই", "আলাইকুম"),
        "আজকে" to listOf("অনেক", "খুব", "যাব", "ছুটি", "বৃষ্টি"),
        "কালকে" to listOf("দেখা", "হবে", "যাব", "কথা"),
        "খুব" to listOf("ভালো", "সুন্দর", "সুন্দর", "কষ্ট", "খুশি")
    )

    // Extensive phonetic dictionary with multiple common spellings
    val COMMON_WORD_MAP: Map<String, List<String>> = mapOf(
        "ami" to listOf("আমি"),
        "amra" to listOf("আমরা"),
        "amar" to listOf("আমার"),
        "amader" to listOf("আমাদের"),
        "amake" to listOf("আমাকে"),
        "amaderke" to listOf("আমাদেরকে"),
        "tumi" to listOf("তুমি"),
        "tomra" to listOf("তোমরা"),
        "tomar" to listOf("তোমার"),
        "tomader" to listOf("তোমাদের"),
        "tomake" to listOf("তোমাকে"),
        "tui" to listOf("তুই"),
        "tor" to listOf("তোর"),
        "toke" to listOf("তোকে"),
        "apni" to listOf("আপনি"),
        "apnar" to listOf("আপনার"),
        "apnader" to listOf("আপনাদের"),
        "apnake" to listOf("আপনাকে"),
        "she" to listOf("সে"),
        "tini" to listOf("তিনি"),
        "tar" to listOf("তার"),
        "tahar" to listOf("তাহার"),
        "tader" to listOf("তাদের"),
        "take" to listOf("তাকে"),
        "tahake" to listOf("তাহাকে"),

        // English loanwords & chat phrases frequently typed in Bengali
        "ok" to listOf("ওকে", "OK", "ঠিক আছে"),
        "okay" to listOf("ওকে", "ঠিক আছে"),
        "oke" to listOf("ওকে", "ঠিক আছে"),
        "thikache" to listOf("ঠিক আছে"),
        "accha" to listOf("আচ্ছা", "ঠিক আছে"),
        "acha" to listOf("আচ্ছা"),
        "hi" to listOf("হাই", "Hi"),
        "hello" to listOf("হ্যালো", "Hello"),
        "bye" to listOf("বাই", "Bye"),
        "yes" to listOf("হ্যাঁ", "Yes"),
        "no" to listOf("না", "No"),

        // Greetings & Well wishes
        "shubho" to listOf("শুভ"),
        "shuvo" to listOf("শুভ"),
        "subho" to listOf("শুভ"),
        "shokal" to listOf("সকাল"),
        "sokal" to listOf("সকাল"),
        "dupur" to listOf("দুপুর"),
        "bikel" to listOf("বিকেল"),
        "shondha" to listOf("সন্ধ্যা"),
        "shondhya" to listOf("সন্ধ্যা"),
        "ratri" to listOf("রাত্রি", "রাত"),
        "raat" to listOf("রাত"),
        "dhonnobad" to listOf("ধন্যবাদ"),
        "dhonyobad" to listOf("ধন্যবাদ"),
        "dhonnobaad" to listOf("ধন্যবাদ"),
        "shagotom" to listOf("স্বাগতম"),
        "swagotom" to listOf("স্বাগতম"),
        "biday" to listOf("বিদায়"),
        "abhaar" to listOf("আভার"),
        "salam" to listOf("সালাম"),
        "assalamu" to listOf("আসসালামু"),
        "alaikum" to listOf("আলাইকুম"),
        "nomoshkar" to listOf("নমস্কার"),
        "nomoskar" to listOf("নমস্কার"),
        "kemon" to listOf("কেমন"),
        "kmn" to listOf("কেমন"),
        "acho" to listOf("আছো"),
        "achho" to listOf("আছো"),
        "achen" to listOf("আছেন"),
        "achhen" to listOf("আছেন"),
        "achi" to listOf("আছি"),
        "achhi" to listOf("আছি"),
        "achis" to listOf("আছিস"),

        // Emotional & Evaluation
        "bhalo" to listOf("ভালো", "ভাল"),
        "valo" to listOf("ভালো", "ভাল"),
        "bhalobasha" to listOf("ভালোবাসা"),
        "valobasha" to listOf("ভালোবাসা"),
        "bhalobashi" to listOf("ভালোবাসি"),
        "valobashi" to listOf("ভালোবাসি"),
        "shundor" to listOf("সুন্দর"),
        "sundor" to listOf("সুন্দর"),
        "khushi" to listOf("খুশি"),
        "anondo" to listOf("আনন্দ"),
        "dukkho" to listOf("দুঃখ"),
        "dukho" to listOf("দুঃখ"),
        "kosto" to listOf("কষ্ট"),
        "baje" to listOf("বাজে"),
        "kharap" to listOf("খারাপ"),
        "shanti" to listOf("শান্তি"),
        "shantite" to listOf("শান্তিতে"),
        "thik" to listOf("ঠিক"),
        "bhul" to listOf("ভুল"),
        "vul" to listOf("ভুল"),
        "shotti" to listOf("সত্যি"),
        "sotti" to listOf("সত্যি"),
        "shohoj" to listOf("সহজ"),
        "kothin" to listOf("কঠিন"),

        // Bangladesh & Culture
        "bangla" to listOf("বাংলা"),
        "bangladesh" to listOf("বাংলাদেশ"),
        "bangladeshi" to listOf("বাংলাদেশি", "বাংলাদেশী"),
        "bangali" to listOf("বাঙালি", "বাঙালী"),
        "shonar" to listOf("সোনার"),
        "sonar" to listOf("সোনার"),
        "desh" to listOf("দেশ"),
        "desher" to listOf("দেশের"),
        "bhasha" to listOf("ভাষা"),
        "bhasa" to listOf("ভাষা"),
        "shadhin" to listOf("স্বাধীন"),
        "shadhinota" to listOf("স্বাধীনতা"),
        "muktijuddho" to listOf("মুক্তিযুদ্ধ"),
        "mukti" to listOf("মুক্তি"),
        "bijoy" to listOf("বিজয়"),
        "probhat" to listOf("প্রভাত"),
        "okkhor" to listOf("অক্ষর"),
        "okkhar" to listOf("অক্ষর"),
        "kibord" to listOf("কিবোর্ড"),
        "keyboard" to listOf("কিবোর্ড"),
        "shongskriti" to listOf("সংস্কৃতি"),
        "sahitya" to listOf("সাহিত্য"),
        "kabita" to listOf("কবিতা"),
        "gaan" to listOf("গান"),
        "dhaka" to listOf("ঢাকা"),
        "chittagong" to listOf("চট্টগ্রাম"),
        "ctg" to listOf("চট্টগ্রাম"),
        "sylhet" to listOf("সিলেট"),
        "khulna" to listOf("খুলনা"),
        "rajshahi" to listOf("রাজশাহী"),
        "barisal" to listOf("বরিশাল"),
        "rangpur" to listOf("রংপুর"),
        "mymensingh" to listOf("ময়মনসিংহ"),
        "cumilla" to listOf("কুমিল্লা"),

        // Questions & Conjunctions
        "ki" to listOf("কি", "কী"),
        "ke" to listOf("কে"),
        "kake" to listOf("কাকে"),
        "kar" to listOf("কার"),
        "keno" to listOf("কেন"),
        "kothay" to listOf("কোথায়"),
        "kokhon" to listOf("কখন"),
        "kivabe" to listOf("কিভাবে"),
        "koto" to listOf("কত"),
        "kotogulo" to listOf("কতগুলো"),
        "kintu" to listOf("কিন্তু"),
        "ebong" to listOf("এবং"),
        "ar" to listOf("আর"),
        "ba" to listOf("বা"),
        "othoba" to listOf("অথবা"),
        "jodi" to listOf("যদি"),
        "tobe" to listOf("তবে"),
        "tahole" to listOf("তাহলে"),
        "karon" to listOf("কারণ"),
        "tai" to listOf("তাই"),
        "jemon" to listOf("যেমন"),
        "temon" to listOf("তেমন"),
        "oboshyoi" to listOf("অবশ্যই"),
        "hoyto" to listOf("হয়তো"),
        "shob" to listOf("সব"),
        "sob" to listOf("সব"),
        "shobai" to listOf("সবাই"),
        "sobai" to listOf("সবাই"),
        "onek" to listOf("অনেক"),
        "ektu" to listOf("একটু"),
        "kichu" to listOf("কিছু"),
        "shathe" to listOf("সাথে"),
        "sathe" to listOf("সাথে"),
        "shonge" to listOf("সঙ্গে"),
        "songe" to listOf("সঙ্গে"),
        "ekshathe" to listOf("একসাথে"),

        // Family & People
        "manush" to listOf("মানুষ"),
        "bondhu" to listOf("বন্ধু"),
        "poribar" to listOf("পরিবার"),
        "ma" to listOf("মা"),
        "ammu" to listOf("আম্মু"),
        "baba" to listOf("বাবা"),
        "abbu" to listOf("আব্বু"),
        "bhai" to listOf("ভাই"),
        "vai" to listOf("ভাই"),
        "bon" to listOf("বোন"),
        "chele" to listOf("ছেলে"),
        "meye" to listOf("মেয়ে"),
        "shontan" to listOf("সন্তান"),

        // Actions & Verbs
        "korchi" to listOf("করছি"),
        "korbo" to listOf("করব", "করবো"),
        "koro" to listOf("করো"),
        "koren" to listOf("করেন"),
        "koris" to listOf("করিস"),
        "dekhi" to listOf("দেখি"),
        "dekho" to listOf("দেখো"),
        "dekhte" to listOf("দেখতে"),
        "dekha" to listOf("দেখা"),
        "bolchi" to listOf("বলছি"),
        "bolo" to listOf("বলো"),
        "bolun" to listOf("বলুন"),
        "kotha" to listOf("কথা"),
        "jani" to listOf("জানি"),
        "jano" to listOf("জানো"),
        "janen" to listOf("জানেন"),
        "jante" to listOf("জানতে"),
        "shuni" to listOf("শুনি"),
        "shuno" to listOf("শুনো"),
        "shunche" to listOf("শুনছে"),
        "shikhi" to listOf("শিখি"),
        "shikhbo" to listOf("শিখব"),
        "likhchi" to listOf("লিখছি"),
        "likhbo" to listOf("লিখব"),
        "porashona" to listOf("পড়াশোনা"),
        "pori" to listOf("পড়ি"),
        "poro" to listOf("পড়ো"),
        "khai" to listOf("খাই"),
        "khao" to listOf("খাও"),
        "khaben" to listOf("খাবেন"),
        "ashun" to listOf("আসুন"),
        "asho" to listOf("আসো"),
        "ashbo" to listOf("আসব", "আসবো"),
        "jachhi" to listOf("যাচ্ছি"),
        "jabo" to listOf("যাব", "যাবো"),
        "jao" to listOf("যাও"),
        "jan" to listOf("যান"),
        "thako" to listOf("থাকো"),
        "thakun" to listOf("থাকুন"),
        "thakbo" to listOf("থাকব"),
        "hobe" to listOf("হবে"),
        "hoiche" to listOf("হয়েছে"),
        "hoyeche" to listOf("হয়েছে"),

        // Objects & Places
        "bari" to listOf("বাড়ি"),
        "ghor" to listOf("ঘর"),
        "basha" to listOf("বাসা"),
        "shohor" to listOf("শহর"),
        "gram" to listOf("গ্রাম"),
        "rasta" to listOf("রাস্তা"),
        "gari" to listOf("গাড়ি"),
        "boi" to listOf("বই"),
        "khata" to listOf("খাতা"),
        "kolom" to listOf("কলম"),
        "phone" to listOf("ফোন"),
        "mobile" to listOf("মোবাইল"),
        "chobi" to listOf("ছবি"),
        "khabar" to listOf("খাবার"),
        "pani" to listOf("পানি"),
        "cha" to listOf("চা"),
        "bhat" to listOf("ভাত"),
        "mach" to listOf("মাছ"),
        "akash" to listOf("আকাশ"),
        "batas" to listOf("বাতাস"),
        "brishti" to listOf("বৃষ্টি"),
        "shurjo" to listOf("সূর্য"),
        "chand" to listOf("চাঁদ"),
        "tara" to listOf("তারা"),

        // Time & Everyday Conversational Words
        "shomoy" to listOf("সময়"),
        "somoy" to listOf("সময়"),
        "din" to listOf("দিন"),
        "bochor" to listOf("বছর"),
        "aj" to listOf("আজ"),
        "ajke" to listOf("আজকে"),
        "kal" to listOf("কাল"),
        "kalke" to listOf("কালকে"),
        "ekhon" to listOf("এখন"),
        "ekhonoi" to listOf("এখনই"),
        "tokhon" to listOf("তখন"),
        "shobshomoy" to listOf("সবসময়"),
        "khobor" to listOf("খবর"),
        "shobaike" to listOf("সবাইকে"),
        "sobaike" to listOf("সবাইকে"),
        "banglay" to listOf("বাংলায়"),
        "shesh" to listOf("শেষ"),
        "sesh" to listOf("শেষ"),
        "fire" to listOf("ফিরে"),
        "phire" to listOf("ফিরে"),
        "egie" to listOf("এগিয়ে"),
        "egiye" to listOf("এগিয়ে"),
        "jachhe" to listOf("যাচ্ছে"),
        "jacche" to listOf("যাচ্ছে"),
        "biggyan" to listOf("বিজ্ঞান"),
        "biggan" to listOf("বিজ্ঞান"),
        "deshe" to listOf("দেশে"),
        "protidin" to listOf("প্রতিদিন"),
        "shomoshsha" to listOf("সমস্যা"),
        "somossa" to listOf("সমস্যা"),
        "dorkar" to listOf("দরকার"),
        "lagbe" to listOf("লাগবে"),
        "parbo" to listOf("পারব", "পারবো"),
        "pare" to listOf("পারে"),
        "korche" to listOf("করছে"),
        "bolche" to listOf("বলছে")
    )

    private val HIGH_FREQUENCY_ROMAN_ORDER: Map<String, Int> = listOf(
        "ami", "amar", "amra", "amader", "tumi", "tomar", "apni", "apnar",
        "bangla", "bangladesh", "banglay", "baba", "bari", "basha", "bhalo", "valo",
        "kemon", "ki", "kothay", "keno", "khobor", "khub", "kaj", "kotha", "korchi",
        "shob", "shobai", "shobaike", "shubho", "shokal", "shesh", "sathe", "shathe",
        "dhonnobad", "dekha", "din", "ajke", "ekhon", "onek", "achhi", "achi", "acho", "achen"
    ).withIndex().associate { it.value to it.index }

    private val HIGH_FREQUENCY_BANGLA_ORDER: Map<String, Int> = listOf(
        "আমি", "আমার", "আমরা", "তুমি", "তোমার", "আপনি", "আপনার", "বাংলা", "বাংলাদেশ",
        "বাংলায়", "বাবা", "বাড়ি", "বাসা", "ভালো", "কেমন", "কি", "খবর", "খুব", "কাজ",
        "সব", "সবাই", "সবাইকে", "শুভ", "সকাল", "শেষ", "ধন্যবাদ", "দেখা", "আজকে", "এখন", "অনেক", "আছি"
    ).withIndex().associate { it.value to it.index }

    // Prefix list for autocomplete and next-word suggestions
    val ALL_WORDS: List<String> = COMMON_WORD_MAP.values.flatten().distinct()

    private val ENTRIES_BY_FIRST_CHAR: Map<Char, List<Pair<String, List<String>>>> =
        COMMON_WORD_MAP.entries
            .groupBy { it.key.first() }
            .mapValues { (_, list) ->
                list.map { it.key to it.value }
                    .sortedWith(
                        compareBy<Pair<String, List<String>>>(
                            { HIGH_FREQUENCY_ROMAN_ORDER[it.first] ?: Int.MAX_VALUE },
                            { it.first.length }
                        )
                    )
            }

    private val BANGLA_WORDS_BY_FIRST_CHAR: Map<Char, List<String>> =
        ALL_WORDS
            .filter { it.isNotEmpty() }
            .groupBy { it.first() }
            .mapValues { (_, words) ->
                words.sortedWith(
                    compareBy<String>(
                        { HIGH_FREQUENCY_BANGLA_ORDER[it] ?: Int.MAX_VALUE },
                        { it.length }
                    )
                )
            }

    fun findWordSuggestions(query: String, maxResults: Int = 5): List<String> {
        if (query.isBlank() || maxResults <= 0) return emptyList()
        val trimmed = query.trim()
        val results = LinkedHashSet<String>(maxResults * 2)

        if (BanglaUnicodeHelper.isBanglaText(trimmed)) {
            val firstBn = trimmed.firstOrNull() ?: return emptyList()
            val bucket = BANGLA_WORDS_BY_FIRST_CHAR[firstBn] ?: return emptyList()
            for (word in bucket) {
                if (word != trimmed && word.startsWith(trimmed)) {
                    results.add(word)
                    if (results.size >= maxResults) break
                }
            }
            return results.toList()
        }

        val queryLower = trimmed.lowercase()

        // 1. Exact match in transliteration map (O(1))
        COMMON_WORD_MAP[queryLower]?.forEach { word ->
            results.add(word)
            if (results.size >= maxResults) return results.toList()
        }

        // 2. Prefix match in indexed bucket sorted by shortest Roman key
        val bucket = ENTRIES_BY_FIRST_CHAR[queryLower[0]]
        if (bucket != null) {
            for ((key, words) in bucket) {
                if (key.length > queryLower.length && key.startsWith(queryLower)) {
                    for (word in words) {
                        results.add(word)
                        if (results.size >= maxResults) return results.toList()
                    }
                }
            }
        }

        return results.toList()
    }

    fun getNextWordPredictions(lastWord: String, maxResults: Int = 4): List<String> {
        val clean = lastWord.trim()
        val predictions = NEXT_WORD_PREDICTIONS[clean] ?: return emptyList()
        return if (predictions.size <= maxResults) predictions else predictions.take(maxResults)
    }
}
