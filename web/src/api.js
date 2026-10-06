import { CURATED_MANGAS } from "./curatedData.js";

const BASE_URL = "https://api.mangadex.org";
const UPLOADS_URL = "https://uploads.mangadex.org";

// Cache in-memory untuk mengurangi fetch berulang
const cache = {
  mangaDetails: new Map(),
  chapterLists: new Map(),
  chapterPages: new Map()
};

/**
 * Helper untuk regex escape
 */
function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Format data manga mentah dari MangaDex menjadi objek aplikasi yang rapi
 */
export function formatMangaDexManga(raw, searchQuery = "") {
  const id = raw.id;
  const attr = raw.attributes || {};
  const rels = raw.relationships || [];

  const titleObj = attr.title || {};
  const allAlts = (attr.altTitles || []).map(a => Object.values(a)[0]).filter(Boolean);

  let title = "";
  let altTitle = "";
  const cleanQ = typeof searchQuery === "string" ? searchQuery.trim().toLowerCase() : "";

  // Jika query cocok dengan salah satu alt title (misal cari "Solo Leveling" sementara judul utamanya "Na Honjaman Level-Up")
  if (cleanQ) {
    const matchedAlt = allAlts.find(a => a.toLowerCase().includes(cleanQ));
    if (matchedAlt) {
      title = matchedAlt;
      altTitle = titleObj.en || titleObj["ja-ro"] || titleObj["ko-ro"] || Object.values(titleObj)[0] || "";
    }
  }

  if (!title) {
    title = titleObj.en || titleObj["ja-ro"] || titleObj["ko-ro"] || Object.values(titleObj)[0] || "Judul Tidak Diketahui";
    if (allAlts.length > 0) {
      altTitle = allAlts.find(a => a !== title) || allAlts[0] || "";
    }
  }

  // Cover fileName
  let coverFileName = "";
  for (const rel of rels) {
    if (rel.type === "cover_art" && rel.attributes?.fileName) {
      coverFileName = rel.attributes.fileName;
      break;
    }
  }

  // Author name
  let authorName = "Pengarang Tidak Diketahui";
  for (const rel of rels) {
    if ((rel.type === "author" || rel.type === "artist") && rel.attributes?.name) {
      authorName = rel.attributes.name;
      break;
    }
  }

  // Genres / tags
  const genres = (attr.tags || [])
    .map(t => t.attributes?.name?.en)
    .filter(Boolean)
    .slice(0, 6);

  const coverUrl = coverFileName
    ? `${UPLOADS_URL}/covers/${id}/${coverFileName}.512.jpg`
    : "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80";

  const status = attr.status || "ongoing";
  const statusId = status === "completed" ? "Tamat" : "Berjalan";
  const synopsis = attr.description?.id || attr.description?.en || "Sinopsis belum tersedia untuk seri ini.";

  // Estimasi rating konsisten berbasis ID
  const rating = (8.0 + (Math.abs(id.split("").reduce((acc, c) => acc + c.charCodeAt(0), 0)) % 18) / 10).toFixed(1);

  return {
    id: `md-${id}`,
    mangadexId: id,
    title,
    altTitle,
    author: authorName,
    status: status.charAt(0).toUpperCase() + status.slice(1),
    statusId,
    genres: genres.length > 0 ? genres : ["Manga"],
    rating,
    follows: "MangaDex",
    cover: coverUrl,
    banner: coverUrl,
    synopsis,
    type: "Manga",
    isMangaDex: true,
    chapters: []
  };
}

/**
 * Menghitung skor relevansi manga terhadap kata kunci pencarian
 */
