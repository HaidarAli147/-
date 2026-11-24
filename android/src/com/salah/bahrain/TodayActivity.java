package com.salah.bahrain;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Picks today's ziyara and dua by weekday, shows the text and plays the recitation automatically. */
public class TodayActivity extends Activity {
    private static final String[] DAYS = {"الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"};

    private final List<Duas.Dua> items = new ArrayList<>();
    private final List<String> files = new ArrayList<>();
    private final List<TextView> titles = new ArrayList<>();
    private final Audio.Player player = new Audio.Player();
    private TextView control;
    private int current = -1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        int today = LocalDate.now(Scheduler.BAHRAIN).getDayOfWeek().getValue();

        List<Duas.Dua> all = Duas.load(this);
        for (Duas.Dua d : all) if (d.day == today) items.add(d);       // ziyara first
        for (Duas.Dua d : all) if (d.dailyDua == today) items.add(d);  // then the day's dua

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(Ui.header(this, "ورد يوم " + DAYS[today - 1]));

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 14);
        body.setPadding(pad, pad, pad, pad);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        boolean anyAudio = false;
        for (Duas.Dua d : items) {
            String f = Audio.resolve(this, d);
            files.add(f);
            anyAudio |= f != null;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Ui.rounded(Color.WHITE, 16, this));
            int p = Ui.dp(this, 16);
            card.setPadding(p, p, p, p);

            TextView t = Ui.text(this, d.title, 22, Ui.GREEN, true);
            titles.add(t);
            TextView text = Ui.text(this, d.text.toString(), 28, Ui.TEXT, false);
            text.setTypeface(Ui.duaFont(this));
            text.setLineSpacing(0, 1.15f);
            text.setGravity(Gravity.START);
            card.addView(t);
            card.addView(text, Ui.lp(this, 8));
            body.addView(card, Ui.lp(this, 10));
        }
        TextView credit = Ui.text(this, "النصوص والتسجيلات من موقع duas.org", 14, Color.parseColor("#6B6B6B"), false);
        credit.setGravity(Gravity.CENTER);
        body.addView(credit, Ui.lp(this, 12));
        if (items.isEmpty()) {
            body.addView(Ui.text(this, "لا توجد زيارة أو دعاء مضاف لهذا اليوم", 22, Ui.TEXT, false));
        }

        if (anyAudio) {
            control = Ui.button(this, "إيقاف مؤقت", v -> {
                player.toggle();
                control.setText(player.isPaused() ? "متابعة" : "إيقاف مؤقت");
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(pad, 6, pad, 0);
            root.addView(control, cp);
        }
        TextView back = Ui.button(this, "رجوع", v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(pad, Ui.dp(this, 8), pad, pad);
        root.addView(back, bp);
        setContentView(root);

        playFrom(0); // starts automatically
    }

    /** Plays the next item (from index i) that has a recording, then continues to the following one. */
    private void playFrom(int i) {
        while (i < items.size() && files.get(i) == null) i++;
        if (current >= 0 && current < titles.size()) titles.get(current).setTextColor(Ui.GREEN);
        if (i >= items.size()) { current = -1; if (control != null) control.setText("إعادة التشغيل"); return; }
        current = i;
        titles.get(i).setTextColor(Ui.GOLD);
        final int next = i + 1;
        player.play(this, files.get(i), () -> playFrom(next));
        if (control != null) control.setText("إيقاف مؤقت");
    }

    @Override protected void onDestroy() { super.onDestroy(); player.release(); }
}
