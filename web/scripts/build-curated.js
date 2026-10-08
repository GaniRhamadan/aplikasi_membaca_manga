import fs from "fs";
import path from "path";
import { CURATED_MANGAS } from "../src/curatedData.js";

async function run() {
  console.log("Memulai pengayaan data kurasi manga...");

  const slugsToFetch = [
    { id: "curated-frieren", slug: "sousou-no-frieren" },
    { id: "curated-spy-family", slug: "spy-x-family" },
    { id: "curated-chainsaw-man", slug: "chainsaw-man" },
    { id: "curated-kage-jitsuryokusha", slug: "kage-no-jitsuryokusha-ni-naritakute" },
    { id: "curated-iruma-kun", slug: "mairimashita-iruma-kun" },
    { id: "curated-naruto", slug: "naruto" },
    { id: "curated-bleach", slug: "461782-bleach" }
  ];

  for (const item of slugsToFetch) {
    const manga = CURATED_MANGAS.find(m => m.id === item.id);
    if (!manga) {
      console.warn(`Manga tidak ditemukan: ${item.id}`);
      continue;
    }

    console.log(`Mengambil chapter untuk ${manga.title} (${item.slug})...`);
    try {
      const res = await fetch(`http://localhost:5173/api/manga-chapters?slug=${item.slug}`);
      const data = await res.json();
      if (data.success && Array.isArray(data.chapters) && data.chapters.length > 0) {
        manga.chapters = data.chapters;
        console.log(`  -> Berhasil memuat ${data.chapters.length} chapter untuk ${manga.title}`);

        // Ambil halaman untuk chapter pertama agar bisa langsung dibaca instant
        const firstCh = manga.chapters[0];
        if (firstCh && firstCh.url) {
          console.log(`  -> Mengambil halaman untuk ${firstCh.title}...`);
          const pRes = await fetch(`http://localhost:5173/api/chapter-pages?url=${encodeURIComponent(firstCh.url)}`);
          const pData = await pRes.json();
          if (pData.success && Array.isArray(pData.pages) && pData.pages.length > 0) {
            firstCh.pages = pData.pages;
            console.log(`     Halaman termuat: ${pData.pages.length} lembar`);
          }
        }
      }
    } catch (e) {
      console.error(`Gagal memuat chapter ${manga.title}:`, e.message);
    }
  }

  // Bersihkan semua banner bermasalah (foto bunga unsplash)
  for (const m of CURATED_MANGAS) {
    if (m.banner && m.banner.includes("photo-1579783900882-c0d3dad7b119")) {
      m.banner = m.cover;
    }
  }

  const outPath = path.resolve("./src/curatedData.js");
  const content = `// Data kurasi manga populer Bahasa Indonesia lengkap dengan ribuan chapter siap baca nyata\nexport const CURATED_MANGAS = ${JSON.stringify(CURATED_MANGAS, null, 2)};\n`;

  fs.writeFileSync(outPath, content, "utf-8");
  console.log("Berhasil memperbarui file src/curatedData.js!");
}

run();
