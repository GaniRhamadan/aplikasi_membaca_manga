import { icons } from "./icons.js";

/**
 * Creates a clean, poster-first manga card with 2:3 aspect ratio
 * Follows strict design rules:
 * - 2:3 poster ratio, object-fit: cover, border-radius 6px
 * - No heavy framed box around the card
 * - Under cover: Title max 2 lines clamp, then 1 line metadata
 * - Optional rank number (1, 2, 3...) for Popular Leaderboard
 */
export function createMangaCard(manga, options = {}) {
  const { rank = null, onSelect = null } = options;

  const card = document.createElement("article");
  card.className = "manga-card" + (rank ? " ranked" : "");
  card.setAttribute("tabindex", "0");
  card.setAttribute("role", "button");
  card.setAttribute("aria-label", `Buka detail ${manga.title}`);

  const isCompleted = manga.statusId === "Tamat" || manga.status === "Completed";
  const displayRating = manga.rating ? Number(manga.rating).toFixed(1) : "8.8";
  const latestCh = manga.totalChapters ? `Ch. ${manga.totalChapters}` : (manga.type || "Manga");

  const rankHtml = rank
    ? `<div class="card-rank ${rank <= 3 ? "rank-top" : ""}">${rank}</div>`
    : "";

  card.innerHTML = `
    <div class="card-poster-wrap">
      ${rankHtml}
      <img
        class="card-poster-img"
        src="${manga.cover}"
        alt="Cover ${manga.title}"
        loading="lazy"
        onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80'"
      />
      <div class="card-poster-overlay">
        <span class="poster-rating">${icons.star("icon-rating", 12, true)} ${displayRating}</span>
        ${isCompleted ? '<span class="poster-status-tag">Tamat</span>' : ""}
      </div>
    </div>
    <div class="card-meta-wrap">
      <h4 class="card-title" title="${manga.title}">${manga.title}</h4>
      <div class="card-sub-row">
        <span class="card-sub-meta">${latestCh}</span>
        <span class="card-sub-dot">·</span>
        <span class="card-sub-source">${manga.source || "ID"}</span>
      </div>
    </div>
  `;

  const handleClick = () => {
    if (typeof onSelect === "function") {
      onSelect(manga);
    }
  };

  card.addEventListener("click", handleClick);
  card.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      handleClick();
    }
  });

  return card;
}
