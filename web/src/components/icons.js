/**
 * Cohesive Lucide-style SVG icons for MangaVerse
 * All icons use 24x24 viewBox, stroke="currentColor", stroke-width="2", stroke-linecap="round", stroke-linejoin="round"
 */

function svg(paths, className = "icon", size = 20) {
  return `<svg class="${className}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`;
}

export const icons = {
  home: (cls = "icon", size = 20) =>
    svg('<path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>', cls, size),

  compass: (cls = "icon", size = 20) =>
    svg('<circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>', cls, size),

  bookmark: (cls = "icon", size = 20, filled = false) =>
    svg(
      filled
        ? '<path d="m19 21-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" fill="currentColor"/>'
        : '<path d="m19 21-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>',
      cls,
      size
    ),

  history: (cls = "icon", size = 20) =>
    svg('<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>', cls, size),

  search: (cls = "icon", size = 20) =>
    svg('<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>', cls, size),

  sun: (cls = "icon", size = 20) =>
    svg('<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>', cls, size),

  moon: (cls = "icon", size = 20) =>
    svg('<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>', cls, size),

  play: (cls = "icon", size = 20) =>
    svg('<polygon points="6 3 20 12 6 21 6 3" fill="currentColor"/>', cls, size),

  bookOpen: (cls = "icon", size = 20) =>
    svg('<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>', cls, size),

  chevronRight: (cls = "icon", size = 20) =>
    svg('<path d="m9 18 6-6-6-6"/>', cls, size),

  chevronLeft: (cls = "icon", size = 20) =>
    svg('<path d="m15 18-6-6 6-6"/>', cls, size),

  star: (cls = "icon", size = 16, filled = false) =>
    svg(
      filled
        ? '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" fill="currentColor"/>'
        : '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
      cls,
      size
    ),

  arrowLeft: (cls = "icon", size = 20) =>
    svg('<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>', cls, size),

  arrowRight: (cls = "icon", size = 20) =>
    svg('<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>', cls, size),

  x: (cls = "icon", size = 18) =>
    svg('<path d="M18 6 6 18"/><path d="m6 6 12 12"/>', cls, size),

  check: (cls = "icon", size = 18) =>
    svg('<polyline points="20 6 9 17 4 12"/>', cls, size),

  clock: (cls = "icon", size = 16) =>
    svg('<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>', cls, size),

  sort: (cls = "icon", size = 18) =>
    svg('<path d="m7 15 5 5 5-5"/><path d="m7 9 5-5 5 5"/>', cls, size),

  fullscreen: (cls = "icon", size = 18) =>
    svg('<path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/>', cls, size),

  scroll: (cls = "icon", size = 18) =>
    svg('<path d="M19 17V5a2 2 0 0 0-2-2H4"/><path d="M8 21h12a2 2 0 0 0 2-2v-1a1 1 0 0 0-1-1H11a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1z"/><path d="M4 17a2 2 0 0 1 2-2h13"/>', cls, size),

  trash: (cls = "icon", size = 16) =>
    svg('<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>', cls, size),

  layers: (cls = "icon", size = 18) =>
    svg('<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>', cls, size),

  sparkles: (cls = "icon", size = 18) =>
    svg('<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>', cls, size)
};
