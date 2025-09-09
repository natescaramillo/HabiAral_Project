package com.example.habiaral.Utils;

import android.os.Handler;
import android.widget.TextView;

public class TextAnimationUtils {
    public static void animateText(TextView textView, String text, int delay) {
        Handler handler = new Handler();
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
        handler.post(runnable);
    }
}