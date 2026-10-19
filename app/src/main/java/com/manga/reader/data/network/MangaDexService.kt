package com.manga.reader.data.network

import com.manga.reader.model.Chapter
import com.manga.reader.model.Manga
import com.manga.reader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDexService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .dns(MangaDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.mangadex.org"
    private val uploadsUrl = "https://uploads.mangadex.org"

    val GENRE_MAP: Map<String, String> = mapOf(
        "Action" to "391b0423-d847-456f-aff0-8b0cfc03066b",
        "Romance" to "423e2eae-a7a2-4a8b-ac03-a8351462d71d",
        "Comedy" to "4d32cc48-9f00-4cca-9b5a-a839f0764984",
        "Fantasy" to "cdc58593-87dd-415e-bbc0-2ec27bf404cc",
        "Isekai" to "ace04997-f6bd-436e-b261-779182193d3d",
        "Adventure" to "87cc87cd-a395-47af-b27a-93258283bbc6",
        "Slice of Life" to "e5301a23-ebd9-49dd-a0cb-2add944c7fe9",
        "Drama" to "b9af3a63-f058-46de-a9a0-e0c13906197a",
        "Supernatural" to "eabc5b4c-6aff-42f3-b657-3e90cbd00b75",
        "Mystery" to "ee968100-4191-4968-93d3-f82d72be7e46",
        "Horror" to "cdad7e68-1419-41dd-bdce-27753074a640",
        "Sci-Fi" to "256c8bd9-4904-4360-bf4f-508a76d67183",
        "Psychological" to "3b60b75c-a2d7-4860-ab56-05f391bb889c",
        "Thriller" to "07251805-a27e-4d59-b488-f0bfbec15168",
        "Martial Arts" to "799c202e-7daa-44eb-9cf7-8a3c0441531e",
        "School Life" to "caaa44eb-cd40-4177-b930-79d3ef2afe87",
        "Sports" to "69964a64-2f90-4d33-beeb-f3ed2875eb4c",
        "Historical" to "33771934-028e-4cb3-8744-691e866a923e",
        "Shounen" to "demographic:shounen",
        "Seinen" to "demographic:seinen",
        "Shoujo" to "demographic:shoujo"
    )

    suspend fun searchManga(query: String, limit: Int = 40): List<Manga> {
        return searchMangaWithFilters(query = query, genres = emptySet(), limit = limit)
    }

    suspend fun searchMangaWithFilters(
        query: String = "",
        genres: Set<String> = emptySet(),
        limit: Int = 40
    ): List<Manga> = withContext(Dispatchers.IO) {
        val cleanQ = query.trim()
        val queryParams = mutableListOf<String>()

        if (cleanQ.isNotEmpty()) {
            val encoded = URLEncoder.encode(cleanQ, StandardCharsets.UTF_8.name())
            queryParams.add("title=$encoded")
            queryParams.add("order[relevance]=desc")
        } else {
            queryParams.add("order[followedCount]=desc")
        }

        queryParams.add("limit=$limit")
        queryParams.add("includes[]=cover_art")
        queryParams.add("includes[]=author")
        queryParams.add("contentRating[]=safe")
        queryParams.add("contentRating[]=suggestive")

        if (genres.isNotEmpty()) {
            queryParams.add("includedTagsMode=OR")
        }

        for (g in genres) {
            val key = GENRE_MAP.keys.firstOrNull { it.equals(g, ignoreCase = true) }
            val tag = if (key != null) GENRE_MAP[key] else null
            if (tag != null) {
                if (tag.startsWith("demographic:")) {
                    val demo = tag.substringAfter("demographic:")
                    queryParams.add("publicationDemographic[]=$demo")
                } else {
                    queryParams.add("includedTags[]=$tag")
                }
            }
        }

        val url = "$baseUrl/manga?" + queryParams.joinToString("&")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return@withContext emptyList()

                val results = mutableListOf<Manga>()
                for (i in 0 until data.length()) {
                    val raw = data.optJSONObject(i) ?: continue
                    parseMangaObject(raw, cleanQ, genres)?.let { results.add(it) }
                }
                results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getMangaDetails(mangaId: String): Manga? = withContext(Dispatchers.IO) {
        val cleanId = mangaId.removePrefix("md-")
        val url = "$baseUrl/manga/$cleanId?includes[]=cover_art&includes[]=author"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MangaVerse/1.0.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                val raw = root.optJSONObject("data") ?: return@withContext null
                parseMangaObject(raw)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getChapters(mangadexId: String, preferredLang: String = "id"): List<Chapter> = withContext(Dispatchers.IO) {
        val cleanId = mangadexId.removePrefix("md-")

        val preferredChapters = fetchFeedChapters(cleanId, preferredLang)
        if (preferredChapters.isNotEmpty()) {
            return@withContext preferredChapters
        }

        // Fallback: jika bahasa target tidak memiliki rilisan chapter di MangaDex, ambil chapter yang tersedia
        if (preferredLang != "en") {
            val enChapters = fetchFeedChapters(cleanId, "en")
            if (enChapters.isNotEmpty()) return@withContext enChapters
        }

        fetchFeedChapters(cleanId, null)
    }

    private fun fetchFeedChapters(mangadexId: String, lang: String?): List<Chapter> {
        val langParam = if (lang != null) "&translatedLanguage[]=$lang" else ""
        val url = "$baseUrl/manga/$mangadexId/feed?limit=500&offset=0&order[chapter]=desc$langParam&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&includeExternalUrl=0"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MangaVerse/1.0.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return emptyList()

                val seenChapters = HashSet<String>()
                val chapters = mutableListOf<Chapter>()

                for (i in 0 until data.length()) {
                    val raw = data.optJSONObject(i) ?: continue
                    val chId = raw.optString("id")
                    val attr = raw.optJSONObject("attributes") ?: JSONObject()

                    val chNumber = attr.optString("chapter", "").ifEmpty { (i + 1).toString() }
                    val chTitle = attr.optString("title", "")
                    val chLang = attr.optString("translatedLanguage", lang ?: "en")
                    val publishAt = attr.optString("publishAt", "").substringBefore("T").ifEmpty { "Terbaru" }

                    val dedupeKey = "$chLang-$chNumber"
                    if (seenChapters.contains(dedupeKey)) continue
                    seenChapters.add(dedupeKey)

                    val displayTitle = if (chTitle.isNotBlank()) "Chapter $chNumber - $chTitle" else "Chapter $chNumber"

                    chapters.add(
                        Chapter(
                            id = "md-ch-$chId",
                            mangaId = "md-$mangadexId",
                            chapterNumber = chNumber,
                            title = displayTitle,
                            language = chLang,
                            scanlationGroup = "MangaDex",
                            releaseDate = publishAt,
                            source = "MangaDex"
                        )
                    )
                }
                return chapters
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    suspend fun getChapterPages(chapterUUID: String): List<ReaderPage> = withContext(Dispatchers.IO) {
        val cleanUuid = chapterUUID.removePrefix("md-ch-")
        val url = "$baseUrl/at-home/server/$cleanUuid"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MangaVerse/1.0.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)

                val hostUrl = root.optString("baseUrl")
                val chapterObj = root.optJSONObject("chapter") ?: return@withContext emptyList()
                val hash = chapterObj.optString("hash")
                val (subFolder, fileArray) = if (chapterObj.has("data") && (chapterObj.optJSONArray("data")?.length() ?: 0) > 0) {
                    "data" to chapterObj.optJSONArray("data")!!
                } else if (chapterObj.has("dataSaver") && (chapterObj.optJSONArray("dataSaver")?.length() ?: 0) > 0) {
                    "data-saver" to chapterObj.optJSONArray("dataSaver")!!
                } else {
                    return@withContext emptyList()
                }

                val pages = mutableListOf<ReaderPage>()
                for (i in 0 until fileArray.length()) {
                    val fileName = fileArray.optString(i)
                    if (fileName.isNotBlank()) {
                        pages.add(
                            ReaderPage(
                                pageNumber = i + 1,
                                imageUrl = "$hostUrl/$subFolder/$hash/$fileName",
                                width = 1200,
                                height = 1800
                            )
                        )
                    }
                }
                pages
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseMangaObject(
        raw: JSONObject,
        searchQuery: String = "",
        explicitGenres: Set<String> = emptySet()
    ): Manga? {
        val id = raw.optString("id")
        if (id.isBlank()) return null

        val attr = raw.optJSONObject("attributes") ?: JSONObject()
        val titleObj = attr.optJSONObject("title") ?: JSONObject()
        val altArray = attr.optJSONArray("altTitles") ?: JSONArray()

        val allAlts = mutableListOf<String>()
        for (i in 0 until altArray.length()) {
            val obj = altArray.optJSONObject(i) ?: continue
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k)
                if (v.isNotBlank()) allAlts.add(v)
            }
        }

        var title = titleObj.optString("en")
            .ifEmpty { titleObj.optString("ja-ro") }
            .ifEmpty { titleObj.optString("ko-ro") }

        if (title.isBlank()) {
            val keys = titleObj.keys()
            if (keys.hasNext()) {
                title = titleObj.optString(keys.next())
            }
        }

        // Cek jika pencarian mencocokkan judul alternatif
        if (searchQuery.isNotBlank()) {
            val matchedAlt = allAlts.find { it.contains(searchQuery, ignoreCase = true) }
            if (matchedAlt != null && !title.contains(searchQuery, ignoreCase = true)) {
                title = matchedAlt
            }
        }

        if (title.isBlank()) title = "Judul Tidak Diketahui"

        var coverFileName = ""
        var authorName = "Pengarang MangaDex"
        val rels = raw.optJSONArray("relationships") ?: JSONArray()
        for (i in 0 until rels.length()) {
            val rel = rels.optJSONObject(i) ?: continue
            val type = rel.optString("type")
            val relAttr = rel.optJSONObject("attributes") ?: JSONObject()

            if (type == "cover_art") {
                val f = relAttr.optString("fileName")
                if (f.isNotBlank()) coverFileName = f
            } else if (type == "author" || type == "artist") {
                val name = relAttr.optString("name")
                if (name.isNotBlank()) authorName = name
            }
        }

        val coverUrl = if (coverFileName.isNotBlank()) {
            "$uploadsUrl/covers/$id/$coverFileName.512.jpg"
        } else {
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80"
        }

        val tagsArray = attr.optJSONArray("tags") ?: JSONArray()
        val genres = mutableListOf<String>()
        for (i in 0 until tagsArray.length()) {
            val t = tagsArray.optJSONObject(i) ?: continue
            val nameObj = t.optJSONObject("attributes")?.optJSONObject("name")
            val gName = nameObj?.optString("en") ?: ""
            if (gName.isNotBlank() && !genres.contains(gName)) genres.add(gName)
        }

        val pubDemo = attr.optString("publicationDemographic", "")
        if (pubDemo.isNotBlank()) {
            val demoTitle = pubDemo.replaceFirstChar { it.uppercase() }
            if (!genres.any { it.equals(demoTitle, ignoreCase = true) }) {
                genres.add(demoTitle)
            }
        }

        for (eg in explicitGenres) {
            if (!genres.any { it.equals(eg, ignoreCase = true) }) {
                genres.add(eg)
            }
        }

        val descObj = attr.optJSONObject("description") ?: JSONObject()
        val synopsis = descObj.optString("id")
            .ifEmpty { descObj.optString("en") }
            .ifEmpty { "Sinopsis belum tersedia." }

        val status = attr.optString("status", "ongoing")
        val statusId = if (status.equals("completed", true)) "Tamat" else "Berjalan"
        val year = attr.optString("year", "")

        val idSum = id.fold(0) { acc, c -> acc + c.code }
        val rating = 8.0f + ((Math.abs(idSum) % 18) / 10f)

        return Manga(
            id = "md-$id",
            title = title,
            altTitles = allAlts,
            coverUrl = coverUrl,
            bannerUrl = coverUrl,
            author = authorName,
            status = status.replaceFirstChar { it.uppercase() },
            statusId = statusId,
            synopsis = synopsis,
            genres = genres.ifEmpty { listOf("Manga") },
            rating = rating,
            releaseYear = year,
            totalChapters = 0,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        )
    }
}
