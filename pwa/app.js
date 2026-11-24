'use strict';
/* ---------- helpers ---------- */
const $ = (s, r = document) => r.querySelector(s);
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const AR = n => String(n).replace(/[0-9]/g, d => '٠١٢٣٤٥٦٧٨٩'[d]);
const DAYS = ['الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة', 'السبت'];   // JS getUTCDay order
const MONTHS = ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];
const store = {
  get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch (e) { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) {} }
};

/** "Now" in Bahrain (UTC+3, no daylight saving) as an object of UTC getters. */
function bahrainNow() {
  const d = new Date(Date.now() + 3 * 3600e3);
  return { y: d.getUTCFullYear(), m: d.getUTCMonth() + 1, d: d.getUTCDate(), wd: d.getUTCDay(),
           min: d.getUTCHours() * 60 + d.getUTCMinutes(), sec: d.getUTCSeconds() };
}
const javaDay = wd => ((wd + 6) % 7) + 1;   // 1=Monday .. 7=Sunday, same numbering as duas.txt
function time12(mins) {
  mins = ((mins % 1440) + 1440) % 1440;
  const h = Math.floor(mins / 60), m = mins % 60;
  return AR(`${h % 12 === 0 ? 12 : h % 12}:${String(m).padStart(2, '0')}`) + ' ' + (h < 12 ? 'ص' : 'م');
}

/* ---------- duas.txt ---------- */
let DUAS = [];
async function loadDuas() {
  const txt = await (await fetch('data/duas.txt')).text();
  const out = []; let cur = null;
  for (const line of txt.split('\n')) {
    if (line.startsWith('# ')) { cur = { id: out.length, title: line.slice(2).trim(), text: [], tasbih: false, day: 0, dua: 0, audio: null }; out.push(cur); }
    else if (cur) {
      if (line.trim() === '@tasbih') cur.tasbih = true;
      else if (line.startsWith('@day ')) cur.day = parseInt(line.slice(5), 10);
      else if (line.startsWith('@dua ')) cur.dua = parseInt(line.slice(5), 10);
      else if (line.startsWith('@audio ')) cur.audio = line.slice(7).trim();
      else cur.text.push(line);
    }
  }
  out.forEach(d => { d.text = d.text.join('\n').trim(); if (d.audio === 'none') d.audio = null; });
  DUAS = out;
}
const audioUrl = d => d.audio ? 'audio/' + d.audio : null;

/* ---------- audio player (one at a time) ---------- */
const player = {
  a: new Audio(), queue: [], idx: -1, onchange: null,
  get playing() { return !this.a.paused && !this.a.ended; },
  playList(list, onchange) {
    this.stop(); this.queue = list.filter(d => audioUrl(d)); this.onchange = onchange; this.idx = -1; this.next();
  },
  next() {
    this.idx++;
    if (this.idx >= this.queue.length) { this.idx = -1; this.onchange && this.onchange(null, false); return; }
    const d = this.queue[this.idx];
    this.a.src = audioUrl(d);
    this.a.play().catch(() => {});
    this.onchange && this.onchange(d, true);
  },
  toggle() { if (this.a.paused) this.a.play().catch(() => {}); else this.a.pause(); this.onchange && this.onchange(this.queue[this.idx] || null, !this.a.paused); },
  stop() { this.a.pause(); this.a.removeAttribute('src'); this.queue = []; this.idx = -1; this.onchange = null; }
};
player.a.addEventListener('ended', () => player.next());

/* ---------- views ---------- */
const view = $('#view'), titleEl = $('#title'), backBtn = $('#back');
let timer = null;
function setView(title, isHome) { titleEl.textContent = title; backBtn.hidden = !!isHome; clearInterval(timer); view.replaceChildren(); window.scrollTo(0, 0); }
backBtn.onclick = () => { if (location.hash && location.hash !== '#/') history.length > 1 ? history.back() : (location.hash = '#/'); };

const PRAYERS = [['fajr', 'الفجر', true], ['sunrise', 'الشروق', false], ['dhuhr', 'الظهر', true],
                 ['asr', 'العصر', true], ['maghrib', 'المغرب', true], ['isha', 'العشاء', true], ['midnight', 'منتصف الليل الشرعي', false]];

