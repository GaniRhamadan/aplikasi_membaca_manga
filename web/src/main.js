import { CURATED_MANGAS } from "./curatedData.js";
import { getPopularManga, searchManga, getMangaChapters, getChapterPages } from "./api.js";
import { state } from "./state.js";

// ==========================================================================
// Inisialisasi & Setup Simulator
// ==========================================================================

const GENRES = ["Semua", "Action", "Fantasy", "Adventure", "Comedy", "Shounen", "Supernatural", "Drama", "Horror"];

// Referensi DOM elemen penting
const elements = {
  body: document.body,
  simBar: document.getElementById("simulator-bar"),
  btnModePhone: document.getElementById("btn-mode-phone"),
  btnModeDesktop: document.getElementById("btn-mode-desktop"),
  btnRotatePhone: document.getElementById("btn-rotate-phone"),
  btnScalePhone: document.getElementById("btn-scale-phone"),
  scaleLabel: document.getElementById("scale-label"),
  btnThemeToggle: document.getElementById("btn-theme-toggle"),
  themeIcon: document.getElementById("theme-icon"),
  statusBarClock: document.getElementById("status-bar-clock"),

  // Header & Navigasi Aplikasi
  appHeader: document.getElementById("app-header"),
  btnBackNav: document.getElementById("btn-back-nav"),
  btnHeaderHome: document.getElementById("btn-header-home"),
  headerBrand: document.getElementById("header-brand"),
  btnQuickSearch: document.getElementById("btn-quick-search"),
  btnHeaderFav: document.getElementById("btn-header-fav"),
  bottomNav: document.getElementById("app-bottom-nav"),
  navTabs: document.querySelectorAll(".nav-tab"),
  androidHomeBar: document.getElementById("android-home-bar"),

  // Sub-view Back to Home Buttons
  btnExploreBackHome: document.getElementById("btn-explore-back-home"),
  btnDetailBackHome: document.getElementById("btn-detail-back-home"),
  btnLibraryBackHome: document.getElementById("btn-library-back-home"),
  btnHistoryBackHome: document.getElementById("btn-history-back-home"),
  btnReaderHome: document.getElementById("btn-reader-home"),

  // Views
  viewHome: document.getElementById("view-home"),
  viewExplore: document.getElementById("view-explore"),
  viewDetail: document.getElementById("view-detail"),
  viewReader: document.getElementById("view-reader"),
  viewLibrary: document.getElementById("view-library"),
  viewHistory: document.getElementById("view-history"),

  // Home Components
  homeHero: document.getElementById("home-hero"),
  heroTitle: document.getElementById("hero-title"),
  heroSynopsis: document.getElementById("hero-synopsis"),
  btnHeroRead: document.getElementById("btn-hero-read"),
  btnHeroDetail: document.getElementById("btn-hero-detail"),
  genrePillsContainer: document.getElementById("genre-pills-container"),
  popularMangaGrid: document.getElementById("popular-manga-grid"),
  latestMangaGrid: document.getElementById("latest-manga-grid"),
  continueSection: document.getElementById("home-continue-section"),
  continueCardContainer: document.getElementById("continue-card-container"),
  btnSeeMorePopular: document.getElementById("btn-see-more-popular"),

  // Explore Components
  searchInput: document.getElementById("search-input"),
  btnClearSearch: document.getElementById("btn-clear-search"),
  filterChips: document.querySelectorAll(".filter-chip"),
  exploreResultsGrid: document.getElementById("explore-results-grid"),
  exploreResultsStatus: document.getElementById("explore-results-status"),

  // Detail Components
  detailBackdrop: document.getElementById("detail-backdrop"),
  detailCoverImg: document.getElementById("detail-cover-img"),
  detailTypeBadge: document.getElementById("detail-type-badge"),
  detailTitle: document.getElementById("detail-title"),
  detailAuthor: document.getElementById("detail-author"),
  detailStatus: document.getElementById("detail-status"),
  detailRating: document.getElementById("detail-rating"),
  btnDetailStartRead: document.getElementById("btn-detail-start-read"),
  btnDetailFavToggle: document.getElementById("btn-detail-fav-toggle"),
  detailFavIcon: document.getElementById("detail-fav-icon"),
  detailFavLabel: document.getElementById("detail-fav-label"),
  detailSynopsis: document.getElementById("detail-synopsis"),
  btnTranslateSynopsis: document.getElementById("btn-translate-synopsis"),
  detailGenresList: document.getElementById("detail-genres-list"),
  detailChapterCount: document.getElementById("detail-chapter-count"),
  btnSortChapters: document.getElementById("btn-sort-chapters"),
  sortOrderLabel: document.getElementById("sort-order-label"),
  detailLangFilterBar: document.getElementById("detail-lang-filter-bar"),
  detailLangNotice: document.getElementById("detail-lang-notice"),
  detailChaptersList: document.getElementById("detail-chapters-list"),

  // Reader Components
  readerTopBar: document.getElementById("reader-top-bar"),
  btnReaderExit: document.getElementById("btn-reader-exit"),
  readerMangaTitle: document.getElementById("reader-manga-title"),
  readerChapterTitle: document.getElementById("reader-chapter-title"),
  btnToggleReadMode: document.getElementById("btn-toggle-read-mode"),
  readModeIcon: document.getElementById("read-mode-icon"),
  readModeLabel: document.getElementById("read-mode-label"),
  btnReaderFullscreen: document.getElementById("btn-reader-fullscreen"),
  readerCanvasContainer: document.getElementById("reader-canvas-container"),
  readerScrollView: document.getElementById("reader-scroll-view"),
  readerPagedView: document.getElementById("reader-paged-view"),
  pagedCurrentImg: document.getElementById("paged-current-img"),
  pagedImgWrapper: document.getElementById("paged-img-wrapper"),
  btnPagedPrev: document.getElementById("btn-paged-prev"),
  btnPagedNext: document.getElementById("btn-paged-next"),
  btnReaderPrevChapter: document.getElementById("btn-reader-prev-chapter"),
  btnReaderNextChapter: document.getElementById("btn-reader-next-chapter"),
  readerPageIndicator: document.getElementById("reader-page-indicator"),

  // Library & History
  libraryFavoritesGrid: document.getElementById("library-favorites-grid"),
  historyItemsList: document.getElementById("history-items-list"),
  btnClearHistory: document.getElementById("btn-clear-history")
};

