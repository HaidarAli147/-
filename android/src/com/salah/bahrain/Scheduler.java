package com.salah.bahrain;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

    private static PendingIntent pending(Context c, String name) { return pending(c, name, 0); }

    /** {@code at} = the moment the alarm is meant to fire (so the receiver can tell a stale alarm after a clock change). */
    private static PendingIntent pending(Context c, String name, long at) {
        Intent i = new Intent(c, AdhanReceiver.class).setAction(ACTION_ADHAN).putExtra("name", name).putExtra("at", at);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancel(Context c) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c, ""));
    }

    static void scheduleNext(Context c) {
        try { doSchedule(c); } catch (Exception ignored) { } // e.g. SecurityException on some phones: app must still work
    }

    private static void doSchedule(Context c) {
        if (!enabled(c)) return;
        AdhanPlan.Next n = AdhanPlan.next(LocalDateTime.now(BAHRAIN));
        if (n == null) return;
        long ms = n.at.atZone(BAHRAIN).toInstant().toEpochMilli();
        Intent show = new Intent(c, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(c, 2, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // same PendingIntent every time: this replaces any earlier alarm, so the time always follows the latest calculation
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE))
                .setAlarmClock(new AlarmManager.AlarmClockInfo(ms, showPi), pending(c, n.name, ms));
    }
}
