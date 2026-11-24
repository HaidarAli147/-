# مواقيت الصلاة - PWA

Installable website (no build step): prayer times for Manama (Ja'fari), daily ziyara and dua with
audio, tasbih counter. Works offline after the first visit (service worker).

Files: `index.html`, `style.css`, `app.js`, `prayer.js`, `sw.js`, `manifest.webmanifest`,
`data/duas.txt` (same format as the Android app), `audio/`, `fonts/`, `icons/`.

Run locally: `cd pwa && python3 -m http.server 8000` then open http://localhost:8000
(a PWA must be served over HTTPS, or localhost, to install).

Limit: a website cannot wake itself at prayer time. The adhan alert (takbir via the phone's Arabic
voice + notification) works only while the app is open. Texts/audio: duas.org (credited in the app).
