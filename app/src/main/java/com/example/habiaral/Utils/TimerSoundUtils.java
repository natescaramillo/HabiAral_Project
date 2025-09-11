package com.example.habiaral.Utils;

import android.content.Context;
import android.media.MediaPlayer;

public class TimerSoundUtils {

    private static MediaPlayer currentPlayer;

    public static void playTimerSound(Context context, int soundResId, MediaPlayer.OnCompletionListener completionListener) {
        stop();

        currentPlayer = MediaPlayer.create(context, soundResId);
        if (currentPlayer != null) {
            currentPlayer.setVolume(0.2f, 0.2f);
            currentPlayer.setOnCompletionListener(mp -> {
                stop();
                if (completionListener != null) {
                    completionListener.onCompletion(mp);
                }
            });
            currentPlayer.start();
        }
    }

    public static void stop() {
        if (currentPlayer != null) {
            if (currentPlayer.isPlaying()) {
                currentPlayer.stop();
            }
            currentPlayer.release();
            currentPlayer = null;
        }
    }
}