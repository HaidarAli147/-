# مواقيت الصلاة - البحرين

Prayer times for Bahrain (Ja'fari / Shia), with daily ziyara and dua, built for an Arabic-speaking family member.
Everything is in Arabic, right-to-left, with large readable text.

| Folder | What it is |
|---|---|
| [`android/`](android/) | The Android app (plain Java, no Gradle). `./build.sh` -> `salah-bahrain.apk` (a prebuilt APK is included). |
| [`pwa/`](pwa/) | The same app as an installable website (works offline), plus an optional push-notification server. |

## Features
- Prayer times for Manama computed offline (Ja'fari: Fajr 16°, Maghrib 4°, Isha 14°), next-prayer countdown.
- Adhan alert at Fajr, Dhuhr and Maghrib (only the opening "الله أكبر، الله أكبر").
- "ورد اليوم": the ziyara and dua of the current weekday, shown with text and played automatically.
- Dua menu: Dua al-Sabah, Ziyarat Ashura, Warith and al-Abbas, short Quran suras and duas, morning/evening adhkar.

## Content
Texts and recordings of the daily ziyarat/duas come from [duas.org](https://www.duas.org)
("No Copyright - Extend credit if using the content"). They are fetched by `android/tools/fetch_duas.py`.
Check the texts against a trusted Mafatih before relying on them.

See `android/README.md` and `pwa/README.md` for details.
