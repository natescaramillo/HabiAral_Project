package com.example.habiaral.Utils;

import android.app.Activity;
import android.os.Handler;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.habiaral.R;

public class LessonGifUtils {

    private static boolean isActive = false;
    private static Handler handler = new Handler();
    private static Runnable runnable;

    public static void startLessonGifRandomizer(Activity activity, ImageView imageView) {
        if (activity == null || imageView == null) return;

        isActive = true;

        if (!activity.isFinishing() && !activity.isDestroyed()) {
            Glide.with(activity).asGif().load(R.drawable.idle).into(imageView);
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isActive || activity.isFinishing() || activity.isDestroyed() || imageView == null) return;

                int delay = 2000;
                if (Math.random() < 0.4) {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        Glide.with(activity).asGif().load(R.drawable.right_2).into(imageView);
                    }

                    handler.postDelayed(() -> {
                        if (isActive && !activity.isFinishing() && !activity.isDestroyed() && imageView != null) {
                            Glide.with(activity).asGif().load(R.drawable.idle).into(imageView);
                        }
                        handler.postDelayed(runnable, delay);
                    }, 2000);

                } else {
                    handler.postDelayed(runnable, delay);
                }
            }
        };

        handler.postDelayed(runnable, 2000);
    }

    public static void stopIdleGifRandomizer(Activity activity, ImageView imageView) {
        isActive = false;
        handler.removeCallbacksAndMessages(null);

        if (activity != null && imageView != null && !activity.isFinishing() && !activity.isDestroyed()) {
            Glide.with(activity).asGif().load(R.drawable.idle).into(imageView);
        }
    }
}
