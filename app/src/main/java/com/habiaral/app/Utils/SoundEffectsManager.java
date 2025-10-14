package com.habiaral.app.Utils;

// SoundEffectsManager.java
import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.habiaral.app.R;

import java.util.HashMap;

public class SoundEffectsManager {
    private static SoundPool soundPool;
    private static HashMap<String, Integer> sounds = new HashMap<>();
    private static boolean loaded = false;

    public static void init(Context ctx) {
        if (soundPool != null) return;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();

        // load raw resources (add all you need)
        sounds.put("CORRECT", soundPool.load(ctx, R.raw.correct, 1));
        sounds.put("WRONG", soundPool.load(ctx, R.raw.wrong, 1));
        sounds.put("HEART_POP", soundPool.load(ctx, R.raw.heart_pop, 1));
        sounds.put("BEEP", soundPool.load(ctx, R.raw.beep, 1));
        sounds.put("BUTTON_CLICK", soundPool.load(ctx, R.raw.button_click, 1));
        loaded = true;
    }

    public static void play(String key) {
        if (!loaded || soundPool == null) return;
        Integer id = sounds.get(key);
        if (id == null) return;
        soundPool.play(id, 1f, 1f, 1, 0, 1f);
    }

    public static void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            loaded = false;
        }
    }
}