function home() {
  player.stop(); setView('مواقيت الصلاة', true);
  const head = el('div', 'center');
  const city = el('p', 'city', 'المنامة - مملكة البحرين'), date = el('p', 'date');
  head.append(city, date);
  const next = el('div', 'next', '<div class="lbl">الصلاة القادمة</div><div class="name"></div><div class="count"></div>');
  const list = el('div');
  view.append(head, next, list);

  const status = el('p', 'note'), testBtn = el('button', 'btn small', 'تجربة إشعار الأذان الآن');
  testBtn.style.marginTop = '8px';
  testBtn.onclick = async () => { testBtn.disabled = true; status.textContent = await testPush(); testBtn.disabled = false; };
  const adhanBtn = el('button', 'btn gold'); adhanBtn.onclick = async () => {
    const on = !store.get('adhan', true);
    store.set('adhan', on);
    if (on) { status.textContent = 'جارٍ التفعيل...'; const r = await enablePush(); status.textContent = r.msg; } else { await disablePush(); status.textContent = ''; }
    renderAdhan();
  };
  if (store.get('adhan', true) && pushSupported() && Notification.permission === 'granted') enablePush().then(r => { status.textContent = r.msg; });
  const renderAdhan = () => { const on = store.get('adhan', true); adhanBtn.textContent = on ? 'أذان الفجر والظهر والمغرب: مفعّل ✓' : 'أذان الفجر والظهر والمغرب: متوقف'; adhanBtn.classList.toggle('off', !on); testBtn.hidden = !on; };
  renderAdhan();
  const two = el('div', 'two');
  const duasBtn = el('a', 'btn', 'الأدعية'); duasBtn.href = '#/duas';
  const todayBtn = el('a', 'btn', 'ورد اليوم'); todayBtn.href = '#/today';
  two.append(duasBtn, todayBtn);
  const offline = el('button', 'btn small', 'تحميل الأصوات للاستخدام بدون إنترنت'); offline.style.marginTop = '10px'; offline.onclick = () => downloadAudio(offline);
  view.append(adhanBtn, status, testBtn, two, offline,
    el('p', 'note', 'الحساب وفق المذهب الجعفري (الفجر ١٦° ، المغرب ٤° ، العشاء ١٤°)'),
    el('p', 'note', 'يصلك إشعار الأذان حتى والتطبيق مغلق (يلزم اتصال بالإنترنت وأن تسمحي بالإشعارات)'));

  let shown = '';
  const tick = () => {
    const n = bahrainNow();
    const key = `${n.y}-${n.m}-${n.d}`;
    const t = PrayerCalc.compute(n.y, n.m, n.d);
    if (shown !== key) {
      shown = key;
      date.textContent = `${DAYS[n.wd]}  ${AR(n.d)} ${MONTHS[n.m - 1]} ${AR(n.y)}`;
    }
    // next prayer
    const nowSec = n.min * 60 + n.sec;
    let nextKey = null, nextSec = 0;
    for (const [k, , p] of PRAYERS) if (p && PrayerCalc.minutes(t[k]) * 60 > nowSec) { nextKey = k; nextSec = PrayerCalc.minutes(t[k]) * 60; break; }
    if (!nextKey) { nextKey = 'fajr'; const tm = new Date(Date.now() + 3 * 3600e3 + 86400e3); nextSec = 86400 + PrayerCalc.minutes(PrayerCalc.compute(tm.getUTCFullYear(), tm.getUTCMonth() + 1, tm.getUTCDate()).fajr) * 60; }
    const left = Math.max(0, nextSec - nowSec);
    $('.name', next).textContent = PRAYERS.find(p => p[0] === nextKey)[1];
    $('.count', next).textContent = 'بعد ' + AR([Math.floor(left / 3600), Math.floor(left / 60) % 60, left % 60].map(v => String(v).padStart(2, '0')).join(':'));
    list.replaceChildren(...PRAYERS.map(([k, name, pr]) => {
      const r = el('div', 'row' + (pr ? '' : ' minor') + (k === nextKey ? ' is-next' : ''));
      r.append(el('span', 'n', name), el('span', 't', time12(PrayerCalc.minutes(t[k]))));
      return r;
    }));
  };
  tick(); timer = setInterval(tick, 1000);
}

function duaList() {
  player.stop(); setView('الأدعية');
  const today = javaDay(bahrainNow().wd);
  DUAS.forEach(d => {
    const a = el('a', 'item' + (d.day === today || d.dua === today ? ' today' : ''), d.title);
    a.href = '#/dua/' + d.id; view.append(a);
  });
}

function playButton(onclick) { const b = el('button', 'btn'); b.onclick = onclick; return b; }

