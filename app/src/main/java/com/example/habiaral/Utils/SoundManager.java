package com.example.habiaral.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

public class SoundManager {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_MUTED = "isMuted";

    private static boolean isMuted = false;
    private static TextToSpeech tts;

    public static void init(Context context, TextToSpeech textToSpeech) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean(KEY_MUTED, false);

        tts = textToSpeech;

        if (isMuted && tts != null) {
            tts.stop();
        }
    }

    public static void toggleMute(Context context) {
        boolean currentMuted = isMuted(context); // kunin latest value
        boolean newMuted = !currentMuted;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_MUTED, newMuted).apply();

        isMuted = newMuted;

        if (newMuted && tts != null) {
            tts.stop();
        }
    }

    public static boolean isMuted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_MUTED, false);
    }

    public static void setMuted(Context context, boolean mute) {
        isMuted = mute;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_MUTED, isMuted).apply();

        if (isMuted && tts != null) {
            tts.stop();
        }
    }

    public static void setTts(TextToSpeech textToSpeech) {
        tts = textToSpeech;
        if (isMuted && tts != null) {
            tts.stop();
        }
    }
}
