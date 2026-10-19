package com.manga.reader.data.network

import com.manga.reader.model.Chapter
import com.manga.reader.model.Manga
import com.manga.reader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KomikindoService @Inject constructor() {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val adKeywords = listOf("promosi", "iklan", "banner", "slot", "judi", "zeus", "gacor")

    private val client = OkHttpClient.Builder()
        .dns(MangaDns)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun fetchDoc(url: String, timeoutMs: Int = 6000): Document {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "https://komikindo.ch/")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string()
                    if (!html.isNullOrBlank()) {
                        return Jsoup.parse(html, url)
                    }
                }
            }
        } catch (_: Exception) {}

        return Jsoup.connect(url)
            .userAgent(userAgent)
            .header("Referer", "https://komikindo.ch/")
            .timeout(timeoutMs)
            .get()
    }

    suspend fun searchManga(query: String): List<Manga> = withContext(Dispatchers.IO) {
        val cleanQ = query.trim()
        if (cleanQ.isEmpty()) return@withContext emptyList()

        val targetUrl = "https://komikindo.ch/?s=${URLEncoder.encode(cleanQ, StandardCharsets.UTF_8.name())}"

        try {
            val doc = fetchDoc(targetUrl, 4000)

            val elements = doc.select(".animepost")
            val results = mutableListOf<Manga>()
            val seenSlugs = HashSet<String>()

            for (el in elements) {
                val link = el.select("a").firstOrNull()?.attr("href") ?: continue
                val slug = Regex("/komik/([^/]+)/").find(link)?.groupValues?.get(1) ?: continue
                if (seenSlugs.contains(slug)) continue
                seenSlugs.add(slug)

                val rawTitle = el.select("h4").text().ifEmpty { el.select("a").attr("title") }
                val title = rawTitle.replace(Regex("^Komik\\s+", RegexOption.IGNORE_CASE), "").trim()
                if (title.isBlank()) continue

                val img = el.select("img").firstOrNull()
                val cover = img?.attr("src")?.ifEmpty { img.attr("data-src") } ?: ""

                results.add(
                    Manga(
                        id = "komikindo-$slug",
                        title = title,
                        altTitles = emptyList(),
                        coverUrl = cover,
                        bannerUrl = cover,
                        author = "Komikindo (Sub Indo)",
                        status = "Ongoing",
                        statusId = "Berjalan",
                        synopsis = "Komik $title terjemahan Bahasa Indonesia lengkap di MangaVerse.",
                        genres = listOf("Manga", "Sub Indo"),
                        rating = 8.6f,
                        releaseYear = "",
                        totalChapters = 0,
                        source = "Komikindo (Sub Indo)",
                        type = "Manga",
                        hasIndonesian = true
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getMangaDetails(slug: String): Manga? = withContext(Dispatchers.IO) {
        val targetUrl = "https://komikindo.ch/komik/$slug/"

        try {
            val doc = fetchDoc(targetUrl, 6000)

            val title = doc.select("h1.entry-title").text()
                .replace(Regex("^Komik\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifEmpty { slug.replace("-", " ").capitalizeWords() }

            val img = doc.select(".thumb img").firstOrNull()
            val cover = img?.attr("src")?.ifEmpty { img.attr("data-src") } ?: ""

            val synopsis = doc.select(".entry-content-single p").text()
                .ifEmpty { "Komik $title dalam Bahasa Indonesia." }

            val genres = doc.select(".genre-info a").map { it.text() }

            Manga(
                id = "komikindo-$slug",
                title = title,
                altTitles = emptyList(),
                coverUrl = cover,
                bannerUrl = cover,
                author = "Komikindo (Sub Indo)",
                status = "Ongoing",
                statusId = "Berjalan",
                synopsis = synopsis,
                genres = genres.ifEmpty { listOf("Manga", "Sub Indo") },
                rating = 8.8f,
                releaseYear = "",
                totalChapters = 0,
                source = "Komikindo (Sub Indo)",
                type = "Manga",
                hasIndonesian = true
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getChapters(slug: String): List<Chapter> = withContext(Dispatchers.IO) {
        val targetUrl = "https://komikindo.ch/komik/$slug/"

        try {
            val doc = fetchDoc(targetUrl, 6000)

            val chapterItems = doc.select("#chapter_list li")
            val chapters = mutableListOf<Chapter>()

            for (item in chapterItems) {
                val a = item.select("a").firstOrNull() ?: continue
                val chUrl = a.attr("href")
                if (chUrl.isBlank()) continue

                val chText = item.select(".lchx").text().ifEmpty { a.text() }
                val date = item.select(".dt").text().ifEmpty { "Terbaru" }

                val num = Regex("(?i)chapter\\s*([0-9.]+)").find(chText)?.groupValues?.get(1)
                    ?: Regex("([0-9.]+)").find(chText)?.value
                    ?: "?"

                val encodedUrl = URLEncoder.encode(chUrl, StandardCharsets.UTF_8.name())

                chapters.add(
                    Chapter(
                        id = "komikindo-ch-$encodedUrl",
                        mangaId = "komikindo-$slug",
                        chapterNumber = num,
                        title = if (chText.startsWith("Chapter", ignoreCase = true)) chText else "Chapter $num",
                        language = "id",
                        scanlationGroup = "Komikindo (Sub Indo)",
                        releaseDate = date,
                        source = "Komikindo"
                    )
                )
            }
            chapters
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getChapterPages(encodedUrl: String): List<ReaderPage> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = encodedUrl.trim()
            while (targetUrl.contains("%3A", ignoreCase = true) || targetUrl.contains("%2F", ignoreCase = true)) {
                targetUrl = URLDecoder.decode(targetUrl, StandardCharsets.UTF_8.name())
            }
            if (!targetUrl.startsWith("http://", ignoreCase = true) && !targetUrl.startsWith("https://", ignoreCase = true)) {
                targetUrl = if (targetUrl.startsWith("/")) "https://komikindo.ch$targetUrl" else "https://komikindo.ch/$targetUrl"
            }

            val doc = fetchDoc(targetUrl, 10000)

            val images = doc.select("#chimg-auh img, #Baca_Komik img, .chapter-image img, .main-reading-area img, .readerarea img, #readerarea img")
            val validUrls = images.mapNotNull { el ->
                val src = el.attr("src").ifEmpty { el.attr("data-src") }.ifEmpty { el.attr("data-lazy-src") }
                if (src.isNotBlank()) src.trim() else null
            }.filter { u ->
                val lower = u.lowercase()
                val clean = lower.substringBefore("?")
                val isImage = clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") || clean.endsWith(".webp") ||
                        lower.contains("/data/") || lower.contains("/media/") || lower.contains("/uploads/") || lower.contains("komik")
                val isNotAd = !adKeywords.any { lower.contains(it) } && !lower.endsWith(".gif")
                isNotAd && isImage
            }

            validUrls.mapIndexed { index, url ->
                ReaderPage(
                    pageNumber = index + 1,
                    imageUrl = url,
                    width = 1200,
                    height = 1800
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
