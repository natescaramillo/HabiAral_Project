package com.habiaral.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MuteButtonUtils {
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";

    public static void setSoundEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SOUND_ENABLED, enabled);
        editor.apply();
    }

    public static boolean isSoundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void toggleSound(Context context) {
        boolean current = isSoundEnabled(context);
        setSoundEnabled(context, !current);
    }
}
