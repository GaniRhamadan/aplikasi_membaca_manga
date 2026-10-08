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

    // Filter ketat availableTranslatedLanguage[]=id agar hanya manga dengan translasi Bahasa Indonesia yang diambil
    const url = `${BASE_URL}/manga?order[followedCount]=desc&limit=${limit}&availableTranslatedLanguage[]=id&includes[]=cover_art&includes[]=author&contentRating[]=safe`;
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
 * Daftar Genre Lengkap Manga/Webtoon
 */
export const COMPLETE_GENRE_LIST = [
  "Semua",
  "Action",
  "Romance",
  "Fantasy",
  "Comedy",
  "Isekai",
  "Adventure",
  "Slice of Life",
  "Drama",
  "Supernatural",
  "Mystery",
  "Horror",
  "Sci-Fi",
  "Psychological",
  "Thriller",
  "Martial Arts",
  "School Life",
  "Sports",
  "Historical",
  "Shounen",
  "Seinen"
];

export const GENRE_MAP = {
  "Action": "391b0423-d847-456f-aff0-8b0cfc03066b",
  "Romance": "423e2eae-a7a2-4a8b-ac03-a8351462d71d",
  "Comedy": "4d32cc48-9f00-4cca-9b5a-a839f0764984",
  "Fantasy": "cdc58593-87dd-415e-bbc0-2ec27bf404cc",
  "Isekai": "ace04997-f6bd-436e-b261-779182193d3d",
  "Adventure": "87cc87cd-a395-47af-b27a-93258283bbc6",
  "Slice of Life": "e5301a23-ebd9-49dd-a0cb-2add944c7fe9",
  "Drama": "b9af3a63-f058-46de-a9a0-e0c13906197a",
  "Supernatural": "eabc5b4c-6aff-42f3-b657-3e90cbd00b75",
  "Mystery": "ee968100-4191-4968-93d3-f82d72be7e46",
  "Horror": "cdad7e68-1419-41dd-bdce-27753074a640",
  "Sci-Fi": "256c8bd9-4904-4360-bf4f-508a76d67183",
  "Psychological": "3b60b75c-a2d7-4860-ab56-05f391bb889c",
  "Thriller": "07251805-a27e-4d59-b488-f0bfbec15168",
  "Martial Arts": "799c202e-7daa-44eb-9cf7-8a3c0441531e",
  "School Life": "caaa44eb-cd40-4177-b930-79d3ef2afe87",
  "Sports": "69964a64-2f90-4d33-beeb-f3ed2875eb4c",
  "Historical": "33771934-028e-4cb3-8744-691e866a923e",
  "Shounen": "demographic:shounen",
  "Seinen": "demographic:seinen",
  "Shoujo": "demographic:shoujo"
};

/**
 * Mengambil daftar manga berdasarkan genre dengan filter ketat dan cache
 */
