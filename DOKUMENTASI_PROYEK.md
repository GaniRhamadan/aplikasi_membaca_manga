# Dokumentasi Arsitektur, Teknologi, dan Sumber API

Dokumen ini menjelaskan secara menyeluruh seluruh teknologi, pustaka (*library*), struktur folder, serta sumber API yang digunakan di dalam proyek **Aplikasi Membaca Manga (MangaVerse)**, baik untuk aplikasi Android (`app/`) maupun aplikasi Web (`web/`).

---

## 1. Struktur Folder Workspace

```text
aplikasi_membaca_manga/
├── app/                  # Proyek Aplikasi Android Asli (Native Jetpack Compose)
│   ├── build.gradle.kts  # Konfigurasi dependensi dan build Android
│   └── src/main/java/com/manga/reader/
│       ├── MainActivity.kt        # Titik masuk utama & navigasi rute aplikasi Android
│       ├── data/                  # Repository data, database Room, scraper Jsoup
│       ├── model/                 # Data class (Manga, Chapter, ReaderPage, dll.)
│       └── ui/
│           ├── screens/           # Tampilan antarmuka (Home, Detail, Reader, Search, Library, History)
│           └── theme/             # Tema Material 3, skema warna, dan tipografi
│
├── web/                  # Proyek Aplikasi Web & Reader Imersif (React/Vite SPA)
│   ├── index.html        # Struktur HTML utama, semantic view, toolbar, dan reader canvas
│   ├── vite.config.js    # Konfigurasi Vite + Middleware Backend Proxy & Anti-Iklan
│   ├── package.json      # Dependensi dan skrip build web
│   └── src/
│       ├── main.js       # Logika aplikasi web, pengendali rute, interaksi pembaca
│       ├── api.js        # Integrasi multi-sumber API (MangaDex, Komikindo, Komiku)
│       ├── state.js      # Pengelola state reaktif (Favorit, Riwayat, Preferensi)
│       ├── curatedData.js# Data kurasi manga populer Bahasa Indonesia siap baca
│       └── style.css     # Desain sistem kustom Vanilla CSS (Desain anti-slop modern)
│
├── gradle/               # Wrapper dan version catalog Gradle Android
├── build.gradle.kts      # Konfigurasi root build Gradle
├── DESIGN.md             # Panduan estetika desain dan prinsip antarmuka
└── DOKUMENTASI_PROYEK.md # Dokumen spesifikasi teknis dan API (berkas ini)
```

---

## 2. Aplikasi Mobile Android (`app/`)

Aplikasi Android dibangun dengan arsitektur modern berbasis **Kotlin** dan **Jetpack Compose** (tanpa XML Layout tradisional) yang mengikuti pola *Clean Architecture* dan *Unidirectional Data Flow (UDF)*.

### A. Teknologi & Dependensi Utama
| Kategori | Library / Framework | Fungsi & Alasan Penggunaan |
| :--- | :--- | :--- |
| **Bahasa & SDK** | Kotlin 2.0.20, Java 21 | Bahasa modern dengan fitur coroutines dan type-safety ketat. |
| **UI Framework** | Jetpack Compose (BOM) & Material 3 | Framework antarmuka deklaratif Android modern, ringan, dan responsif. |
| **Navigasi** | AndroidX Navigation Compose | Mengelola perpindahan layar (Home $\leftrightarrow$ Detail $\leftrightarrow$ Reader $\leftrightarrow$ Search). |
| **Image Loading** | Coil 3 (`coil3.compose`, `coil3.network.okhttp`) | Pustaka pemuat gambar modern berbasis Kotlin Coroutines untuk memuat lembaran bab manga secara cepat dan hemat memori. |
| **Penyimpanan Lokal** | Room Database (SQLite) & DataStore Preferences | Menyimpan daftar komik favorit offline dan preferensi pengguna. |
| **Jaringan & Scraper**| OkHttp 4 + Jsoup | Melakukan request HTTP dengan header khusus, mem-bypass anti-hotlinking, dan membedah HTML (*DOM scraping*) dari situs komik Indonesia. |
| **Dependency Injection**| Google Hilt / Dagger (KSP) | Manajemen injeksi dependensi antar modul (Repository, Context, Database). |
| **Asinkron & Reaktif** | Kotlin Coroutines & StateFlow | Menjalankan proses unduh data di background thread tanpa memblokir antarmuka pengguna. |

### B. Fitur & Komponen Layar (`ui/screens/`)
1. **`HomeScreen.kt`**: Menampilkan hero banner, sorotan komik terpopuler, filter kategori/genre, dan rilisan terbaru.
2. **`DetailScreen.kt`**: Menampilkan sinopsis lengkap, status (Tamat/Berjalan), rating, tombol simpan favorit, dan daftar seluruh chapter.
3. **`ReaderScreen.kt`**:
   * Mode ganda: **Webtoon Scroll** (vertikal menyambung mulus tanpa celah hitam) dan **Halaman** (horizontal swipe).
   * Fitur bebas celah hitam (*zero black void*): tidak ada pemaksaan tinggi gambar, gambar meregang penuh sesuai rasio aslinya.
   * Floating page indicator transparan minimalis di sudut layar.
   * Navigasi bab sebelumnya & berikutnya secara instan.
