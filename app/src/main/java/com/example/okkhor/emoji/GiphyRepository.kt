package com.example.okkhor.emoji

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * GIPHY API v1 repository for trending GIFs, keyword search, categories, autocomplete tags,
 * and local disk caching for FileProvider commitContent insertion.
 *
 * Security & privacy:
 * - Reads GIPHY_API_KEY via BuildConfig (injected from .env / AI Studio Secrets panel).
 * - Never sends typed keyboard text; only explicit GIF search queries or category selections.
 */
class GiphyRepository(
    private val context: Context,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val apiKeyOverride: String? = null,
    private val networkCheckOverride: (() -> Boolean)? = null,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    fun getApiKey(): String = (apiKeyOverride ?: BuildConfig.GIPHY_API_KEY).trim()

    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() &&
            key != "MY_GIPHY_API_KEY" &&
            !key.startsWith("YOUR_")
    }

    fun isNetworkAvailable(): Boolean {
        networkCheckOverride?.let { return it() }
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    suspend fun fetchTrending(offset: Int = 0, limit: Int = PAGE_SIZE): GifSearchUiState =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext GifSearchUiState.Unconfigured
            if (!isNetworkAvailable()) return@withContext GifSearchUiState.Offline

            runCatching {
                val url = "${baseUrl.trimEnd('/')}/v1/gifs/trending".toHttpUrl().newBuilder()
                    .addQueryParameter("api_key", getApiKey())
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("offset", offset.toString())
                    .addQueryParameter("rating", "pg-13")
                    .build()
                executeGifListRequest(url.toString())
            }.getOrElse { err ->
                GifSearchUiState.Error(err.message ?: "Unable to load trending GIFs")
            }
        }

    suspend fun searchGifs(query: String, offset: Int = 0, limit: Int = PAGE_SIZE): GifSearchUiState =
        withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return@withContext fetchTrending(offset, limit)
            if (!isConfigured()) return@withContext GifSearchUiState.Unconfigured
            if (!isNetworkAvailable()) return@withContext GifSearchUiState.Offline

            runCatching {
                val url = "${baseUrl.trimEnd('/')}/v1/gifs/search".toHttpUrl().newBuilder()
                    .addQueryParameter("api_key", getApiKey())
                    .addQueryParameter("q", cleanQuery)
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("offset", offset.toString())
                    .addQueryParameter("rating", "pg-13")
                    .addQueryParameter("lang", "en")
                    .build()
                executeGifListRequest(url.toString())
            }.getOrElse { err ->
                GifSearchUiState.Error(err.message ?: "Unable to search GIFs")
            }
        }

    suspend fun fetchCategories(): List<GifCategoryItem> = withContext(Dispatchers.IO) {
        if (!isConfigured() || !isNetworkAvailable()) return@withContext DEFAULT_CATEGORIES
        runCatching {
            val url = "${baseUrl.trimEnd('/')}/v1/gifs/categories".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", getApiKey())
                .build()
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching DEFAULT_CATEGORIES
                val body = resp.body?.string().orEmpty()
                val data = JSONObject(body).optJSONArray("data") ?: return@runCatching DEFAULT_CATEGORIES
                buildList {
                    for (i in 0 until minOf(data.length(), 16)) {
                        val obj = data.getJSONObject(i)
                        val name = obj.optString("name").trim()
                        if (name.isNotEmpty()) {
                            add(GifCategoryItem(name = name, searchQuery = name))
                        }
                    }
                }.ifEmpty { DEFAULT_CATEGORIES }
            }
        }.getOrDefault(DEFAULT_CATEGORIES)
    }

    suspend fun fetchSearchSuggestions(prefix: String): List<String> = withContext(Dispatchers.IO) {
        val clean = prefix.trim()
        if (clean.length < 2 || !isConfigured() || !isNetworkAvailable()) return@withContext emptyList()
        runCatching {
            val url = "${baseUrl.trimEnd('/')}/v1/gifs/search/tags".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", getApiKey())
                .addQueryParameter("q", clean)
                .addQueryParameter("limit", "8")
                .build()
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyList()
                val body = resp.body?.string().orEmpty()
                val data = JSONObject(body).optJSONArray("data") ?: return@runCatching emptyList()
                buildList {
                    for (i in 0 until data.length()) {
                        val name = data.getJSONObject(i).optString("name").trim()
                        if (name.isNotEmpty()) add(name)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun downloadGifToCache(gif: GifItem): File? = withContext(Dispatchers.IO) {
        val mediaDir = File(context.cacheDir, "shared_media/gifs").apply { mkdirs() }
        val safeId = gif.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val targetFile = File(mediaDir, "gif_$safeId.gif")
        if (targetFile.exists() && targetFile.length() > 100L) {
            return@withContext targetFile
        }
        if (!isNetworkAvailable()) return@withContext null

        val sourceUrl = gif.gifUrl.ifBlank { gif.previewUrl }
        if (sourceUrl.isBlank()) return@withContext null

        runCatching {
            val request = Request.Builder().url(sourceUrl).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val bytes = resp.body?.bytes() ?: return@runCatching null
                if (bytes.isEmpty()) return@runCatching null
                targetFile.writeBytes(bytes)
                pruneGifCache(mediaDir)
                targetFile
            }
        }.getOrNull()
    }

    private fun pruneGifCache(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        var totalSize = 0L
        for (file in files) {
            totalSize += file.length()
            if (totalSize > MAX_CACHE_BYTES) {
                file.delete()
            }
        }
    }

    private fun executeGifListRequest(url: String): GifSearchUiState {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = when (resp.code) {
                    401, 403 -> "Invalid or unauthorized GIPHY API key (HTTP ${resp.code})."
                    429 -> "GIPHY API rate limit reached. Please try again shortly."
                    else -> "GIF service error (HTTP ${resp.code})."
                }
                return GifSearchUiState.Error(msg)
            }
            val root = JSONObject(bodyStr)
            val data = root.optJSONArray("data")
            val pagination = root.optJSONObject("pagination")
            val items = mutableListOf<GifItem>()
            if (data != null) {
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.optString("id")
                    val title = obj.optString("title", "GIF")
                    val images = obj.optJSONObject("images") ?: continue
                    val previewObj = images.optJSONObject("fixed_width_small")
                        ?: images.optJSONObject("fixed_width")
                        ?: images.optJSONObject("downsized")
                    val fullObj = images.optJSONObject("downsized_medium")
                        ?: images.optJSONObject("fixed_width")
                        ?: images.optJSONObject("downsized")
                        ?: previewObj
                    val previewUrl = previewObj?.optString("url").orEmpty()
                    val gifUrl = fullObj?.optString("url").orEmpty().ifBlank { previewUrl }
                    if (id.isNotBlank() && (previewUrl.isNotBlank() || gifUrl.isNotBlank())) {
                        items.add(
                            GifItem(
                                id = id,
                                title = title,
                                previewUrl = previewUrl.ifBlank { gifUrl },
                                gifUrl = gifUrl,
                                width = previewObj?.optInt("width", 200) ?: 200,
                                height = previewObj?.optInt("height", 200) ?: 200
                            )
                        )
                    }
                }
            }
            val totalCount = pagination?.optInt("total_count", 0) ?: 0
            val count = pagination?.optInt("count", items.size) ?: items.size
            val offset = pagination?.optInt("offset", 0) ?: 0
            val hasMore = (offset + count) < totalCount && items.isNotEmpty()
            return GifSearchUiState.Success(items = items, hasMore = hasMore)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.giphy.com"
        const val PAGE_SIZE = 24
        private const val MAX_CACHE_BYTES = 20L * 1024L * 1024L

        val DEFAULT_CATEGORIES = listOf(
            GifCategoryItem("Trending", ""),
            GifCategoryItem("Happy", "happy"),
            GifCategoryItem("Love", "love"),
            GifCategoryItem("Funny", "funny"),
            GifCategoryItem("Applause", "applause"),
            GifCategoryItem("Sad", "sad"),
            GifCategoryItem("Greeting", "hello"),
            GifCategoryItem("Celebrate", "celebration"),
            GifCategoryItem("Thanks", "thank you"),
            GifCategoryItem("Wow", "wow")
        )
    }
}
