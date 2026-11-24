package com.salah.bahrain;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Schedules the next adhan alarm (exact, Doze-proof via setAlarmClock). */
final class Scheduler {
    static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    static final String ACTION_ADHAN = "com.salah.bahrain.ADHAN";
    private static final String PREFS = "salah";

    static boolean enabled(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("adhan", true);
    }

    static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("adhan", on).apply();
        if (on) scheduleNext(c); else cancel(c);
    }

    private static PendingIntent pending(Context c, String name) {
        Intent i = new Intent(c, AdhanReceiver.class).setAction(ACTION_ADHAN).putExtra("name", name);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancel(Context c) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c, ""));
    }

    static void scheduleNext(Context c) {
        if (!enabled(c)) return;
        LocalDateTime now = LocalDateTime.now(BAHRAIN);
        String[] names = {"الفجر", "الظهر", "المغرب"}; // adhan only at these three
        for (int d = 0; d < 2; d++) {
            LocalDate date = now.toLocalDate().plusDays(d);
            PrayerCalc.Times t = PrayerCalc.compute(date);
            double[] hours = {t.fajr, t.dhuhr, t.maghrib};
            for (int i = 0; i < names.length; i++) {
                LocalDateTime at = PrayerCalc.at(date, hours[i]);
                if (at.isAfter(now)) {
                    long ms = at.atZone(BAHRAIN).toInstant().toEpochMilli();
                    Intent show = new Intent(c, MainActivity.class);
                    PendingIntent showPi = PendingIntent.getActivity(c, 2, show,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE))
                            .setAlarmClock(new AlarmManager.AlarmClockInfo(ms, showPi), pending(c, names[i]));
                    return;
                }
            }
        }
    }
}
