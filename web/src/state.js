// Manajemen State MangaVerse (Clean Production State)

const STORAGE_KEYS = {
  FAVORITES: "mangaverse_favorites_v2",
  HISTORY: "mangaverse_history_v2",
  PREFS: "mangaverse_prefs_v2"
};

function loadStored(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function saveStored(key, val) {
  try {
    localStorage.setItem(key, JSON.stringify(val));
  } catch (e) {
    console.error("Gagal menyimpan ke localStorage:", e);
  }
}

const savedPrefs = loadStored(STORAGE_KEYS.PREFS, {
  theme: "dark",
  readerMode: "webtoon", // 'webtoon' | 'paged'
  readerDirection: "ltr"
});

export const state = {
  currentView: "home", // 'home' | 'explore' | 'detail' | 'reader' | 'library' | 'history'
  activeNavTab: "home",

  theme: savedPrefs.theme || "dark",

  // Data Manga & Reader Aktif
  currentManga: null,
  currentChapter: null,
  currentPages: [],
  currentPageIndex: 0,

  // Preferensi Reader
  readerMode: savedPrefs.readerMode || "webtoon",
  readerDirection: savedPrefs.readerDirection || "ltr",

  // Filter & Pencarian
  activeGenre: "Semua",
  searchQuery: "",

  // Data Koleksi (tersimpan di localStorage)
  favorites: loadStored(STORAGE_KEYS.FAVORITES, []),
  history: loadStored(STORAGE_KEYS.HISTORY, []),

  listeners: new Set(),

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  },

  notify(event, payload) {
    for (const listener of this.listeners) {
      try {
        listener(event, payload, this);
      } catch (err) {
        console.error("Error in state listener:", err);
      }
    }
  },

  setTheme(theme) {
    this.theme = theme;
    this.persistPrefs();
    this.notify("themeChange", theme);
  },

  toggleTheme() {
    const next = this.theme === "dark" ? "light" : "dark";
    this.setTheme(next);
  },

  setView(view, params = {}) {
    this.currentView = view;
    if (["home", "explore", "library", "history"].includes(view)) {
      this.activeNavTab = view;
    }
    this.notify("viewChange", { view, params });
  },

  setReaderMode(mode) {
    this.readerMode = mode;
    this.persistPrefs();
    this.notify("readerModeChange", mode);
  },

  isFavorite(mangaId) {
    return this.favorites.some((f) => f.id === mangaId);
  },

  toggleFavorite(manga) {
    const idx = this.favorites.findIndex((f) => f.id === manga.id);
    if (idx >= 0) {
      this.favorites.splice(idx, 1);
    } else {
      this.favorites.unshift({
        id: manga.id,
        mangadexId: manga.mangadexId,
        komikuSlug: manga.komikuSlug,
        komikandoSlug: manga.komikandoSlug,
        title: manga.title,
        cover: manga.cover,
        author: manga.author,
        statusId: manga.statusId,
        status: manga.status,
        genres: manga.genres,
        rating: manga.rating,
        totalChapters: manga.totalChapters,
        source: manga.source,
        savedAt: new Date().toISOString()
      });
    }
    saveStored(STORAGE_KEYS.FAVORITES, this.favorites);
    this.notify("favoritesChange", this.favorites);
  },

  recordHistory(manga, chapter, pageIndex = 0) {
    this.history = this.history.filter((h) => h.mangaId !== manga.id);
    this.history.unshift({
      mangaId: manga.id,
      mangadexId: manga.mangadexId,
      mangaTitle: manga.title,
      cover: manga.cover,
      chapterId: chapter.id,
      chapterNumber: chapter.number,
      chapterTitle: chapter.title || `Chapter ${chapter.number}`,
      pageIndex,
      lastReadAt: new Date().toISOString()
    });

    if (this.history.length > 50) {
      this.history = this.history.slice(0, 50);
    }

    saveStored(STORAGE_KEYS.HISTORY, this.history);
    this.notify("historyChange", this.history);
  },

  clearHistory() {
    this.history = [];
    saveStored(STORAGE_KEYS.HISTORY, []);
    this.notify("historyChange", []);
  },

  persistPrefs() {
    saveStored(STORAGE_KEYS.PREFS, {
      theme: this.theme,
      readerMode: this.readerMode,
      readerDirection: this.readerDirection
    });
  }
};
