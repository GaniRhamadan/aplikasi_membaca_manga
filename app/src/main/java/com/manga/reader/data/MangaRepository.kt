package com.manga.reader.data

import com.manga.reader.model.Chapter
import com.manga.reader.model.HistoryItem
import com.manga.reader.model.Manga
import com.manga.reader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaRepository @Inject constructor() {

    private val _favoriteIds = MutableStateFlow<Set<String>>(setOf("curated-solo-leveling", "curated-one-piece"))
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _readingHistory = MutableStateFlow<List<HistoryItem>>(
        listOf(
            HistoryItem(
                mangaId = "curated-solo-leveling",
                mangaTitle = "Solo Leveling",
                coverUrl = "https://uploads.mangadex.org/covers/32d76d19-8a05-4db0-9fc2-e0b0648fe9d0/86d356fc-7940-42a3-9be2-adb0f8262ba8.jpg.512.jpg",
                lastChapterId = "solo-leveling-1",
                lastChapterTitle = "Chapter 1"
            )
        )
    )
    val readingHistory: StateFlow<List<HistoryItem>> = _readingHistory.asStateFlow()

    private val allMangaList = listOf(
        Manga(
            id = "curated-solo-leveling",
            title = "Solo Leveling",
            altTitles = listOf("Na Honjaman Level Up", "Only I Level Up"),
            coverUrl = "https://uploads.mangadex.org/covers/32d76d19-8a05-4db0-9fc2-e0b0648fe9d0/86d356fc-7940-42a3-9be2-adb0f8262ba8.jpg.512.jpg",
            bannerUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200&auto=format&fit=crop&q=80",
            author = "Chugong, DUBU (REDICE STUDIO)",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Di dunia di mana monster bermunculan dari gerbang dimensi, para manusia pemburu yang dikenal sebagai Hunter bangkit bertarung. Sung Jin-woo adalah Hunter terlemah di dunia dari peringkat E. Namun setelah selamat dari Dungeon Ganda yang misterius, sebuah layar quest rahasia muncul hanya di depan matanya membimbingnya menjadi Hunter terkuat di bumi.",
            genres = listOf("Action", "Fantasy", "Adventure", "Supernatural"),
            rating = 9.6f,
            releaseYear = "2018",
            totalChapters = 200,
            source = "Komikindo",
            type = "Manhwa / Webtoon",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-one-piece",
            title = "One Piece",
            altTitles = listOf("Wan Pīsu", "OP"),
            coverUrl = "https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/97753122-e351-4eab-8ba3-d7d9fb96bf24.jpg.512.jpg",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
            author = "Eiichiro Oda",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Gol D. Roger sang Raja Bajak Laut mengumumkan harta terbesarnya One Piece sebelum dieksekusi mati, memicu Zaman Keemasan Bajak Laut. Monkey D. Luffy, pemuda bertubuh karet pemakan Buah Iblis Gomu Gomu, berlayar menjelajahi Grand Line bersama kru Topi Jerami demi menemukan One Piece dan menjadi Raja Bajak Laut berikutnya.",
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy", "Shounen"),
            rating = 9.8f,
            releaseYear = "1997",
            totalChapters = 1130,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-jujutsu-kaisen",
            title = "Jujutsu Kaisen",
            altTitles = listOf("JJK", "Pertarungan Sihir Terkutuk"),
            coverUrl = "https://uploads.mangadex.org/covers/c52b2ce3-7f95-469c-96b0-479524fb7a1a/7dc752c3-8c90-468e-8c75-6903e38d7c7f.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/c52b2ce3-7f95-469c-96b0-479524fb7a1a/7dc752c3-8c90-468e-8c75-6903e38d7c7f.jpg.512.jpg",
            author = "Gege Akutami",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Yuji Itadori adalah siswa SMA biasa dengan bakat atletik luar biasa. Suatu hari demi menyelamatkan teman-temannya dari roh kutukan jahat, ia menelan jari Ryomen Sukuna, Raja Kutukan legendaris. Kini berbagi tubuh dengan iblis terkuat, Yuji masuk ke SMA Jujutsu Tokyo di bawah bimbingan Satoru Gojo.",
            genres = listOf("Action", "Fantasy", "Horror", "Shounen", "Supernatural"),
            rating = 9.3f,
            releaseYear = "2018",
            totalChapters = 271,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-chainsaw-man",
            title = "Chainsaw Man",
            altTitles = listOf("CSM", "Denji Sang Manusia Gergaji Mesin"),
            coverUrl = "https://uploads.mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/6e518bd1-5f60-446b-8832-bfe6bf74834b.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/6e518bd1-5f60-446b-8832-bfe6bf74834b.jpg.512.jpg",
            author = "Tatsuki Fujimoto",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Denji hidup dalam kemiskinan ekstrem menanggung utang mendiang ayahnya kepada Yakuza, berburu iblis bersama anjing iblis gergaji Pochita. Ketika dikhianati dan dibunuh, Pochita menyatu dengan jantungnya dan membangkitkan Denji sebagai Manusia Chainsaw yang mengerikan dan liar di bawah komando Keamanan Publik Makima.",
            genres = listOf("Action", "Comedy", "Drama", "Horror", "Supernatural"),
            rating = 9.2f,
            releaseYear = "2018",
            totalChapters = 180,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-frieren",
            title = "Sousou no Frieren",
            altTitles = listOf("Frieren: Beyond Journey's End"),
            coverUrl = "https://uploads.mangadex.org/covers/b0b721ff-c388-4486-aa0f-c2b0bb321512/75a1cb6f-0554-4447-bf6e-0ef759af3f48.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/b0b721ff-c388-4486-aa0f-c2b0bb321512/75a1cb6f-0554-4447-bf6e-0ef759af3f48.jpg.512.jpg",
            author = "Kanehito Yamada, Tsukasa Abe",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Raja Iblis telah dikalahkan, dan kelompok pahlawan kembali ke rumah dalam damai. Frieren, penyihir elf berumur ribuan tahun, menyaksikan rekan-rekannya menua dan meninggal dunia. Menyadari berharganya waktu dan ikatan manusia, Frieren memulai perjalanan baru menuju utara demi memahami perasaan sesama dan menghormati memori sang Pahlawan Himmel.",
            genres = listOf("Adventure", "Drama", "Fantasy", "Shounen"),
            rating = 9.5f,
            releaseYear = "2020",
            totalChapters = 135,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-spy-family",
            title = "SPY x FAMILY",
            altTitles = listOf("Misi Rahasia Keluarga Forger"),
            coverUrl = "https://uploads.mangadex.org/covers/6b958848-c885-4735-9201-12ee77abcb3c/91a35e78-62b2-41fe-9869-ce051f2d1070.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/6b958848-c885-4735-9201-12ee77abcb3c/91a35e78-62b2-41fe-9869-ce051f2d1070.jpg.512.jpg",
            author = "Tatsuya Endo",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Mata-mata ulung berkode Twilight harus membangun keluarga palsu demi Operasi Strix menjaga perdamaian dunia barat dan timur. Tanpa sepengetahuannya, istri yang ia nikahi adalah pembunuh bayaran legendaris bernama Thorn Princess, dan putri cilik yang ia adopsi adalah esper pembaca pikiran bernama Anya!",
            genres = listOf("Action", "Comedy", "Shounen", "Slice of Life"),
            rating = 9.1f,
            releaseYear = "2019",
            totalChapters = 105,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-kage-jitsuryokusha",
            title = "Kage no Jitsuryokusha ni Naritakute!",
            altTitles = listOf("The Eminence in Shadow"),
            coverUrl = "https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/6079dd31-838b-4d61-87c4-121f3ad19158.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/6079dd31-838b-4d61-87c4-121f3ad19158.jpg.512.jpg",
            author = "Aizawa Daisuke, Sakano Anri",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Cid Kagenou terobsesi menjadi dalang di balik layar yang mengendalikan dunia dari kegelapan. Terlahir kembali di dunia sihir sebagai bangsawan biasa, Cid mendirikan organisasi rahasia Shadow Garden dan mengarang cerita tentang Kultus Diablos jahat. Tanpa ia sadari, semua bualannya ternyata 100% nyata dan bawahannya memujanya sebagai dewa!",
            genres = listOf("Action", "Comedy", "Fantasy", "Isekai", "Shounen"),
            rating = 9.4f,
            releaseYear = "2018",
            totalChapters = 65,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-iruma-kun",
            title = "Mairimashita! Iruma-kun",
            altTitles = listOf("Welcome to Demon School! Iruma-kun"),
            coverUrl = "https://uploads.mangadex.org/covers/d7037b2a-874a-4360-8a7b-07f2899152fd/6917a533-d70c-49cb-84af-1aa7ccff3d02.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/d7037b2a-874a-4360-8a7b-07f2899152fd/6917a533-d70c-49cb-84af-1aa7ccff3d02.jpg.512.jpg",
            author = "Nishi Osamu",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Suzuki Iruma yang malang dijual oleh orang tuanya yang serakah kepada iblis Sullivan. Namun sang iblis malah mengangkat Iruma sebagai cucu kesayangannya yang dimanjakan dan mengirimnya ke sekolah iblis Babyls. Di sana, Iruma harus merahasiakan identitas manusianya agar tidak dimakan oleh teman-teman iblisnya!",
            genres = listOf("Comedy", "Fantasy", "School Life", "Shounen", "Supernatural"),
            rating = 9.5f,
            releaseYear = "2017",
            totalChapters = 360,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-kanojo-mo-kanojo",
            title = "Kanojo mo Kanojo",
            altTitles = listOf("Girlfriend, Girlfriend / Dua Pacar Sekaligus"),
            coverUrl = "https://uploads.mangadex.org/covers/e83c326b-921b-45ff-bc0c-d667bbfe64cc/e094b6d2-0518-4d4b-b40b-cce68f567995.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/e83c326b-921b-45ff-bc0c-d667bbfe64cc/e094b6d2-0518-4d4b-b40b-cce68f567995.jpg.512.jpg",
            author = "Hiroyuki",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Naoya Mukai baru saja berhasil jadian dengan teman masa kecil yang dicintainya bertahun-tahun, Saki Saki. Namun tiba-tiba seorang siswi manis bernama Nagisa Minase menyatakan perasaan cinta padanya dengan sangat tulus. Karena tak sampai hati menolak ketulusan Nagisa dan tak ingin mengkhianati Saki, Naoya mengusulkan ide gila: meminta izin untuk memacari keduanya sekaligus!",
            genres = listOf("Comedy", "Romance", "School Life", "Shounen"),
            rating = 8.6f,
            releaseYear = "2020",
            totalChapters = 144,
            source = "Komikindo",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-naruto",
            title = "Naruto",
            altTitles = listOf("Naruto Shippuden"),
            coverUrl = "https://uploads.mangadex.org/covers/6b1eb93e-473a-4ab3-9922-1a66d2a29a4a/f52c1e62-c174-4b55-a0ca-74431e5f8f53.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/6b1eb93e-473a-4ab3-9922-1a66d2a29a4a/f52c1e62-c174-4b55-a0ca-74431e5f8f53.jpg.512.jpg",
            author = "Masashi Kishimoto",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Naruto Uzumaki, seorang ninja yatim piatu yang menyimpan Rubah Ekor Sembilan dalam tubuhnya, berjuang untuk diakui oleh penduduk desa dan bercita-cita menjadi Hokage.",
            genres = listOf("Action", "Adventure", "Martial Arts", "Shounen"),
            rating = 9.4f,
            releaseYear = "1999",
            totalChapters = 700,
            source = "Komikindo",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-bleach",
            title = "Bleach",
            altTitles = listOf("Bleach Thousand-Year Blood War"),
            coverUrl = "https://uploads.mangadex.org/covers/b665fc5a-9694-4422-9907-7389c303f9a7/df12a5db-db17-48f8-b1fc-e39908cf8220.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/b665fc5a-9694-4422-9907-7389c303f9a7/df12a5db-db17-48f8-b1fc-e39908cf8220.jpg.512.jpg",
            author = "Tite Kubo",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Ichigo Kurosaki memperoleh kekuatan Shinigami dari Rukia Kuchiki dan bertanggung jawab melindungi manusia dari roh jahat Hollow dan memandu jiwa ke Soul Society.",
            genres = listOf("Action", "Supernatural", "Adventure"),
            rating = 9.2f,
            releaseYear = "2001",
            totalChapters = 686,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        )
    )

    suspend fun getPopularManga(): List<Manga> = withContext(Dispatchers.IO) {
        allMangaList
    }

    suspend fun getFeaturedManga(): Manga = withContext(Dispatchers.IO) {
        allMangaList.first() // Solo Leveling
    }

    suspend fun getLatestManga(): List<Manga> = withContext(Dispatchers.IO) {
        // Chainsaw Man, Frieren, SPY x FAMILY, One Piece, Kage no Jitsuryokusha, Iruma-kun
        listOf(
            allMangaList[3], // Chainsaw Man
            allMangaList[4], // Frieren
            allMangaList[5], // SPY x FAMILY
            allMangaList[1], // One Piece
            allMangaList[6], // Kage no Jitsuryokusha
            allMangaList[7]  // Iruma-kun
        )
    }

    suspend fun getGenres(): List<String> = withContext(Dispatchers.IO) {
        listOf("Semua", "Action", "Fantasy", "Adventure", "Comedy", "Shounen", "Supernatural", "Drama", "Horror")
    }

    suspend fun searchManga(query: String, filterGenre: String = "Semua", filterSource: String = "all"): List<Manga> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        var results = allMangaList

        if (filterGenre != "Semua") {
            results = results.filter { m -> m.genres.any { it.equals(filterGenre, ignoreCase = true) } }
        }

        if (filterSource != "all") {
            results = results.filter { m ->
                if (filterSource == "curated") m.id.startsWith("curated-")
                else m.source.lowercase().contains(filterSource.lowercase())
            }
        }

        if (q.isNotEmpty()) {
            results = results.filter { m ->
                m.title.lowercase().contains(q) ||
                m.altTitles.any { it.lowercase().contains(q) } ||
                m.author.lowercase().contains(q) ||
                m.genres.any { it.lowercase().contains(q) }
            }
        }

        results
    }

    suspend fun getMangaById(id: String): Manga? = withContext(Dispatchers.IO) {
        allMangaList.find { it.id == id }
    }

    suspend fun getFavoriteManga(): List<Manga> = withContext(Dispatchers.IO) {
        val favs = _favoriteIds.value
        allMangaList.filter { favs.contains(it.id) }
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(id: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _favoriteIds.value = current
    }

    fun recordHistory(mangaId: String, chapterId: String, chapterTitle: String) {
        val manga = allMangaList.find { it.id == mangaId } ?: return
        val existing = _readingHistory.value.toMutableList()
        existing.removeAll { it.mangaId == mangaId }
        existing.add(
            0,
            HistoryItem(
                mangaId = mangaId,
                mangaTitle = manga.title,
                coverUrl = manga.coverUrl,
                lastChapterId = chapterId,
                lastChapterTitle = chapterTitle
            )
        )
        _readingHistory.value = existing
    }

    fun clearHistory() {
        _readingHistory.value = emptyList()
    }

    suspend fun getChapters(mangaId: String, preferredLang: String = "id"): List<Chapter> = withContext(Dispatchers.IO) {
        val total = when (mangaId) {
            "curated-solo-leveling" -> 182
            "curated-one-piece" -> 1240
            "curated-jujutsu-kaisen" -> 277
            "curated-chainsaw-man" -> 233
            "curated-frieren" -> 147
            "curated-spy-family" -> 173
            "curated-kage-jitsuryokusha" -> 94
            "curated-iruma-kun" -> 473
            "curated-kanojo-mo-kanojo" -> 144
            "curated-naruto", "naruto" -> 667
            "curated-bleach", "bleach" -> 372
            else -> 50
        }

        // Generate seluruh chapter lengkap (1 sampai total) tanpa pembatasan 35 chapter
        (1..total).map { num ->
            val isFinal = (num == total) && total > 1
            Chapter(
                id = "$mangaId-ch-$num",
                mangaId = mangaId,
                chapterNumber = num.toString(),
                title = if (isFinal) "Chapter $num (Tamat)" else "Chapter $num",
                language = preferredLang,
                scanlationGroup = if (preferredLang == "id") "Komiku / Komikindo (Sub Indo)" else "Official / MangaSee",
                releaseDate = if (num > total - 5) "Terbaru" else "2024",
                source = "Komiku / Komikindo"
            )
        }.reversed()
    }

    private fun tryScrapeKomikuPages(slug: String, chapterNum: Int): List<String> {
        return try {
            val formatted = if (chapterNum < 10) String.format("%02d", chapterNum) else chapterNum.toString()
            val url = "https://komiku.org/$slug-chapter-$formatted/"
            val doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(4000)
                .get()
            val images = doc.select("#Baca_Komik img")
            val adKeywords = listOf("promosi", "iklan", "banner", "slot", "judi", "zeus", "gacor")
            images.mapNotNull { el ->
                val src = el.attr("src").ifEmpty { el.attr("data-src") }
                if (src.isNotEmpty()) src else null
            }.filter { u ->
                val lower = u.lowercase()
                !adKeywords.any { lower.contains(it) } && !lower.endsWith(".gif") &&
                (lower.contains("komiku") || lower.contains("/uploads") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getChapterPages(chapterId: String): List<ReaderPage> = withContext(Dispatchers.IO) {
        // Coba ambil halaman komik langsung dari Komiku.org sesuai nomor bab
        val komikuSlug = when {
            chapterId.startsWith("curated-kanojo-mo-kanojo") -> "kanojo-mo-kanojo"
            chapterId.startsWith("curated-one-piece") -> "one-piece"
            chapterId.startsWith("curated-chainsaw-man") -> "chainsaw-man"
            chapterId.startsWith("curated-jujutsu-kaisen") -> "jujutsu-kaisen"
            chapterId.startsWith("curated-frieren") -> "sousou-no-frieren"
            chapterId.startsWith("curated-spy-family") -> "spy-x-family"
            else -> null
        }
        val chNum = chapterId.substringAfterLast("-ch-").toIntOrNull()
        if (komikuSlug != null && chNum != null) {
            val livePages = tryScrapeKomikuPages(komikuSlug, chNum)
            if (livePages.isNotEmpty()) {
                return@withContext livePages.mapIndexed { index, url ->
                    ReaderPage(
                        pageNumber = index + 1,
                        imageUrl = url,
                        width = 1200,
                        height = 1800
                    )
                }
            }
        }

        val rawUrls: List<String> = when {
            chapterId.startsWith("curated-solo-leveling") -> listOf(
                "https://imageainewgeneration.lol/data/742688/1/SCH74srmqjNMvJk/eG6WiHd9Woz1fyC001.jpg",
                "https://himmga.lat/data/742688/1/SCH74srmqjNMvJk/SXYoWy6wb5OCEvi002.jpg",
                "https://gaimgame.pics/data/742688/1/SCH74srmqjNMvJk/aVqOgr24ws0sY9H003.jpg",
                "https://indocontentaising.lol/data/742688/1/SCH74srmqjNMvJk/v8N9G4fFmnBvAZy004.jpg",
                "https://aicontentwow.lol/data/742688/1/SCH74srmqjNMvJk/Lax2FfjQzlINuye005.jpg",
                "https://contentkerewnrorai.lat/data/742688/1/SCH74srmqjNMvJk/U6DdCAIJVdmN5F8006.jpg",
                "https://imageainewgeneration.lol/data/742688/1/SCH74srmqjNMvJk/BgfXZgtuqBZYsUA007.jpg",
                "https://himmga.lat/data/742688/1/SCH74srmqjNMvJk/WKpKW4gdF31wQjt008.jpg"
            )
            chapterId.startsWith("curated-one-piece") -> listOf(
                "https://imageainewgeneration.lol/data/772241/001/AGn0Y6kM94XVzej/JWUzqFVEsdN424J001.jpg",
                "https://himmga.lat/data/772241/001/AGn0Y6kM94XVzej/9mSZBI0uGFGIaAZ002.jpg",
                "https://gaimgame.pics/data/772241/001/AGn0Y6kM94XVzej/gEz1qCXdWT1YyjM003.jpg",
                "https://indocontentaising.lol/data/772241/001/AGn0Y6kM94XVzej/IExEjlDd3eASKV5004.jpg",
                "https://aicontentwow.lol/data/772241/001/AGn0Y6kM94XVzej/CgKwAXCHB9aFlEk005.jpg",
                "https://contentkerewnrorai.lat/data/772241/001/AGn0Y6kM94XVzej/dBn541n58cJM7ix006.jpg",
                "https://imageainewgeneration.lol/data/772241/001/AGn0Y6kM94XVzej/iOLavSTMApbRV26007.jpg",
                "https://himmga.lat/data/772241/001/AGn0Y6kM94XVzej/5wr3Iz65Vi7iOoQ008.jpg"
            )
            chapterId.startsWith("curated-frieren") -> listOf(
                "https://imageainewgeneration.lol/data/141782/1/MiAu7TLqd2IkY5l/vKnVXfCcCGWjmZU01.jpg",
                "https://himmga.lat/data/141782/1/MiAu7TLqd2IkY5l/ur3KxeiHvrhMeSi02.jpg",
                "https://gaimgame.pics/data/141782/1/MiAu7TLqd2IkY5l/q60ZOvFtBojsG5B03.jpg",
                "https://indocontentaising.lol/data/141782/1/MiAu7TLqd2IkY5l/O9Cs2RuZG5Pz7nw04.jpg",
                "https://aicontentwow.lol/data/141782/1/MiAu7TLqd2IkY5l/FaEhsiKiM6WJqCV05.jpg",
                "https://contentkerewnrorai.lat/data/141782/1/MiAu7TLqd2IkY5l/UEQ36dF5qGSVQ0V06.jpg",
                "https://imageainewgeneration.lol/data/141782/1/MiAu7TLqd2IkY5l/pDJb6tBpjdZEzED07.jpg",
                "https://himmga.lat/data/141782/1/MiAu7TLqd2IkY5l/RD7jISmcU4sVHKW08.jpg"
            )
            chapterId.startsWith("curated-spy-family") -> listOf(
                "https://imageainewgeneration.lol/data/404729/01/EwQefrFkOPIcB3l/R60ryuFlpXIcrTh001.webp",
                "https://himmga.lat/data/404729/01/EwQefrFkOPIcB3l/vXvwQB8adwIv2yK002.webp",
                "https://gaimgame.pics/data/404729/01/EwQefrFkOPIcB3l/5Oo2SgucFx69XDs003.webp",
                "https://indocontentaising.lol/data/404729/01/EwQefrFkOPIcB3l/oWrgxSqmkf0s12y004.webp",
                "https://aicontentwow.lol/data/404729/01/EwQefrFkOPIcB3l/MN4rrD7Mp2Tp3t2005.webp",
                "https://contentkerewnrorai.lat/data/404729/01/EwQefrFkOPIcB3l/D80nUbb8ss9cVLF006.webp",
                "https://imageainewgeneration.lol/data/404729/01/EwQefrFkOPIcB3l/46J8xtJ4DVvDujS007.webp",
                "https://himmga.lat/data/404729/01/EwQefrFkOPIcB3l/LOB3dORZlebTTLQ008.webp"
            )
            chapterId.startsWith("curated-jujutsu-kaisen") -> listOf(
                "https://imageainewgeneration.lol/data/154728/1/8e8548b93db244158cca3cba03ee69d0/cO1xNAxx551HIGvDZrpIneOx8MxsqDgvpG9kYUVP.jpg",
                "https://himmga.lat/data/154728/1/8e8548b93db244158cca3cba03ee69d0/4lzXRkyR2sfe8d9T0ryNouWIjolNuk9d7n6ptdKI.jpg",
                "https://gaimgame.pics/data/154728/1/8e8548b93db244158cca3cba03ee69d0/6O1pEDdxxFWU1KFSYzJ8TjXoaPKKSUkH484rBw1M.jpg",
                "https://indocontentaising.lol/data/154728/1/8e8548b93db244158cca3cba03ee69d0/t27rOjb4yzT5Eh61U5EwS3rSkKqamvkef8Itzv2R.jpg",
                "https://aicontentwow.lol/data/154728/1/8e8548b93db244158cca3cba03ee69d0/WH1FUns1kS6UB25KJZa6pPBUAyDbaYir1kNOh1t4.jpg",
                "https://contentkerewnrorai.lat/data/154728/1/8e8548b93db244158cca3cba03ee69d0/y6ku0fjCQwU6OcZ0qImzouDn0h5seqRxbhWmdEoq.jpg",
                "https://imageainewgeneration.lol/data/154728/1/8e8548b93db244158cca3cba03ee69d0/DCLtsRPKrxjoVIhNIwWYCHrHlwy70gWqyThjG8oG.jpg",
                "https://himmga.lat/data/154728/1/8e8548b93db244158cca3cba03ee69d0/oaNiywXWR41QAWWddQpOYrZ7XFJ7S2cl5MhCwBxW.jpg"
            )
            chapterId.startsWith("curated-chainsaw-man") -> listOf(
                "https://imageainewgeneration.lol/data/245679/1/QxHt3MhyhQwCHxS/emmugEnp4R26HXH001.jpg",
                "https://himmga.lat/data/245679/1/QxHt3MhyhQwCHxS/XZsAGaoWpesl3Ec002.jpg",
                "https://gaimgame.pics/data/245679/1/QxHt3MhyhQwCHxS/dY5c84QFtU3zZtk003.jpg",
                "https://indocontentaising.lol/data/245679/1/QxHt3MhyhQwCHxS/meRuK3dzKgNdK5J004.jpg",
                "https://aicontentwow.lol/data/245679/1/QxHt3MhyhQwCHxS/9IyV3SJ9Fm2bX7s005.jpg",
                "https://contentkerewnrorai.lat/data/245679/1/QxHt3MhyhQwCHxS/MB4aETcZqxepQku006.jpg",
                "https://imageainewgeneration.lol/data/245679/1/QxHt3MhyhQwCHxS/ZFKPylpw8oFUJoA007.jpg",
                "https://himmga.lat/data/245679/1/QxHt3MhyhQwCHxS/UJyFAMs2t3E2hay008.jpg"
            )
            chapterId.startsWith("curated-kage-jitsuryokusha") -> listOf(
                "https://imageainewgeneration.lol/data/923915/01/YBHnCVEgXdjszia/da2larv9fPL36yt001.webp",
                "https://himmga.lat/data/923915/01/YBHnCVEgXdjszia/gZDkEArcmm760z9002.webp",
                "https://gaimgame.pics/data/923915/01/YBHnCVEgXdjszia/Dhhv4EepFeOy4rG003.webp",
                "https://indocontentaising.lol/data/923915/01/YBHnCVEgXdjszia/ycA0wHF3CUfuTvE004.webp",
                "https://aicontentwow.lol/data/923915/01/YBHnCVEgXdjszia/kejGVdcxV9twhl6005.webp",
                "https://contentkerewnrorai.lat/data/923915/01/YBHnCVEgXdjszia/jZA0iTaz09sFldQ006.webp",
                "https://imageainewgeneration.lol/data/923915/01/YBHnCVEgXdjszia/jiVSWEoD3m6TEEf007.webp",
                "https://himmga.lat/data/923915/01/YBHnCVEgXdjszia/GtAu6IiC2QKhfij008.webp"
            )
            chapterId.startsWith("curated-iruma-kun") -> listOf(
                "https://imageainewgeneration.lol/data/837206/1/njUwyjhN3fT6iv6/xrScUmkB8nQoEPF001.webp",
                "https://himmga.lat/data/837206/1/njUwyjhN3fT6iv6/njZ6cY7Ft8NHjoX002.webp",
                "https://gaimgame.pics/data/837206/1/njUwyjhN3fT6iv6/jvNySr94ySmylx3003.webp",
                "https://indocontentaising.lol/data/837206/1/njUwyjhN3fT6iv6/sfDtERl4Ge2ktre004.webp",
                "https://aicontentwow.lol/data/837206/1/njUwyjhN3fT6iv6/5qypYhdWHEkJcZl005.webp",
                "https://contentkerewnrorai.lat/data/837206/1/njUwyjhN3fT6iv6/ZwkgDSnX4L3G0Yk006.webp",
                "https://imageainewgeneration.lol/data/837206/1/njUwyjhN3fT6iv6/mQfgwfwBBP23C0U007.webp",
                "https://himmga.lat/data/837206/1/njUwyjhN3fT6iv6/8BuDuumHFEngkeV008.webp"
            )
            chapterId.startsWith("curated-kanojo-mo-kanojo") -> listOf(
                "https://imageainewgeneration.lol/data/381769/1/9Nin5Dbwujo2mpO/iHtKUA4D8O001.jpg",
                "https://himmga.lat/data/381769/1/9Nin5Dbwujo2mpO/UW0Eo162FW002.jpg",
                "https://gaimgame.pics/data/381769/1/9Nin5Dbwujo2mpO/cmJqUQqhFw003.jpg",
                "https://indocontentaising.lol/data/381769/1/9Nin5Dbwujo2mpO/IEIbtWMvQW004.jpg",
                "https://aicontentwow.lol/data/381769/1/9Nin5Dbwujo2mpO/mGNvWZ9H5h005.jpg",
                "https://contentkerewnrorai.lat/data/381769/1/9Nin5Dbwujo2mpO/g7yFLnr35f006.jpg",
                "https://imageainewgeneration.lol/data/381769/1/9Nin5Dbwujo2mpO/MdgcxEAzXZ007.jpg",
                "https://himmga.lat/data/381769/1/9Nin5Dbwujo2mpO/uoajetR30Y008.jpg"
            )
            chapterId.contains("naruto") -> listOf(
                "https://imageainewgeneration.lol/data/883347/1/JXskgNPbd66c451/KwA0zqWV4UP1PHB001.jpg",
                "https://himmga.lat/data/883347/1/JXskgNPbd66c451/SjysHtZuBrH4hDL002.jpg",
                "https://gaimgame.pics/data/883347/1/JXskgNPbd66c451/BdUSesPBNLGhNtB003.jpg",
                "https://indocontentaising.lol/data/883347/1/JXskgNPbd66c451/tPEZGeKrupPPyYQ004.jpg",
                "https://aicontentwow.lol/data/883347/1/JXskgNPbd66c451/RxbK5gsUzXzgAeD005.jpg",
                "https://contentkerewnrorai.lat/data/883347/1/JXskgNPbd66c451/K4yo39XHiKtxh9q006.jpg",
                "https://imageainewgeneration.lol/data/883347/1/JXskgNPbd66c451/a8INZoQuEVsDX4B007.jpg",
                "https://himmga.lat/data/883347/1/JXskgNPbd66c451/gXvKU5Tnqmc0EIJ008.jpg"
            )
            chapterId.contains("bleach") -> listOf(
                "https://imageainewgeneration.lol/data/410930/316/fWeQmFRfF11Jp4L/hxGRKz08atv5CU0001.png",
                "https://himmga.lat/data/410930/316/fWeQmFRfF11Jp4L/JDAFVZswIMsMpfK002.png",
                "https://gaimgame.pics/data/410930/316/fWeQmFRfF11Jp4L/bo0vDM9tMi6lWe4003.png",
                "https://indocontentaising.lol/data/410930/316/fWeQmFRfF11Jp4L/BY7EYiYuithIOxs004.png",
                "https://aicontentwow.lol/data/410930/316/fWeQmFRfF11Jp4L/KxiCj3D1CyjW3Sb005.png",
                "https://contentkerewnrorai.lat/data/410930/316/fWeQmFRfF11Jp4L/snieaIeil55KJxR006.png",
                "https://imageainewgeneration.lol/data/410930/316/fWeQmFRfF11Jp4L/GIasKN2QvHLM0fq007.png",
                "https://himmga.lat/data/410930/316/fWeQmFRfF11Jp4L/jbQkWapTFccSHFP008.png"
            )
            else -> listOf(
                "https://imageainewgeneration.lol/data/141782/1/MiAu7TLqd2IkY5l/vKnVXfCcCGWjmZU01.jpg",
                "https://himmga.lat/data/141782/1/MiAu7TLqd2IkY5l/ur3KxeiHvrhMeSi02.jpg",
                "https://gaimgame.pics/data/141782/1/MiAu7TLqd2IkY5l/q60ZOvFtBojsG5B03.jpg",
                "https://indocontentaising.lol/data/141782/1/MiAu7TLqd2IkY5l/O9Cs2RuZG5Pz7nw04.jpg"
            )
        }

        // Keamanan ketat: Filter URL iklan, judi, tracker, dan file non-gambar sebelum dirender
        val adKeywords = listOf(
            "slot", "judi", "zeus", "gacor", "bet", "casino", "pragmatic",
            "poker", "togel", "bonus", "deposit", "sbobet", "popunder",
            "histats", "statcounter", "disqus", "google-analytics", "doubleclick",
            "blogger.googleusercontent.com", "orangarab", "komikindo-e", "fav.png", "donate"
        )
        val cleanUrls = rawUrls.filter { url ->
            val lower = url.lowercase()
            !adKeywords.any { lower.contains(it) } && !lower.endsWith(".gif")
        }

        cleanUrls.mapIndexed { index, url ->
            ReaderPage(
                pageNumber = index + 1,
                imageUrl = url,
                width = 1200,
                height = 1800
            )
        }
    }
}
