import { icons } from "./icons.js";

/**
 * Navigation component:
 * - Desktop (≥768px): full top navbar with logo, nav links, live search bar, and theme toggle.
 * - Mobile (<768px): clean top mobile header + bottom navigation bar with 4 tabs.
 */
export function renderNavbar(elements, state, callbacks = {}) {
  const { onNavigate, onSearch, onToggleTheme } = callbacks;

  // 1. Desktop & Mobile Top Header
  const headerHtml = `
    <div class="navbar-inner">
      <!-- Logo / Brand -->
      <a class="nav-brand" href="#home" id="nav-brand-logo" title="MangaVerse - Beranda">
        <span class="brand-monogram">MV</span>
        <span class="brand-name">MangaVerse</span>
      </a>

      <!-- Desktop Nav Links -->
      <nav class="nav-links-desktop" aria-label="Navigasi Utama">
        <button class="nav-link-item ${state.currentView === "home" ? "active" : ""}" data-view="home" type="button">
          ${icons.home("nav-link-icon", 16)}
          <span>Beranda</span>
        </button>
        <button class="nav-link-item ${state.currentView === "explore" ? "active" : ""}" data-view="explore" type="button">
          ${icons.compass("nav-link-icon", 16)}
          <span>Jelajah</span>
        </button>
        <button class="nav-link-item ${state.currentView === "library" ? "active" : ""}" data-view="library" type="button">
          ${icons.bookmark("nav-link-icon", 16)}
          <span>Favorit</span>
        </button>
        <button class="nav-link-item ${state.currentView === "history" ? "active" : ""}" data-view="history" type="button">
          ${icons.history("nav-link-icon", 16)}
          <span>Riwayat</span>
        </button>
      </nav>

      <!-- Live Search Bar in Desktop Navbar -->
      <div class="nav-search-wrap">
        <div class="nav-search-input-box">
          ${icons.search("nav-search-icon", 16)}
          <input
            id="nav-search-input"
            type="search"
            placeholder="Cari judul manga, genre, pengarang..."
            autocomplete="off"
            value="${state.searchQuery || ""}"
          />
          <button id="btn-nav-search-clear" class="btn-search-clear ${state.searchQuery ? "" : "hidden"}" type="button" aria-label="Hapus kata kunci">
            ${icons.x("icon-x", 14)}
          </button>
        </div>
      </div>

      <!-- Right Utility Controls -->
      <div class="nav-actions">
        <!-- Quick Search Mobile Trigger -->
        <button id="btn-mobile-search-toggle" class="btn-icon-nav mobile-only" type="button" title="Pencarian">
          ${icons.search("nav-icon", 20)}
        </button>

        <!-- Theme Toggle Button -->
        <button id="btn-theme-switcher" class="btn-icon-nav" type="button" title="Ganti Tema (Terang/Gelap)" aria-label="Ganti Tema">
          ${state.theme === "dark" ? icons.sun("nav-icon", 19) : icons.moon("nav-icon", 19)}
        </button>
      </div>
    </div>
  `;

  if (elements.siteHeader) {
    elements.siteHeader.innerHTML = headerHtml;

    // Attach Top Nav Links
    elements.siteHeader.querySelectorAll(".nav-link-item").forEach((btn) => {
      btn.addEventListener("click", () => {
        const view = btn.getAttribute("data-view");
        if (typeof onNavigate === "function") onNavigate(view);
      });
    });

    // Brand click
    const brand = elements.siteHeader.querySelector("#nav-brand-logo");
    if (brand) {
      brand.addEventListener("click", (e) => {
        e.preventDefault();
        if (typeof onNavigate === "function") onNavigate("home");
      });
    }

    // Search Input
    const searchInput = elements.siteHeader.querySelector("#nav-search-input");
    const clearBtn = elements.siteHeader.querySelector("#btn-nav-search-clear");
    const mobileSearchBtn = elements.siteHeader.querySelector("#btn-mobile-search-toggle");

    if (searchInput) {
      searchInput.addEventListener("input", (e) => {
        const val = e.target.value;
        if (clearBtn) clearBtn.classList.toggle("hidden", !val);
        if (typeof onSearch === "function") onSearch(val);
      });
      searchInput.addEventListener("focus", () => {
        if (state.currentView !== "explore" && typeof onNavigate === "function") {
          onNavigate("explore");
        }
      });
    }

    if (clearBtn && searchInput) {
      clearBtn.addEventListener("click", () => {
        searchInput.value = "";
        clearBtn.classList.add("hidden");
        if (typeof onSearch === "function") onSearch("");
        searchInput.focus();
      });
    }

    if (mobileSearchBtn && typeof onNavigate === "function") {
      mobileSearchBtn.addEventListener("click", () => onNavigate("explore"));
    }

    // Theme Switcher
    const themeBtn = elements.siteHeader.querySelector("#btn-theme-switcher");
    if (themeBtn && typeof onToggleTheme === "function") {
      themeBtn.addEventListener("click", onToggleTheme);
    }
  }

  // 2. Mobile Bottom Navigation (Shown only on mobile screens <768px)
  const bottomNavHtml = `
    <div class="mobile-nav-inner">
      <button class="mobile-tab-btn ${state.currentView === "home" ? "active" : ""}" data-view="home" type="button">
        ${icons.home("tab-icon", 20)}
        <span class="tab-label">Beranda</span>
      </button>
      <button class="mobile-tab-btn ${state.currentView === "explore" ? "active" : ""}" data-view="explore" type="button">
        ${icons.compass("tab-icon", 20)}
        <span class="tab-label">Jelajah</span>
      </button>
      <button class="mobile-tab-btn ${state.currentView === "library" ? "active" : ""}" data-view="library" type="button">
        ${icons.bookmark("tab-icon", 20)}
        <span class="tab-label">Favorit</span>
      </button>
      <button class="mobile-tab-btn ${state.currentView === "history" ? "active" : ""}" data-view="history" type="button">
        ${icons.history("tab-icon", 20)}
        <span class="tab-label">Riwayat</span>
      </button>
    </div>
  `;

  if (elements.mobileNav) {
    elements.mobileNav.innerHTML = bottomNavHtml;

    elements.mobileNav.querySelectorAll(".mobile-tab-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const view = btn.getAttribute("data-view");
        if (typeof onNavigate === "function") onNavigate(view);
      });
    });
  }
}
