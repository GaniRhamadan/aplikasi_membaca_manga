import { icons } from "./icons.js";
import { createMangaCard } from "./MangaCard.js";

/**
 * Renders a clean section of manga items with header, optional "Lihat Semua" text link,
 * and responsive grid or ranked cover row.
 */
export function renderMangaSection(container, options = {}) {
  const {
    title = "",
    actionLabel = null,
    onActionClick = null,
    mangas = [],
    isRanked = false,
    onSelectManga = null
  } = options;

  if (!container) return;
  container.innerHTML = "";

  // Section Header (No emojis, clean text with optional link)
  const header = document.createElement("div");
  header.className = "section-heading-bar";

  const titleEl = document.createElement("h2");
  titleEl.className = "section-heading-text";
  titleEl.textContent = title;
  header.appendChild(titleEl);

  if (actionLabel && typeof onActionClick === "function") {
    const actionBtn = document.createElement("button");
    actionBtn.className = "section-link-action";
    actionBtn.type = "button";
    actionBtn.innerHTML = `<span>${actionLabel}</span> ${icons.chevronRight("icon-chevron", 14)}`;
    actionBtn.addEventListener("click", onActionClick);
    header.appendChild(actionBtn);
  }

  container.appendChild(header);

  // Grid / Row container
  const grid = document.createElement("div");
  grid.className = isRanked ? "manga-grid ranked-row" : "manga-grid standard-grid";

  if (!mangas || mangas.length === 0) {
    grid.innerHTML = `
      <div class="empty-state-notice">
        <p class="empty-text">Belum ada manga yang tersedia di kategori ini.</p>
      </div>
    `;
  } else {
    mangas.forEach((manga, index) => {
      const card = createMangaCard(manga, {
        rank: isRanked ? index + 1 : null,
        onSelect: onSelectManga
      });
      grid.appendChild(card);
    });
  }

  container.appendChild(grid);
}

/**
 * Renders the Genre Tabs as clean segmented text with active underline
 * Replaces bulky bubble pills
 */
export function renderGenreTabs(container, genres, activeGenre, onSelectGenre) {
  if (!container) return;
  container.innerHTML = "";

  const bar = document.createElement("div");
  bar.className = "genre-segmented-bar";
  bar.setAttribute("role", "tablist");
  bar.setAttribute("aria-label", "Pilihan Genre Manga");

  genres.forEach((genre) => {
    const btn = document.createElement("button");
    btn.className = "genre-tab-btn" + (genre === activeGenre ? " active" : "");
    btn.type = "button";
    btn.setAttribute("role", "tab");
    btn.setAttribute("aria-selected", genre === activeGenre);
    btn.textContent = genre;

    btn.addEventListener("click", () => {
      if (typeof onSelectGenre === "function") {
        onSelectGenre(genre);
      }
    });

    bar.appendChild(btn);
  });

  container.appendChild(bar);
}
