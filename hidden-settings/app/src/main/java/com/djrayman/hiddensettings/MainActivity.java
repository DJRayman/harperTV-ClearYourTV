package com.djrayman.hiddensettings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** One scrolling list of the hidden screens from Catalog that exist here; OK opens one. */
public class MainActivity extends Activity {
    private static final int BACKGROUND = 0xFF10151B;
    private static final int ROW = 0xFF1F2A36;
    private static final int ROW_FOCUSED = 0xFFFFFFFF;
    private float density;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        density = getResources().getDisplayMetrics().density;
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(64), dp(36), dp(64), dp(48));

        list.addView(text(getString(R.string.title), 30, true, 0xFFFFFFFF));
        TextView subtitle = text(getString(R.string.subtitle), 15, false, 0xFFA9B4BF);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        list.addView(subtitle);

        View first = null;
        for (Catalog.Section section : Catalog.SECTIONS) {
            boolean header = false;
            for (Catalog.Entry entry : section.entries) {
                if (!available(entry)) continue;
                if (!header) {
                    TextView title = text(section.title, 18, true, 0xFF7FB7FF);
                    title.setPadding(0, dp(20), 0, dp(8));
                    list.addView(title);
                    header = true;
                }
                View row = row(entry);
                list.addView(row);
                if (first == null) first = row;
            }
        }
        if (first == null) list.addView(text(getString(R.string.nothing), 18, false, 0xFFFFFFFF));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.addView(list);
        setContentView(scroll);
        if (first != null) first.requestFocus();
    }

    /** True when the entry's activity or receiver is installed, enabled and open to other apps. */
    private boolean available(Catalog.Entry entry) {
        PackageManager pm = getPackageManager();
        try {
            if (!pm.getApplicationInfo(entry.pkg, 0).enabled) return false;
            if (entry.activity != null) {
                ActivityInfo info = pm.getActivityInfo(new ComponentName(entry.pkg, entry.activity), 0);
                return info.exported && info.isEnabled();
            }
            return !pm.queryBroadcastReceivers(new Intent(entry.action).setPackage(entry.pkg), 0).isEmpty();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void open(Catalog.Entry entry) {
        try {
            if (entry.activity != null) {
                startActivity(new Intent().setComponent(new ComponentName(entry.pkg, entry.activity))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                sendBroadcast(new Intent(entry.action).setPackage(entry.pkg));
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, getString(R.string.failed, entry.title), Toast.LENGTH_LONG).show();
        }
    }

    private View row(Catalog.Entry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setPadding(dp(20), dp(10), dp(20), dp(10));
        row.setBackground(rowBackground());
        TextView title = text(entry.title, 18, false, 0xFFFFFFFF);
        row.addView(title);
        TextView hint = null;
        if (!entry.hint.isEmpty()) {
            hint = text(entry.hint, 13, false, 0xFFA9B4BF);
            row.addView(hint);
        }
        final TextView hintView = hint;
        row.setOnFocusChangeListener((v, focused) -> {
            title.setTextColor(focused ? 0xFF10151B : 0xFFFFFFFF);
            if (hintView != null) hintView.setTextColor(focused ? 0xFF3A4652 : 0xFFA9B4BF);
        });
        row.setOnClickListener(v -> open(entry));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);
        return row;
    }

    private StateListDrawable rowBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, rounded(ROW_FOCUSED));
        states.addState(new int[]{}, rounded(ROW));
        return states;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