export function calculateRelevanceScore(manga, cleanQ) {
  const title = (manga.title || "").toLowerCase();
  const altTitle = (manga.altTitle || "").toLowerCase();
  const author = (manga.author || "").toLowerCase();
  const genres = (manga.genres || []).map(g => g.toLowerCase());

  let score = 0;

  // 1. Kecocokan persis 100% pada judul utama
  if (title === cleanQ) {
    score += 200;
  } else if (altTitle === cleanQ) {
    score += 180;
  }
  // 2. Judul diawali kata kunci yang dicari
  else if (title.startsWith(cleanQ)) {
    score += 150;
  } else if (altTitle.startsWith(cleanQ)) {
    score += 130;
  }
  // 3. Kata kunci adalah kata utuh dalam judul
  else if (new RegExp(`(?:^|\\s)${escapeRegex(cleanQ)}(?:$|\\s)`, "i").test(title)) {
    score += 110;
  } else if (new RegExp(`(?:^|\\s)${escapeRegex(cleanQ)}(?:$|\\s)`, "i").test(altTitle)) {
    score += 95;
  }
  // 4. Substring pada judul atau judul alternatif
  else if (title.includes(cleanQ)) {
    score += 75;
  } else if (altTitle.includes(cleanQ)) {
    score += 65;
  }
  // 5. Kecocokan nama pengarang
  else if (author.includes(cleanQ)) {
    score += 45;
  }
  // 6. Kecocokan genre persis (hanya jika query adalah nama genre, misal "Action" atau "Comedy")
  else if (genres.some(g => g === cleanQ)) {
    score += 35;
  } else {
    // Tidak ada kecocokan sama sekali
    return 0;
  }

  // Bonus untuk manga kurasi lokal lengkap
  if (manga.id?.startsWith("curated-")) {
    score += 20;
  }

  // Bonus rating
  const r = parseFloat(manga.rating) || 0;
  score += Math.min(10, r);

  // Penalti besar untuk Doujinshi (komik parodi) agar komik utama selalu muncul di atas
  if (
    title.includes("doujinshi") ||
    altTitle.includes("doujinshi") ||
    genres.includes("doujinshi")
  ) {
    score -= 95;
  }

  return score;
}

/**
 * Mengambil manga trending / populer dari MangaDex dengan fallback kurasi
 */
export async function getPopularManga(limit = 10) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 6000);

    const url = `${BASE_URL}/manga?order[followedCount]=desc&limit=${limit}&includes[]=cover_art&includes[]=author&contentRating[]=safe`;
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeout);

    if (!res.ok) throw new Error(`HTTP error ${res.status}`);
    const json = await res.json();
    if (!json.data || json.data.length === 0) throw new Error("Data kosong");

    const liveManga = json.data.map(raw => formatMangaDexManga(raw));
    return [...CURATED_MANGAS, ...liveManga];
  } catch (err) {
    console.warn("Gagal mengambil manga populer dari MangaDex, menggunakan kurasi lokal:", err.message);
    return CURATED_MANGAS;
  }
}

/**
 * Mencari manga dengan filter kecocokan tinggi dan perankingan relevansi
 */
