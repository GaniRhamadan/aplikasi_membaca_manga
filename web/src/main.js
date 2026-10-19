import { CURATED_MANGAS } from "./curatedData.js";
import {
  COMPLETE_GENRE_LIST,
  fetchMangasByGenre,
  getPopularManga,
  searchLocalManga,
  searchManga,
  getMangaChapters,
  getChapterPages
} from "./api.js";
import { state } from "./state.js";
import { icons } from "./components/icons.js";
import { renderNavbar } from "./components/Navbar.js";
import { renderHero } from "./components/Hero.js";
import { createMangaCard } from "./components/MangaCard.js";
import { renderMangaSection, renderGenreTabs } from "./components/MangaRow.js";
import { renderContinueReading } from "./components/ContinueReading.js";

// DOM Elements Cache
const elements = {
  body: document.body,
  siteHeader: document.getElementById("site-header"),
  mobileNav: document.getElementById("mobile-nav"),

  // Views
  viewHome: document.getElementById("view-home"),
  viewExplore: document.getElementById("view-explore"),
  viewDetail: document.getElementById("view-detail"),
  viewReader: document.getElementById("view-reader"),
  viewLibrary: document.getElementById("view-library"),
  viewHistory: document.getElementById("view-history"),

  // Home Mount Points
  homeHeroMount: document.getElementById("home-hero-mount"),
  homeGenresMount: document.getElementById("home-genres-mount"),
  continueReadingMount: document.getElementById("continue-reading-mount"),
  popularMangaMount: document.getElementById("popular-manga-mount"),
  latestMangaMount: document.getElementById("latest-manga-mount"),

  // Explore
  searchInput: document.getElementById("search-input"),
  btnClearSearch: document.getElementById("btn-clear-search"),
  sourceChips: document.querySelectorAll(".source-chip"),
  exploreGenresMount: document.getElementById("explore-genres-mount"),
  exploreResultsGrid: document.getElementById("explore-results-grid"),
  exploreResultsStatus: document.getElementById("explore-results-status"),

  // Detail
  btnDetailBackHome: document.getElementById("btn-detail-back-home"),
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

  // Reader
  readerTopBar: document.getElementById("reader-top-bar"),
  btnReaderExit: document.getElementById("btn-reader-exit"),
  btnReaderHome: document.getElementById("btn-reader-home"),
  readerMangaTitle: document.getElementById("reader-manga-title"),
  readerChapterTitle: document.getElementById("reader-chapter-title"),
  readerChapterSelect: document.getElementById("reader-chapter-select"),
  btnToggleReadMode: document.getElementById("btn-toggle-read-mode"),
  readModeLabel: document.getElementById("read-mode-label"),
  btnReaderFullscreen: document.getElementById("btn-reader-fullscreen"),
  readerCanvasContainer: document.getElementById("reader-canvas-container"),
  readerScrollView: document.getElementById("reader-scroll-view"),
  readerPagedView: document.getElementById("reader-paged-view"),
  pagedCurrentImg: document.getElementById("paged-current-img"),
  btnPagedPrev: document.getElementById("btn-paged-prev"),
  btnPagedNext: document.getElementById("btn-paged-next"),
  readerBottomBar: document.getElementById("reader-bottom-bar"),
  btnReaderPrevChapter: document.getElementById("btn-reader-prev-chapter"),
  btnReaderNextChapter: document.getElementById("btn-reader-next-chapter"),
  readerPageIndicator: document.getElementById("reader-page-indicator"),

  // Library & History
  libraryFavoritesGrid: document.getElementById("library-favorites-grid"),
  historyItemsList: document.getElementById("history-items-list"),
  btnClearHistory: document.getElementById("btn-clear-history")
};

let currentChapterSortOrder = "desc";
let currentChapterLang = "all";
let popularMangaList = [];
let latestMangaList = [];
let allMangaChaptersForReader = [];
let exploreSourceFilter = "all";

// ==========================================================================
// Inisialisasi & Setup Tampilan Aplikasi
// ==========================================================================

