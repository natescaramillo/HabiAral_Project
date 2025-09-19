package com.example.habiaral.Utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.SoundPool;

import androidx.core.content.ContextCompat;

import com.example.habiaral.R;

public class AppPreloaderUtils {
    public static SoundPool soundPool;
    public static int greenSoundId, orangeSoundId, redSoundId;
    public static Drawable redDrawable, orangeDrawable, greenDrawable;

    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;

        soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        greenSoundId = soundPool.load(context, R.raw.green_timer, 1);
        orangeSoundId = soundPool.load(context, R.raw.orange_timer, 1);
        redSoundId = soundPool.load(context, R.raw.red_timer, 1);

        redDrawable = ContextCompat.getDrawable(context, R.drawable.timer_color_red);
        orangeDrawable = ContextCompat.getDrawable(context, R.drawable.timer_color_orange);
        greenDrawable = ContextCompat.getDrawable(context, R.drawable.timer_color_green);

        initialized = true;
    }

    public static void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        initialized = false;
    }
}