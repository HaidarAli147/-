package com.salah.bahrain;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;

final class Ui {
    static final int GREEN = Color.parseColor("#0B5D4B");
    static final int GREEN_LIGHT = Color.parseColor("#DDEFE8");
    static final int GOLD = Color.parseColor("#C9A227");
    static final int CREAM = Color.parseColor("#FBF6E9");
    static final int TEXT = Color.parseColor("#1F2A27");

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static String ar(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        return b.toString();
    }

    static String ar(long n) { return ar(Long.toString(n)); }

    /** 12-hour time such as "٥:٣٠ ص". */
    static String time12(LocalTime t) {
        int h = t.getHour();
        String suffix = h < 12 ? "ص" : "م";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return ar(h12 + ":" + (t.getMinute() < 10 ? "0" : "") + t.getMinute()) + " " + suffix;
    }

    static GradientDrawable rounded(int color, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(t.getTypeface(), bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return t;
    }

    static TextView button(Context c, String s, View.OnClickListener l) {
        TextView t = text(c, s, 26, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(GREEN, 18, c));
        t.setPadding(dp(c, 16), dp(c, 18), dp(c, 16), dp(c, 18));
        t.setOnClickListener(l);
        return t;
    }

    static LinearLayout.LayoutParams lp(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, topDp);
        return p;
    }

    /** Green title bar. */
    static TextView header(Context c, String title) {
        TextView t = text(c, title, 28, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(GREEN);
        t.setPadding(dp(c, 16), dp(c, 22), dp(c, 16), dp(c, 22));
        return t;
    }
}
