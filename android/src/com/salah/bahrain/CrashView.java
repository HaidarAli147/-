package com.salah.bahrain;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Shown instead of the normal screen after a crash: lets the user share the error. */
final class CrashView {
    static boolean pending(Context c) { return SalahApp.crashFile(c).exists(); }

    static void show(Activity a) {
        File f = SalahApp.crashFile(a);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String l; while ((l = r.readLine()) != null) sb.append(l).append('\n');
        } catch (Exception ignored) { }
        final String report = sb.toString();

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        int pad = Ui.dp(a, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = Ui.text(a, "حدث خطأ في التطبيق في المرة السابقة", 22, Ui.GREEN, true);
        root.addView(title);
        TextView body = new TextView(a);
        body.setText(report);
        body.setTextSize(11);
        body.setTextColor(Ui.TEXT);
        body.setTextIsSelectable(true);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ScrollView sv = new ScrollView(a);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(Ui.button(a, "إرسال تقرير الخطأ", v -> {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Salah app crash report:\n" + report);
            a.startActivity(Intent.createChooser(i, "إرسال"));
        }), Ui.lp(a, 8));
        root.addView(Ui.button(a, "نسخ", v -> {
            ((ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("crash", report));
        }), Ui.lp(a, 8));
        root.addView(Ui.button(a, "متابعة", v -> { f.delete(); a.recreate(); }), Ui.lp(a, 8));
        a.setContentView(root);
    }
}
