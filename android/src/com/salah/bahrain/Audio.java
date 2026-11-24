package com.salah.bahrain;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

/**
 * Audio lives in assets/audio/. File names (any of mp3, ogg, m4a, wav):
 *   ziyara_N  - ziyara of weekday N      dua_N - dua of weekday N      (N: 1=Monday .. 7=Sunday)
 *   adhan_takbir - the opening "Allahu Akbar, Allahu Akbar" for the adhan alert
 * An entry can also name a file explicitly with an "@audio file.mp3" line in duas.txt.
 */
final class Audio {
    static final AudioAttributes ATTRS = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();

    /** Returns the asset file name (inside assets/audio) for a base name such as "ziyara_2", or null. */
    static String find(Context c, String base) {
        try {
            String[] files = c.getAssets().list("audio");
            if (files == null) return null;
            for (String f : files) {
                int dot = f.lastIndexOf('.');
                if (dot > 0 && f.substring(0, dot).equals(base)) return f;
            }
        } catch (Exception ignored) { }
        return null;
    }

    static String resolve(Context c, Duas.Dua d) {
        if (d.audio != null) {
            if (d.audio.equals("none")) return null;
            return find(c, d.audio.replaceAll("\\.[A-Za-z0-9]+$", ""));
        }
        if (d.day > 0) return find(c, "ziyara_" + d.day);
        if (d.dailyDua > 0) return find(c, "dua_" + d.dailyDua);
        return null;
    }

    /** Single-file player with pause/resume and a completion callback. */
    static final class Player {
        private MediaPlayer mp;
        private boolean paused;

        void play(Context c, String file, Runnable onDone) {
            release();
            try (AssetFileDescriptor fd = c.getAssets().openFd("audio/" + file)) {
                mp = new MediaPlayer();
                mp.setAudioAttributes(ATTRS);
                mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mp.setOnCompletionListener(m -> { if (onDone != null) onDone.run(); });
                mp.prepare();
                mp.start();
                paused = false;
            } catch (Exception e) {
                release();
                if (onDone != null) onDone.run();
            }
        }

        boolean isActive() { return mp != null; }
        boolean isPaused() { return paused; }

        void toggle() {
            if (mp == null) return;
            if (paused) { mp.start(); paused = false; } else { mp.pause(); paused = true; }
        }

        void release() {
            if (mp != null) { try { mp.release(); } catch (Exception ignored) { } mp = null; }
            paused = false;
        }
    }
}