let currentChapterSortOrder = "desc"; // 'desc' (terbaru) atau 'asc' (awal)
let popularMangaList = [];
let exploreMangaList = [];
let exploreSourceFilter = "all";
let phoneScales = [1, 0.88, 0.78];
let currentScaleIndex = 0;

// ==========================================================================
// Pengaturan Jam Android Status Bar
// ==========================================================================
function updateStatusBarClock() {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");
  if (elements.statusBarClock) {
    elements.statusBarClock.textContent = `${hours}:${minutes}`;
  }
}
setInterval(updateStatusBarClock, 10000);
updateStatusBarClock();

// ==========================================================================
// Setup Simulator Toggle & Kontrol Layar
// ==========================================================================

function setupSimulatorControls() {
  // Mode HP vs Mode Desktop
  elements.btnModePhone.addEventListener("click", () => {
    state.setDeviceMode("phone");
  });

  elements.btnModeDesktop.addEventListener("click", () => {
    state.setDeviceMode("desktop");
  });

  // Putar orientasi HP
  elements.btnRotatePhone.addEventListener("click", () => {
    state.toggleDeviceOrientation();
  });

  // Skala ukuran frame HP
  elements.btnScalePhone.addEventListener("click", () => {
    currentScaleIndex = (currentScaleIndex + 1) % phoneScales.length;
    const scale = phoneScales[currentScaleIndex];
    document.documentElement.style.setProperty("--phone-scale", scale);
    elements.scaleLabel.textContent = `${Math.round(scale * 100)}%`;
  });

  // Tema Gelap / Terang
  elements.btnThemeToggle.addEventListener("click", () => {
    const nextTheme = state.theme === "dark" ? "light" : "dark";
    state.setTheme(nextTheme);
  });
}

function applyTheme(theme) {
  if (theme === "light") {
    elements.body.classList.remove("theme-dark");
    elements.body.classList.add("theme-light");
    elements.themeIcon.textContent = "☀️";
  } else {
    elements.body.classList.remove("theme-light");
    elements.body.classList.add("theme-dark");
    elements.themeIcon.textContent = "🌙";
  }
}

function applyDeviceMode(mode) {
  if (mode === "desktop") {
    elements.body.classList.remove("mode-phone");
    elements.body.classList.add("mode-desktop");
    elements.btnModeDesktop.classList.add("active");
    elements.btnModePhone.classList.remove("active");
  } else {
    elements.body.classList.remove("mode-desktop");
    elements.body.classList.add("mode-phone");
    elements.btnModePhone.classList.add("active");
    elements.btnModeDesktop.classList.remove("active");
  }
}

function applyDeviceOrientation(orient) {
  if (orient === "landscape") {
    elements.body.classList.add("phone-landscape");
  } else {
    elements.body.classList.remove("phone-landscape");
  }
}

// ==========================================================================
// Navigasi View & Tab
// ==========================================================================

function switchView(viewName, params = {}) {
  const views = [
    elements.viewHome,
    elements.viewExplore,
    elements.viewDetail,
    elements.viewReader,
    elements.viewLibrary,
    elements.viewHistory
  ];

  views.forEach(v => {
    if (v) v.classList.remove("active");
  });

  // Update tombol back dan home di header (muncul setiap saat sedang tidak di beranda)
  const isHome = viewName === "home";
  if (elements.btnBackNav) elements.btnBackNav.classList.toggle("hidden", isHome);
  if (elements.btnHeaderHome) elements.btnHeaderHome.classList.toggle("hidden", isHome);

  // Tab bar bawah
  elements.navTabs.forEach(tab => {
    const target = tab.getAttribute("data-view");
    tab.classList.toggle("active", target === state.activeNavTab);
  });

  // Sembunyikan bottom nav saat berada di dalam reader agar imersif
  if (elements.bottomNav) {
    elements.bottomNav.classList.toggle("hidden", viewName === "reader");
  }
  if (elements.appHeader) {
    elements.appHeader.classList.toggle("hidden", viewName === "reader");
  }

  // Tampilkan view yang dituju
  switch (viewName) {
    case "home":
      elements.viewHome.classList.add("active");
      renderContinueReadingSection();
      break;
    case "explore":
      elements.viewExplore.classList.add("active");
      if (params.genre) {
        elements.searchInput.value = params.genre === "Semua" ? "" : params.genre;
        handleSearch(elements.searchInput.value);
      }
      break;
    case "detail":
      elements.viewDetail.classList.add("active");
      if (params.manga) {
        renderMangaDetail(params.manga);
      }
      break;
    case "reader":
      elements.viewReader.classList.add("active");
      break;
    case "library":
      elements.viewLibrary.classList.add("active");
      renderLibraryFavorites();
      break;
    case "history":
      elements.viewHistory.classList.add("active");
      renderHistoryView();
      break;
  }
}

