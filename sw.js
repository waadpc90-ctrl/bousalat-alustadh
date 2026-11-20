const CACHE_NAME = "bousalat-alustadh-v4.4-offline-v4";

const CORE_ASSETS = [
  "./",
  "./index.html",
  "./bousalat_alustadh_iphone.html",
  "./manifest.json",
  "./apple-touch-icon.png",
  "./icon-192.png",
  "./icon-512.png"
];

// 1. Install: Pre-cache each asset individually so one failure never aborts the rest,
// and explicitly store index.html under both "./" and "./index.html" for iOS PWA start_url matching.
self.addEventListener("install", (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      for (const url of CORE_ASSETS) {
        try {
          const req = new Request(url, { cache: "reload" });
          const res = await fetch(req);
          if (res && res.ok) {
            await cache.put(url, res.clone());
            if (url === "./index.html") {
              await cache.put("./", res.clone());
              await cache.put("index.html", res.clone());
            }
          }
        } catch (err) {
          // Ignore individual asset error so install still succeeds
        }
      }
    })
  );
});

// 2. Activate: Clean up old caches and immediately claim all open iOS tabs/standalone windows
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      )
    ).then(() => self.clients.claim())
  );
});

// Helper: Find cached HTML shell regardless of query parameters, trailing slash, or filename
async function getOfflineAppShell(cache) {
  const candidates = [
    "./index.html",
    "index.html",
    "./",
    "./bousalat_alustadh_iphone.html",
    "bousalat_alustadh_iphone.html"
  ];
  for (const key of candidates) {
    const match = await cache.match(key, { ignoreSearch: true });
    if (match) return match;
  }
  const allKeys = await cache.keys();
  for (const req of allKeys) {
    if (req.url.endsWith(".html") || req.url.endsWith("/")) {
      const match = await cache.match(req);
      if (match) return match;
    }
  }
  return null;
}

// 3. Fetch: Cache-First (Stale-While-Revalidate) for instant 0ms offline launch on iOS Safari & Standalone PWA
self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);

  // Handle cross-origin requests (e.g. Google Fonts) gracefully when offline
  if (url.origin !== self.location.origin) {
    event.respondWith(
      caches.match(req).then((cached) => {
        const netFetch = fetch(req)
          .then((res) => {
            if (res && (res.status === 200 || res.type === "opaque")) {
              const copy = res.clone();
              caches.open(CACHE_NAME).then((c) => c.put(req, copy));
            }
            return res;
          })
          .catch(() => cached || new Response("", { status: 200, headers: { "Content-Type": "text/css" } }));
        return cached || netFetch;
      })
    );
    return;
  }

  const isNavigation =
    req.mode === "navigate" ||
    req.destination === "document" ||
    (req.headers.get("accept") || "").includes("text/html") ||
    url.pathname.endsWith("/") ||
    url.pathname.endsWith(".html");

  if (isNavigation) {
    event.respondWith(
      caches.open(CACHE_NAME).then(async (cache) => {
        // 1. Check cache first for instant offline startup on iPhone
        const cachedPage =
          (await cache.match(req, { ignoreSearch: true })) ||
          (await getOfflineAppShell(cache));

        // 2. Refresh cache in background when internet is available (with 3.5s timeout)
        const networkUpdate = new Promise((resolve, reject) => {
          const timer = setTimeout(() => reject(new Error("timeout")), 3500);
          fetch(req)
            .then((res) => {
              clearTimeout(timer);
              if (res && res.ok) {
                cache.put(req, res.clone());
                cache.put("./index.html", res.clone());
                cache.put("./", res.clone());
                resolve(res);
              } else {
                reject(new Error("bad_status"));
              }
            })
            .catch((err) => {
              clearTimeout(timer);
              reject(err);
            });
        });

        if (cachedPage) {
          // Update in background without blocking offline startup
          networkUpdate.catch(() => {});
          return cachedPage;
        }

        // First visit ever: wait for network, fallback to cached shell
        try {
          return await networkUpdate;
        } catch (err) {
          return (await getOfflineAppShell(cache)) || Response.error();
        }
      })
    );
    return;
  }

  // Same-origin static assets (icons, manifest.json, sw.js): Cache-First with background refresh
  event.respondWith(
    caches.open(CACHE_NAME).then(async (cache) => {
      const cached = await cache.match(req, { ignoreSearch: true });
      const fetchPromise = fetch(req)
        .then((res) => {
          if (res && res.ok) {
            cache.put(req, res.clone());
          }
          return res;
        })
        .catch(() => cached);

      return cached || fetchPromise;
    })
  );
});

// 4. Listen for manual cache-warmup message from the page
self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "WARMUP_OFFLINE_CACHE") {
    caches.open(CACHE_NAME).then(async (cache) => {
      for (const url of CORE_ASSETS) {
        try {
          const res = await fetch(new Request(url, { cache: "reload" }));
          if (res && res.ok) {
            await cache.put(url, res.clone());
          }
        } catch (_) {}
      }
      if (event.source) {
        event.source.postMessage({ type: "OFFLINE_CACHE_READY" });
      }
    });
  }
});
