package com.salah.bahrain;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plays only the opening "الله أكبر، الله أكبر" of the adhan.
 * If assets/audio/adhan_takbir.(mp3|ogg|m4a|wav) exists it is played; otherwise the phone's Arabic voice says it.
 */
final class AdhanSound {
    private static final AudioAttributes ATTRS = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();

    /** Fallback when no Arabic voice exists: the default notification sound, so the alert is never silent. */
    private static void beep(Context app, Runnable finish) {
        try {
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(app,
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION));
            r.setAudioAttributes(ATTRS);
            r.play();
            new Handler(Looper.getMainLooper()).postDelayed(() -> { r.stop(); finish.run(); }, 3000);
        } catch (Exception e) {
            finish.run();
        }
    }

    static void play(Context c, Runnable done) {
        final Context app = c.getApplicationContext();
        final AtomicBoolean finished = new AtomicBoolean(false);
        final Runnable finish = () -> { if (finished.compareAndSet(false, true)) done.run(); };
        new Handler(Looper.getMainLooper()).postDelayed(finish, 9000); // receiver must end within ~10s (the clip keeps playing)

        try {   // the bundled recording
            MediaPlayer mp = MediaPlayer.create(app, R.raw.adhan_takbir, ATTRS, 0);
            if (mp != null) {
                mp.setOnCompletionListener(m -> { m.release(); finish.run(); });
                mp.start();
                return;
            }
        } catch (Exception ignored) { }
        String file = Audio.find(app, "adhan_takbir");
        if (file != null) {
            try (android.content.res.AssetFileDescriptor fd = app.getAssets().openFd("audio/" + file)) {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(ATTRS);
                mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mp.setOnCompletionListener(m -> { m.release(); finish.run(); });
                mp.prepare();
                mp.start();
                return;
            } catch (Exception ignored) { }
        }
        final TextToSpeech[] holder = new TextToSpeech[1];
        holder[0] = new TextToSpeech(app, status -> {
            TextToSpeech tts = holder[0];
            if (status != TextToSpeech.SUCCESS || tts == null) { beep(app, finish); return; }
            int r = tts.setLanguage(new Locale("ar"));
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.shutdown(); beep(app, finish); return;
            }
            tts.setAudioAttributes(ATTRS);
            tts.setSpeechRate(0.55f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { tts.shutdown(); finish.run(); }
                @Override public void onError(String id) { tts.shutdown(); beep(app, finish); }
            });
            tts.speak("اَللّٰهُ أَكْبَرُ . اَللّٰهُ أَكْبَرُ", TextToSpeech.QUEUE_FLUSH, null, "adhan");
        });
    }
}
