package com.salah.bahrain;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;

public class AdhanReceiver extends BroadcastReceiver {
    static final String CHANNEL = "adhan2";

    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Scheduler.ACTION_ADHAN.equals(action)) {
            String name = intent.getStringExtra("name");
            Scheduler.scheduleNext(c);               // always line up the next prayer first
            boolean systemPlays = notifyPrayer(c, name == null ? "" : name);
            if (systemPlays) return;                 // the notification channel plays the adhan itself
            PendingResult pr = goAsync();            // notifications blocked: play it ourselves
            AdhanSound.play(c, pr::finish);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Scheduler.scheduleNext(c);
        }
    }

    /** Posts the notification. Returns true if the system will play the adhan sound (channel enabled). */
    private boolean notifyPrayer(Context c, String name) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "أذان", NotificationManager.IMPORTANCE_HIGH);
        android.net.Uri sound = android.net.Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.adhan_takbir);
        ch.setSound(sound, new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
        // the old silent channel from earlier versions is no longer used
        try { nm.deleteNotificationChannel("adhan"); } catch (Exception ignored) { }

        PrayerCalc.Times t = PrayerCalc.compute(LocalDate.now(Scheduler.BAHRAIN));
        double hours = name.equals("الفجر") ? t.fajr : name.equals("المغرب") ? t.maghrib : t.dhuhr;
        String strip = "الفجر " + Ui.time12(PrayerCalc.timeOf(t.fajr))
                + "   •   الظهر " + Ui.time12(PrayerCalc.timeOf(t.dhuhr))
                + "   •   المغرب " + Ui.time12(PrayerCalc.timeOf(t.maghrib));

        RemoteViews small = view(c, name, hours, null);
        RemoteViews big = view(c, name, hours, strip);

        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomContentView(small)
                .setCustomBigContentView(big)
                .setColor(Ui.GREEN)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(1001, n);
        NotificationChannel cur = nm.getNotificationChannel(CHANNEL);
        return nm.areNotificationsEnabled() && cur != null && cur.getImportance() != NotificationManager.IMPORTANCE_NONE
                && cur.getSound() != null;
    }

    private RemoteViews view(Context c, String name, double hours, String strip) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.notif_adhan);
        v.setInt(R.id.notif_root, "setBackgroundColor", Ui.GREEN);
        v.setTextViewText(R.id.notif_title, "حان وقت أذان " + name);
        v.setTextViewText(R.id.notif_sub, "المنامة  •  " + Ui.time12(PrayerCalc.timeOf(hours)));
        if (strip == null) v.setViewVisibility(R.id.notif_times, View.GONE);
        else v.setTextViewText(R.id.notif_times, strip);
        return v;
    }
}