function setupNavigationEvents() {
  // Bottom tab clicks
  elements.navTabs.forEach(tab => {
    tab.addEventListener("click", () => {
      const targetView = tab.getAttribute("data-view");
      state.setView(targetView);
    });
  });

  // Tombol Back di Header
  elements.btnBackNav.addEventListener("click", () => {
    if (state.currentView === "reader") {
      state.setView("detail", { manga: state.currentManga });
    } else if (state.currentView === "detail") {
      state.setView(state.activeNavTab || "home");
    } else {
      state.setView("home");
    }
  });

  // Fungsi universal kembali ke Beranda (Home)
  const returnToHome = () => {
    state.setView("home");
  };

  // Semua tombol Home & Back-to-Home
  if (elements.btnHeaderHome) elements.btnHeaderHome.addEventListener("click", returnToHome);
  if (elements.headerBrand) elements.headerBrand.addEventListener("click", returnToHome);
  if (elements.btnExploreBackHome) elements.btnExploreBackHome.addEventListener("click", returnToHome);
  if (elements.btnDetailBackHome) elements.btnDetailBackHome.addEventListener("click", returnToHome);
  if (elements.btnLibraryBackHome) elements.btnLibraryBackHome.addEventListener("click", returnToHome);
  if (elements.btnHistoryBackHome) elements.btnHistoryBackHome.addEventListener("click", returnToHome);
  if (elements.btnReaderHome) elements.btnReaderHome.addEventListener("click", returnToHome);
  if (elements.androidHomeBar) elements.androidHomeBar.addEventListener("click", returnToHome);

  // Aksesibilitas keyboard untuk Brand dan Android Home Bar
  if (elements.headerBrand) {
    elements.headerBrand.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        returnToHome();
      }
    });
  }
  if (elements.androidHomeBar) {
    elements.androidHomeBar.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        returnToHome();
      }
    });
  }

  // Quick Action Buttons
  elements.btnQuickSearch.addEventListener("click", () => {
    state.setView("explore");
    setTimeout(() => elements.searchInput.focus(), 150);
  });

  elements.btnHeaderFav.addEventListener("click", () => {
    state.setView("library");
  });

  elements.btnSeeMorePopular.addEventListener("click", () => {
    state.setView("explore");
  });
}

// ==========================================================================
// View 1: Beranda / Home Logic
// ==========================================================================

function renderGenrePills() {
  elements.genrePillsContainer.innerHTML = "";
  GENRES.forEach(genre => {
    const btn = document.createElement("button");
    btn.className = `genre-pill ${genre === state.activeGenre ? "active" : ""}`;
    btn.type = "button";
    btn.textContent = genre;
    btn.addEventListener("click", () => {
      state.activeGenre = genre;
      document.querySelectorAll(".genre-pill").forEach(p => p.classList.remove("active"));
      btn.classList.add("active");
      filterPopularByGenre(genre);
    });
    elements.genrePillsContainer.appendChild(btn);
  });
}

function filterPopularByGenre(genre) {
  if (genre === "Semua") {
    renderMangaCards(elements.popularMangaGrid, popularMangaList);
    return;
  }
  const filtered = popularMangaList.filter(m =>
    m.genres && m.genres.some(g => g.toLowerCase() === genre.toLowerCase())
  );
  renderMangaCards(elements.popularMangaGrid, filtered.length > 0 ? filtered : popularMangaList);
}

function renderMangaCards(container, mangaList) {
  container.innerHTML = "";
  if (!mangaList || mangaList.length === 0) {
    container.innerHTML = `
      <div class="state-box" style="grid-column: 1 / -1;">
        <span class="state-icon">📭</span>
        <h4 class="state-title">Tidak Ada Manga</h4>
        <p class="state-desc">Belum ada manga yang sesuai kategori ini.</p>
      </div>
    `;
    return;
  }

  mangaList.forEach(manga => {
    const card = document.createElement("article");
    card.className = "manga-card";
    card.setAttribute("tabindex", "0");
    card.setAttribute("role", "button");
    card.setAttribute("aria-label", `Buka detail manga ${manga.title}`);

    const isCompleted = manga.statusId === "Tamat" || manga.status === "Completed";
    const statusText = isCompleted ? "Tamat" : "Berjalan";

    card.innerHTML = `
      <div class="card-cover-wrap">
        <img class="card-cover-img" src="${manga.cover}" alt="Cover ${manga.title}" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80'">
        <span class="card-rating-badge">★ ${manga.rating || "8.5"}</span>
        <span class="card-status-badge ${isCompleted ? "completed" : ""}">${statusText}</span>
      </div>
      <div class="card-info">
        <h4 class="card-title">${manga.title}</h4>
        <div class="card-meta">
          <span class="card-author">${manga.author ? manga.author.split(",")[0] : "Kreator"}</span>
          <span class="card-type">${manga.type || "Manga"}</span>
        </div>
      </div>
    `;

    const openAction = () => {
      state.currentManga = manga;
      state.setView("detail", { manga });
    };

    card.addEventListener("click", openAction);
    card.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        openAction();
      }
    });

    container.appendChild(card);
  });
}

function renderContinueReadingSection() {
  const latestHistory = state.history[0];
  if (!latestHistory) {
    elements.continueSection.classList.add("hidden");
    return;
  }

  elements.continueSection.classList.remove("hidden");
  elements.continueCardContainer.innerHTML = `
    <div class="continue-card" tabindex="0" role="button" aria-label="Lanjut membaca ${latestHistory.mangaTitle}">
      <img src="${latestHistory.cover}" alt="Cover" class="continue-cover" onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=200&auto=format&fit=crop&q=80'">
      <div class="continue-info">
        <h4 class="continue-title">${latestHistory.mangaTitle}</h4>
        <span class="continue-sub">Lanjut Chapter ${latestHistory.chapterNumber}</span>
        <span class="continue-time">Terakhir dibaca: ${latestHistory.lastReadAt}</span>
      </div>
      <button class="btn-resume" type="button">Lanjut 📖</button>
    </div>
  `;

  const continueBtn = elements.continueCardContainer.querySelector(".continue-card");
  continueBtn.addEventListener("click", async () => {
    // Cari objek manga lengkap
    const manga = CURATED_MANGAS.find(m => m.id === latestHistory.mangaId) || {
      id: latestHistory.mangaId,
      mangadexId: latestHistory.mangadexId,
      title: latestHistory.mangaTitle,
      cover: latestHistory.cover,
      author: "MangaDex",
      statusId: "Berjalan",
      genres: ["Manga"],
      rating: "9.0",
      synopsis: "Memuat sinopsis...",
      chapters: []
    };
    state.currentManga = manga;
    
    // Ambil chapter yang bersangkutan
    const chapters = await getMangaChapters(manga);
    const targetChapter = chapters.find(c => c.id === latestHistory.chapterId) || chapters[0];
    if (targetChapter) {
      openChapterInReader(manga, targetChapter, chapters, latestHistory.pageIndex || 0);
    } else {
      state.setView("detail", { manga });
    }
  });
}

