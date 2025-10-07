package com.example.habiaral.Utils;

import android.os.Handler;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class TextAnimationUtils {

    private static final Map<TextView, Runnable> runnableMap = new HashMap<>();
    private static final Handler handler = new Handler();

    public static void animateText(TextView textView, String text, int delay) {
        cancelAnimation(textView);

        textView.setText("");
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    textView.append(String.valueOf(text.charAt(index[0])));
                    index[0]++;
                    handler.postDelayed(this, delay);
                }
            }
        };

        runnableMap.put(textView, runnable);
        handler.post(runnable);
    }

    public static void cancelAnimation(TextView textView) {
        Runnable oldRunnable = runnableMap.get(textView);
        if (oldRunnable != null) {
            handler.removeCallbacks(oldRunnable);
            runnableMap.remove(textView);
        }
    }
}
