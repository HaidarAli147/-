#!/usr/bin/env python3
"""
Builds the weekday ziyara + dua content from duas.org (their site states:
"No Copyright - Extend credit if using the content").

Writes entries into assets/duas.txt and re-encoded audio into assets/audio/.
Run from android/:  python3 tools/fetch_duas.py
Needs network access to www.duas.org and mp3.duas.org and ffmpeg.
"""
import json, os, re, subprocess, sys, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DUAS_TXT = os.path.join(ROOT, "assets", "duas.txt")
AUDIO_DIR = os.path.join(ROOT, "assets", "audio")
UA = {"User-Agent": "Mozilla/5.0"}

# java.time numbering: 1=Monday .. 7=Sunday
DAYS = {
    "monday":    (1, "الاثنين"),
    "tuesday":   (2, "الثلاثاء"),
    "wednesday": (3, "الأربعاء"),
    "thursday":  (4, "الخميس"),
    "friday":    (5, "الجمعة"),
    "saturday":  (6, "السبت"),
    "sunday":    (7, "الأحد"),
}
# (index in the page's "duas" list, Arabic title) - one entry per recording.
ZIYARA = {
    "saturday":  [(5, "زيارة السبت - رسول الله ﷺ")],
    "sunday":    [(5, "زيارة الأحد - أمير المؤمنين (ع)"),
                  (6, "زيارة الأحد - السيدة فاطمة الزهراء (ع)"),
                  (8, "زيارة الأحد - السيدة فاطمة الزهراء (ع) - أخرى")],
    # one recording covers all three text blocks of Monday's ziyara
    "monday":    [((5, 7, 9), "زيارة الاثنين - الإمامين الحسن والحسين (ع)")],
    "tuesday":   [(5, "زيارة الثلاثاء - السجاد والباقر والصادق (ع)")],
    "wednesday": [(5, "زيارة الأربعاء - الكاظم والرضا والجواد والهادي (ع)"),
                  (6, "زيارة الأربعاء - الإمام الرضا (ع)")],
    "thursday":  [(5, "زيارة الخميس - الإمام العسكري (ع)")],
    "friday":    [(35, "زيارة الجمعة - الإمام المهدي (عج)")],
}
DUA_INDEX = {"friday": 11}  # every other day: index 4
DUA_TITLE = "دعاء يوم {} - الإمام زين العابدين (ع)"


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read()


def text_of(item):
    segs = [s["arabic"].strip() for s in item.get("segments", []) if s.get("arabic")]
    return "\n".join(segs)


def fetch_audio(url, name):
    out = os.path.join(AUDIO_DIR, name + ".mp3")
    if os.path.exists(out):
        return
    raw = os.path.join(AUDIO_DIR, name + ".src")
    with open(raw, "wb") as f:
        f.write(get(url.replace(" ", "%20")))
    # speech: mono, 48 kbps keeps the APK small
    subprocess.check_call(["ffmpeg", "-loglevel", "error", "-y", "-i", raw, "-ac", "1",
                           "-b:a", "48k", out])
    os.remove(raw)


def main():
    os.makedirs(AUDIO_DIR, exist_ok=True)
    blocks = []
    for day, (n, ar) in DAYS.items():
        data = json.loads(get(f"https://www.duas.org/data_v2/{day}-rites-taqeeb.json"))
        items = data["duas"]
        entries = []
        for k, (idx, title) in enumerate(ZIYARA[day]):
            idxs = idx if isinstance(idx, tuple) else (idx,)
            it = items[idxs[0]]
            name = f"ziyara_{n}" if k == 0 else f"ziyara_{n}_{k + 1}"
            audio = it.get("audio")
            if audio:
                fetch_audio(audio, name)
            text = "\n\n".join(text_of(items[i]) for i in idxs)
            entries.append((title, f"@day {n}", name + ".mp3" if audio else "none", text))
        it = items[DUA_INDEX.get(day, 4)]
        assert it.get("audio"), day
        fetch_audio(it["audio"], f"dua_{n}")
        entries.append((DUA_TITLE.format(ar), f"@dua {n}", f"dua_{n}.mp3", text_of(it)))
        for title, tag, audio, text in entries:
            assert text, (day, title)
            blocks.append(f"# {title}\n{tag}\n@audio {audio}\n{text}\n")

    # keep everything except previously generated weekday entries
    old = open(DUAS_TXT, encoding="utf-8").read()
    parts = re.split(r"(?m)^(?=# )", old)
    kept = [p for p in parts if p.strip() and not re.search(r"(?m)^@(day|dua) \d", p)]
    new = kept[0].rstrip() + "\n\n" + "\n".join(blocks) + "\n" + "\n".join(p.rstrip() + "\n" for p in kept[1:])
    open(DUAS_TXT, "w", encoding="utf-8").write(new)
    print("entries:", len(blocks), "audio files:", len([f for f in os.listdir(AUDIO_DIR) if f.endswith('.mp3')]))


if __name__ == "__main__":
    sys.exit(main())