async function initApp() {
  applyTheme(state.theme);
  setupNavigation();
  setupExploreEvents();
  setupReaderControls();
  setupHistoryEvents();

  // Load initial data
  try {
    popularMangaList = await getPopularManga();
    if (!popularMangaList || popularMangaList.length === 0) {
      popularMangaList = CURATED_MANGAS;
    }
  } catch {
    popularMangaList = CURATED_MANGAS;
  }

  latestMangaList = [
    popularMangaList[3], // Chainsaw Man
    popularMangaList[4], // Frieren
    popularMangaList[5], // SPY x FAMILY
    popularMangaList[1], // One Piece
    popularMangaList[6], // Kage no Jitsuryokusha
    popularMangaList[7], // Iruma-kun
    popularMangaList[8], // Kanojo mo Kanojo
    popularMangaList[0]  // Solo Leveling
  ].filter(Boolean);

  // Render initial Home view
  renderHomeView();

  // Subscribe to state changes
  state.subscribe((event, payload) => {
    if (event === "themeChange") {
      applyTheme(payload);
    } else if (event === "viewChange") {
      switchView(payload.view, payload.params);
    } else if (event === "historyChange") {
      renderContinueReading(elements.continueReadingMount, state.history, (item) => openHistoryItem(item));
    } else if (event === "favoritesChange") {
      if (state.currentView === "library") renderLibraryFavorites();
      if (state.currentManga) updateFavButtonState(state.currentManga.id);
    }
  });

  // Render Navbar
  updateNavbar();
}

function applyTheme(theme) {
  elements.body.className = theme === "light" ? "theme-light" : "theme-dark";
  updateNavbar();
}

function updateNavbar() {
  renderNavbar(
    { siteHeader: elements.siteHeader, mobileNav: elements.mobileNav },
    state,
    {
      onNavigate: (view) => state.setView(view),
      onSearch: (q) => {
        state.searchQuery = q;
        if (state.currentView !== "explore") {
          state.setView("explore");
        }
        if (elements.searchInput) {
          elements.searchInput.value = q;
        }
        handleSearch(q);
      },
      onToggleTheme: () => state.toggleTheme()
    }
  );
}

// ==========================================================================
// Navigasi Antar Halaman
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

  views.forEach((v) => {
    if (v) v.classList.remove("active");
  });

  // Sembunyikan header & mobile nav saat berada di mode Reader
  const isReader = viewName === "reader";
  if (elements.siteHeader) elements.siteHeader.classList.toggle("hidden", isReader);
  if (elements.mobileNav) elements.mobileNav.classList.toggle("hidden", isReader);

  switch (viewName) {
    case "home":
      elements.viewHome?.classList.add("active");
      renderContinueReading(elements.continueReadingMount, state.history, (item) => openHistoryItem(item));
      window.scrollTo(0, 0);
      break;

    case "explore":
      elements.viewExplore?.classList.add("active");
      renderExploreGenreBar(params.genre || state.activeGenre || "Semua");
      if (params.genre && params.genre !== "Semua") {
        state.activeGenre = params.genre;
        if (elements.searchInput) elements.searchInput.value = "";
        loadExploreByGenre(params.genre);
      } else {
        handleSearch(elements.searchInput?.value || state.searchQuery || "");
      }
      window.scrollTo(0, 0);
      break;

    case "detail":
      elements.viewDetail?.classList.add("active");
      if (params.manga) {
        renderMangaDetail(params.manga);
      }
      window.scrollTo(0, 0);
      break;

    case "reader":
      elements.viewReader?.classList.add("active");
      break;

    case "library":
      elements.viewLibrary?.classList.add("active");
      renderLibraryFavorites();
      window.scrollTo(0, 0);
      break;

    case "history":
      elements.viewHistory?.classList.add("active");
      renderHistoryView();
      window.scrollTo(0, 0);
      break;
  }

  updateNavbar();
}

function setupNavigation() {
  if (elements.btnDetailBackHome) {
    elements.btnDetailBackHome.addEventListener("click", () => {
      state.setView(state.activeNavTab || "home");
    });
  }
}

// ==========================================================================
// View 1: Beranda (Home)
// ==========================================================================

function renderHomeView() {
  // 1. Two-Column Hero Spotlight (Top 3 Manga)
  const heroList = popularMangaList.slice(0, 3);
  renderHero(elements.homeHeroMount, heroList, {
    onRead: (manga) => {
      state.currentManga = manga;
      state.setView("detail", { manga });
      setTimeout(() => {
        const btn = document.getElementById("btn-detail-start-read");
        if (btn && !btn.disabled) btn.click();
      }, 300);
    },
    onDetail: (manga) => {
      state.currentManga = manga;
      state.setView("detail", { manga });
    }
  });

  // 2. Segmented Genre Tabs (Lengkap & Berfungsi Penuh)
  renderGenreTabs(elements.homeGenresMount, COMPLETE_GENRE_LIST, state.activeGenre, (genre) => {
    filterHomeByGenre(genre);
  });

  // 3. Continue Reading Horizontal Row
  renderContinueReading(elements.continueReadingMount, state.history, (item) => openHistoryItem(item));

  // 4. Manga Terpopuler (Ranked Leaderboard 1..6)
  renderMangaSection(elements.popularMangaMount, {
    title: "Manga Terpopuler",
    actionLabel: "Lihat Semua",
    onActionClick: () => state.setView("explore"),
    mangas: popularMangaList.slice(0, 6),
    isRanked: true,
    onSelectManga: (manga) => {
      state.currentManga = manga;
      state.setView("detail", { manga });
    }
  });

  // 5. Update Terbaru
  renderMangaSection(elements.latestMangaMount, {
    title: "Update Terbaru",
    actionLabel: "Jelajah",
    onActionClick: () => state.setView("explore"),
    mangas: latestMangaList,
    isRanked: false,
    onSelectManga: (manga) => {
      state.currentManga = manga;
      state.setView("detail", { manga });
    }
  });
}