function duaView(id) {
  const d = DUAS[id]; if (!d) return (location.hash = '#/duas');
  player.stop(); setView(d.title);
  if (d.tasbih) return tasbih();
  const p = el('p', 'dua'); p.textContent = d.text;
  const card = el('div', 'card'); card.append(p); view.append(card);
  fontControls();
  if (audioUrl(d)) {
    const bar = el('div', 'audio-bar');
    const b = playButton(() => { if (player.queue.length === 0) player.playList([d], upd); else player.toggle(); });
    const upd = (cur, playing) => { b.textContent = playing ? 'إيقاف مؤقت' : (player.a.ended || !player.queue.length ? 'تشغيل' : 'متابعة'); };
    bar.append(b); view.append(bar); upd(null, false);
    player.playList([d], upd);   // starts automatically
  }
}

function fontControls() {
  const apply = () => document.documentElement.style.setProperty('--fs', store.get('fs', 30) + 'px');
  apply();
  const two = el('div', 'two');
  const plus = el('button', 'btn small', 'تكبير الخط +'), minus = el('button', 'btn small', 'تصغير الخط −');
  plus.onclick = () => { store.set('fs', Math.min(64, store.get('fs', 30) + 4)); apply(); };
  minus.onclick = () => { store.set('fs', Math.max(20, store.get('fs', 30) - 4)); apply(); };
  two.append(plus, minus); two.style.marginTop = '10px'; view.append(two);
}

function today() {
  player.stop(); setView('ورد اليوم');
  const n = bahrainNow(), day = javaDay(n.wd);
  titleEl.textContent = 'ورد يوم ' + DAYS[n.wd];
  const items = [...DUAS.filter(d => d.day === day), ...DUAS.filter(d => d.dua === day)];   // ziyara first, then the day's dua
  const heads = new Map();
  items.forEach(d => {
    const c = el('div', 'card'), h = el('h2', '', d.title), p = el('p', 'dua'); p.textContent = d.text;
    c.append(h, p); view.append(c); heads.set(d.id, h);
  });
  if (!items.length) view.append(el('p', 'center', 'لا توجد زيارة أو دعاء مضاف لهذا اليوم'));
  fontControls();
  view.append(el('p', 'credit', 'النصوص والتسجيلات من موقع duas.org'));
  if (items.some(audioUrl)) {
    const bar = el('div', 'audio-bar');
    const b = playButton(() => { if (!player.queue.length) player.playList(items, upd); else player.toggle(); });
    const upd = (cur, playing) => {
      heads.forEach(h => h.classList.remove('playing'));
      if (cur) heads.get(cur.id).classList.add('playing');
      b.textContent = playing ? 'إيقاف مؤقت' : (player.queue.length ? 'متابعة' : 'إعادة التشغيل');
    };
    bar.append(b); view.append(bar); upd(null, false);
    player.playList(items, upd);   // starts automatically
  }
}

function tasbih() {
  const steps = [['اللَّهُ أَكْبَرُ', 34], ['الْحَمْدُ لِلَّهِ', 33], ['سُبْحَانَ اللَّهِ', 33]];
  let step = 0, count = 0;
  const name = el('div', 'tasbih-name'), circle = el('button', 'tasbih-circle'), hint = el('p', 'note');
  const reset = el('button', 'btn', 'إعادة من البداية');
  const draw = () => {
    if (step >= 3) { name.textContent = 'تقبّل الله أعمالكم'; circle.textContent = '✓'; hint.textContent = 'تمّت التسبيحة'; return; }
    name.textContent = steps[step][0]; circle.textContent = AR(count); hint.textContent = `المطلوب: ${AR(steps[step][1])} - اضغطي على الدائرة`;
  };
  circle.onclick = () => { if (step >= 3) return; if (++count >= steps[step][1]) { step++; count = 0; if (navigator.vibrate) navigator.vibrate(60); } draw(); };
  reset.onclick = () => { step = 0; count = 0; draw(); };
  view.append(name, circle, hint, reset); draw();
}

/* ---------- router ---------- */
function route() {
  const h = location.hash || '#/';
  if (h === '#/') home();
  else if (h === '#/duas') duaList();
  else if (h === '#/today') today();
  else if (h.startsWith('#/dua/')) duaView(parseInt(h.slice(6), 10));
  else home();
}

/* ---------- offline audio ---------- */
async function downloadAudio(btn) {
  const files = [...new Set(DUAS.map(audioUrl).filter(Boolean))];
  btn.disabled = true;
  try {
    const cache = await caches.open('salah-audio-v1');
    let done = 0;
    for (const f of files) {
      if (!(await cache.match(f))) { const r = await fetch(f); if (r.ok) await cache.put(f, r); }
      btn.textContent = `جارٍ التحميل ${AR(++done)} / ${AR(files.length)}`;
    }
    btn.textContent = 'تم التحميل ✓ يعمل بدون إنترنت';
  } catch (e) { btn.textContent = 'تعذّر التحميل - حاول مرة أخرى'; btn.disabled = false; }
}

