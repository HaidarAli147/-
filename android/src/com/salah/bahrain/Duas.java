package com.salah.bahrain;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Loads assets/duas.txt. Entries start with "# title"; the "@tasbih" line marks the counter entry. */
final class Duas {
    static final class Dua {
        String title;
        boolean tasbih;
        int day; // 1=Monday..7=Sunday (java.time), 0 = not tied to a day
        final StringBuilder text = new StringBuilder();
    }

    static List<Dua> load(Context c) {
        List<Dua> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open("duas.txt"), "UTF-8"))) {
            Dua cur = null;
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("# ")) {
                    cur = new Dua();
                    cur.title = line.substring(2).trim();
                    out.add(cur);
                } else if (cur != null) {
                    if (line.trim().equals("@tasbih")) cur.tasbih = true;
                    else if (line.startsWith("@day ")) cur.day = Integer.parseInt(line.substring(5).trim());
                    else cur.text.append(line).append('\n');
                }
            }
        } catch (Exception ignored) { }
        for (Dua d : out) {
            String s = d.text.toString().trim();
            d.text.setLength(0);
            d.text.append(s);
        }
        return out;
    }
}
