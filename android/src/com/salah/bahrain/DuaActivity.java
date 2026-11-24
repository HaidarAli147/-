package com.salah.bahrain;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class DuaActivity extends Activity {
    private static final String[] STEP_NAME = {"اللَّهُ أَكْبَرُ", "الْحَمْدُ لِلَّهِ", "سُبْحَانَ اللَّهِ"};
    private static final int[] STEP_GOAL = {34, 33, 33};

    private int step = 0, count = 0;
    private TextView stepName, counter, hint;
    private float textSp = 30;
    private TextView duaText;
    private final Audio.Player player = new Audio.Player();
    private String audioFile;
    private TextView audioBtn;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        List<Duas.Dua> duas = Duas.load(this);
        int idx = getIntent().getIntExtra("index", 0);
        Duas.Dua dua = duas.get(Math.max(0, Math.min(idx, duas.size() - 1)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(Ui.header(this, dua.title));

        int pad = Ui.dp(this, 16);
        if (dua.tasbih) {
            buildTasbih(root, pad);
        } else {
            ScrollView sv = new ScrollView(this);
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(pad, pad, pad, pad);
            sv.addView(body);
            duaText = Ui.text(this, dua.text.toString(), 30, Ui.TEXT, false);
            duaText.setTypeface(Ui.duaFont(this));
            duaText.setLineSpacing(0, 1.6f);
            duaText.setGravity(Gravity.START);
            duaText.setBackground(Ui.rounded(Color.WHITE, 16, this));
            int p = Ui.dp(this, 18);
            duaText.setPadding(p, p, p, p);
            body.addView(duaText);
            root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

            LinearLayout sizes = new LinearLayout(this);
            sizes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView bigger = Ui.button(this, "تكبير +", v -> resize(4));
            TextView smaller = Ui.button(this, "تصغير −", v -> resize(-4));
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1f);
            a.setMargins(pad, 6, 6, 0);
            LinearLayout.LayoutParams c = new LinearLayout.LayoutParams(0, -2, 1f);
            c.setMargins(6, 6, pad, 0);
            sizes.addView(bigger, a);
            sizes.addView(smaller, c);
            root.addView(sizes, new LinearLayout.LayoutParams(-1, -2));
        }

        String file = Audio.resolve(this, dua);
        if (file != null) {
            audioFile = file;
            audioBtn = Ui.button(this, "إيقاف مؤقت", v -> {
                if (!player.isActive()) player.play(this, audioFile, this::audioDone);
                else player.toggle();
                audioBtn.setText(player.isPaused() ? "متابعة" : "إيقاف مؤقت");
            });
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
            ap.setMargins(pad, 6, pad, 0);
            root.addView(audioBtn, ap);
        }
        TextView back = Ui.button(this, "رجوع", v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(pad, Ui.dp(this, 8), pad, pad);
        root.addView(back, bp);
        setContentView(root);
        if (audioFile != null) player.play(this, audioFile, this::audioDone); // plays automatically
    }

    private void audioDone() {
        player.release();
        if (audioBtn != null) audioBtn.setText("إعادة التشغيل");
    }

    @Override protected void onDestroy() { super.onDestroy(); player.release(); }

    private void resize(int delta) {
        textSp = Math.max(20, Math.min(60, textSp + delta));
        duaText.setTextSize(textSp);
    }

    private void buildTasbih(LinearLayout root, int pad) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(pad, pad, pad, pad);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        stepName = Ui.text(this, "", 40, Ui.GREEN, true);
        stepName.setGravity(Gravity.CENTER);
        body.addView(stepName);

        counter = Ui.text(this, "", 96, Color.WHITE, true);
        counter.setGravity(Gravity.CENTER);
        counter.setBackground(Ui.rounded(Ui.GREEN, 400, this));
        int size = Ui.dp(this, 260);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(size, size);
        cp.topMargin = Ui.dp(this, 20);
        cp.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(counter, cp);
        counter.setOnClickListener(v -> tap());

        hint = Ui.text(this, "", 22, Color.parseColor("#6B6B6B"), false);
        hint.setGravity(Gravity.CENTER);
        body.addView(hint, Ui.lp(this, 16));

        TextView reset = Ui.button(this, "إعادة من البداية", v -> { step = 0; count = 0; renderTasbih(); });
        root.addView(reset, bpWithMargins(pad));
        renderTasbih();
    }

    private LinearLayout.LayoutParams bpWithMargins(int pad) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(pad, 0, pad, 0);
        return p;
    }

    private void tap() {
        if (step >= 3) return;
        count++;
        if (count >= STEP_GOAL[step]) { step++; count = 0; }
        renderTasbih();
    }

    private void renderTasbih() {
        if (step >= 3) {
            stepName.setText("تقبّل الله أعمالكم");
            counter.setText("✓");
            hint.setText("تمّت التسبيحة");
        } else {
            stepName.setText(STEP_NAME[step]);
            counter.setText(Ui.ar(count));
            hint.setText("المطلوب: " + Ui.ar(STEP_GOAL[step]) + "  -  اضغطي على الدائرة");
        }
    }
}