async function filterHomeByGenre(genre) {
  state.activeGenre = genre;

  // Render ulang segmented bar agar indikator garis bawah langsung berpindah ke tab terpilih
  renderGenreTabs(elements.homeGenresMount, COMPLETE_GENRE_LIST, state.activeGenre, (g) => {
    filterHomeByGenre(g);
  });

  if (genre === "Semua") {
    renderMangaSection(elements.popularMangaMount, {
      title: "Manga Terpopuler",
      actionLabel: "Lihat Semua",
      onActionClick: () => state.setView("explore"),
      mangas: popularMangaList.slice(0, 6),
      isRanked: true,
      onSelectManga: (m) => {
        state.currentManga = m;
        state.setView("detail", { manga: m });
      }
    });
    // Tampilkan kembali update terbaru
    if (elements.latestMangaMount) {
      elements.latestMangaMount.closest(".home-content-section")?.classList.remove("hidden");
    }
    return;
  }

  // Tampilkan indikator memuat pada area grid
  if (elements.popularMangaMount) {
    elements.popularMangaMount.innerHTML = `
      <div class="section-heading-bar">
        <h2 class="section-heading-text">Kategori: ${genre}</h2>
      </div>
      <div class="manga-grid standard-grid">
        <div style="grid-column: 1 / -1; padding: 36px 16px; text-align: center; color: var(--text-secondary);">
          <p>Memuat koleksi manga genre ${genre}...</p>
        </div>
      </div>
    `;
  }

  // Sembunyikan section update terbaru sementara agar beranda fokus pada hasil genre
  if (elements.latestMangaMount) {
    elements.latestMangaMount.closest(".home-content-section")?.classList.add("hidden");
  }

  // Ambil data manga sesuai genre yang dipilih
  const results = await fetchMangasByGenre(genre, 18);

  renderMangaSection(elements.popularMangaMount, {
    title: `Manga Populer: ${genre}`,
    actionLabel: "Jelajahi Semua",
    onActionClick: () => {
      state.setView("explore", { genre });
    },
    mangas: results,
    isRanked: false,
    onSelectManga: (m) => {
      state.currentManga = m;
      state.setView("detail", { manga: m });
    }
  });
}

async function openHistoryItem(historyItem) {
  let manga = popularMangaList.find((m) => m.id === historyItem.mangaId);
  if (!manga) manga = CURATED_MANGAS.find((m) => m.id === historyItem.mangaId);
  if (!manga) {
    manga = {
      id: historyItem.mangaId,
      title: historyItem.mangaTitle,
      cover: historyItem.cover,
      chapters: []
    };
  }

  const chapters = (manga.chapters && manga.chapters.length > 0)
    ? manga.chapters
    : await getMangaChapters(manga);
  manga.chapters = chapters;

  const targetCh =
    chapters.find((c) => c.id === historyItem.chapterId) ||
    chapters.find((c) => c.number && historyItem.chapterNumber && String(c.number) === String(historyItem.chapterNumber)) ||
    chapters[0];
  if (targetCh) {
    openChapterInReader(manga, targetCh, chapters, historyItem.pageIndex || 0);
  } else {
    state.setView("detail", { manga });
  }
}

// ==========================================================================
// View 2: Jelajah & Pencarian (Explore)
// ==========================================================================

let searchDebounceTimer = null;
let activeSearchSeq = 0;

function filterBySource(list, source) {
  if (source === "curated") {
    return list.filter((m) => m.id.startsWith("curated-"));
  } else if (source === "komiku") {
    return list.filter((m) => m.source === "Komiku" || m.komikuSlug || m.id.startsWith("komiku-"));
  } else if (source === "mangadex") {
    return list.filter((m) => m.mangadexId && !m.id.startsWith("curated-"));
  }
  return list;
}

