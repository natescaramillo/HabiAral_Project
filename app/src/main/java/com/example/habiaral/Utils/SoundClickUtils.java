package com.example.habiaral.Utils;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundClickUtils {
    private static long lastClickTime = 0;

    private static boolean canPlay() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 300) {
            return false;
        }
        lastClickTime = now;
        return true;
    }

    public static void playClickSound(Context context, int soundResId) {
        if (!MuteButtonUtils.isSoundEnabled(context)) return;

        MediaPlayer mp = MediaPlayer.create(context, soundResId);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }
}
