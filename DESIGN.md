# DESIGN DIRECTION: MangaVerse / Manga Reader

## Brand Identity & Vision
- **Product**: MangaVerse (Web & Mobile Manga Reader)
- **Concept**: A sleek, immersive, distraction-free manga and comic reader built for manga enthusiasts. Provides quick search, curated trending titles, chapter history/bookmarks, and a fluid dual-mode reader (Vertical Webtoon Scroll & Horizontal Page-by-Page flip).
- **Dual-Experience**: Includes an interactive "Mobile Frame Preview" mode that simulates an Android smartphone screen (with status bar, bezel, and mobile viewport) allowing testing without a physical phone or emulator, plus an expanded "Full Responsive Desktop" mode.

## Dials (Antislop Liveliness Toolkit)
- **ENERGY**: 2 (Balanced - rich contrast, immersive dark manga atmosphere, intentional warm vermilion shonen accent)
- **RHYTHM**: 2 (Structured layout with hero spotlight, quick genre filters, chapter grid, and full-screen distraction-free reader)
- **MOTION**: 2 (Subtle micro-interactions: smooth page transitions, chapter drawer slide-in, reader modal fade, mobile/desktop viewport transition)

## Color Palette (WCAG AA Compliant)
- **Background Deep**: `#0D0E12` (Obsidian black, non-reflective, comfortable for reading)
- **Surface Card**: `#161922` (Slightly elevated dark slate for manga cards & containers)
- **Surface Floating / Header**: `#1E222D` (Subtle boundary elevation)
- **Border / Divider**: `#2A303F` (Crisp contrast without harsh lines)
- **Primary Accent**: `#FF5533` (Energetic manga ink orange-vermilion)
- **Accent Hover / Active**: `#FF6F52`
- **Text High-Contrast**: `#F4F6FB` (Readable, crisp off-white for headings & manga titles)
- **Text Muted**: `#9BA3B5` (Contrast ratio > 4.8:1 against `#0D0E12` and `#161922`)
- **Status / Badge Success**: `#2ED573` (Completed status)
- **Status Ongoing**: `#FFA502`

## Typography
- **Headings & Logo**: "Outfit", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif. Modern, geometric, bold comic book energy.
- **Body & Metadata**: "Plus Jakarta Sans", -apple-system, BlinkMacSystemFont, sans-serif. Clean, highly legible at small sizes (12-14px).
- **Manga Numbers & Chapters**: Monospace / tabular numbers for chapter IDs ("Ch. 124", "Vol. 12").

## Functional Principles & Rules
- No fake numbers or fake testimonials (R-17, R-18, R-36).
- No em dashes in UI text (R-02).
- Real data sourced from MangaDex API with CORS support + curated fast-loading fallbacks (R-38).
- Clear 3 UI states for every dynamic view: Empty, Loading, and Error with Retry (R-27).
- Full keyboard support: Left/Right arrow keys for page turning, Escape to exit reader, Space/Arrows to scroll, visible focus rings (R-32).
