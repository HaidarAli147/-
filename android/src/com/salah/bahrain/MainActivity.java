package com.salah.bahrain;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class MainActivity extends Activity {
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    private static final String[] DAYS = {"الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"};
    private static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};

    private static final class Row {
        final String name; final double hours; final boolean isPrayer;
        Row(String n, double h, boolean p) { name = n; hours = h; isPrayer = p; }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView nextName, nextCountdown, dateView;
    private LinearLayout list;
    private LocalDate shownDate;
    private final Runnable tick = this::tickOnce;

    private void tickOnce() {
        refresh();
        handler.postDelayed(tick, 1000);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(tick); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        root.addView(Ui.header(this, "مواقيت الصلاة"));

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        body.setPadding(pad, pad, pad, pad);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView city = Ui.text(this, "المنامة - مملكة البحرين", 22, Ui.GREEN, true);
        city.setGravity(Gravity.CENTER);
        body.addView(city);
        dateView = Ui.text(this, "", 20, Ui.TEXT, false);
        dateView.setGravity(Gravity.CENTER);
        body.addView(dateView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Ui.rounded(Ui.GREEN, 22, this));
        card.setPadding(pad, Ui.dp(this, 20), pad, Ui.dp(this, 20));
        TextView lbl = Ui.text(this, "الصلاة القادمة", 20, Color.parseColor("#E8C547"), true);
        lbl.setGravity(Gravity.CENTER);
        nextName = Ui.text(this, "", 40, Color.WHITE, true);
        nextName.setGravity(Gravity.CENTER);
        nextCountdown = Ui.text(this, "", 30, Color.WHITE, false);
        nextCountdown.setGravity(Gravity.CENTER);
        card.addView(lbl);
        card.addView(nextName);
        card.addView(nextCountdown);
        body.addView(card, Ui.lp(this, 14));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list, Ui.lp(this, 10));

        TextView note = Ui.text(this, "الحساب وفق المذهب الجعفري (الفجر ١٦° ، المغرب ٤° ، العشاء ١٤°)", 15,
                Color.parseColor("#6B6B6B"), false);
        note.setGravity(Gravity.CENTER);
        body.addView(note, Ui.lp(this, 6));

        TextView duas = Ui.button(this, "الأدعية", v -> startActivity(new Intent(this, DuaListActivity.class)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(pad, Ui.dp(this, 6), pad, pad);
        root.addView(duas, bp);

        setContentView(root);
    }

    private void refresh() {
        LocalDateTime now = LocalDateTime.now(BAHRAIN);
        LocalDate today = now.toLocalDate();

        if (!today.equals(shownDate)) {
            shownDate = today;
            dateView.setText(DAYS[today.getDayOfWeek().getValue() - 1] + "  " + Ui.ar(today.getDayOfMonth())
                    + " " + MONTHS[today.getMonthValue() - 1] + " " + Ui.ar(today.getYear()));
        }

        PrayerCalc.Times t = PrayerCalc.compute(today);
        Row[] rows = {
                new Row("الفجر", t.fajr, true),
                new Row("الشروق", t.sunrise, false),
                new Row("الظهر", t.dhuhr, true),
                new Row("العصر", t.asr, true),
                new Row("المغرب", t.maghrib, true),
                new Row("العشاء", t.isha, true),
                new Row("منتصف الليل الشرعي", t.midnight, false),
        };

        // Next prayer
        String nextLabel = null;
        LocalDateTime nextAt = null;
        for (Row r : rows) {
            if (!r.isPrayer) continue;
            LocalDateTime at = PrayerCalc.at(today, r.hours);
            if (at.isAfter(now)) { nextLabel = r.name; nextAt = at; break; }
        }
        if (nextAt == null) {
            nextLabel = "الفجر";
            nextAt = PrayerCalc.at(today.plusDays(1), PrayerCalc.compute(today.plusDays(1)).fajr);
        }
        long secs = Math.max(0, Duration.between(now, nextAt).getSeconds());
        nextName.setText(nextLabel);
        nextCountdown.setText("بعد " + Ui.ar(String.format("%02d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60)));

        // List
        list.removeAllViews();
        for (Row r : rows) {
            boolean next = r.isPrayer && r.name.equals(nextLabel);
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(this, 14);
            line.setPadding(p, p, p, p);
            line.setBackground(Ui.rounded(next ? Ui.GREEN_LIGHT : Color.WHITE, 14, this));
            int nameColor = r.isPrayer ? Ui.GREEN : Color.parseColor("#7A7A7A");
            TextView n = Ui.text(this, r.name, r.isPrayer ? 26 : 20, nameColor, r.isPrayer);
            TextView tm = Ui.text(this, Ui.time12(PrayerCalc.timeOf(r.hours)), r.isPrayer ? 28 : 22, Ui.TEXT, r.isPrayer);
            tm.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            line.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
            line.addView(tm, new LinearLayout.LayoutParams(-2, -2));
            list.addView(line, Ui.lp(this, 8));
        }
    }
}
