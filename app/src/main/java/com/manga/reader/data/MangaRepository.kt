package com.manga.reader.data

import com.manga.reader.model.Chapter
import com.manga.reader.model.Manga
import com.manga.reader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaRepository @Inject constructor() {

    // Featured and popular catalog
    suspend fun getPopularManga(): List<Manga> = withContext(Dispatchers.IO) {
        listOf(
            Manga(
                id = "one-piece",
                title = "One Piece",
                altTitles = listOf("Wan Pisu", "OP"),
                coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&q=80",
                author = "Eiichiro Oda",
                status = "Ongoing",
                synopsis = "Gol D. Roger dikenal sebagai Raja Bajak Laut. Sebelum dieksekusi, dia mengumumkan bahwa hartanya, One Piece, disembunyikan di Grand Line. Monkey D. Luffy memulai petualangan untuk menjadi Raja Bajak Laut berikutnya.",
                genres = listOf("Action", "Adventure", "Fantasy", "Shounen"),
                rating = 4.9f,
                releaseYear = "1997",
                totalChapters = 1130,
                source = "MangaDex",
                hasIndonesian = true
            ),
            Manga(
                id = "jujutsu-kaisen",
                title = "Jujutsu Kaisen",
                altTitles = listOf("JJK", "Sorcery Fight"),
                coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&q=80",
                author = "Gege Akutami",
                status = "Completed",
                synopsis = "Yuji Itadori adalah siswa SMA dengan kekuatan fisik luar biasa yang menelan jari Sukuna, Raja Kutukan, untuk menyelamatkan temannya. Kini ia harus mengumpulkan sisa jarinya di Sekolah Jujutsu Tokyo.",
                genres = listOf("Action", "Supernatural", "School", "Dark Fantasy"),
                rating = 4.8f,
                releaseYear = "2018",
                totalChapters = 271,
                source = "MangaDex",
                hasIndonesian = true
            ),
            Manga(
                id = "solo-leveling",
                title = "Solo Leveling",
                altTitles = listOf("Na Honjaman Rebeleop", "Only I Level Up"),
                coverUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=500&q=80",
                author = "Chugong, DUBU",
                status = "Completed",
                synopsis = "Sung Jin-woo adalah Hunter terlemah peringkat E. Terjebak di Double Dungeon yang mematikan, dia menerima Quest rahasia misterius yang memungkinkannya naik level tanpa batas.",
                genres = listOf("Action", "Fantasy", "Manhwa", "Supernatural"),
                rating = 4.9f,
                releaseYear = "2018",
                totalChapters = 200,
                source = "Komikindo",
                hasIndonesian = true
            ),
            Manga(
                id = "chainsaw-man",
                title = "Chainsaw Man",
                altTitles = listOf("CSM", "Denji"),
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&q=80",
                author = "Tatsuki Fujimoto",
                status = "Ongoing",
                synopsis = "Denji hidup dalam kemiskinan ekstrem membayar hutang ayahnya dengan memburu iblis bersama Pochita, Iblis Gergaji. Setelah dibunuh, Pochita bergabung dengan Denji menciptakan Manusia Gergaji.",
                genres = listOf("Action", "Horror", "Gore", "Supernatural"),
                rating = 4.7f,
                releaseYear = "2018",
                totalChapters = 180,
                source = "MangaDex",
                hasIndonesian = true
            ),
            Manga(
                id = "naruto",
                title = "Naruto",
                altTitles = listOf("Naruto Shippuden"),
                coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
                author = "Masashi Kishimoto",
                status = "Completed",
                synopsis = "Naruto Uzumaki, seorang ninja yatim piatu yang menyimpan Rubah Ekor Sembilan dalam tubuhnya, berjuang untuk diakui oleh penduduk desa dan bercita-cita menjadi Hokage.",
                genres = listOf("Action", "Adventure", "Martial Arts", "Shounen"),
                rating = 4.8f,
                releaseYear = "1999",
                totalChapters = 700,
                source = "MangaDex",
                hasIndonesian = true
            ),
            Manga(
                id = "bleach",
                title = "Bleach",
                altTitles = listOf("Bleach Thousand-Year Blood War"),
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
                author = "Tite Kubo",
                status = "Completed",
                synopsis = "Ichigo Kurosaki memperoleh kekuatan Shinigami dari Rukia Kuchiki dan bertanggung jawab melindungi manusia dari roh jahat Hollow dan memandu jiwa ke Soul Society.",
                genres = listOf("Action", "Supernatural", "Adventure"),
                rating = 4.7f,
                releaseYear = "2001",
                totalChapters = 686,
                source = "MangaDex",
                hasIndonesian = true
            )
        )
    }

    suspend fun searchManga(query: String): List<Manga> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList()
        val all = getPopularManga()
        all.filter {
            it.title.lowercase().contains(q) ||
            it.altTitles.any { alt -> alt.lowercase().contains(q) } ||
            it.author.lowercase().contains(q) ||
            it.genres.any { g -> g.lowercase().contains(q) }
        }
    }

    suspend fun getMangaById(id: String): Manga? = withContext(Dispatchers.IO) {
        getPopularManga().find { it.id == id }
    }

    suspend fun getChapters(mangaId: String, preferredLang: String = "id"): List<Chapter> = withContext(Dispatchers.IO) {
        val total = when (mangaId) {
            "one-piece" -> 20
            "jujutsu-kaisen" -> 25
            "solo-leveling" -> 20
            "chainsaw-man" -> 15
            "naruto" -> 25
            "bleach" -> 20
            else -> 10
        }
        (1..total).map { num ->
            Chapter(
                id = "$mangaId-ch-$num",
                mangaId = mangaId,
                chapterNumber = num.toString(),
                title = "Chapter $num",
                language = preferredLang,
                scanlationGroup = if (preferredLang == "id") "KomikIndo / MangaDex ID" else "Official / MangaSee",
                releaseDate = "2024",
                source = "MangaDex"
            )
        }.reversed()
    }

    suspend fun getChapterPages(chapterId: String): List<ReaderPage> = withContext(Dispatchers.IO) {
        // High quality preview pages
        val sampleImages = listOf(
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1000&q=85",
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1000&q=85",
            "https://images.unsplash.com/photo-1563089145-599997674d42?w=1000&q=85",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=85"
        )
        (1..12).map { index ->
            ReaderPage(
                pageNumber = index,
                imageUrl = sampleImages[index % sampleImages.size],
                width = 1200,
                height = 1800
            )
        }
    }
}