function setupExploreEvents() {
  if (elements.searchInput) {
    elements.searchInput.addEventListener("input", (e) => {
      const q = e.target.value;
      if (elements.btnClearSearch) elements.btnClearSearch.classList.toggle("hidden", !q);

      // Tampilkan hasil pencarian lokal instan selagi menunggu debounce
      const cleanQ = (q || "").trim();
      if (cleanQ) {
        const instantMatches = filterBySource(searchLocalManga(cleanQ, 30), exploreSourceFilter);
        if (instantMatches.length > 0) {
          renderExploreGrid(instantMatches);
          if (elements.exploreResultsStatus) {
            elements.exploreResultsStatus.textContent = `Menampilkan ${instantMatches.length} hasil lokal, mencari sumber online...`;
          }
        }
      }

      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(() => handleSearch(q), 220);
    });
  }

  if (elements.btnClearSearch) {
    elements.btnClearSearch.addEventListener("click", () => {
      if (elements.searchInput) elements.searchInput.value = "";
      elements.btnClearSearch.classList.add("hidden");
      handleSearch("");
      elements.searchInput?.focus();
    });
  }

  elements.sourceChips.forEach((chip) => {
    chip.addEventListener("click", () => {
      elements.sourceChips.forEach((c) => c.classList.remove("active"));
      chip.classList.add("active");
      exploreSourceFilter = chip.getAttribute("data-source") || "all";
      handleSearch(elements.searchInput?.value || "");
    });
  });
}

function renderExploreGenreBar(selectedGenre = "Semua") {
  if (!elements.exploreGenresMount) return;
  renderGenreTabs(elements.exploreGenresMount, COMPLETE_GENRE_LIST, selectedGenre, (genre) => {
    state.activeGenre = genre;
    renderExploreGenreBar(genre);
    if (elements.searchInput) elements.searchInput.value = "";
    if (elements.btnClearSearch) elements.btnClearSearch.classList.add("hidden");
    loadExploreByGenre(genre);
  });
}

