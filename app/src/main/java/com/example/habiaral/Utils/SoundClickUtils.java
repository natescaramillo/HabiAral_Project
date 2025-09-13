package com.example.habiaral.Utils;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundClickUtils {
    public static void playClickSound(Context context, int soundResId) {
        if (!SoundManager.isMuted(context)) {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, soundResId);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        }
    }
}