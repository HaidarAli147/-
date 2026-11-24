package com.salah.bahrain;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AdhanReceiver extends BroadcastReceiver {
    static final String CHANNEL = "adhan";

    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Scheduler.ACTION_ADHAN.equals(action)) {
            String name = intent.getStringExtra("name");
            Scheduler.scheduleNext(c);               // always line up the next prayer first
            notifyPrayer(c, name == null ? "" : name);
            PendingResult pr = goAsync();
            AdhanSound.play(c, pr::finish);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Scheduler.scheduleNext(c);
        }
    }

    private void notifyPrayer(Context c, String name) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "أذان", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null); // the sound is played by AdhanSound
        nm.createNotificationChannel(ch);
        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت صلاة " + name)
                .setContentText("حان الآن موعد أذان " + name + " في المنامة")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(1001, n);
    }
}