export async function searchManga(query, limit = 20) {
  const cleanQ = (query || "").trim().toLowerCase();
  if (!cleanQ) return CURATED_MANGAS;

  // 1. Ambil kecocokan dari kurasi lokal dengan skor relevansi
  const localCandidates = CURATED_MANGAS.map(m => ({
    manga: m,
    score: calculateRelevanceScore(m, cleanQ)
  })).filter(item => item.score > 0);

  // 2. Cari dari MangaDex dengan urutan relevance dan exclude doujinshi
  let liveCandidates = [];
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);

    const encoded = encodeURIComponent(query.trim());
    // Gunakan order[relevance]=desc agar judul yang cocok diprioritaskan oleh MangaDex
    const url = `${BASE_URL}/manga?title=${encoded}&limit=25&includes[]=cover_art&includes[]=author&contentRating[]=safe&contentRating[]=suggestive&order[relevance]=desc`;
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeout);

    if (res.ok) {
      const json = await res.json();
      const rawData = json.data || [];
      const liveMangas = rawData.map(raw => formatMangaDexManga(raw, cleanQ));

      liveCandidates = liveMangas.map(m => ({
        manga: m,
        score: calculateRelevanceScore(m, cleanQ)
      })).filter(item => item.score > 0);
    }
  } catch (err) {
    console.warn("Pencarian online MangaDex gagal:", err.message);
  }

  // 3. Cari dari Komikindo via server proxy (untuk rilisan Bahasa Indonesia)
  let komikandoCandidates = [];
  try {
    const endpoint = typeof window !== "undefined"
      ? `/api/search-komikindo?q=${encodeURIComponent(query.trim())}`
      : `http://localhost:5173/api/search-komikindo?q=${encodeURIComponent(query.trim())}`;
    const res = await fetch(endpoint);
    if (res.ok) {
      const json = await res.json();
      if (json.success && Array.isArray(json.items)) {
        komikandoCandidates = json.items.map(m => ({
          manga: m,
          score: calculateRelevanceScore(m, cleanQ)
        })).filter(item => item.score > 0);
      }
    }
  } catch {
    // Abaikan jika endpoint komikindo tidak dapat dihubungi
  }

  // 4. Gabungkan seluruh kandidat dan urutkan berdasarkan skor relevansi tertinggi
  const allCandidates = [...localCandidates, ...komikandoCandidates, ...liveCandidates];
  allCandidates.sort((a, b) => b.score - a.score);

  // 5. Deduplikasi cerdas berdasarkan nama bersih
  const seenKeys = new Set();
  const seenIds = new Set();
  const uniqueRanked = [];

  for (const { manga } of allCandidates) {
    const normTitle = (manga.title || "").trim().toLowerCase();
    const key = normTitle.replace(/[^a-z0-9]/g, "");

    if (seenIds.has(manga.id)) continue;
    if (key && seenKeys.has(key)) continue;

    seenIds.add(manga.id);
    if (key) seenKeys.add(key);

    uniqueRanked.push(manga);
    if (uniqueRanked.length >= limit) break;
  }

  return uniqueRanked;
}

/**
 * Fetch semua chapter dari endpoint dengan pagination otomatis
 */
async function fetchAllChapters(mangadexId, lang, signal) {
  const limit = 100;
  let offset = 0;
  let allData = [];

  while (true) {
    const url = `${BASE_URL}/manga/${mangadexId}/feed?limit=${limit}&offset=${offset}&order[chapter]=asc&translatedLanguage[]=${lang}&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica`;
    try {
      const res = await fetch(url, { signal });
      if (!res.ok) break;
      const json = await res.json();
      const batch = json.data || [];
      allData = allData.concat(batch);
      const total = json.total || 0;
      if (allData.length >= total || batch.length === 0) break;
      offset += limit;
      if (offset > 2000) break; // safety cap
    } catch {
      break;
    }
  }
  return allData;
}

/**
 * Deduplikasi chapter: ambil satu entri per nomor chapter per bahasa
 * Prioritaskan yang tidak punya externalUrl (bisa dibaca langsung)
 */
function deduplicateChapters(chapters) {
  const seen = new Map();
  for (const ch of chapters) {
    const key = `${ch.lang}-${ch.number}`;
    if (!seen.has(key)) {
      seen.set(key, ch);
    } else {
      // Kalau yang sudah ada punya externalUrl, ganti dengan yang tidak punya (bisa dibaca)
      const existing = seen.get(key);
      if (existing.externalUrl && !ch.externalUrl) {
        seen.set(key, ch);
      }
    }
  }
  return Array.from(seen.values());
}

/**
 * Mengambil daftar chapter untuk suatu manga
 * Memprioritaskan Bahasa Indonesia, mendukung pagination untuk manga 100+ chapter
 */
