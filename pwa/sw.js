/* Service worker: the app works offline after the first visit.
   Audio is cached when played (or via the "download audio" button) and served with Range support. */
const VERSION = 'v6';
const SHELL = `salah-shell-${VERSION}`, AUDIO = 'salah-audio-v1';
const FILES = ['./', 'index.html', 'style.css', 'app.js', 'prayer.js', 'manifest.webmanifest', 'data/duas.txt',
  'fonts/AmiriQuran.ttf', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/maskable-512.png', 'icons/apple-touch-icon.png'];

self.addEventListener('install', e => {
  e.waitUntil(Promise.all([
    caches.open(SHELL).then(c => c.addAll(FILES)),
    caches.open(AUDIO).then(c => c.add('audio/adhan_takbir_v3.mp3'))      // the adhan must work offline
  ]).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k.startsWith('salah-shell-') && k !== SHELL).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

async function audioResponse(req) {
  const cache = await caches.open(AUDIO);
  let res = await cache.match(req.url);
  if (!res) {
    const net = await fetch(req.url);               // full file, not the ranged request
    if (!net.ok) return net;
    await cache.put(req.url, net.clone());
    res = net;
  }
  const range = req.headers.get('range');
  if (!range) return res;
  const buf = await res.arrayBuffer();
  const m = /bytes=(\d*)-(\d*)/.exec(range) || [];
  const start = m[1] ? parseInt(m[1], 10) : 0;
  const end = m[2] ? Math.min(parseInt(m[2], 10), buf.byteLength - 1) : buf.byteLength - 1;
  return new Response(buf.slice(start, end + 1), { status: 206, headers: {
    'Content-Type': 'audio/mpeg', 'Accept-Ranges': 'bytes',
    'Content-Range': `bytes ${start}-${end}/${buf.byteLength}`, 'Content-Length': String(end - start + 1) } });
}

self.addEventListener('fetch', e => {
  const req = e.request, url = new URL(req.url);
  if (req.method !== 'GET' || url.origin !== location.origin) return;
  if (url.pathname.endsWith('.mp3')) { e.respondWith(audioResponse(req)); return; }
  // app shell: cache first, refresh in the background
  e.respondWith(caches.match(req, { ignoreSearch: true }).then(hit => {
    const net = fetch(req).then(r => { if (r.ok) caches.open(SHELL).then(c => c.put(req, r.clone())); return r; }).catch(() => hit);
    return hit || net;
  }));
});

self.addEventListener('push', e => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch (err) { d = { body: e.data && e.data.text() }; }
  // if the app is open, let it play the adhan right away
  e.waitUntil(self.clients.matchAll({ type: 'window' }).then(cs => cs.forEach(c => c.postMessage({ type: 'adhan' }))));
  e.waitUntil(self.registration.showNotification(d.title || 'حان وقت الأذان', {
    body: d.body || '', icon: 'icons/icon-192.png', badge: 'icons/icon-192.png',
    tag: d.tag || 'adhan', renotify: true, lang: 'ar', dir: 'rtl',
    vibrate: [300, 150, 300, 150, 600], requireInteraction: true, data: { url: './#/adhan' } }));
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: 'window' }).then(cs => {
    if (cs.length) { cs[0].navigate('./#/adhan'); return cs[0].focus(); }
    return self.clients.openWindow('./#/adhan');
  }));
});