async function loadExploreByGenre(genre) {
  if (genre === "Semua") {
    handleSearch("");
    return;
  }

  if (elements.exploreResultsStatus) {
    elements.exploreResultsStatus.textContent = `Memuat koleksi genre "${genre}"...`;
  }

  if (elements.exploreResultsGrid) {
    elements.exploreResultsGrid.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 40px 16px; text-align: center; color: var(--text-secondary);">
        <p>Mengambil daftar komik kategori ${genre}...</p>
      </div>
    `;
  }

  const results = await fetchMangasByGenre(genre, 30);
  renderExploreGrid(results);

  if (elements.exploreResultsStatus) {
    elements.exploreResultsStatus.textContent = `Menampilkan ${results.length} manga kategori "${genre}"`;
  }
}

async function handleSearch(query) {
  const cleanQ = (query || "").trim();
  const currentSeq = ++activeSearchSeq;

  if (!cleanQ) {
    const list = filterBySource(CURATED_MANGAS, exploreSourceFilter);
    renderExploreGrid(list);
    if (elements.exploreResultsStatus) {
      elements.exploreResultsStatus.textContent = "Menampilkan katalog manga pilihan";
    }
    return;
  }

  // Tampilkan hasil lokal instan terlebih dahulu
  const localList = filterBySource(searchLocalManga(cleanQ, 30), exploreSourceFilter);
  if (localList.length > 0) {
    renderExploreGrid(localList);
  }

  if (elements.exploreResultsStatus) {
    elements.exploreResultsStatus.textContent = localList.length > 0
      ? `Menampilkan ${localList.length} hasil lokal, menyinkronkan online...`
      : `Mencari "${cleanQ}" dari semua sumber...`;
  }

  try {
    let results = await searchManga(cleanQ, 30);
    if (currentSeq !== activeSearchSeq) return; // Discard jika pengguna sudah mengetik kueri baru

    results = filterBySource(results, exploreSourceFilter);
    renderExploreGrid(results);
    if (elements.exploreResultsStatus) {
      elements.exploreResultsStatus.textContent = results.length > 0
        ? `Ditemukan ${results.length} hasil untuk "${cleanQ}"`
        : `Tidak ada manga yang ditemukan untuk "${cleanQ}"`;
    }
  } catch {
    if (currentSeq !== activeSearchSeq) return;
    renderExploreGrid(localList.length > 0 ? localList : CURATED_MANGAS);
  }
}

function renderExploreGrid(list) {
  if (!elements.exploreResultsGrid) return;
  elements.exploreResultsGrid.innerHTML = "";

  if (!list || list.length === 0) {
    elements.exploreResultsGrid.innerHTML = `
      <div class="empty-state-box" style="grid-column: 1 / -1; padding: 48px; text-align: center; color: var(--text-tertiary);">
        <p style="font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px;">Tidak ada manga yang ditemukan</p>
        <p style="font-size: 13px;">Coba gunakan kata kunci lain seperti Solo Leveling, One Piece, atau Kanojo mo Kanojo.</p>
      </div>
    `;
    return;
  }

  list.forEach((manga) => {
    const card = createMangaCard(manga, {
      onSelect: (m) => {
        state.currentManga = m;
        state.setView("detail", { manga: m });
      }
    });
    elements.exploreResultsGrid.appendChild(card);
  });
}

// ==========================================================================
// View 3: Detail Manga
// ==========================================================================

async function renderMangaDetail(manga) {
  if (!elements.detailTitle) return;

  elements.detailCoverImg.src = manga.cover;
  elements.detailCoverImg.onerror = function () {
    this.src = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80";
  };
  if (elements.detailBackdrop) {
    elements.detailBackdrop.style.backgroundImage = `url('${manga.cover}')`;
  }

  elements.detailTitle.textContent = manga.title;
  elements.detailAuthor.textContent = manga.author ? `Pengarang: ${manga.author}` : "Pengarang Tidak Diketahui";
  elements.detailTypeBadge.textContent = manga.type || "Manga";

  const isCompleted = manga.statusId === "Tamat" || manga.status === "Completed";
  elements.detailStatus.textContent = isCompleted ? "Tamat" : "Berjalan";
  const ratingScore = manga.rating ? Number(manga.rating).toFixed(1) : "8.8";
  elements.detailRating.innerHTML = `${icons.star("icon-rating-svg", 13, true)} <span>${ratingScore}</span>`;

  // Sinopsis
  const localMatch = CURATED_MANGAS.find((m) => m.id === manga.id || m.title === manga.title);
  const synopsisText = (localMatch && localMatch.synopsis) || manga.synopsis || "Sinopsis belum tersedia.";
  elements.detailSynopsis.textContent = synopsisText;

  // Tombol Terjemahan
  const isEnglish = /^(After|In |When |The |A |An |During|Following)/i.test(synopsisText.trim()) && !synopsisText.includes(" yang ");
  if (elements.btnTranslateSynopsis) {
    elements.btnTranslateSynopsis.classList.toggle("hidden", !isEnglish);
    elements.btnTranslateSynopsis.onclick = () => {
      elements.detailSynopsis.textContent = `[Bahasa Indonesia]: ${synopsisText
        .replace(/After harboring an unrequited love for years/i, "Setelah memendam cinta bertepuk sebelah tangan bertahun-tahun")
        .replace(/finally gets to date/i, "akhirnya berpacaran dengan")
        .replace(/childhood friend/i, "teman masa kecilnya")}`;
      elements.btnTranslateSynopsis.classList.add("hidden");
    };
  }

  updateFavButtonState(manga.id);

  // Genre tags
  if (elements.detailGenresList) {
    elements.detailGenresList.innerHTML = "";
    (manga.genres || []).forEach((genre) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "genre-pill-tag";
      btn.textContent = genre;
      btn.title = `Lihat komik kategori ${genre}`;
      btn.style.cursor = "pointer";
      btn.onclick = () => {
        state.activeGenre = genre;
        state.setView("explore", { genre });
      };
      elements.detailGenresList.appendChild(btn);
    });
  }

  // Load Chapter list
  elements.detailChaptersList.innerHTML = `
    <div style="padding: 32px; text-align: center; color: var(--text-tertiary);">
      <p style="font-size: 13px;">Memuat daftar chapter...</p>
    </div>
  `;

  try {
    const chapters = await getMangaChapters(manga);
    manga.chapters = chapters;

    const idCount = chapters.filter((c) => c.lang === "id").length;
    currentChapterLang = idCount > 0 ? "id" : "all";

    renderChaptersList(manga, chapters);

    // Tombol Mulai Baca Chapter 1
    if (chapters.length > 0) {
      elements.btnDetailStartRead.disabled = false;
      elements.btnDetailStartRead.style.opacity = "1";
      const sortedAsc = [...chapters].sort((a, b) => parseFloat(a.number || 0) - parseFloat(b.number || 0));
      const firstCh = (currentChapterLang === "id" ? sortedAsc.find((c) => c.lang === "id") : sortedAsc[0]) || sortedAsc[0];
      const readBtnLabel = elements.btnDetailStartRead.querySelector("span");
      if (readBtnLabel && firstCh) {
        readBtnLabel.textContent = `Baca Chapter ${firstCh.number}`;
      }
      elements.btnDetailStartRead.onclick = () => {
        openChapterInReader(manga, firstCh, chapters);
      };
    } else {
      elements.btnDetailStartRead.disabled = true;
      elements.btnDetailStartRead.style.opacity = "0.5";
    }
  } catch {
    elements.detailChaptersList.innerHTML = `
      <div style="padding: 24px; text-align: center; color: var(--text-tertiary);">
        <p style="font-size: 13px; color: var(--accent-primary);">Gagal memuat chapter.</p>
        <button id="btn-retry-chapters" style="margin-top: 8px; padding: 6px 14px; font-size: 12px; background: var(--bg-elevated); border: 1px solid var(--border-subtle); border-radius: 4px; cursor: pointer;">Coba Lagi</button>
      </div>
    `;
    document.getElementById("btn-retry-chapters")?.addEventListener("click", () => renderMangaDetail(manga));
  }

  // Favorite toggle
  elements.btnDetailFavToggle.onclick = () => {
    state.toggleFavorite(manga);
    updateFavButtonState(manga.id);
  };

  // Sort toggle
  elements.btnSortChapters.onclick = () => {
    currentChapterSortOrder = currentChapterSortOrder === "desc" ? "asc" : "desc";
    elements.sortOrderLabel.textContent = currentChapterSortOrder === "desc" ? "Terbaru" : "Awal";
    renderChaptersList(manga, manga.chapters || []);
  };
}

function updateFavButtonState(mangaId) {
  const isFav = state.isFavorite(mangaId);
  if (elements.detailFavLabel) elements.detailFavLabel.textContent = isFav ? "Tersimpan" : "Favorit";
  if (elements.detailFavIcon) {
    elements.detailFavIcon.innerHTML = isFav
      ? '<path d="m19 21-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" fill="currentColor"/>'
      : '<path d="m19 21-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>';
  }
  elements.btnDetailFavToggle?.classList.toggle("active", isFav);
}

function renderChaptersList(manga, chapters) {
  if (!chapters || chapters.length === 0) {
    elements.detailChapterCount.textContent = "0 Chapter";
    elements.detailChaptersList.innerHTML = `
      <div style="padding: 24px; text-align: center; color: var(--text-tertiary);">
        <p style="font-size: 13px;">Belum tersedia bab bacaan untuk judul ini.</p>
      </div>
    `;
    return;
  }

  const idChapters = chapters.filter((c) => c.lang === "id");
  const enChapters = chapters.filter((c) => c.lang === "en");

  // Filter Tabs
  elements.detailLangFilterBar.innerHTML = `
    <button class="lang-tab-btn ${currentChapterLang === "id" ? "active" : ""}" data-lang="id" type="button">
      Indonesia (${idChapters.length})
    </button>
    ${enChapters.length > 0 ? `
      <button class="lang-tab-btn ${currentChapterLang === "en" ? "active" : ""}" data-lang="en" type="button">
        English (${enChapters.length})
      </button>
    ` : ""}
    <button class="lang-tab-btn ${currentChapterLang === "all" ? "active" : ""}" data-lang="all" type="button">
      Semua (${chapters.length})
    </button>
  `;

  elements.detailLangFilterBar.querySelectorAll(".lang-tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      currentChapterLang = btn.getAttribute("data-lang");
      renderChaptersList(manga, chapters);
    });
  });

  let displayChapters = chapters;
  if (currentChapterLang === "id") {
    displayChapters = idChapters.length > 0 ? idChapters : chapters;
  } else if (currentChapterLang === "en") {
    displayChapters = enChapters.length > 0 ? enChapters : chapters;
  }

  elements.detailChapterCount.textContent = `${displayChapters.length} Chapter Tersedia`;

  // Sort
  const sorted = [...displayChapters].sort((a, b) => {
    const numA = parseFloat(a.number) || 0;
    const numB = parseFloat(b.number) || 0;
    return currentChapterSortOrder === "desc" ? numB - numA : numA - numB;
  });

  elements.detailChaptersList.innerHTML = "";
  sorted.forEach((ch) => {
    const row = document.createElement("div");
    row.className = "chapter-row-item";
    row.setAttribute("tabindex", "0");
    row.setAttribute("role", "button");
    row.setAttribute("aria-label", `Baca Chapter ${ch.number}: ${ch.title}`);

    const isId = ch.lang === "id";
    row.innerHTML = `
      <div class="chapter-item-meta">
        <div class="chapter-item-title-line">
          <span class="chapter-num-text">Chapter ${ch.number}</span>
          ${isId ? '<span class="chapter-lang-badge">ID</span>' : ""}
        </div>
        <span class="chapter-item-date">${ch.date || "Terbaru"} · ${ch.source || "Komikindo / Komiku"}</span>
      </div>
      <span class="btn-read-action">Baca</span>
    `;

    const openAction = () => openChapterInReader(manga, ch, chapters);
    row.addEventListener("click", openAction);
    row.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        openAction();
      }
    });

    elements.detailChaptersList.appendChild(row);
  });
}

// ==========================================================================
// View 4: Reader Imersif
// ==========================================================================

async function openChapterInReader(manga, chapter, allChapters = [], initialPageIndex = 0) {
  state.currentManga = manga;
  state.currentChapter = chapter;
  allMangaChaptersForReader = allChapters.length > 0 ? allChapters : manga.chapters || [];

  elements.readerMangaTitle.textContent = manga.title;
  if (elements.readerChapterTitle) {
    elements.readerChapterTitle.textContent = `Chapter ${chapter.number}`;
  }

  // Selalu reset scroll ke posisi paling atas saat membuka chapter baru
  if (elements.readerCanvasContainer) {
    elements.readerCanvasContainer.scrollTop = 0;
  }

  state.setView("reader");

  elements.readerScrollView.innerHTML = `
    <div style="padding: 60px 16px; text-align: center; color: var(--text-tertiary);">
      <p style="font-size: 14px; color: #FFFFFF;">Memuat halaman chapter ${chapter.number}...</p>
    </div>
  `;

  try {
    const pages = await getChapterPages(chapter, manga);
    if (!pages || pages.length === 0) throw new Error("Halaman tidak tersedia");

    state.currentPages = pages;
    state.currentPageIndex = initialPageIndex;

    state.recordHistory(manga, chapter, initialPageIndex);
    updateChapterNavButtons();
    renderReaderPages();

    if (elements.readerCanvasContainer) {
      elements.readerCanvasContainer.scrollTop = 0;
    }
  } catch {
    elements.readerScrollView.innerHTML = `
      <div style="padding: 60px 16px; text-align: center; color: #94A3B8;">
        <p style="font-size: 14px; color: var(--accent-primary); margin-bottom: 8px;">Gagal memuat halaman Chapter ${chapter.number}.</p>
        <button id="btn-retry-reader" style="padding: 6px 16px; font-size: 12px; background: var(--bg-elevated); color: #FFFFFF; border: 1px solid var(--border-subtle); border-radius: 4px; cursor: pointer;">Coba Lagi</button>
      </div>
    `;
    document.getElementById("btn-retry-reader")?.addEventListener("click", () =>
      openChapterInReader(manga, chapter, allChapters, initialPageIndex)
    );
  }
}

function updateChapterNavButtons() {
  const currentLang = state.currentChapter?.lang || "id";
  const langFiltered = allMangaChaptersForReader.some((c) => c.lang === currentLang)
    ? allMangaChaptersForReader.filter((c) => c.lang === currentLang)
    : allMangaChaptersForReader;

  const seenNumbers = new Set();
  const sorted = [];
  const rawSorted = [...langFiltered].sort(
    (a, b) => (parseFloat(a.number) || 0) - (parseFloat(b.number) || 0)
  );
  for (const ch of rawSorted) {
    const key = ch.number ? String(ch.number) : ch.id;
    if (!seenNumbers.has(key)) {
      seenNumbers.add(key);
      sorted.push(ch);
    }
  }

  const currentIdx = sorted.findIndex(
    (c) => c.id === state.currentChapter?.id || (c.number && state.currentChapter?.number && String(c.number) === String(state.currentChapter.number))
  );

  elements.btnReaderPrevChapter.disabled = currentIdx <= 0;
  elements.btnReaderNextChapter.disabled = currentIdx < 0 || currentIdx >= sorted.length - 1;

  elements.btnReaderPrevChapter.onclick = () => {
    if (currentIdx > 0) openChapterInReader(state.currentManga, sorted[currentIdx - 1], allMangaChaptersForReader);
  };
  elements.btnReaderNextChapter.onclick = () => {
    if (currentIdx < sorted.length - 1)
      openChapterInReader(state.currentManga, sorted[currentIdx + 1], allMangaChaptersForReader);
  };

  // Sinkronkan daftar chapter pada dropdown toolbar reader
  if (elements.readerChapterSelect) {
    elements.readerChapterSelect.innerHTML = "";
    sorted.forEach((ch) => {
      const opt = document.createElement("option");
      opt.value = ch.id || ch.number;
      opt.textContent = `Chapter ${ch.number}${ch.lang === "id" ? " [ID]" : ""}`;
      const isSelected = ch.id === state.currentChapter?.id || String(ch.number) === String(state.currentChapter?.number);
      if (isSelected) {
        opt.selected = true;
      }
      elements.readerChapterSelect.appendChild(opt);
    });

    elements.readerChapterSelect.onchange = (e) => {
      const selectedVal = e.target.value;
      const targetCh = sorted.find((c) => (c.id || c.number) === selectedVal || String(c.number) === selectedVal);
      if (targetCh) {
        openChapterInReader(state.currentManga, targetCh, allMangaChaptersForReader);
      }
    };
  }
}

function renderReaderPages() {
  const pages = state.currentPages;
  if (!pages || pages.length === 0) return;

  if (state.readerMode === "webtoon") {
    elements.readerScrollView.classList.remove("hidden");
    elements.readerPagedView.classList.add("hidden");
    elements.readModeLabel.textContent = "Webtoon";

    elements.readerScrollView.innerHTML = "";
    pages.forEach((url, idx) => {
      const img = document.createElement("img");
      img.className = "reader-page-image";
      img.referrerPolicy = "no-referrer";
      img.src = url;
      img.alt = `Halaman ${idx + 1}`;
      img.loading = idx < 3 ? "eager" : "lazy";
      img.onerror = () => {
        if (!img.dataset.retried) {
          img.dataset.retried = "1";
          img.src = `/api/proxy-image?url=${encodeURIComponent(url)}`;
        }
      };
      elements.readerScrollView.appendChild(img);
    });

    elements.readerPageIndicator.textContent = `Total ${pages.length} Halaman`;

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
    elements.readerScrollView.classList.add("hidden");
    elements.readerPagedView.classList.remove("hidden");
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
    elements.btnPagedPrev.disabled = idx <= 0;
    elements.btnPagedNext.disabled = idx >= pages.length - 1;
    state.recordHistory(state.currentManga, state.currentChapter, idx);
  }
}

function setupReaderControls() {
  elements.btnReaderExit.addEventListener("click", () => {
    state.setView("detail", { manga: state.currentManga });
  });

  elements.btnReaderHome.addEventListener("click", () => {
    state.setView("home");
  });

  elements.btnToggleReadMode.addEventListener("click", () => {
    const next = state.readerMode === "webtoon" ? "paged" : "webtoon";
    state.setReaderMode(next);
    renderReaderPages();
  });

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

  elements.btnReaderFullscreen.addEventListener("click", () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  });
}

// ==========================================================================
// View 5: Favorit & View 6: Riwayat
// ==========================================================================

function renderLibraryFavorites() {
  if (!elements.libraryFavoritesGrid) return;
  elements.libraryFavoritesGrid.innerHTML = "";

  if (state.favorites.length === 0) {
    elements.libraryFavoritesGrid.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 48px; text-align: center; color: var(--text-tertiary);">
        <p style="font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px;">Belum ada komik favorit</p>
        <p style="font-size: 13px;">Klik tombol "Favorit" pada halaman detail manga untuk menyimpannya di sini.</p>
      </div>
    `;
    return;
  }

  state.favorites.forEach((manga) => {
    const card = createMangaCard(manga, {
      onSelect: (m) => {
        state.currentManga = m;
        state.setView("detail", { manga: m });
      }
    });
    elements.libraryFavoritesGrid.appendChild(card);
  });
}