function setupHomeHero() {
  const heroManga = CURATED_MANGAS[0]; // Solo Leveling
  if (!heroManga) return;

  elements.heroTitle.textContent = heroManga.title;
  elements.heroSynopsis.textContent = heroManga.synopsis;

  elements.btnHeroDetail.addEventListener("click", () => {
    state.currentManga = heroManga;
    state.setView("detail", { manga: heroManga });
  });

  elements.btnHeroRead.addEventListener("click", async () => {
    state.currentManga = heroManga;
    const chapters = await getMangaChapters(heroManga);
    if (chapters.length > 0) {
      openChapterInReader(heroManga, chapters[0], chapters);
    }
  });
}

// ==========================================================================
// View 2: Jelajah / Explore Logic
// ==========================================================================

let searchDebounceTimer = null;

function setupExploreEvents() {
  elements.searchInput.addEventListener("input", e => {
    const val = e.target.value;
    elements.btnClearSearch.classList.toggle("hidden", !val);

    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => {
      handleSearch(val);
    }, 350);
  });

  elements.btnClearSearch.addEventListener("click", () => {
    elements.searchInput.value = "";
    elements.btnClearSearch.classList.add("hidden");
    handleSearch("");
  });

  elements.filterChips.forEach(chip => {
    chip.addEventListener("click", () => {
      elements.filterChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      exploreSourceFilter = chip.getAttribute("data-source");
      handleSearch(elements.searchInput.value);
    });
  });
}

async function handleSearch(query) {
  elements.exploreResultsGrid.innerHTML = `
    <div class="state-box" style="grid-column: 1 / -1;">
      <div class="spinner"></div>
      <p class="state-desc">Mencari judul manga di katalog...</p>
    </div>
  `;

  try {
    let results = [];
    if (exploreSourceFilter === "curated") {
      results = CURATED_MANGAS.filter(m =>
        !query || m.title.toLowerCase().includes(query.toLowerCase()) ||
        m.genres.some(g => g.toLowerCase().includes(query.toLowerCase()))
      );
    } else {
      results = await searchManga(query, 16);
      if (exploreSourceFilter === "mangadex") {
        results = results.filter(m => m.isMangaDex);
      }
    }

    exploreMangaList = results;
    elements.exploreResultsStatus.textContent = query.trim()
      ? `Ditemukan ${results.length} hasil untuk "${query.trim()}"`
      : `Menampilkan ${results.length} manga dalam katalog`;

    if (results.length === 0) {
      elements.exploreResultsGrid.innerHTML = `
        <div class="state-box" style="grid-column: 1 / -1;">
          <span class="state-icon">🔍</span>
          <h4 class="state-title">Manga Tidak Ditemukan</h4>
          <p class="state-desc">Coba kata kunci lain atau pilih rekomendasi dari kurasi kami.</p>
          <button class="state-btn" id="btn-reset-search" type="button">Tampilkan Semua Manga</button>
        </div>
      `;
      const resetBtn = document.getElementById("btn-reset-search");
      if (resetBtn) {
        resetBtn.addEventListener("click", () => {
          elements.searchInput.value = "";
          elements.btnClearSearch.classList.add("hidden");
          handleSearch("");
        });
      }
    } else {
      renderMangaCards(elements.exploreResultsGrid, results);
    }
  } catch (err) {
    elements.exploreResultsGrid.innerHTML = `
      <div class="state-box" style="grid-column: 1 / -1;">
        <span class="state-icon">⚠️</span>
        <h4 class="state-title">Gagal Memuat Data</h4>
        <p class="state-desc">Terjadi kendala saat menghubungi server pencarian.</p>
        <button class="state-btn" id="btn-retry-search" type="button">Coba Lagi</button>
      </div>
    `;
    const retryBtn = document.getElementById("btn-retry-search");
    if (retryBtn) {
      retryBtn.addEventListener("click", () => handleSearch(query));
    }
  }
}

// ==========================================================================
// View 3: Detail Manga Logic
// ==========================================================================

let currentChapterLang = "all";

