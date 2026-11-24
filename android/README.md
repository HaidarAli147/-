# مواقيت الصلاة - Bahrain Ja'fari prayer times app

Offline Android app (plain Java, no Gradle). Build: `./build.sh` -> `salah-bahrain.apk`.

## Adding texts: `assets/duas.txt`

Each entry starts with `# Title`. Optional tag lines come right after it:

| Tag | Meaning |
|---|---|
| `@day N` | This entry is **the ziyara of weekday N** (1=Monday ... 7=Sunday). The "ورد اليوم" screen shows it automatically on that day. |
| `@dua N` | This entry is **the dua of weekday N**, shown after the ziyara on that day. |
| `@audio file.mp3` | Use this specific recording (inside `assets/audio/`) instead of the naming convention below. |
| `@tasbih` | The Zahra tasbih counter. |

Paste the exact text under the tags. Text is shown in the Amiri Quran font (OFL, `assets/fonts`).

Example:

```
# دعاء يوم الثلاثاء
@dua 2
...paste the dua here...
```

## Adding audio: `assets/audio/`

Drop files (mp3 / ogg / m4a / wav) with these names, no code changes needed:

| File | Plays |
|---|---|
| `ziyara_1` ... `ziyara_7` | the day's ziyara, automatically when "ورد اليوم" opens |
| `dua_1` ... `dua_7` | the day's dua, right after the ziyara |
| `adhan_takbir` | the adhan alert - **only** "الله أكبر الله أكبر" (trim the clip to end there) |
| anything else | name it in an `@audio` line |

Days: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday.
Without an `adhan_takbir` file the alert uses the phone's Arabic voice instead.

Only add recordings you have the right to use.

## Prayer times

Computed offline for Manama (26.2285, 50.5860, UTC+3), Ja'fari method: Fajr 16°,
Maghrib 4° below horizon, Isha 14°, Asr shadow ratio 1, midnight = midpoint sunset-Fajr.

## Daily ziyara and dua (generated)

`python3 tools/fetch_duas.py` pulls each weekday's ziyara and the dua of Imam Zain al-Abidin (ع)
for that day, with their audio, from duas.org (their site states "No Copyright - Extend credit if
using the content" - the app shows that credit). It rewrites the `@day` / `@dua` entries in
`assets/duas.txt` and re-encodes audio into `assets/audio/` (mono 48 kbps). The day dua is
recited by Abdulhay Al-Qamber according to the source file name.

The adhan opening (`adhan_takbir`) is not included: add your own clip, otherwise the phone's Arabic voice is used.