export async function getMangaChapters(manga) {
  // 1. Cek apakah ada data kurasi lokal lengkap (cocokkan id, mangadexId, atau judul)
  const normTitle = (manga.title || "").trim().toLowerCase();
  const local = CURATED_MANGAS.find(m => {
    const curTitle = (m.title || "").trim().toLowerCase();
    const curAlt = (m.altTitle || "").trim().toLowerCase();
    return (
      m.id === manga.id ||
      (manga.mangadexId && m.mangadexId === manga.mangadexId) ||
      curTitle === normTitle ||
      (curTitle && normTitle.includes(curTitle)) ||
      (curAlt && normTitle.includes(curAlt))
    );
  });

  // Gunakan data kurasi lokal jika sudah punya chapter lengkap
  if (local && local.chapters && local.chapters.length > 0) {
    return local.chapters;
  }

  // 2. Jika manga berasal dari Komikindo atau memiliki komikandoSlug
  if (manga.komikandoSlug) {
    try {
      const endpoint = typeof window !== "undefined"
        ? `/api/manga-chapters?slug=${encodeURIComponent(manga.komikandoSlug)}`
        : `http://localhost:5173/api/manga-chapters?slug=${encodeURIComponent(manga.komikandoSlug)}`;
      const res = await fetch(endpoint);
      if (res.ok) {
        const json = await res.json();
        if (json.success && Array.isArray(json.chapters) && json.chapters.length > 0) {
          manga.chapters = json.chapters;
          return json.chapters;
        }
      }
    } catch (e) {
      console.warn("Gagal mengambil chapter Komikindo:", e.message);
    }
  }

  const mangadexId = manga.mangadexId || (manga.id.startsWith("md-") ? manga.id.replace("md-", "") : null);
  if (!mangadexId) {
    return [];
  }

  if (cache.chapterLists.has(mangadexId)) {
    return cache.chapterLists.get(mangadexId);
  }

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20000);

    const mapChapter = (ch, forcedLang) => {
      const attr = ch.attributes || {};
      const lang = forcedLang || attr.translatedLanguage || "en";
      return {
        id: ch.id,
        number: attr.chapter || "?",
        title: attr.title || `Chapter ${attr.chapter || "?"}`,
        date: attr.publishAt ? attr.publishAt.split("T")[0] : "Terbaru",
        lang,
        externalUrl: attr.externalUrl || null,
        pages: null
      };
    };

    // Ambil semua chapter ID dan EN secara paralel (dengan pagination otomatis)
    const [dataId, dataEn] = await Promise.all([
      fetchAllChapters(mangadexId, "id", controller.signal),
      fetchAllChapters(mangadexId, "en", controller.signal),
    ]);
    clearTimeout(timeout);

    const chaptersId = dataId.map(c => mapChapter(c, "id"));
    const chaptersEn = dataEn.map(c => mapChapter(c, "en"));

    let allChapters = deduplicateChapters([...chaptersId, ...chaptersEn]);

    // Fallback: kalau masih kosong, coba tanpa filter bahasa
    if (allChapters.length === 0) {
      const fallbackUrl = `${BASE_URL}/manga/${mangadexId}/feed?limit=100&order[chapter]=asc`;
      const fallbackRes = await fetch(fallbackUrl);
      if (fallbackRes.ok) {
        const fallbackJson = await fallbackRes.json();
        allChapters = deduplicateChapters((fallbackJson.data || []).map(c => mapChapter(c)));
      }
    }

    cache.chapterLists.set(mangadexId, allChapters);
    return allChapters;
  } catch (err) {
    console.warn("Gagal memuat chapter online:", err.message);
    if (local && local.chapters) return local.chapters;
    return [];
  }
}


/**
 * Cek apakah string adalah UUID MangaDex yang valid
 */
function isUUID(str) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(str || "");
}

/**
 * Untuk chapter kurasi lokal (pages: null, ID bukan UUID),
 * cari ID MangaDex yang sesuai berdasarkan nomor chapter dari feed
 */