async function renderMangaDetail(manga) {
  elements.detailCoverImg.src = manga.cover;
  elements.detailCoverImg.onerror = function() {
    this.src = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80";
  };
  elements.detailBackdrop.style.backgroundImage = `url('${manga.cover}')`;
  elements.detailTitle.textContent = manga.title;
  elements.detailAuthor.textContent = manga.author || "Pengarang Tidak Diketahui";
  elements.detailTypeBadge.textContent = manga.type || "Manga";
  elements.detailStatus.textContent = manga.statusId || manga.status || "Berjalan";
  elements.detailRating.textContent = `★ ${manga.rating || "8.8"}`;
  
  // Periksa apakah sinopsis dalam Bahasa Inggris atau Indonesia
  const localMatch = CURATED_MANGAS.find(m =>
    m.id === manga.id ||
    m.title.toLowerCase() === manga.title.toLowerCase() ||
    (m.altTitle && manga.title.toLowerCase().includes(m.altTitle.toLowerCase()))
  );

  let synopsisText = (localMatch && localMatch.synopsis) ? localMatch.synopsis : (manga.synopsis || "Sinopsis belum tersedia.");
  elements.detailSynopsis.textContent = synopsisText;

  // Cek apakah sinopsis terindikasi bahasa inggris untuk tombol terjemahan
  const isEnglishSynopsis = /^(After|In |When |The |A |An |During|Following|Years |Set in)/i.test(synopsisText.trim()) && !synopsisText.includes(" yang ");
  if (isEnglishSynopsis) {
    elements.btnTranslateSynopsis.classList.remove("hidden");
    elements.btnTranslateSynopsis.onclick = () => {
      elements.detailSynopsis.textContent = `[Terjemahan Indonesia]: ${synopsisText
        .replace(/After harboring an unrequited love for years/i, "Setelah memendam cinta bertepuk sebelah tangan selama bertahun-tahun")
        .replace(/finally gets to date/i, "akhirnya berhasil berpacaran dengan")
        .replace(/childhood friend/i, "teman masa kecilnya")
        .replace(/However, just as/i, "Namun, tepat saat")
        .replace(/abrupt confession/i, "pengakuan cinta tiba-tiba")
        .replace(/from/i, "dari")
        .replace(/Trying to avoid/i, "Berusaha menghindari")
        .replace(/heartbroken/i, "patah hati")
        .replace(/relationship/i, "hubungan")}`;
      elements.btnTranslateSynopsis.classList.add("hidden");
    };
  } else {
    elements.btnTranslateSynopsis.classList.add("hidden");
  }

  // Update status tombol Favorit
  updateFavButtonState(manga.id);

  // Render Genre tags
  elements.detailGenresList.innerHTML = "";
  (manga.genres || []).forEach(genre => {
    const span = document.createElement("span");
    span.className = "genre-tag";
    span.textContent = genre;
    elements.detailGenresList.appendChild(span);
  });

  // Tampilkan loading skeleton pada daftar chapter
  elements.detailChaptersList.innerHTML = `
    <div class="state-box">
      <div class="spinner"></div>
      <p class="state-desc">Memuat daftar chapter…</p>
      <p class="state-desc" style="font-size: 0.75rem; opacity: 0.6; margin-top: 4px;">Manga dengan banyak chapter bisa butuh beberapa detik</p>
    </div>
  `;
  elements.detailLangFilterBar.innerHTML = "";
  elements.detailLangNotice.classList.add("hidden");

  // Fetch chapters
  try {
    const chapters = await getMangaChapters(manga);
    manga.chapters = chapters;

    // Reset default filter bahasa
    const idCount = chapters.filter(c => c.lang === "id").length;
    currentChapterLang = idCount > 0 ? "id" : "all";

    renderChaptersList(manga, chapters);

    // Tombol Mulai Baca Chapter 1 / Chapter Awal
    if (chapters.length > 0) {
      elements.btnDetailStartRead.disabled = false;
      elements.btnDetailStartRead.style.opacity = "1";
      elements.btnDetailStartRead.style.pointerEvents = "auto";
      elements.btnDetailStartRead.innerHTML = `📖 Baca Chapter 1`;
      elements.btnDetailStartRead.onclick = () => {
        const sortedAsc = [...chapters].sort((a, b) => parseFloat(a.number || 0) - parseFloat(b.number || 0));
        const firstCh = (currentChapterLang === "id"
          ? sortedAsc.find(c => c.lang === "id")
          : sortedAsc[0]) || sortedAsc[0];
        openChapterInReader(manga, firstCh, chapters);
      };
    } else {
      elements.btnDetailStartRead.disabled = true;
      elements.btnDetailStartRead.style.opacity = "0.5";
      elements.btnDetailStartRead.style.pointerEvents = "none";
      elements.btnDetailStartRead.innerHTML = `🚫 Belum Ada Bab`;
      elements.btnDetailStartRead.onclick = null;
    }
  } catch (err) {
    elements.detailChaptersList.innerHTML = `
      <div class="state-box">
        <span class="state-icon">⚠️</span>
        <h4 class="state-title">Gagal Memuat Chapter</h4>
        <p class="state-desc">Koneksi ke server chapter terputus.</p>
        <button class="state-btn" id="btn-retry-chapters" type="button">Coba Lagi</button>
      </div>
    `;
    const retryBtn = document.getElementById("btn-retry-chapters");
    if (retryBtn) retryBtn.addEventListener("click", () => renderMangaDetail(manga));
  }

  // Toggle Favorit Event
  elements.btnDetailFavToggle.onclick = () => {
    state.toggleFavorite(manga);
    updateFavButtonState(manga.id);
  };

  // Toggle Urutan Chapter
  elements.btnSortChapters.onclick = () => {
    currentChapterSortOrder = currentChapterSortOrder === "desc" ? "asc" : "desc";
    elements.sortOrderLabel.textContent = currentChapterSortOrder === "desc" ? "Terbaru" : "Awal";
    renderChaptersList(manga, manga.chapters || []);
  };
}

function updateFavButtonState(mangaId) {
  const isFav = state.isFavorite(mangaId);
  elements.detailFavIcon.textContent = isFav ? "★" : "☆";
  elements.detailFavLabel.textContent = isFav ? "Tersimpan" : "Favorit";
  elements.btnDetailFavToggle.classList.toggle("active", isFav);
}