export async function fetchMangasByGenre(genre, limit = 18) {
  if (!genre || genre === "Semua") {
    return getPopularManga(limit);
  }

  // 1. Cek in-memory cache
  const cacheKey = `genre_${genre.toLowerCase().replace(/[^a-z0-9]/g, "_")}`;
  if (cache[cacheKey] && cache[cacheKey].length > 0) {
    return cache[cacheKey];
  }

  // 2. Cocokkan dari koleksi kurasi lokal terlebih dahulu (instan)
  const localMatched = CURATED_MANGAS.filter(m =>
    (m.genres || []).some(g => g.toLowerCase() === genre.toLowerCase())
  );

  let liveMangaDex = [];
  const tagVal = GENRE_MAP[genre];

  // 3. Ambil dari MangaDex jika genre terdaftar
  if (tagVal) {
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 6000);
      const isDemo = tagVal.startsWith("demographic:");
      const queryParam = isDemo
        ? `publicationDemographic[]=${tagVal.split(":")[1]}`
        : `includedTags[]=${tagVal}`;

      const url = `${BASE_URL}/manga?${queryParam}&availableTranslatedLanguage[]=id&order[followedCount]=desc&limit=${limit}&includes[]=cover_art&includes[]=author&contentRating[]=safe`;
      const res = await fetch(url, { signal: controller.signal });
      clearTimeout(timeout);

      if (res.ok) {
        const json = await res.json();
        if (json.data && Array.isArray(json.data)) {
          liveMangaDex = json.data.map(raw => formatMangaDexManga(raw));
        }
      }
    } catch (err) {
      console.warn(`Gagal mengambil genre ${genre} dari MangaDex:`, err.message);
    }
  }

  // 4. Jika hasil masih sedikit, cari melalui Komiku.org proxy
  let komikuItems = [];
  if (localMatched.length + liveMangaDex.length < 6) {
    try {
      const endpoint = typeof window !== "undefined"
        ? `/api/search-komiku?q=${encodeURIComponent(genre)}`
        : `http://localhost:5173/api/search-komiku?q=${encodeURIComponent(genre)}`;
      const res = await fetch(endpoint);
      if (res.ok) {
        const json = await res.json();
        if (json.success && Array.isArray(json.items)) {
          komikuItems = json.items;
        }
      }
    } catch {}
  }

  // 5. Gabungkan dan deduplikasi
  const seenIds = new Set();
  const seenTitles = new Set();
  const combined = [];

  for (const m of [...localMatched, ...liveMangaDex, ...komikuItems]) {
    const norm = (m.title || "").trim().toLowerCase().replace(/[^a-z0-9]/g, "");
    if (!m.id || seenIds.has(m.id)) continue;
    if (norm && seenTitles.has(norm)) continue;

    // Pastikan tag genre muncul di manga objek
    if (!m.genres || !m.genres.some(g => g.toLowerCase() === genre.toLowerCase())) {
      m.genres = [genre, ...(m.genres || [])];
    }

    seenIds.add(m.id);
    if (norm) seenTitles.add(norm);
    combined.push(m);
    if (combined.length >= limit) break;
  }

  const finalResult = combined.length > 0 ? combined : localMatched;
  cache[cacheKey] = finalResult;
  return finalResult;
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
    // Gunakan order[relevance]=desc dan filter bahasa Indonesia agar hasil pencarian relevan dan berbahasa Indonesia
    const url = `${BASE_URL}/manga?title=${encoded}&limit=25&availableTranslatedLanguage[]=id&includes[]=cover_art&includes[]=author&contentRating[]=safe&contentRating[]=suggestive&order[relevance]=desc`;
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

  // 3.5 Cari dari Komiku.org via server proxy (Bahasa Indonesia)
  let komikuCandidates = [];
  try {
    const endpoint = typeof window !== "undefined"
      ? `/api/search-komiku?q=${encodeURIComponent(query.trim())}`
      : `http://localhost:5173/api/search-komiku?q=${encodeURIComponent(query.trim())}`;
    const res = await fetch(endpoint);
    if (res.ok) {
      const json = await res.json();
      if (json.success && Array.isArray(json.items)) {
        komikuCandidates = json.items.map(m => ({
          manga: m,
          score: calculateRelevanceScore(m, cleanQ)
        })).filter(item => item.score > 0);
      }
    }
  } catch {
    // Abaikan jika endpoint komiku tidak dapat dihubungi
  }

  // 4. Gabungkan seluruh kandidat dan urutkan berdasarkan skor relevansi tertinggi
  const allCandidates = [...localCandidates, ...komikandoCandidates, ...komikuCandidates, ...liveCandidates];
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
  if (!manga) return [];

  // 1. Cek apakah ada data kurasi lokal lengkap dengan pencocokan ID/slug/judul yang ketat dan presisi
  const normTitle = (manga.title || "").trim().toLowerCase();
  const local = CURATED_MANGAS.find(m => {
    if (m.id === manga.id) return true;
    if (m.mangadexId && manga.mangadexId && m.mangadexId === manga.mangadexId) return true;
    if (m.komikandoSlug && manga.komikandoSlug && m.komikandoSlug === manga.komikandoSlug) return true;
    const curTitle = (m.title || "").trim().toLowerCase();
    const curAlt = (m.altTitle || "").trim().toLowerCase();
    if (curTitle && curTitle === normTitle) return true;
    if (curAlt && curAlt === normTitle) return true;
    return false;
  });

  if (local) {
    if (local.komikandoSlug && !manga.komikandoSlug) {
      manga.komikandoSlug = local.komikandoSlug;
    }
    // Gunakan data kurasi lokal jika sudah punya chapter lengkap
    if (local.chapters && local.chapters.length > 0) {
      manga.chapters = local.chapters;
      return local.chapters;
    }
  }

  // 2. Jika manga berasal dari Komikindo atau memiliki komikandoSlug (100% Bahasa Indonesia)
  const slug = manga.komikandoSlug || local?.komikandoSlug;
  if (slug) {
    try {
      const endpoint = typeof window !== "undefined"
        ? `/api/manga-chapters?slug=${encodeURIComponent(slug)}`
        : `http://localhost:5173/api/manga-chapters?slug=${encodeURIComponent(slug)}`;
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

  // 2.5 Jika manga berasal dari Komiku.org (Bahasa Indonesia)
  const kSlug = manga.komikuSlug;
  if (kSlug) {
    try {
      const endpoint = typeof window !== "undefined"
        ? `/api/komiku-chapters?slug=${encodeURIComponent(kSlug)}`
        : `http://localhost:5173/api/komiku-chapters?slug=${encodeURIComponent(kSlug)}`;
      const res = await fetch(endpoint);
      if (res.ok) {
        const json = await res.json();
        if (json.success && Array.isArray(json.chapters) && json.chapters.length > 0) {
          manga.chapters = json.chapters;
          return json.chapters;
        }
      }
    } catch (e) {
      console.warn("Gagal mengambil chapter Komiku:", e.message);
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
      const lang = forcedLang || attr.translatedLanguage || "id";
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

    // Prioritaskan chapter Bahasa Indonesia
    const dataId = await fetchAllChapters(mangadexId, "id", controller.signal);
    clearTimeout(timeout);

    let allChapters = [];
    if (dataId.length > 0) {
      allChapters = deduplicateChapters(dataId.map(c => mapChapter(c, "id")));
    } else {
      // Jika MangaDex tidak punya rilisan Bahasa Indonesia
      console.info("MangaDex tidak memiliki rilisan Bahasa Indonesia untuk:", manga.title);
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
      const url = `${BASE_URL}/manga/${mangadexId}/feed?limit=100&translatedLanguage[]=${lang}&order[chapter]=asc&includeExternalUrl=0`;
      const res = await fetch(url);
      if (!res.ok) return null;
      const json = await res.json();
      feedData = json.data || [];
      cache.chapterLists.set(cacheKey, feedData);
    }

    const targetNum = String(chapter.number).trim();
    const targetFloat = parseFloat(targetNum);
    const found = feedData.find(c => {
      const chNum = String(c.attributes?.chapter || "").trim();
      const chFloat = parseFloat(chNum);
      const matches = chNum === targetNum || (!isNaN(targetFloat) && !isNaN(chFloat) && targetFloat === chFloat);
      return matches && !c.attributes?.externalUrl;
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
          chapter.pages = json.pages;
          return json.pages;
        }
      }
    } catch (err) {
      console.warn("Gagal fetch pages dari proxy server:", err.message);
    }
  }

  // 3. Resolusi ID MangaDex yang valid
  let chapterMangaDexId = isUUID(chapter.id) ? chapter.id : null;

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

  // Jika halaman tidak dapat dimuat, kembalikan array kosong agar UI menampilkan error bersih, JANGAN gambar lukisan palsu
  return [];
}
