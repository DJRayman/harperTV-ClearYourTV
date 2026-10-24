package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

/**
 * Launcher settings' power modes: how many apps Android keeps cached in the background. Low-RAM firmware (SaluteTV
 * sets activity_manager_constants to max_cached_processes=3) kills a third app as soon as it leaves the screen;
 * a higher limit keeps apps where they were, at the cost of RAM and zram swap. Writing the global setting needs
 * WRITE_SECURE_SETTINGS (pm grant com.gathin.tvhome android.permission.WRITE_SECURE_SETTINGS). Android applies it
 * at once; Home applies the chosen mode again at every start, in case the firmware resets it on boot.
 */
final class PowerMode {
    static final String KEY = "power_mode";
    /** The firmware's own value, saved before the first change so Economy puts exactly that back. */
    private static final String ORIGINAL_KEY = "power_mode_original";
    private static final String SETTING = "activity_manager_constants";
    private static final String TAG = "PowerMode";

    static final int ECONOMY = 0, BALANCED = 1, MULTITASKING = 2;
    /** max_cached_processes for BALANCED and MULTITASKING; ECONOMY restores the firmware's value. */
    private static final int[] CACHED = {0, 8, 16};

    private PowerMode() { }

    /** The chosen mode, or -1 when none was ever chosen (the firmware decides). */
    static int chosen(SharedPreferences choices) {
        return choices.getInt(KEY, -1);
    }

    /** The activity_manager_constants value for {@code mode}, given the firmware's own value. */
    static String value(int mode, String original) {
        if (mode == ECONOMY || mode < 0 || mode >= CACHED.length) return original;
        return "max_cached_processes=" + CACHED[mode];
    }

    /** Saves and applies {@code mode}; false when Android refused the write (the permission is not granted). */
    static boolean set(Context context, SharedPreferences choices, int mode) {
        choices.edit().putInt(KEY, mode).apply();
        return apply(context, choices);
    }

    /** Applies the chosen mode, if any; true when nothing needed writing or the write went through. */
    static boolean apply(Context context, SharedPreferences choices) {
        int mode = chosen(choices);
        if (mode < 0) return true;
        try {
            String current = Settings.Global.getString(context.getContentResolver(), SETTING);
            if (!choices.contains(ORIGINAL_KEY)) {
                choices.edit().putString(ORIGINAL_KEY, current == null ? "" : current).apply();
            }
            String wanted = value(mode, choices.getString(ORIGINAL_KEY, ""));
            if (wanted.equals(current == null ? "" : current)) return true;
            return Settings.Global.putString(context.getContentResolver(), SETTING, wanted.isEmpty() ? null : wanted);
        } catch (SecurityException e) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted: " + e.getMessage());
            return false;
        }
    }
}