4. **`SearchScreen.kt`**: Pencarian instan manga dengan *zero-latency local feedback*, *in-memory cache*, timeout terisolasi, dan chip genre/sumber.
5. **`LibraryScreen.kt` & `HistoryScreen.kt`**: Pengelolaan komik yang disimpan dan riwayat membaca terakhir.

---

## 3. Aplikasi Web (`web/`)

Aplikasi web dirancang dengan pendekatan **Single Page Application (SPA)** berbasis standar web terbuka berperforma tinggi tanpa dependensi framework berat (*AI-slop free*).

### A. Teknologi & Dependensi
| Komponen | Teknologi | Keterangan |
| :--- | :--- | :--- |
| **Build Tool & Dev Server** | Vite 8.3.2 | Bundler ultra cepat dengan Hot Module Replacement (HMR). |
| **Struktur Antarmuka** | HTML5 Semantik | Tag semantik murni (`<header>`, `<nav>`, `<section>`, `<select>`, `<button>`) ramah SEO dan aksesibilitas keyboard. |
| **Logika & State** | Vanilla Modern JavaScript (ES2022+) | Menggunakan module ES6, tanpa library state kompleks; state dikelola reaktif di `state.js` via `localStorage`. |
| **Styling & Desain** | Vanilla CSS (`style.css`) | Menggunakan CSS Custom Properties (Design Tokens), rasio poster 2:3, tipografi Google Fonts (*Outfit* & *Plus Jakarta Sans*), dan palet warna dark obsidian (`#0D0E12`, `#161922`). |

