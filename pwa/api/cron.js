/* Called every minute by a scheduler. Sends the adhan push at Fajr, Dhuhr and Maghrib (Bahrain, Ja'fari times).
   Protected by CRON_SECRET (?key=... or "Authorization: Bearer ..."). */
const P = require('../prayer.js');
const { listSubs, removeSub, wasSent, markSent } = require('./_store');
const { send } = require('./_push');

const AR = n => String(n).replace(/[0-9]/g, d => '٠١٢٣٤٥٦٧٨٩'[d]);
const time12 = m => { const h = Math.floor(m / 60) % 24, mm = m % 60; return AR(`${h % 12 === 0 ? 12 : h % 12}:${String(mm).padStart(2, '0')}`) + ' ' + (h < 12 ? 'ص' : 'م'); };
const PRAYERS = [['fajr', 'الفجر'], ['dhuhr', 'الظهر'], ['maghrib', 'المغرب']];

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  const secret = process.env.CRON_SECRET;
  const given = (req.query && req.query.key) || (req.headers.authorization || '').replace(/^Bearer /, '');
  if (!secret || given !== secret) return res.status(401).json({ error: 'unauthorized' });

  // Bahrain is UTC+3 all year. `at` (unix ms) lets us test a specific moment.
  const at = req.query && req.query.at ? Number(req.query.at) : Date.now();
  const d = new Date(at + 3 * 3600e3);
  const y = d.getUTCFullYear(), m = d.getUTCMonth() + 1, day = d.getUTCDate();
  const nowMin = d.getUTCHours() * 60 + d.getUTCMinutes();
  const times = P.compute(y, m, day);
  const result = { now: `${d.getUTCHours()}:${d.getUTCMinutes()}`, sent: [] };

  for (const [k, name] of PRAYERS) {
    const pm = P.minutes(times[k]);
    // a small window so one missed cron tick doesn't lose the adhan; the marker prevents duplicates
    if (nowMin < pm || nowMin >= pm + 3) continue;
    const key = `${y}-${m}-${day}-${k}`;
    if (await wasSent(key)) continue;
    await markSent(key);
    const subs = await listSubs();
    let ok = 0, gone = 0;
    await Promise.all(subs.map(async s => {
      const r = await send(s, { title: 'حان وقت أذان ' + name, body: `المنامة • ${time12(pm)}`, tag: 'adhan-' + k, prayer: k });
      if (r === 'ok') ok++; else if (r === 'gone') { gone++; await removeSub(s.endpoint); }
    }));
    result.sent.push({ prayer: k, subscribers: subs.length, ok, expired: gone });
  }
  res.status(200).json(result);
};