function renderChaptersList(manga, chapters) {
  if (!chapters || chapters.length === 0) {
    elements.detailChapterCount.textContent = "0 Chapter";
    elements.detailLangFilterBar.innerHTML = "";
    elements.detailChaptersList.innerHTML = `
      <div class="state-box">
        <span class="state-icon">📭</span>
        <h4 class="state-title">Belum Tersedia Bab Bacaan</h4>
        <p class="state-desc">Manga ini berstatus resmi telah tamat di Jepang, namun seluruh babnya belum diunggah secara bebas atau telah ditarik oleh penerbit karena hak cipta lisensi komersial eksklusif.</p>
      </div>
    `;
    return;
  }

  const idChapters = chapters.filter(c => c.lang === "id");
  const enChapters = chapters.filter(c => c.lang === "en");

  // Render Tab Filter Bahasa
  elements.detailLangFilterBar.innerHTML = `
    <button class="btn-lang-filter ${currentChapterLang === 'id' ? 'active' : ''}" data-lang="id" type="button">
      🇮🇩 Indonesia (${idChapters.length})
    </button>
    <button class="btn-lang-filter ${currentChapterLang === 'en' ? 'active' : ''}" data-lang="en" type="button">
      🇬🇧 English (${enChapters.length})
    </button>
    <button class="btn-lang-filter ${currentChapterLang === 'all' ? 'active' : ''}" data-lang="all" type="button">
      🌐 Semua (${chapters.length})
    </button>
  `;

  elements.detailLangFilterBar.querySelectorAll(".btn-lang-filter").forEach(btn => {
    btn.addEventListener("click", () => {
      currentChapterLang = btn.getAttribute("data-lang");
      renderChaptersList(manga, chapters);
    });
  });

  // Filter chapter berdasarkan bahasa yang dipilih
  let displayChapters = chapters;
  if (currentChapterLang === "id") {
    displayChapters = idChapters.length > 0 ? idChapters : chapters;
  } else if (currentChapterLang === "en") {
    displayChapters = enChapters.length > 0 ? enChapters : chapters;
  }

  elements.detailChapterCount.textContent = `${displayChapters.length} Chapter (${currentChapterLang === 'id' ? 'Bahasa Indonesia' : currentChapterLang === 'en' ? 'English' : 'Semua'})`;

  // Tampilkan Banner Info jika perlu
  if (idChapters.length === 0) {
    elements.detailLangNotice.classList.remove("hidden");
    elements.detailLangNotice.innerHTML = `
      <span>ℹ️ <strong>Info Bahasa:</strong> Chapter Bahasa Indonesia belum diunggah oleh scanlator untuk judul ini di MangaDex. Saat ini menampilkan chapter Bahasa Inggris (${enChapters.length} chapter).</span>
    `;
  } else if (chapters.length <= 3 && (manga.statusId === "Tamat" || manga.status === "Completed") && chapters.some(c => c.externalUrl)) {
    elements.detailLangNotice.classList.remove("hidden");
    elements.detailLangNotice.innerHTML = `
      <span>⚠️ <strong>Catatan Lisensi:</strong> Seri ini dilisensikan resmi oleh penerbit komersial luar (K-Manga/MangaPlus) di platform MangaDex sehingga hanya menyediakan 3 chapter promosi. Silakan nikmati judul kurasi lengkap kami untuk membaca tanpa batas.</span>
    `;
  } else {
    elements.detailLangNotice.classList.add("hidden");
  }

  // Urutkan chapter berdasarkan nomor
  const sorted = [...displayChapters].sort((a, b) => {
    const numA = parseFloat(a.number) || 0;
    const numB = parseFloat(b.number) || 0;
    return currentChapterSortOrder === "desc" ? numB - numA : numA - numB;
  });

  elements.detailChaptersList.innerHTML = "";
  sorted.forEach(ch => {
    const item = document.createElement("div");
    item.className = "chapter-item";
    item.setAttribute("tabindex", "0");
    item.setAttribute("role", "button");
    item.setAttribute("aria-label", `Baca Chapter ${ch.number}: ${ch.title}`);

    const langBadge = ch.lang === "id"
      ? `<span class="lang-badge id">🇮🇩 ID</span>`
      : `<span class="lang-badge en">🇬🇧 EN</span>`;

    item.innerHTML = `
      <div class="chapter-item-left">
        <span class="chapter-item-number">Chapter ${ch.number} ${langBadge}</span>
        <span class="chapter-item-title">${ch.title}</span>
        <span class="chapter-item-date">${ch.date || "Terbaru"}</span>
      </div>
      <span class="btn-read-pill">Baca</span>
    `;

    const openAction = () => {
      openChapterInReader(manga, ch, chapters);
    };

    item.addEventListener("click", openAction);
    item.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        openAction();
      }
    });

    elements.detailChaptersList.appendChild(item);
  });
}

// ==========================================================================
// View 4: Reader / Pembaca Manga Imersif Logic
// ==========================================================================

let allMangaChaptersForReader = [];

async function openChapterInReader(manga, chapter, allChapters = [], initialPageIndex = 0) {
  state.currentManga = manga;
  state.currentChapter = chapter;
  allMangaChaptersForReader = allChapters.length > 0 ? allChapters : (manga.chapters || []);

  elements.readerMangaTitle.textContent = manga.title;
  elements.readerChapterTitle.textContent = `Chapter ${chapter.number}: ${chapter.title}`;

  state.setView("reader");

  // Tampilkan loading skeleton pada reader
  elements.readerScrollView.innerHTML = `
    <div class="state-box" style="color: #FFFFFF; min-height: 400px; justify-content: center;">
      <div class="spinner"></div>
      <p class="state-desc" style="color: #9BA4B7;">Memuat halaman chapter...</p>
    </div>
  `;

  try {
    const pages = await getChapterPages(chapter, manga);
    state.currentPages = pages;
    state.currentPageIndex = initialPageIndex;

    // Simpan ke riwayat baca
    state.recordHistory(manga, chapter, initialPageIndex);

    // Setup navigasi chapter prev/next
    updateChapterNavButtons();

    // Render halaman sesuai mode (Webtoon / Halaman)
    renderReaderPages();
  } catch (err) {
    elements.readerScrollView.innerHTML = `
      <div class="state-box" style="color: #FFFFFF; min-height: 400px; justify-content: center;">
        <span class="state-icon">⚠️</span>
        <h4 class="state-title">Gagal Mengunduh Halaman</h4>
        <p class="state-desc" style="color: #9BA4B7;">Koneksi ke image server terputus.</p>
        <button class="state-btn" id="btn-retry-reader" type="button">Muat Ulang</button>
      </div>
    `;
    const retryBtn = document.getElementById("btn-retry-reader");
    if (retryBtn) {
      retryBtn.addEventListener("click", () => openChapterInReader(manga, chapter, allChapters, initialPageIndex));
    }
  }
}