### B. Modul Kode Web
* **[main.js](file:///home/gani/aplikasi_membaca_manga/web/src/main.js)**: Pengendali utama event DOM, perutean tab (Beranda, Jelajah, Detail, Reader, Favorit, Riwayat), pemilih chapter interaktif (`readerChapterSelect`), pencarian progresif anti-race condition, dan penanganan keyboard (panah kiri/kanan, Esc).
* **[api.js](file:///home/gani/aplikasi_membaca_manga/web/src/api.js)**: Logika konsumsi data multi-sumber secara paralel (`Promise.allSettled`), fast timeout, skor relevansi pencarian (*relevance scoring*), deduplikasi chapter, serta caching in-memory via `Map()`.
* **[curatedData.js](file:///home/gani/aplikasi_membaca_manga/web/src/curatedData.js)**: Database lokal komik pilihan populer (Solo Leveling, One Piece, Jujutsu Kaisen, Chainsaw Man, dll.) yang berisi pemetaan ratusan bab siap baca.
* **[vite.config.js](file:///home/gani/aplikasi_membaca_manga/web/vite.config.js)**: Server middleware proxy lokal yang mem-bypass masalah CORS pada browser dan memblokir konten iklan.

---

## 4. Sumber API dan Cara Mendapatkannya

Aplikasi ini menggunakan **kombinasi 3 sumber data utama** untuk memastikan koleksi manga lengkap, tersedia dalam Bahasa Indonesia, dan dapat dibaca tanpa gangguan iklan:

```mermaid
graph TD
    Client[Klien Web / Android] --> Router{Permintaan Data}
    
    Router -->|Pencarian Global & Metadata Internasional| MangaDex[MangaDex Official API]
    Router -->|Chapter Komikindo Bahasa Indonesia| Proxy1[/api/chapter-pages & /api/manga-chapters/]
    Router -->|Chapter Komiku.org Bahasa Indonesia| Proxy2[/api/komiku-chapters & /api/search-komiku/]
    
    Proxy1 --> KomikindoSite[Komikindo.ch CDN]
    Proxy2 --> KomikuSite[Komiku.org CDN]
    
    Client -->|Proxy Gambar Anti-Iklan| ImageProxy[/api/proxy-image?url=...]
    ImageProxy --> TargetCDN[Server Gambar Asli dengan Header Spoofing]
```

### A. Sumber 1: MangaDex Official REST API
* **Penyedia**: [MangaDex.org](https://mangadex.org/)
* **Akses**: Publik & Resmi (Bebas biaya, tidak memerlukan API Key untuk konsumsi standar).
* **Base URL**: `https://api.mangadex.org`
* **CDN Gambar**: `https://uploads.mangadex.org`
* **Endpoint yang Digunakan**:
  1. **Pencarian Komik**:  
     `GET https://api.mangadex.org/manga?limit=24&title={query}&includes[]=cover_art&includes[]=author`  
     *Fungsi:* Mengambil judul asli, judul alternatif, pengarang, deskripsi, genre/tags, dan nama berkas cover.
  2. **Daftar Bab / Feed Chapter**:  
     `GET https://api.mangadex.org/manga/{mangaId}/feed?limit=100&translatedLanguage[]=id&order[chapter]=asc`  
     *Fungsi:* Mengambil daftar UUID chapter terjemahan Bahasa Indonesia.
  3. **Server Pengambilan Gambar At-Home**:  
     `GET https://api.mangadex.org/at-home/server/{chapterUUID}`  
     *Fungsi:* Mengembalikan URL basis host, hash bab, dan nama-nama file halaman komik resmi.
  4. **Cover Gambar Manga**:  
     `GET https://uploads.mangadex.org/covers/{mangaId}/{coverFileName}.512.jpg`

---

### B. Sumber 2: Scraper Komikindo (Bahasa Indonesia)
* **Penyedia**: [Komikindo.ch](https://komikindo.ch/)
* **Akses**: Diambil melalui proxy lokal (`vite.config.js` di web dan OkHttp/Jsoup di Android) dengan meniru *User-Agent* peramban dan *Referer*.
* **Alasan Penggunaan**: Menyediakan terjemahan Bahasa Indonesia terlengkap untuk manga/manhwa populer (seperti *Solo Leveling*, *One Piece*, *Jujutsu Kaisen*).
* **Endpoint Lokal / Jalur Scraper**:
  1. **Pencarian Judul**:  
     `GET /api/search-komikindo?q={kata_kunci}`  
     *Sumber asal:* `https://komikindo.ch/?s={query}`  
     *Cara kerja:* Membedah elemen HTML `<div class="animepost">` untuk mengambil tautan slug komik, cover, dan judul.
  2. **Daftar Semua Chapter**:  
     `GET /api/manga-chapters?slug={manga_slug}`  
     *Sumber asal:* `https://komikindo.ch/komik/{slug}/`  
     *Cara kerja:* Mencari pola regex link chapter `<a href="...chapter-{nomor}...">` dan mengurutkannya dari bab terkecil ke bab terbesar.
  3. **Pengambilan Lembaran Gambar**:  
     `GET /api/chapter-pages?url={chapter_url}`  
     *Sumber asal:* Halaman baca komik (contoh: `https://komikindo.ch/solo-leveling-chapter-2/`)  
     *Cara kerja:* Mengekstrak container `#Baca_Komik` atau `#readerarea` dan mengumpulkan semua tautan gambar `/data/{id}/{chapter}/...`.

---

### C. Sumber 3: Scraper Komiku.org (Bahasa Indonesia)
* **Penyedia**: [Komiku.org](https://komiku.org/)
* **Akses**: Diambil melalui proxy scraper internal untuk komik-komik rilisan webtoon dan manga alternatif.
* **Endpoint Lokal / Jalur Scraper**:
  1. **Pencarian Komiku**:  
     `GET /api/search-komiku?q={kata_kunci}`  
     *Sumber asal:* `https://api.komiku.org/?post_type=manga&s={query}`
  2. **Daftar Chapter Komiku**:  
     `GET /api/komiku-chapters?slug={manga_slug}`  
     *Sumber asal:* `https://komiku.org/manga/{slug}/`
  3. **Halaman Gambar Komiku**:  
     Diproses otomatis oleh `/api/chapter-pages?url={url}` yang mengenali struktur gambar server `image.komiku.to`.

---

### D. Fitur Keamanan: Image Proxy & Anti-Iklan Ketat
* **Endpoint**: `GET /api/proxy-image?url={encoded_image_url}`
* **Lokasi Kode**: [vite.config.js](file:///home/gani/aplikasi_membaca_manga/web/vite.config.js#L23-L77)
* **Masalah yang Diatasi**:
  1. **Anti-Hotlinking / 403 Forbidden**: Banyak server komik menolak gambar jika `Referer` bukan berasal dari situs mereka. Proxy ini secara otomatis menyematkan `Referer: https://komikindo.ch/` atau `https://mangadex.org/`.
  2. **Pemblokir Iklan & Judi Slot**: URL gambar diperiksa secara ketat. Semua tautan yang mengandung kata kunci judi atau pelacak (`slot`, `judi`, `zeus`, `gacor`, `casino`, `popunder`, `adsterra`, `.gif`, banner promosi) akan **langsung ditolak (HTTP 403)** sehingga pembaca terlindungi 100% dari iklan mengganggu.
  3. **Penyimpanan Cache Cepat**: Menggunakan header `Cache-Control: public, max-age=86400` untuk mengurangi beban kuota pengguna dan mempercepat pembacaan ulang.

---

## 5. Ringkasan Perintah Menjalankan Aplikasi

1. **Menjalankan Web Reader (Localhost:5173)**:
   ```bash
   cd web
   npm install      # Pasang dependensi jika belum
   npm run dev      # Jalankan server pengembangan Vite
   ```

2. **Membangun Bundle Web untuk Produksi**:
   ```bash
   cd web
   npm run build
   ```

3. **Mengompilasi Aplikasi Android**:
   ```bash
   ./gradlew compileDebugKotlin    # Kompilasi kode sumber Kotlin
   ./gradlew assembleDebug         # Membuat berkas APK debug
   ```
