package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The remote's Info button shows what is playing, over any app: the video's frame size and dynamic range, the screen
 * mode, and the audio format, channels and output. The facts come from dumpsys SurfaceFlinger and
 * media.audio_flinger (MediaInfo), which needs the DUMP permission granted over adb:
 * pm grant com.gathin.tvhome android.permission.DUMP. The key still reaches the app in front, so a Live TV app's own
 * info banner keeps working. A second press, or eight seconds, hides the card.
 */
public class InfoKeyService extends AccessibilityService {
    private static final long SHOW_MS = 8000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable hide = this::hide;
    private TextView card;
    /** Counts the presses, so a slow dump never fills a card that was already closed or replaced. */
    private int shown;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_INFO) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (card != null) hide();
            else show();
        }
        return false;
    }

    private void show() {
        WindowManager wm = getSystemService(WindowManager.class);
        TextView view = new TextView(this);
        view.setBackgroundResource(R.drawable.glass_panel);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        view.setLineSpacing(0, 1.25f);
        int pad = dp(22);
        view.setPadding(pad, dp(16), pad, dp(16));
        view.setText(R.string.info_loading);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = dp(40);
        lp.y = dp(40);
        wm.addView(view, lp);
        card = view;
        int token = ++shown;
        main.postDelayed(hide, SHOW_MS);
        new Thread(() -> {
            String text = describe();
            main.post(() -> {
                if (card != null && token == shown) card.setText(text);
            });
        }, "InfoKeyService").start();
    }

    private void hide() {
        main.removeCallbacks(hide);
        if (card == null) return;
        getSystemService(WindowManager.class).removeView(card);
        card = null;
    }

    /** The card's text, built off the main thread: the dumps take a moment. */
    private String describe() {
        try {
            return describeOrThrow();
        } catch (RuntimeException e) {
            Log.w("InfoKeyService", "describe failed", e);
            return getString(R.string.info_none);
        }
    }

    private String describeOrThrow() {
        StringBuilder out = new StringBuilder();
        List<String> sf = dumpsys("SurfaceFlinger");
        List<String> af = dumpsys("media.audio_flinger");
        if (sf == null && af == null) return getString(R.string.info_no_permission);

        MediaInfo.Video video = sf == null ? null : MediaInfo.video(sf);
        if (video != null) {
            String app = label(video.owner);
            if (!app.isEmpty()) out.append(app).append('\n');
            if (video.width > 0 && video.height > 0) {
                out.append(getString(R.string.info_video, MediaInfo.resolutionName(video.width, video.height)));
                if (!video.range.isEmpty()) out.append(" · ").append(video.range);
            } else if (video.sideband) {
                out.append(getString(R.string.info_video, getString(R.string.info_tv_input)));
            }
        } else {
            out.append(getString(R.string.info_video, getString(R.string.info_none)));
        }
        out.append('\n').append(getString(R.string.info_screen, screenMode()));

        MediaInfo.Audio audio = af == null ? null : MediaInfo.audio(af);
        out.append('\n');
        if (audio == null) {
            out.append(getString(R.string.info_audio, getString(R.string.info_audio_silent)));
        } else {
            StringBuilder a = new StringBuilder(audio.format);
            String channels = MediaInfo.channelName(audio.channels);
            if (!channels.isEmpty()) a.append(", ").append(channels);
            if (audio.sampleRate > 0) {
                a.append(", ").append(String.format(Locale.ROOT, "%.1f кГц", audio.sampleRate / 1000f)
                        .replace(".0 ", " "));
            }
            out.append(getString(R.string.info_audio, a));
            if (!audio.devices.isEmpty()) out.append('\n').append(getString(R.string.info_output, audio.devices));
        }
        return out.toString();
    }

    /** "3840×2160, 60 Гц" for the panel's current mode. */
    private String screenMode() {
        Display display = getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) return "";
        Display.Mode mode = display.getMode();
        return mode.getPhysicalWidth() + "×" + mode.getPhysicalHeight() + ", "
                + Math.round(mode.getRefreshRate()) + " Гц";
    }

    private String label(String pkg) {
        if (pkg.isEmpty()) return "";
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** The lines of dumpsys for one service, or null when it could not run or the DUMP permission is missing. */
    private static List<String> dumpsys(String service) {
        try {
            Process p = new ProcessBuilder("dumpsys", service).redirectErrorStream(true).start();
            List<String> lines = new ArrayList<>();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(),
                    StandardCharsets.UTF_8))) {
                for (String line; (line = in.readLine()) != null; ) {
                    if (lines.isEmpty() && line.startsWith("Permission Denial")) return null;
                    lines.add(line);
                }
            }
            p.waitFor();
            return lines;
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        hide();
        super.onDestroy();
    }
}