async function resolveCuratedChapterToMangaDexId(chapter) {
  if (!chapter.mangadexId && !chapter._parentMangadexId) return null;
  const mangadexId = chapter.mangadexId || chapter._parentMangadexId;
  if (!mangadexId) return null;

  try {
    const lang = chapter.lang || "id";
    const cacheKey = `feed-${mangadexId}-${lang}`;
    let feedData = cache.chapterLists.get(cacheKey);

    if (!feedData) {
      // MangaDex /feed tidak mengizinkan query 'chapter', ambil batch feed terurut
      const url = `${BASE_URL}/manga/${mangadexId}/feed?limit=100&translatedLanguage[]=${lang}&order[chapter]=asc&includeExternalUrl=0`;
      const res = await fetch(url);
      if (!res.ok) return null;
      const json = await res.json();
      feedData = json.data || [];
      cache.chapterLists.set(cacheKey, feedData);
    }

    const targetNum = String(chapter.number).trim();
    const found = feedData.find(c => {
      const chNum = String(c.attributes?.chapter || "").trim();
      return chNum === targetNum && !c.attributes?.externalUrl;
    });

    return found ? found.id : null;
  } catch {
    return null;
  }
}

/**
 * Mengambil daftar URL halaman gambar nyata untuk chapter yang sedang dibaca
 */
export async function getChapterPages(chapter, mangaRef) {
  // 1. Jika chapter sudah punya daftar pages lokal (misal Chapter 1 yang di-cache)
  if (Array.isArray(chapter.pages) && chapter.pages.length > 0) {
    return chapter.pages;
  }

  // 2. Jika chapter memiliki URL sumber langsung (Komikindo / scraper proxy)
  if (chapter.url) {
    try {
      const endpoint = typeof window !== "undefined"
        ? `/api/chapter-pages?url=${encodeURIComponent(chapter.url)}`
        : `http://localhost:5173/api/chapter-pages?url=${encodeURIComponent(chapter.url)}`;
      const res = await fetch(endpoint);
      if (res.ok) {
        const json = await res.json();
        if (json.success && Array.isArray(json.pages) && json.pages.length > 0) {
          chapter.pages = json.pages; // simpan di memori objek
          return json.pages;
        }
      }
    } catch (err) {
      console.warn("Gagal fetch pages dari proxy server:", err.message);
    }
  }

  // 3. Resolusi ID MangaDex yang valid
  let chapterMangaDexId = isUUID(chapter.id) ? chapter.id : null;

  // Kalau bukan UUID, cari ID MangaDex yang sesuai
  if (!chapterMangaDexId && mangaRef && mangaRef.mangadexId) {
    const localChapterWithParent = { ...chapter, _parentMangadexId: mangaRef.mangadexId };
    chapterMangaDexId = await resolveCuratedChapterToMangaDexId(localChapterWithParent);
  }

  // 4. Ambil halaman dari server At-Home MangaDex jika ada chapterMangaDexId
  if (chapterMangaDexId) {
    if (cache.chapterPages.has(chapterMangaDexId)) {
      return cache.chapterPages.get(chapterMangaDexId);
    }

    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 12000);

      const url = `${BASE_URL}/at-home/server/${chapterMangaDexId}`;
      const res = await fetch(url, { signal: controller.signal });
      clearTimeout(timeout);

      if (res.ok) {
        const json = await res.json();
        const baseUrl = json.baseUrl;
        const hash = json.chapter?.hash;
        const filenames = json.chapter?.data || [];

        if (baseUrl && hash && filenames.length > 0) {
          const pageUrls = filenames.map(file => `${baseUrl}/data/${hash}/${file}`);
          cache.chapterPages.set(chapterMangaDexId, pageUrls);
          chapter.pages = pageUrls;
          return pageUrls;
        }
      }
    } catch (err) {
      console.warn("Gagal memuat halaman At-Home MangaDex:", err.message);
    }
  }

  // Fallback halaman jika semua sumber offline
  return getFallbackPages();
}

function getFallbackPages() {
  return [
    "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80"
  ];
}
