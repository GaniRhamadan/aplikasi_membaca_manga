import { icons } from "./icons.js";

/**
 * Renders the Continue Reading horizontal row
 * Strict requirements:
 * - Horizontal list of small 2:3 covers with thin progress bar
 * - Label: "Ch. 144 · 8 Okt"
 * - Action: click cover or play icon button
 */
export function renderContinueReading(container, historyList, onResume) {
  if (!container) return;

  if (!historyList || historyList.length === 0) {
    container.closest(".section-continue")?.classList.add("hidden");
    container.innerHTML = "";
    return;
  }

  container.closest(".section-continue")?.classList.remove("hidden");
  container.innerHTML = "";

  const row = document.createElement("div");
  row.className = "continue-scroll-row";

  historyList.slice(0, 6).forEach((item) => {
    const card = document.createElement("div");
    card.className = "continue-item";
    card.setAttribute("tabindex", "0");
    card.setAttribute("role", "button");
    card.setAttribute("aria-label", `Lanjut membaca ${item.mangaTitle} Chapter ${item.chapterNumber}`);

    const dateStr = item.lastReadAt
      ? new Date(item.lastReadAt).toLocaleDateString("id-ID", { day: "numeric", month: "short" })
      : "Terbaru";

    card.innerHTML = `
      <div class="continue-thumb-wrap">
        <img
          class="continue-thumb-img"
          src="${item.cover}"
          alt="Cover ${item.mangaTitle}"
          loading="lazy"
          onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=200&auto=format&fit=crop&q=80'"
        />
        <div class="continue-play-overlay">
          ${icons.play("icon-continue-play", 16)}
        </div>
        <div class="continue-progress-bar">
          <div class="progress-fill" style="width: 75%"></div>
        </div>
      </div>
      <div class="continue-info-wrap">
        <h5 class="continue-manga-title" title="${item.mangaTitle}">${item.mangaTitle}</h5>
        <p class="continue-meta-text">Ch. ${item.chapterNumber} · ${dateStr}</p>
      </div>
    `;

    const resumeAction = () => {
      if (typeof onResume === "function") {
        onResume(item);
      }
    };

    card.addEventListener("click", resumeAction);
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        resumeAction();
      }
    });

    row.appendChild(card);
  });

  container.appendChild(row);
}
