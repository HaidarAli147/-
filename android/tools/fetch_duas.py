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


# Extra entries (not tied to a weekday). Each: file name, Arabic title, source kind, source, audio URL.
# Placed in the list after the daily entries, in this order.
EXTRAS = [
    ("dua_sabah",      "دعاء الصباح",                 "html",  "https://www.duas.org/mobile/dua-sabah.html",
     "https://mp3.duas.org/sabah%20ali%20bouhamad.mp3"),
    ("ziyara_ashura",  "زيارة عاشوراء",               "json",  ("ziyarat-imam-hussain-ashura", [0], "segments"),
     "https://mp3.duas.org/ziyarat-ashura-imam-hussain.mp3"),
    ("ziyara_warith",  "زيارة وارث",                  "json",  ("ziyarat-waritha-imam-husain", [0], "data"),
     "https://mp3.duas.org/ziyarat/ziarat-warisa.mp3"),
    # the recording (about 9 min) matches the first four text blocks of the page at ~0.9 words/second
    ("ziyara_abbas",   "زيارة أبي الفضل العباس (ع)",  "json",  ("ziyarat-hazrat-abbas-as", [1, 3, 5, 7], "segments"),
     "https://mp3.duas.org/ziyarat/ziyarat-hzabbas.mp3"),
]

# Farsi/Urdu letter forms sometimes used in the source text -> the Arabic forms
NORMALIZE = str.maketrans({"\u06CC": "\u064A", "\u06A9": "\u0643", "\u06C1": "\u0647", "\u06BE": "\u0647"})
ARABIC = re.compile(r"[\u0600-\u06FF]")


def extra_text(kind, src):
    if kind == "json":
        slug, idxs, field = src
        items = json.loads(get(f"https://www.duas.org/data_v2/{slug}.json"))["duas"]
        parts = []
        for i in idxs:
            if field == "segments":
                lines = [x["arabic"].strip() for x in items[i].get("segments", []) if x.get("arabic")]
            else:   # "data": three lines per phrase (Arabic, transliteration, English)
                lines = [l.strip() for l in items[i]["data"].split("\n") if ARABIC.search(l)]
            parts.append("\n".join(lines))
        text = "\n\n".join(parts)
    else:       # old-format page: Arabic lives in <a1> tags; stop before the optional "Sahifa Alavia" addition
        import html as _html
        raw = get(src).decode("utf-8", "ignore")
        raw = raw[:raw.find("Sahifa Alavia dua no 62")]
        lines = []
        for m in re.finditer(r"<a1>(.*?)</a1>", raw, flags=re.S):
            t = re.sub(r"\s+", " ", _html.unescape(re.sub(r"<[^>]+>", "", m.group(1)))).strip()
            if ARABIC.search(t):
                lines.append(t)
        text = "\n".join(lines)
    return text.translate(NORMALIZE)


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
    br = subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=bit_rate",
                                  "-of", "csv=p=0", raw]).decode().strip()
    if br.isdigit() and int(br) <= 64000:
        os.replace(raw, out)            # already small enough
    else:
        # speech: mono, 48 kbps keeps the APK small
        subprocess.check_call(["ffmpeg", "-loglevel", "error", "-y", "-i", raw, "-ac", "1", "-b:a", "48k", out])
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

    for name, title, kind, src, audio_url in EXTRAS:
        text = extra_text(kind, src)
        assert text, name
        fetch_audio(audio_url, name)
        blocks.append(f"# {title}\n@audio {name}.mp3\n{text}\n")

    # keep everything except previously generated entries (and the removed Zahra tasbih)
    old = open(DUAS_TXT, encoding="utf-8").read()
    parts = re.split(r"(?m)^(?=# )", old)
    kept = [p for p in parts if p.strip() and not re.search(r"(?m)^@(day \d|dua \d|audio |tasbih)", p)]
    new = "\n".join(blocks) + "\n" + "\n".join(p.rstrip() + "\n" for p in kept)
    open(DUAS_TXT, "w", encoding="utf-8").write(new)
    print("entries:", len(blocks), "audio files:", len([f for f in os.listdir(AUDIO_DIR) if f.endswith('.mp3')]))


if __name__ == "__main__":
    sys.exit(main())
