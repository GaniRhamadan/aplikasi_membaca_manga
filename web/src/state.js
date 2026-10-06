// Manajemen State Aplikasi MangaVerse

const STORAGE_KEYS = {
  FAVORITES: "mangaverse_favorites_v1",
  HISTORY: "mangaverse_history_v1",
  PREFS: "mangaverse_prefs_v1"
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
  deviceMode: "phone", // 'phone' | 'desktop'
  theme: "dark",
  readerMode: "webtoon", // 'webtoon' | 'paged'
  readerDirection: "ltr" // 'ltr' | 'rtl'
});

export const state = {
  currentView: "home", // 'home' | 'explore' | 'library' | 'history' | 'detail' | 'reader'
  activeNavTab: "home",
  
  // Kontrol tampilan emulator HP / Desktop
  deviceMode: savedPrefs.deviceMode || "phone",
  deviceOrientation: "portrait", // 'portrait' | 'landscape'
  theme: savedPrefs.theme || "dark",
  
  // Data Manga Aktif
  currentManga: null,
  currentChapter: null,
  currentPages: [],
  currentPageIndex: 0,
  
  // Preferensi Reader
  readerMode: savedPrefs.readerMode || "webtoon",
  readerDirection: savedPrefs.readerDirection || "ltr",
  readerZoom: "fit-width",
  readerControlsVisible: true,
  
  // Filter & Pencarian
  activeGenre: "Semua",
  searchQuery: "",
  
  // Data Koleksi (tersimpan di localStorage)
  favorites: loadStored(STORAGE_KEYS.FAVORITES, []),
  history: loadStored(STORAGE_KEYS.HISTORY, []),
  
  // Status UI
  isLoading: false,
  errorMessage: null,

  listeners: new Set(),

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  },

  notify(event, payload) {
    for (const listener of this.listeners) {
      listener(event, payload, this);
    }
  },

  setDeviceMode(mode) {
    this.deviceMode = mode;
    this.persistPrefs();
    this.notify("deviceModeChange", mode);
  },

  toggleDeviceOrientation() {
    this.deviceOrientation = this.deviceOrientation === "portrait" ? "landscape" : "portrait";
    this.notify("orientationChange", this.deviceOrientation);
  },

  setTheme(theme) {
    this.theme = theme;
    this.persistPrefs();
    this.notify("themeChange", theme);
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

  setReaderDirection(dir) {
    this.readerDirection = dir;
    this.persistPrefs();
    this.notify("readerDirectionChange", dir);
  },

  isFavorite(mangaId) {
    return this.favorites.some(f => f.id === mangaId);
  },

  toggleFavorite(manga) {
    const idx = this.favorites.findIndex(f => f.id === manga.id);
    if (idx >= 0) {
      this.favorites.splice(idx, 1);
    } else {
      this.favorites.unshift({
        id: manga.id,
        mangadexId: manga.mangadexId,
        title: manga.title,
        cover: manga.cover,
        author: manga.author,
        statusId: manga.statusId,
        genres: manga.genres,
        rating: manga.rating,
        savedAt: new Date().toISOString()
      });
    }
    saveStored(STORAGE_KEYS.FAVORITES, this.favorites);
    this.notify("favoritesChange", this.favorites);
  },

  recordHistory(manga, chapter, pageIndex = 0) {
    // Hapus entry lama dari manga yang sama jika ada
    this.history = this.history.filter(h => h.mangaId !== manga.id);
    this.history.unshift({
      mangaId: manga.id,
      mangadexId: manga.mangadexId,
      mangaTitle: manga.title,
      cover: manga.cover,
      chapterId: chapter.id,
      chapterNumber: chapter.number,
      chapterTitle: chapter.title,
      pageIndex,
      lastReadAt: new Date().toLocaleDateString("id-ID", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit"
      })
    });
    // Batasi riwayat maksimum 30 item
    if (this.history.length > 30) this.history.pop();
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
      deviceMode: this.deviceMode,
      theme: this.theme,
      readerMode: this.readerMode,
      readerDirection: this.readerDirection
    });
  }
};
