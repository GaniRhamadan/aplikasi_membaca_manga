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
        const AD_AND_TRACKER_KEYWORDS = [
          "slot", "judi", "zeus", "gacor", "bet", "casino", "pragmatic",
          "poker", "togel", "bonus", "deposit", "sbobet", "popunder",
          "histats", "statcounter", "disqus", "google-analytics", "doubleclick",
          "adsystem", "googleadservices", "popads", "propeller", "exoclick",
          "adsterra", "monetag", "blogger.googleusercontent.com", "orangarab",
          "komikindo-e", "fav.png", "donate", "saweria", "sociabuzz", "link-alternatif",
          "banner", "iklan", "promosi"
        ];

        // Endpoint 0: Image Proxy untuk mem-bypass CORS, anti-hotlink, dan pemblokiran browser shield + Proteksi Iklan
        server.middlewares.use(async (req, res, next) => {
          if (req.url?.startsWith("/api/proxy-image")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const targetUrl = parsedUrl.searchParams.get("url");

            if (!targetUrl) {
              res.statusCode = 400;
              res.end("Missing ?url parameter");
              return;
            }

            const lowerTarget = targetUrl.toLowerCase();
            // Keamanan ketat: Blokir URL bukan HTTP/HTTPS atau yang mengandung domain/keyword iklan
            if (
              (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) ||
              AD_AND_TRACKER_KEYWORDS.some(k => lowerTarget.includes(k)) ||
              lowerTarget.endsWith(".gif")
            ) {
              res.statusCode = 403;
              res.end("Diblokir oleh sistem keamanan & anti-iklan MangaVerse.");
              return;
            }

            try {
              const headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
              };
              if (targetUrl.includes("mangadex")) {
                headers["Referer"] = "https://mangadex.org/";
              } else {
                headers["Referer"] = "https://komikindo.ch/";
              }

              const upstream = await fetch(targetUrl, { headers });
              if (!upstream.ok) {
                res.statusCode = upstream.status;
                res.end(`Upstream returned ${upstream.status}`);
                return;
              }

              const cType = upstream.headers.get("content-type") || "image/jpeg";
              res.setHeader("Content-Type", cType);
              res.setHeader("Cache-Control", "public, max-age=86400, immutable");
              res.setHeader("Access-Control-Allow-Origin", "*");
              const arrayBuffer = await upstream.arrayBuffer();
              res.end(Buffer.from(arrayBuffer));
            } catch (err) {
              res.statusCode = 500;
              res.end(err.message);
            }
            return;
          }

          next();
        });

        // Endpoint 1: Ambil daftar halaman gambar dari chapter (Komikindo & sumber eksternal) dengan filter iklan ketat
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
              let bacaIdx = html.indexOf('id="Baca_Komik"');
              if (bacaIdx === -1) {
                bacaIdx = html.indexOf('id="readerarea"');
              }
              if (bacaIdx === -1) {
                bacaIdx = html.indexOf('class="readerarea"');
              }
              if (bacaIdx === -1) {
                bacaIdx = html.indexOf('id="chimg"');
              }

              let pages = [];

              if (bacaIdx !== -1) {
                const section = html.slice(bacaIdx, bacaIdx + 150000);
                const matches = [...section.matchAll(/<img[^>]+src=["']([^"']+)["']/g)].map(m => m[1]);
                pages = matches.filter(url => {
                  const lower = url.toLowerCase();
                  return (
                    !AD_AND_TRACKER_KEYWORDS.some(k => lower.includes(k)) &&
                    !lower.endsWith(".gif") &&
                    (lower.includes("/data/") || /\.(jpg|jpeg|png|webp)(\?.*)?$/i.test(lower))
                  );
                });
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

          // Endpoint 4: Cari komik dari Komiku.org (Bahasa Indonesia)
          if (req.url?.startsWith("/api/search-komiku")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const query = parsedUrl.searchParams.get("q") || "";

            if (!query.trim()) {
              res.setHeader("Content-Type", "application/json");
              res.setHeader("Access-Control-Allow-Origin", "*");
              res.end(JSON.stringify({ success: true, items: [] }));
              return;
            }

            try {
              const targetUrl = `https://api.komiku.org/?post_type=manga&s=${encodeURIComponent(query.trim())}`;
              const upstream = await fetch(targetUrl, {
                headers: {
                  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                  "Referer": "https://komiku.org/"
                }
              });

              const html = await upstream.text();
              const blocks = [...html.matchAll(/<div class=["']bge["']>([\s\S]*?)<\/div>\s*<\/div>/g)];
              const items = [];
              const seen = new Set();

              for (const b of blocks) {
                const linkMatch = b[1].match(/href=["']\/manga\/([^/]+)\/["']/);
                const titleMatch = b[1].match(/<h3>([^<]+)<\/h3>/) || b[1].match(/alt=["']([^"']+)["']/);
                const imgMatch = b[1].match(/<img[^>]+(?:src|data-src)=["']([^"']+)["']/);

                if (linkMatch && titleMatch) {
                  const slug = linkMatch[1];
                  const rawTitle = titleMatch[1].trim();
                  const cover = imgMatch ? imgMatch[1] : "";

                  if (slug && !seen.has(slug)) {
                    seen.add(slug);
                    items.push({
                      id: `komiku-${slug}`,
                      title: rawTitle,
                      altTitle: "",
                      author: "Komiku ID",
                      status: "Ongoing",
                      statusId: "Berjalan",
                      genres: ["Manga"],
                      rating: "8.7",
                      follows: "Indonesia",
                      cover,
                      banner: cover,
                      synopsis: `Baca manga ${rawTitle} Bahasa Indonesia terlengkap di MangaVerse bersumber dari Komiku.org.`,
                      type: "Manga",
                      komikuSlug: slug,
                      url: `https://komiku.org/manga/${slug}/`,
                      source: "Komiku",
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

          // Endpoint 5: Ambil semua chapter dari Komiku.org
          if (req.url?.startsWith("/api/komiku-chapters")) {
            const parsedUrl = new URL(req.url, "http://localhost:5173");
            const slug = parsedUrl.searchParams.get("slug");

            if (!slug) {
              res.statusCode = 400;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: false, error: "Missing ?slug parameter" }));
              return;
            }

            try {
              const targetUrl = `https://komiku.org/manga/${slug}/`;
              const upstream = await fetch(targetUrl, {
                headers: {
                  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
              });

              const html = await upstream.text();
              const matches = [...html.matchAll(/<a href=["'](\/[^"']*chapter-([0-9\.]+)\/)["'][^>]*>[\s\S]*?Chapter\s+([0-9\.]+)/gi)];
              const chapters = [];
              const seen = new Set();

              for (const m of matches) {
                const path = m[1];
                const num = m[2] || m[3];
                const fullUrl = `https://komiku.org${path}`;

                if (!seen.has(fullUrl)) {
                  seen.add(fullUrl);
                  chapters.push({
                    id: `komiku-${slug}-${num}`,
                    number: num,
                    title: `Chapter ${num}`,
                    url: fullUrl,
                    lang: "id",
                    source: "Komiku",
                    pages: null
                  });
                }
              }

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

          next();
        });
      }
    }
  ]
});