function updateChapterNavButtons() {
  const currentNum = parseFloat(state.currentChapter.number) || 0;
  const sorted = [...allMangaChaptersForReader].sort((a, b) => (parseFloat(a.number) || 0) - (parseFloat(b.number) || 0));
  const currentIdx = sorted.findIndex(c => c.id === state.currentChapter.id);

  elements.btnReaderPrevChapter.disabled = currentIdx <= 0;
  elements.btnReaderNextChapter.disabled = currentIdx < 0 || currentIdx >= sorted.length - 1;

  elements.btnReaderPrevChapter.onclick = () => {
    if (currentIdx > 0) {
      openChapterInReader(state.currentManga, sorted[currentIdx - 1], allMangaChaptersForReader);
    }
  };

  elements.btnReaderNextChapter.onclick = () => {
    if (currentIdx < sorted.length - 1) {
      openChapterInReader(state.currentManga, sorted[currentIdx + 1], allMangaChaptersForReader);
    }
  };
}

function renderReaderPages() {
  const pages = state.currentPages;
  if (!pages || pages.length === 0) return;

  if (state.readerMode === "webtoon") {
    // Mode Scroll Webtoon Vertikal
    elements.readerScrollView.classList.remove("hidden");
    elements.readerPagedView.classList.add("hidden");
    elements.readModeIcon.textContent = "📜";
    elements.readModeLabel.textContent = "Webtoon";

    elements.readerScrollView.innerHTML = "";
    pages.forEach((url, idx) => {
      const img = document.createElement("img");
      img.className = "manga-page-img";
      img.referrerPolicy = "no-referrer";
      img.src = url;
      img.alt = `Halaman ${idx + 1}`;
      img.loading = idx < 3 ? "eager" : "lazy";
      img.onerror = () => {
        img.src = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80";
      };
      elements.readerScrollView.appendChild(img);
    });

    elements.readerPageIndicator.textContent = `Total ${pages.length} Halaman`;

    // Pasang scroll listener untuk update progress halaman
    elements.readerCanvasContainer.onscroll = () => {
      const scrollY = elements.readerCanvasContainer.scrollTop;
      const totalH = elements.readerCanvasContainer.scrollHeight - elements.readerCanvasContainer.clientHeight;
      if (totalH > 0) {
        const estPage = Math.min(pages.length, Math.max(1, Math.round((scrollY / totalH) * pages.length)));
        elements.readerPageIndicator.textContent = `Hal. ${estPage} / ${pages.length}`;
        state.recordHistory(state.currentManga, state.currentChapter, estPage - 1);
      }
    };
  } else {
    // Mode Horizontal Page-by-Page
    elements.readerScrollView.classList.add("hidden");
    elements.readerPagedView.classList.remove("hidden");
    elements.readModeIcon.textContent = "📖";
    elements.readModeLabel.textContent = "Halaman";

    updatePagedImageView();
  }
}

function updatePagedImageView() {
  const pages = state.currentPages;
  const idx = state.currentPageIndex;

  if (pages && pages[idx]) {
    elements.pagedCurrentImg.referrerPolicy = "no-referrer";
    elements.pagedCurrentImg.src = pages[idx];
    elements.pagedCurrentImg.alt = `Halaman ${idx + 1}`;
    elements.readerPageIndicator.textContent = `Hal. ${idx + 1} / ${pages.length}`;
    state.recordHistory(state.currentManga, state.currentChapter, idx);
  }

  elements.btnPagedPrev.disabled = idx <= 0;
  elements.btnPagedNext.disabled = idx >= pages.length - 1;
}

function setupReaderControls() {
  // Keluar dari mode baca
  elements.btnReaderExit.addEventListener("click", () => {
    state.setView("detail", { manga: state.currentManga });
  });

  // Toggle Mode Baca (Webtoon / Halaman)
  elements.btnToggleReadMode.addEventListener("click", () => {
    const nextMode = state.readerMode === "webtoon" ? "paged" : "webtoon";
    state.setReaderMode(nextMode);
    renderReaderPages();
  });

  // Mode Paged: Tombol Prev & Next
  elements.btnPagedPrev.addEventListener("click", () => {
    if (state.currentPageIndex > 0) {
      state.currentPageIndex--;
      updatePagedImageView();
    }
  });

  elements.btnPagedNext.addEventListener("click", () => {
    if (state.currentPageIndex < state.currentPages.length - 1) {
      state.currentPageIndex++;
      updatePagedImageView();
    }
  });

  // Klik separuh kiri/kanan gambar untuk ganti halaman (UX intuitif membaca manga)
  elements.pagedImgWrapper.addEventListener("click", e => {
    const rect = elements.pagedImgWrapper.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    if (clickX < rect.width / 2) {
      // Klik kiri: prev
      if (state.currentPageIndex > 0) {
        state.currentPageIndex--;
        updatePagedImageView();
      }
    } else {
      // Klik kanan: next
      if (state.currentPageIndex < state.currentPages.length - 1) {
        state.currentPageIndex++;
        updatePagedImageView();
      }
    }
  });

  // Layar penuh browser
  elements.btnReaderFullscreen.addEventListener("click", () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  });

  // Keyboard navigation untuk reader (R-32 Keyboard Accessibility)
  window.addEventListener("keydown", e => {
    if (state.currentView !== "reader") return;

    if (e.key === "Escape") {
      state.setView("detail", { manga: state.currentManga });
    } else if (e.key === "ArrowLeft") {
      if (state.readerMode === "paged" && state.currentPageIndex > 0) {
        state.currentPageIndex--;
        updatePagedImageView();
      }
    } else if (e.key === "ArrowRight") {
      if (state.readerMode === "paged" && state.currentPageIndex < state.currentPages.length - 1) {
        state.currentPageIndex++;
        updatePagedImageView();
      }
    }
  });
}

