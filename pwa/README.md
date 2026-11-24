# مواقيت الصلاة - PWA

Installable website (no build step): prayer times for Manama (Ja'fari), daily ziyara and dua with
audio, tasbih counter. Works offline after the first visit (service worker).

Files: `index.html`, `style.css`, `app.js`, `prayer.js`, `sw.js`, `manifest.webmanifest`,
`data/duas.txt` (same format as the Android app), `audio/`, `fonts/`, `icons/`.

Run locally: `cd pwa && python3 -m http.server 8000` then open http://localhost:8000
(a PWA must be served over HTTPS, or localhost, to install).

Limit: a website cannot wake itself at prayer time. The adhan alert (takbir via the phone's Arabic
voice + notification) works only while the app is open. Texts/audio: duas.org (credited in the app).

## Adhan notification when the app is closed (Web Push)

A small server (Vercel Functions in `api/`) sends a push at Fajr, Dhuhr and Maghrib (Bahrain, Ja'fari times).
Chrome on Android delivers it even when the app is closed; the user only has to allow notifications.

| File | Role |
|---|---|
| `api/subscribe.js` | stores / removes a device's push subscription (Vercel Blob) |
| `api/cron.js` | called every minute; pushes at the three prayer times (window of 3 min, no duplicates) |
| `api/test.js` | "test notification" button - pushes to the caller's own device |
| `sw.js` | shows the notification when a push arrives |

Environment variables (Vercel project): `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`
(an https URL, e.g. the site URL), `CRON_SECRET`, and `BLOB_READ_WRITE_TOKEN` (created by a Vercel Blob store).
The public key is also in `app.js` (`VAPID_PUBLIC`). Generate a pair with
`node -e "console.log(require('web-push').generateVAPIDKeys())"` - changing keys means everyone must re-enable the adhan.

Scheduler: something must call `https://<site>/api/cron?key=<CRON_SECRET>` **every minute**
(for example a free cron-job.org job). Vercel's own cron only runs every minute on a paid plan.

Limits: needs internet on the phone; the notification uses the phone's normal notification sound
(a web notification cannot play the takbir recording by itself - it plays when the app is opened).