/* ---------- Web Push: adhan notification even when the app is closed ---------- */
const VAPID_PUBLIC = 'BH13bhZ9W3uPqcvbBrlK8WLzuOG1GGta0HXloEKvcESqo_K9DSjJCBGZ-5KTeJ6KDLM50Bx0QZaxj91kvSGJCRg';
const b64ToBytes = b => { const p = '='.repeat((4 - b.length % 4) % 4), r = atob((b + p).replace(/-/g, '+').replace(/_/g, '/')); return Uint8Array.from(r, c => c.charCodeAt(0)); };
const pushSupported = () => 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;

/** Subscribes this device and registers it with the server. Returns a short status message. */
async function enablePush() {
  if (!pushSupported()) return { ok: false, msg: 'هذا المتصفح لا يدعم الإشعارات' };
  if (Notification.permission === 'default') await Notification.requestPermission();
  if (Notification.permission !== 'granted') return { ok: false, msg: 'لم يتم السماح بالإشعارات - فعّلها من إعدادات الموقع' };
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = (await reg.pushManager.getSubscription()) ||
      await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: b64ToBytes(VAPID_PUBLIC) });
    const r = await fetch('api/subscribe', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ subscription: sub.toJSON() }) });
    if (!r.ok) throw new Error('server ' + r.status);
    return { ok: true, msg: 'إشعارات الأذان مفعّلة على هذا الجهاز ✓', endpoint: sub.endpoint };
  } catch (e) { return { ok: false, msg: 'تعذّر تفعيل الإشعارات - تأكد من الاتصال بالإنترنت وحاول مرة أخرى' }; }
}
async function disablePush() {
  try {
    const reg = await navigator.serviceWorker.ready, sub = await reg.pushManager.getSubscription();
    if (sub) { await fetch('api/subscribe', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ endpoint: sub.endpoint }) }).catch(() => {}); await sub.unsubscribe(); }
  } catch (e) {}
}
async function testPush() {
  const reg = await navigator.serviceWorker.ready, sub = await reg.pushManager.getSubscription();
  if (!sub) return 'فعّل الأذان أولاً';
  const r = await fetch('api/test', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ endpoint: sub.endpoint }) }).catch(() => null);
  return r && r.ok ? 'تم إرسال إشعار تجريبي - يجب أن يظهر خلال ثوانٍ' : 'تعذّر إرسال الإشعار التجريبي';
}

/* ---------- adhan alert (only works while the app is open) ---------- */
async function ensureNotifications() {
  if (!('Notification' in window)) return false;
  if (Notification.permission === 'default') await Notification.requestPermission();
  return Notification.permission === 'granted';
}
function takbir() {
  if (!('speechSynthesis' in window)) return;
  const u = new SpeechSynthesisUtterance('اَللّٰهُ أَكْبَرُ . اَللّٰهُ أَكْبَرُ');
  u.lang = 'ar'; u.rate = 0.6; speechSynthesis.cancel(); speechSynthesis.speak(u);
}
async function fireAdhan(name, mins, k) {
  const body = `المنامة • ${time12(mins)}`;
  takbir();
  if (await ensureNotifications()) {
    const reg = await navigator.serviceWorker?.getRegistration();
    const opts = { body, icon: 'icons/icon-192.png', badge: 'icons/icon-192.png', tag: 'adhan-' + k, renotify: true, lang: 'ar', dir: 'rtl' };
    if (reg) reg.showNotification('حان وقت أذان ' + name, opts); else new Notification('حان وقت أذان ' + name, opts);
  }
}
function adhanWatcher() {
  setInterval(() => {
    if (!store.get('adhan', true)) return;
    const n = bahrainNow(), t = PrayerCalc.compute(n.y, n.m, n.d);
    for (const [k, name] of [['fajr', 'الفجر'], ['dhuhr', 'الظهر'], ['maghrib', 'المغرب']]) {
      const m = PrayerCalc.minutes(t[k]), key = `fired-${n.y}-${n.m}-${n.d}-${k}`;
      if (n.min >= m && n.min < m + 2 && !store.get(key, false)) { store.set(key, true); fireAdhan(name, m, k); }
    }
  }, 5000);
}

/* ---------- start ---------- */
(async function start() {
  try { await loadDuas(); } catch (e) { view.textContent = 'تعذّر تحميل الأدعية - تأكد من الاتصال بالإنترنت'; }
  window.addEventListener('hashchange', route);
  route(); adhanWatcher();
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
})();