function renderHistoryView() {
  if (!elements.historyItemsList) return;
  elements.historyItemsList.innerHTML = "";

  if (state.history.length === 0) {
    elements.historyItemsList.innerHTML = `
      <div style="padding: 48px; text-align: center; color: var(--text-tertiary);">
        <p style="font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px;">Riwayat bacaan kosong</p>
        <p style="font-size: 13px;">Bab yang kamu baca akan otomatis tersimpan di sini.</p>
      </div>
    `;
    return;
  }

  state.history.forEach((item) => {
    const row = document.createElement("div");
    row.className = "continue-item";
    row.style.minWidth = "auto";
    row.style.width = "100%";
    row.style.marginBottom = "8px";

    const dateStr = item.lastReadAt
      ? new Date(item.lastReadAt).toLocaleDateString("id-ID", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })
      : "Terbaru";

    row.innerHTML = `
      <div class="continue-thumb-wrap">
        <img class="continue-thumb-img" src="${item.cover}" alt="Cover ${item.mangaTitle}" />
      </div>
      <div class="continue-info-wrap">
        <h5 class="continue-manga-title">${item.mangaTitle}</h5>
        <p class="continue-meta-text">Chapter ${item.chapterNumber} · ${dateStr}</p>
      </div>
      <span class="btn-read-action">Lanjut</span>
    `;

    row.addEventListener("click", () => openHistoryItem(item));
    elements.historyItemsList.appendChild(row);
  });
}

function setupHistoryEvents() {
  elements.btnClearHistory?.addEventListener("click", () => {
    if (confirm("Hapus seluruh riwayat membaca?")) {
      state.clearHistory();
      renderHistoryView();
    }
  });
}

// Start Application
document.addEventListener("DOMContentLoaded", initApp);
if (document.readyState === "complete" || document.readyState === "interactive") {
  initApp();
}
