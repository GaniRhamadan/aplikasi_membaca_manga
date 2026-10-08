import { icons } from "./icons.js";

/**
 * Two-column responsive Hero component with slide carousel support
 */
export function renderHero(container, featuredList, options = {}) {
  const { onRead = null, onDetail = null } = options;
  if (!container || !featuredList || featuredList.length === 0) return;

  let currentIndex = 0;
  let timer = null;

  function update() {
    const manga = featuredList[currentIndex];
    if (!manga) return;

    const displayRating = manga.rating ? Number(manga.rating).toFixed(1) : "9.6";
    const genresList = (manga.genres || ["Action", "Fantasy"]).slice(0, 3).join(" • ");

    container.innerHTML = `
      <div class="hero-stage">
        <!-- Ambient subtle background backdrop -->
        <div class="hero-ambient-bg" style="background-image: url('${manga.cover}')"></div>
        <div class="hero-backdrop-overlay"></div>

        <div class="hero-container">
          <!-- Kolom Kiri: Teks & Aksi -->
          <div class="hero-text-col">
            <div class="hero-eyebrow">
              <span class="hero-badge-tag">Sorotan Utama</span>
              <span class="hero-rating-pill">${icons.star("icon-gold", 13, true)} ${displayRating}</span>
              <span class="hero-genre-text">${genresList}</span>
            </div>

            <h1 class="hero-headline">${manga.title}</h1>
            <p class="hero-synopsis-text">${manga.synopsis || "Ikuti kisah selengkapnya dengan terjemahan Bahasa Indonesia."}</p>

            <div class="hero-cta-row">
              <button class="btn-hero-primary" id="btn-hero-action-read" type="button">
                ${icons.play("btn-icon-svg", 16)}
                <span>Baca Chapter 1</span>
              </button>
              <button class="btn-hero-secondary" id="btn-hero-action-detail" type="button">
                ${icons.bookOpen("btn-icon-svg", 16)}
                <span>Detail Manga</span>
              </button>
            </div>

            <!-- Slide Indicators -->
            <div class="hero-dots-row" role="tablist" aria-label="Pilih Sorotan Manga">
              ${featuredList
                .map(
                  (_, idx) => `
                <button
                  class="hero-dot ${idx === currentIndex ? "active" : ""}"
                  data-index="${idx}"
                  type="button"
                  aria-label="Tampilkan slide ${idx + 1}"
                  aria-selected="${idx === currentIndex}"
                >
                  <span class="dot-fill"></span>
                </button>
              `
                )
                .join("")}
            </div>
          </div>

          <!-- Kolom Kanan: Poster Tajam Rasio 2:3 -->
          <div class="hero-poster-col">
            <div class="hero-poster-frame">
              <img
                class="hero-poster-img"
                src="${manga.cover}"
                alt="Cover ${manga.title}"
                loading="eager"
                onerror="this.src='https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80'"
              />
              <div class="hero-poster-sheen"></div>
            </div>
          </div>
        </div>
      </div>
    `;

    // Wire up CTA buttons
    const btnRead = container.querySelector("#btn-hero-action-read");
    const btnDetail = container.querySelector("#btn-hero-action-detail");

    if (btnRead && typeof onRead === "function") {
      btnRead.addEventListener("click", () => onRead(manga));
    }
    if (btnDetail && typeof onDetail === "function") {
      btnDetail.addEventListener("click", () => onDetail(manga));
    }

    // Wire up Dots
    const dots = container.querySelectorAll(".hero-dot");
    dots.forEach((dot) => {
      dot.addEventListener("click", () => {
        const idx = parseInt(dot.getAttribute("data-index"), 10);
        if (!isNaN(idx) && idx !== currentIndex) {
          currentIndex = idx;
          resetTimer();
          update();
        }
      });
    });
  }

  function resetTimer() {
    if (timer) clearInterval(timer);
    timer = setInterval(() => {
      currentIndex = (currentIndex + 1) % featuredList.length;
      update();
    }, 9000);
  }

  update();
  resetTimer();
}
