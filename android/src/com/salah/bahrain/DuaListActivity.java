package com.salah.bahrain;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class DuaListActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(Ui.header(this, "الأدعية"));

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        body.setPadding(pad, pad, pad, pad);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        List<Duas.Dua> duas = Duas.load(this);
        for (int i = 0; i < duas.size(); i++) {
            final int idx = i;
            TextView item = Ui.text(this, duas.get(i).title, 26, Ui.GREEN, true);
            item.setBackground(Ui.rounded(android.graphics.Color.WHITE, 16, this));
            int p = Ui.dp(this, 20);
            item.setPadding(p, p, p, p);
            item.setOnClickListener(v -> {
                Intent it = new Intent(this, DuaActivity.class);
                it.putExtra("index", idx);
                startActivity(it);
            });
            body.addView(item, Ui.lp(this, 10));
        }

        TextView back = Ui.button(this, "رجوع", v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(pad, Ui.dp(this, 6), pad, pad);
        root.addView(back, bp);
        setContentView(root);
    }
}