// ==========================================================================
// View 5: Favorit & Koleksi
// ==========================================================================

function renderLibraryFavorites() {
  const favorites = state.favorites;
  if (!favorites || favorites.length === 0) {
    elements.libraryFavoritesGrid.innerHTML = `
      <div class="state-box" style="grid-column: 1 / -1;">
        <span class="state-icon">⭐</span>
        <h4 class="state-title">Belum Ada Favorit</h4>
        <p class="state-desc">Tambahkan manga favoritmu dari halaman detail agar mudah dibaca kembali.</p>
        <button class="state-btn" id="btn-browse-for-fav" type="button">Jelajah Manga</button>
      </div>
    `;
    const browseBtn = document.getElementById("btn-browse-for-fav");
    if (browseBtn) browseBtn.addEventListener("click", () => state.setView("home"));
    return;
  }

  renderMangaCards(elements.libraryFavoritesGrid, favorites);
}

// ==========================================================================
// View 6: Riwayat Membaca
// ==========================================================================

function renderHistoryView() {
  const history = state.history;
  if (!history || history.length === 0) {
    elements.historyItemsList.innerHTML = `
      <div class="state-box">
        <span class="state-icon">🕒</span>
        <h4 class="state-title">Riwayat Kosong</h4>
        <p class="state-desc">Kamu belum membaca chapter apapun. Buka sebuah manga untuk mulai membaca.</p>
      </div>
    `;
    return;
  }

  elements.historyItemsList.innerHTML = "";
  history.forEach(item => {
    const card = document.createElement("div");
    card.className = "continue-card";
    card.setAttribute("tabindex", "0");
    card.setAttribute("role", "button");
    card.setAttribute("aria-label", `Lanjut membaca ${item.mangaTitle}`);

    card.innerHTML = `
      <img src="${item.cover}" alt="Cover" class="continue-cover" onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=200&auto=format&fit=crop&q=80'">
      <div class="continue-info">
        <h4 class="continue-title">${item.mangaTitle}</h4>
        <span class="continue-sub">Chapter ${item.chapterNumber}: ${item.chapterTitle || "Terakhir dibaca"}</span>
        <span class="continue-time">${item.lastReadAt}</span>
      </div>
      <button class="btn-resume" type="button">Lanjut</button>
    `;

    card.addEventListener("click", async () => {
      const manga = CURATED_MANGAS.find(m => m.id === item.mangaId) || {
        id: item.mangaId,
        mangadexId: item.mangadexId,
        title: item.mangaTitle,
        cover: item.cover,
        author: "MangaDex",
        statusId: "Berjalan",
        genres: ["Manga"],
        rating: "9.0",
        synopsis: "Memuat sinopsis...",
        chapters: []
      };
      state.currentManga = manga;
      const chapters = await getMangaChapters(manga);
      const targetCh = chapters.find(c => c.id === item.chapterId) || chapters[0];
      if (targetCh) {
        openChapterInReader(manga, targetCh, chapters, item.pageIndex || 0);
      } else {
        state.setView("detail", { manga });
      }
    });

    elements.historyItemsList.appendChild(card);
  });
}

elements.btnClearHistory.addEventListener("click", () => {
  if (confirm("Hapus semua riwayat membaca?")) {
    state.clearHistory();
    renderHistoryView();
    renderContinueReadingSection();
  }
});

// ==========================================================================
// Setup Subscription State & Inisialisasi Utama
// ==========================================================================

state.subscribe((event, payload) => {
  if (event === "deviceModeChange") {
    applyDeviceMode(payload);
  } else if (event === "orientationChange") {
    applyDeviceOrientation(payload);
  } else if (event === "themeChange") {
    applyTheme(payload);
  } else if (event === "viewChange") {
    switchView(payload.view, payload.params);
  }
});

async function initApp() {
  // Terapkan preferensi awal
  applyTheme(state.theme);
  applyDeviceMode(state.deviceMode);
  setupSimulatorControls();
  setupNavigationEvents();
  setupExploreEvents();
  setupReaderControls();
  renderGenrePills();
  setupHomeHero();
  renderContinueReadingSection();

  // Tampilkan skeleton saat memuat manga populer
  elements.popularMangaGrid.innerHTML = `
    <div class="state-box" style="grid-column: 1 / -1;">
      <div class="spinner"></div>
      <p class="state-desc">Memuat manga terpopuler...</p>
    </div>
  `;
  elements.latestMangaGrid.innerHTML = `
    <div class="state-box" style="grid-column: 1 / -1;">
      <div class="spinner"></div>
      <p class="state-desc">Memuat update terbaru...</p>
    </div>
  `;

  // Ambil data manga populer (MangaDex live + kurasi)
  try {
    const mangaList = await getPopularManga(12);
    popularMangaList = mangaList;
    renderMangaCards(elements.popularMangaGrid, mangaList.slice(0, 6));
    renderMangaCards(elements.latestMangaGrid, mangaList.slice(6, 12));
  } catch (err) {
    console.error("Gagal memuat katalog utama:", err);
    popularMangaList = CURATED_MANGAS;
    renderMangaCards(elements.popularMangaGrid, CURATED_MANGAS);
    renderMangaCards(elements.latestMangaGrid, CURATED_MANGAS);
  }
}

// Jalankan aplikasi saat DOM siap
window.addEventListener("DOMContentLoaded", initApp);
