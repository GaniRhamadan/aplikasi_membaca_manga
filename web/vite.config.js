import { defineConfig } from "vite";

export default defineConfig({
  server: {
    port: 5173,
    host: true
  },
  plugins: [
    {
      name: "manga-scraper-proxy",
      configureServer(server) {
        // Endpoint 1: Ambil daftar halaman gambar dari chapter (Komikindo & sumber eksternal)
        server.middlewares.use(async (req, res, next) => {
          if (req.url?.startsWith("/api/chapter-pages")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const chapterUrl = parsedUrl.searchParams.get("url");

            if (!chapterUrl) {
              res.statusCode = 400;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: "Missing ?url parameter" }));
              return;
            }

            try {
              const upstream = await fetch(chapterUrl, {
                headers: {
                  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                  "Referer": "https://komikindo.ch/"
                }
              });

              if (!upstream.ok) {
                throw new Error(`Upstream returned ${upstream.status}`);
              }

              const html = await upstream.text();
              const bacaIdx = html.indexOf('id="Baca_Komik"');
              let pages = [];

              if (bacaIdx !== -1) {
                const section = html.slice(bacaIdx, bacaIdx + 45000);
                const matches = [...section.matchAll(/<img[^>]+src=["']([^"']+)["']/g)].map(m => m[1]);
                pages = matches.filter(url => 
                  !url.endsWith(".gif") && 
                  !url.includes("blogger.googleusercontent.com") && 
                  !url.includes("orangarab") && 
                  !url.includes("wp-content/uploads/202") && 
                  !url.includes("komikindo-e") && 
                  !url.includes("fav.png")
                );
              }

              res.setHeader("Content-Type", "application/json");
              res.setHeader("Access-Control-Allow-Origin", "*");
              res.end(JSON.stringify({ success: true, pages, count: pages.length }));
            } catch (err) {
              res.statusCode = 500;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: err.message, pages: [] }));
            }
            return;
          }

          // Endpoint 2: Ambil semua chapter komik berdasarkan slug Komikindo
          if (req.url?.startsWith("/api/manga-chapters")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const slug = parsedUrl.searchParams.get("slug");

            if (!slug) {
              res.statusCode = 400;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: "Missing ?slug parameter" }));
              return;
            }

            try {
              const targetUrl = `https://komikindo.ch/komik/${slug}/`;
              const upstream = await fetch(targetUrl, {
                headers: {
                  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                }
              });

              if (!upstream.ok) {
                throw new Error(`Upstream returned ${upstream.status}`);
              }

              const html = await upstream.text();
              const matches = [...html.matchAll(/<a href=["'](https:\/\/komikindo\.ch\/[^\s"']+chapter-([0-9\.]+)[^"']*)["'][^>]*>/g)];
              const seen = new Set();
              const chapters = [];

              for (const m of matches) {
                const url = m[1];
                if (!seen.has(url)) {
                  seen.add(url);
                  const num = m[2];
                  chapters.push({
                    id: `${slug}-${num}`,
                    number: num,
                    title: `Chapter ${num}`,
                    url,
                    lang: "id",
                    pages: null
                  });
                }
              }

              // Urutkan nomor terkecil ke terbesar
              chapters.sort((a, b) => parseFloat(a.number) - parseFloat(b.number));

              res.setHeader("Content-Type", "application/json");
              res.setHeader("Access-Control-Allow-Origin", "*");
              res.end(JSON.stringify({ success: true, chapters, count: chapters.length }));
            } catch (err) {
              res.statusCode = 500;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: err.message, chapters: [] }));
            }
            return;
          }

          // Endpoint 3: Cari komik langsung dari Komikindo (Bahasa Indonesia)
          if (req.url?.startsWith("/api/search-komikindo")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const query = parsedUrl.searchParams.get("q") || "";

            if (!query.trim()) {
              res.setHeader("Content-Type", "application/json");
              res.setHeader("Access-Control-Allow-Origin", "*");
              res.end(JSON.stringify({ success: true, items: [] }));
              return;
            }

            try {
              const targetUrl = `https://komikindo.ch/?s=${encodeURIComponent(query.trim())}`;
              const upstream = await fetch(targetUrl, {
                headers: {
                  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                }
              });

              const html = await upstream.text();
              const blocks = [...html.matchAll(/<div class=["']animepost["']>([\s\S]*?)<\/div>\s*<\/div>/g)];
              const items = [];
              const seen = new Set();

              for (const b of blocks) {
                const linkMatch = b[1].match(/href=["'](https:\/\/komikindo\.ch\/komik\/[^"']+)["']/);
                const titleMatch = b[1].match(/<h4>([^<]+)<\/h4>/) || b[1].match(/title=["']([^"']+)["']/);
                const imgMatch = b[1].match(/src=["']([^"']+)["']/);

                if (linkMatch && titleMatch) {
                  const slugMatch = linkMatch[1].match(/\/komik\/([^/]+)\//);
                  const slug = slugMatch ? slugMatch[1] : "";
                  const rawTitle = titleMatch[1].replace(/^Komik\s+/i, "").trim();

                  if (slug && !seen.has(slug)) {
                    seen.add(slug);
                    items.push({
                      id: `komikindo-${slug}`,
                      title: rawTitle,
                      altTitle: "",
                      author: "Komikindo ID",
                      status: "Ongoing",
                      statusId: "Berjalan",
                      genres: ["Manga"],
                      rating: "8.6",
                      follows: "Indonesia",
                      cover: imgMatch ? imgMatch[1] : "",
                      banner: imgMatch ? imgMatch[1] : "",
                      synopsis: `Komik ${rawTitle} dalam Bahasa Indonesia di MangaVerse.`,
                      type: "Manga",
                      komikandoSlug: slug,
                      url: linkMatch[1],
                      isKomikindo: true,
                      chapters: []
                    });
                  }
                }
              }

              res.setHeader("Content-Type", "application/json");
              res.setHeader("Access-Control-Allow-Origin", "*");
              res.end(JSON.stringify({ success: true, items, count: items.length }));
            } catch (err) {
              res.statusCode = 500;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: err.message, items: [] }));
            }
            return;
          }

          next();
        });
      }
    }
  ]
});
