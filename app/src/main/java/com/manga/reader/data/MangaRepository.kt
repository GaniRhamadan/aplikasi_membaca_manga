package com.manga.reader.data

import com.manga.reader.data.network.KomikindoService
import com.manga.reader.data.network.MangaDexService
import com.manga.reader.model.Chapter
import com.manga.reader.model.HistoryItem
import com.manga.reader.model.Manga
import com.manga.reader.model.ReaderPage
import android.content.Context
import com.manga.reader.MangaReaderApp
import com.manga.reader.data.local.AppDatabase
import com.manga.reader.data.local.entity.FavoriteEntity
import com.manga.reader.data.local.entity.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaRepository @Inject constructor(
    private val mangaDexService: MangaDexService = MangaDexService(),
    private val komikindoService: KomikindoService = KomikindoService(),
    context: Context? = MangaReaderApp.instanceOrNull
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database: AppDatabase? = context?.let { AppDatabase.getInstance(it) }
    private val favoriteDao = database?.favoriteDao()
    private val historyDao = database?.historyDao()

    private val dynamicMangaCache = ConcurrentHashMap<String, Manga>()
    private val dynamicChapterCache = ConcurrentHashMap<String, Chapter>()
    private val mangaChaptersCache = ConcurrentHashMap<String, List<Chapter>>()
    private val searchCache = ConcurrentHashMap<String, List<Manga>>()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _readingHistory = MutableStateFlow<List<HistoryItem>>(emptyList())
    val readingHistory: StateFlow<List<HistoryItem>> = _readingHistory.asStateFlow()

    init {
        favoriteDao?.let { dao ->
            repositoryScope.launch {
                dao.getAllFavorites().collect { favList ->
                    _favoriteIds.value = favList.map { it.mangaId }.toSet()
                }
            }
        }

        historyDao?.let { dao ->
            repositoryScope.launch {
                dao.getAllHistory().collect { historyList ->
                    _readingHistory.value = historyList.map { it.toHistoryItem() }
                }
            }
        }
    }

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
        ),
        Manga(
            id = "curated-kaguya",
            title = "Kaguya-sama: Love Is War",
            altTitles = listOf("Kaguya-sama wa Kokurasetai", "Tensai-tachi no Ren'ai Zunousen"),
            coverUrl = "https://uploads.mangadex.org/covers/37f5cce0-8070-4ada-96e5-fa24b1bd4ff9/e21ca520-5054-4041-a07e-de8b7c683522.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/37f5cce0-8070-4ada-96e5-fa24b1bd4ff9/e21ca520-5054-4041-a07e-de8b7c683522.jpg.512.jpg",
            author = "Aka Akasaka",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Kaguya Shinomiya dan Miyuki Shirogane adalah dua elit jenius di puncak dewan murid. Saling jatuh cinta namun terhalang oleh harga diri yang tinggi, mereka bersumpah untuk membuat yang lain menyatakan cinta terlebih dahulu dalam perang akal dan cinta!",
            genres = listOf("Comedy", "Romance", "School Life", "Psychological", "Seinen"),
            rating = 9.3f,
            releaseYear = "2015",
            totalChapters = 281,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-horimiya",
            title = "Horimiya",
            altTitles = listOf("Hori-san to Miyamura-kun"),
            coverUrl = "https://uploads.mangadex.org/covers/a25e46ec-30f7-4db6-89df-cacbc1d9a900/04feed00-d919-4c72-b355-15b37555eb1e.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/a25e46ec-30f7-4db6-89df-cacbc1d9a900/04feed00-d919-4c72-b355-15b37555eb1e.jpg.512.jpg",
            author = "HERO, Daisuke Hagiwara",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Kyouko Hori yang populer di sekolah ternyata mengurus adik dan rumah tangga, sedangkan Izumi Miyamura yang pendiam ternyata bertato dan bertindik di luar sekolah. Keduanya menemukan kenyamanan setelah saling mengetahui rahasia masing-masing.",
            genres = listOf("Romance", "Comedy", "School Life", "Slice of Life", "Shounen"),
            rating = 9.1f,
            releaseYear = "2011",
            totalChapters = 122,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-oshi-no-ko",
            title = "Oshi no Ko",
            altTitles = listOf("Anak Idola", "My Star"),
            coverUrl = "https://uploads.mangadex.org/covers/fa66c641-113f-4ae4-a83a-7dd77a7a10f8/3f9eb46a-74ae-48d0-be95-0b2f9009fdf3.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/fa66c641-113f-4ae4-a83a-7dd77a7a10f8/3f9eb46a-74ae-48d0-be95-0b2f9009fdf3.jpg.512.jpg",
            author = "Aka Akasaka, Mengo Yokoyari",
            status = "Completed",
            statusId = "Tamat",
            synopsis = "Dokter Gorou bereinkarnasi sebagai Aqua, putra kembar idola legendaris Ai Hoshino. Demi membalaskan dendam kematian tragis sang ibu, Aqua terjun ke dalam kegelapan dunia hiburan Jepang.",
            genres = listOf("Drama", "Mystery", "Supernatural", "Psychological", "Seinen"),
            rating = 9.4f,
            releaseYear = "2020",
            totalChapters = 166,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-mushoku-tensei",
            title = "Mushoku Tensei: Isekai Ittara Honki Dasu",
            altTitles = listOf("Jobless Reincarnation"),
            coverUrl = "https://uploads.mangadex.org/covers/38c6f934-7558-4d1b-80d1-91114e0419eb/7d8da1d7-5483-4550-aa44-9c57b7e9eb90.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/38c6f934-7558-4d1b-80d1-91114e0419eb/7d8da1d7-5483-4550-aa44-9c57b7e9eb90.jpg.512.jpg",
            author = "Rifujin na Magonote, Fujikawa Yuka",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Seorang pria pengangguran berusia 34 tahun bereinkarnasi ke dunia sihir sebagai Rudeus Greyrat. Bertekad tak mengulangi penyesalan masa lalunya, Rudeus berjanji menjalani hidup barunya secara sungguh-sungguh.",
            genres = listOf("Action", "Adventure", "Drama", "Fantasy", "Isekai", "Romance"),
            rating = 9.3f,
            releaseYear = "2014",
            totalChapters = 105,
            source = "MangaDex",
            type = "Manga",
            hasIndonesian = true
        ),
        Manga(
            id = "curated-conan",
            title = "Detective Conan",
            altTitles = listOf("Meitantei Conan", "Case Closed"),
            coverUrl = "https://uploads.mangadex.org/covers/7f30dfc3-0b80-4dcc-a3b9-0cd746fac005/e67075b1-7ffc-4fe8-abd1-c8f3b32c28eb.jpg.512.jpg",
            bannerUrl = "https://uploads.mangadex.org/covers/7f30dfc3-0b80-4dcc-a3b9-0cd746fac005/e67075b1-7ffc-4fe8-abd1-c8f3b32c28eb.jpg.512.jpg",
            author = "Gosho Aoyama",
            status = "Ongoing",
            statusId = "Berjalan",
            synopsis = "Shinichi Kudo, detektif SMA jenius, diracuni oleh Organisasi Hitam hingga tubuhnya menyusut menjadi anak SD. Dengan nama samaran Conan Edogawa, ia memecahkan berbagai kasus rumit sembari memburu organisasi tersebut.",
            genres = listOf("Mystery", "Comedy", "Shounen", "Drama"),
            rating = 9.5f,
            releaseYear = "1994",
            totalChapters = 1130,
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
        listOf(
            "Semua",
            "Action",
            "Fantasy",
            "Adventure",
            "Romance",
            "Comedy",
            "Isekai",
            "Slice of Life",
            "Drama",
            "Supernatural",
            "Mystery",
            "Horror",
            "Sci-Fi",
            "Psychological",
            "Thriller",
            "Martial Arts",
            "School Life",
            "Sports",
            "Historical",
            "Shounen",
            "Seinen",
            "Shoujo"
        )
    }

    fun searchLocalManga(
        query: String = "",
        filterGenres: Set<String> = emptySet(),
        filterSource: String = "all"
    ): List<Manga> {
        val q = query.trim()
        val qLower = q.lowercase()
        val activeGenres = filterGenres.filter { !it.equals("Semua", ignoreCase = true) }

        val pool = (allMangaList + dynamicMangaCache.values).distinctBy { it.id }

        var results = if (q.isEmpty()) {
            pool
        } else {
            pool.filter { m ->
                m.title.lowercase().contains(qLower) ||
                m.altTitles.any { it.lowercase().contains(qLower) } ||
                m.author.lowercase().contains(qLower) ||
                m.genres.any { it.lowercase().contains(qLower) }
            }
        }

        if (activeGenres.isNotEmpty()) {
            results = results.filter { m ->
                activeGenres.any { sel -> m.genres.any { it.equals(sel, ignoreCase = true) } }
            }
        }

        if (filterSource != "all") {
            results = results.filter { m ->
                if (filterSource == "curated") m.id.startsWith("curated-")
                else m.source.lowercase().contains(filterSource.lowercase())
            }
        }
        return results
    }

    fun searchLocalManga(
        query: String,
        filterGenre: String,
        filterSource: String = "all"
    ): List<Manga> {
        val genres = if (filterGenre == "Semua") emptySet() else setOf(filterGenre)
        return searchLocalManga(query, genres, filterSource)
    }

    suspend fun searchManga(
        query: String = "",
        filterGenres: Set<String> = emptySet(),
        filterSource: String = "all"
    ): List<Manga> = withContext(Dispatchers.IO) {
        val q = query.trim()
        val qLower = q.lowercase()
        val activeGenres = filterGenres.filter { !it.equals("Semua", ignoreCase = true) }.toSet()

        val sortedGenresKey = activeGenres.sorted().joinToString(",")
        val cacheKey = "${qLower}_[${sortedGenresKey}]_${filterSource}"
        searchCache[cacheKey]?.let { return@withContext it }

        val localMatches = searchLocalManga(query, activeGenres, filterSource)

        if (filterSource == "curated") {
            searchCache[cacheKey] = localMatches
            return@withContext localMatches
        }

        // If both query and activeGenres are empty, return local curated matches
        if (q.isEmpty() && activeGenres.isEmpty()) {
            return@withContext localMatches
        }

        val mdDeferred: Deferred<List<Manga>>? = if (filterSource == "all" || filterSource == "mangadex") {
            async {
                try {
                    withTimeoutOrNull(10000L) {
                        mangaDexService.searchMangaWithFilters(
                            query = q,
                            genres = activeGenres,
                            limit = 40
                        )
                    } ?: emptyList<Manga>()
                } catch (_: Exception) {
                    emptyList<Manga>()
                }
            }
        } else null

        val komikindoDeferred: Deferred<List<Manga>>? = if ((filterSource == "all" || filterSource == "komikindo") && q.isNotEmpty()) {
            async {
                try {
                    withTimeoutOrNull(4000L) { komikindoService.searchManga(q) } ?: emptyList<Manga>()
                } catch (_: Exception) {
                    emptyList<Manga>()
                }
            }
        } else null

        val mdResults = mdDeferred?.await() ?: emptyList()
        val komikindoResults = komikindoDeferred?.await() ?: emptyList()

        mdResults.forEach { dynamicMangaCache[it.id] = it }
        komikindoResults.forEach { dynamicMangaCache[it.id] = it }

        val filteredKomikindo = if (activeGenres.isNotEmpty()) {
            komikindoResults.filter { m ->
                activeGenres.any { sel -> m.genres.any { it.equals(sel, ignoreCase = true) } }
            }
        } else komikindoResults

        val candidateList = when (filterSource) {
            "mangadex" -> mdResults
            "komikindo" -> filteredKomikindo
            else -> localMatches + mdResults + filteredKomikindo
        }

        val seen = HashSet<String>()
        val finalResults = mutableListOf<Manga>()

        for (m in candidateList) {
            val key = m.title.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            if (key.isNotEmpty() && seen.add(key)) {
                if (activeGenres.isEmpty() || m.source == "MangaDex" || activeGenres.any { sel -> m.genres.any { it.equals(sel, ignoreCase = true) } }) {
                    finalResults.add(m)
                }
            }
        }

        if (searchCache.size > 50) {
            searchCache.clear()
        }
        searchCache[cacheKey] = finalResults

        finalResults
    }

    suspend fun searchManga(
        query: String,
        filterGenre: String,
        filterSource: String = "all"
    ): List<Manga> {
        val genres = if (filterGenre == "Semua") emptySet() else setOf(filterGenre)
        return searchManga(query, genres, filterSource)
    }

    suspend fun getMangaById(id: String): Manga? = withContext(Dispatchers.IO) {
        allMangaList.find { it.id == id }
            ?: dynamicMangaCache[id]
            ?: if (id.startsWith("md-")) {
                mangaDexService.getMangaDetails(id)?.also { dynamicMangaCache[it.id] = it }
            } else if (id.startsWith("komikindo-")) {
                komikindoService.getMangaDetails(id.removePrefix("komikindo-"))?.also { dynamicMangaCache[it.id] = it }
            } else null
    }

    suspend fun getFavoriteManga(): List<Manga> = withContext(Dispatchers.IO) {
        val dbFavorites = favoriteDao?.getAllFavorites()?.firstOrNull() ?: emptyList()
        if (dbFavorites.isNotEmpty()) {
            return@withContext dbFavorites.map { it.toManga() }
        }
        val favs = _favoriteIds.value
        val allAvailable = allMangaList + dynamicMangaCache.values
        allAvailable.filter { favs.contains(it.id) }.distinctBy { it.id }
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(id: String, mangaObj: Manga? = null) {
        val current = _favoriteIds.value.toMutableSet()
        val isFav = current.contains(id)
        if (isFav) {
            current.remove(id)
            _favoriteIds.value = current
            repositoryScope.launch {
                favoriteDao?.deleteFavoriteById(id)
            }
        } else {
            current.add(id)
            _favoriteIds.value = current
            repositoryScope.launch {
                val manga = mangaObj
                    ?: allMangaList.find { it.id == id }
                    ?: dynamicMangaCache[id]
                    ?: getMangaById(id)

                if (manga != null) {
                    favoriteDao?.insertFavorite(FavoriteEntity.fromManga(manga))
                } else {
                    favoriteDao?.insertFavorite(
                        FavoriteEntity(
                            mangaId = id,
                            title = id,
                            coverUrl = "",
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun recordHistory(mangaId: String, chapterId: String, chapterTitle: String, pageIndex: Int = 0) {
        val resolvedMangaId = when {
            mangaId.isNotBlank() && mangaId != "komikindo" && mangaId != "md" -> mangaId
            else -> findMangaIdForChapter(chapterId).ifBlank { mangaId }
        }
        val manga = allMangaList.find { it.id == resolvedMangaId } ?: dynamicMangaCache[resolvedMangaId]
        val title = manga?.title ?: (if (resolvedMangaId.isNotBlank()) resolvedMangaId else "Manga")
        val cover = manga?.coverUrl ?: ""

        val historyItem = HistoryItem(
            mangaId = resolvedMangaId,
            mangaTitle = title,
            coverUrl = cover,
            lastChapterId = chapterId,
            lastChapterTitle = chapterTitle
        )

        val existing = _readingHistory.value.toMutableList()
        existing.removeAll { it.mangaId == resolvedMangaId }
        existing.add(0, historyItem)
        _readingHistory.value = existing

        repositoryScope.launch {
            historyDao?.insertOrUpdateHistory(
                HistoryEntity.fromHistoryItem(historyItem, pageIndex)
            )
        }
    }

    fun clearHistory() {
        _readingHistory.value = emptyList()
        repositoryScope.launch {
            historyDao?.clearAllHistory()
        }
    }

    fun getChapter(chapterId: String): Chapter? = dynamicChapterCache[chapterId]

    fun getCachedChapters(mangaId: String): List<Chapter>? = mangaChaptersCache[mangaId]

    fun findMangaIdForChapter(chapterId: String): String {
        dynamicChapterCache[chapterId]?.mangaId?.let { if (it.isNotBlank()) return it }
        _readingHistory.value.firstOrNull { it.lastChapterId == chapterId }?.mangaId?.let { if (it.isNotBlank()) return it }
        for ((mId, chapters) in mangaChaptersCache) {
            if (chapters.any { it.id == chapterId }) return mId
        }
        if (chapterId.startsWith("curated-") && chapterId.contains("-ch-")) {
            return chapterId.substringBefore("-ch-")
        }
        return ""
    }

    suspend fun getChapters(mangaId: String, preferredLang: String = "id", forceRefresh: Boolean = false): List<Chapter> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = mangaChaptersCache[mangaId]
            if (cached != null && cached.isNotEmpty()) {
                return@withContext cached
            }
        }

        if (mangaId.startsWith("md-")) {
            val list = mangaDexService.getChapters(mangaId, preferredLang)
            list.forEach { dynamicChapterCache[it.id] = it }
            mangaChaptersCache[mangaId] = list
            return@withContext list
        }

        if (mangaId.startsWith("komikindo-")) {
            val list = komikindoService.getChapters(mangaId.removePrefix("komikindo-"))
            list.forEach { dynamicChapterCache[it.id] = it }
            mangaChaptersCache[mangaId] = list
            return@withContext list
        }

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

        val list = (1..total).map { num ->
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

        list.forEach { dynamicChapterCache[it.id] = it }
        mangaChaptersCache[mangaId] = list
        list
    }

    fun getChapterTitle(chapterId: String): String {
        dynamicChapterCache[chapterId]?.let { return it.title }
        if (chapterId.contains("-ch-")) {
            val num = chapterId.substringAfterLast("-ch-")
            if (num.isNotBlank()) return "Chapter $num"
        }
        return chapterId
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
        val cleanChapterId = chapterId.trim()
        val decodedChapterId = try { java.net.URLDecoder.decode(cleanChapterId, "UTF-8") } catch (_: Exception) { cleanChapterId }

        if (cleanChapterId.startsWith("md-ch-") || decodedChapterId.startsWith("md-ch-")) {
            val uuid = (if (cleanChapterId.startsWith("md-ch-")) cleanChapterId else decodedChapterId).removePrefix("md-ch-")
            val pages = mangaDexService.getChapterPages(uuid)
            if (pages.isNotEmpty()) return@withContext pages
        }

        if (cleanChapterId.startsWith("komikindo-ch-") || decodedChapterId.startsWith("komikindo-ch-")) {
            val raw = (if (cleanChapterId.startsWith("komikindo-ch-")) cleanChapterId else decodedChapterId).removePrefix("komikindo-ch-")
            val pages = komikindoService.getChapterPages(raw)
            if (pages.isNotEmpty()) return@withContext pages
        }

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
