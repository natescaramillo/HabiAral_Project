package com.habiaral.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;

public class SoundManagerUtils {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_MUTED = "isMuted";

    private static boolean isMuted = false;
    private static TextToSpeech tts;

    private static int originalVolume = -1;

    public static void init(Context context, TextToSpeech textToSpeech) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean(KEY_MUTED, false);

        tts = textToSpeech;

        if (isMuted && tts != null) {
            tts.stop();
        }

        if (originalVolume == -1) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        }
    }

    public static void toggleMute(Context context) {
        boolean currentMuted = isMuted(context);
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

    public static int getOriginalVolume() {
        return originalVolume;
    }
}
