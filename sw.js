/* Discipline Ledger service worker: precache the app shell, serve cache-first.
   Bump CACHE_VERSION on every deploy so installed copies pick up changes. */
const CACHE_VERSION = "v1";
const CACHE = "discipline-ledger-" + CACHE_VERSION;

const PRECACHE = [
  "./",
  "index.html",
  "manifest.webmanifest",
  "icons/favicon-32.png",
  "icons/apple-touch-icon.png",
  "icons/icon-192.png",
  "icons/icon-512.png",
  "icons/icon-maskable-512.png",
  "fonts/space-mono-latin-400-normal.woff2",
  "fonts/space-mono-latin-700-normal.woff2",
  "fonts/public-sans-latin-400-normal.woff2",
  "fonts/public-sans-latin-500-normal.woff2",
  "fonts/public-sans-latin-600-normal.woff2",
  "fonts/fraunces-latin-500-normal.woff2",
  "fonts/fraunces-latin-500-italic.woff2"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith("discipline-ledger-") && k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Page navigations: try cache first, fall back to the shell, then network.
  if (req.mode === "navigate") {
    event.respondWith(
      caches.match("index.html").then((cached) => cached || fetch(req))
    );
    return;
  }

  // Everything else: cache first, then network (and remember what we fetched).
  event.respondWith(
    caches.match(req).then((cached) => {
      if (cached) return cached;
      return fetch(req).then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(req, copy));
        }
        return res;
      });
    })
  );
});
