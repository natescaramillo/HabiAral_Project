package com.example.habiaral.Utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habiaral.R;

public class AchievementDialogUtils {

    public static void showAchievementUnlockedDialog(Context context, String title, int imageRes) {
        final Context appCtx = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        main.post(() -> {
            LayoutInflater inflater = LayoutInflater.from(appCtx);

            final View overlayView = inflater.inflate(R.layout.achievement_unlocked, null);
            bindView(overlayView, title, imageRes);

            try {
                WindowManager wm = (WindowManager) appCtx.getSystemService(Context.WINDOW_SERVICE);
                if (wm == null) throw new RuntimeException("WindowManager is null");

                int type;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (Settings.canDrawOverlays(appCtx)) {
                        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    } else {
                        throw new SecurityException("No overlay permission (Settings.canDrawOverlays==false)");
                    }
                } else {
                    type = WindowManager.LayoutParams.TYPE_TOAST;
                }

                final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT
                );

                params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                params.windowAnimations = android.R.style.Animation_Toast;


                overlayView.post(() -> {
                    int height = overlayView.getHeight();
                    overlayView.setTranslationY(-height);

                    overlayView.animate()
                            .translationY(0)
                            .setDuration(600)
                            .withEndAction(() -> {
                                MediaPlayer mp = safeCreateAndStartPlayer(appCtx);
                                main.postDelayed(() -> {
                                    overlayView.animate()
                                            .translationY(-height)
                                            .setDuration(600)
                                            .withEndAction(() -> {
                                                try {
                                                    wm.removeView(overlayView);
                                                } catch (IllegalArgumentException ignored) {
                                                }
                                            })
                                            .start();
                                }, 3000);
                            })
                            .start();
                });

                return;

            } catch (Exception e) {
            }

            try {
                final View toastView = inflater.inflate(R.layout.achievement_unlocked, null);
                bindView(toastView, title, imageRes);

                Toast toast = new Toast(appCtx);
                toast.setView(toastView);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 50);
                toast.show();

                safeCreateAndStartPlayer(appCtx);

                main.postDelayed(toast::cancel, 3000);

            } catch (Exception ex) {
            }
        });
    }

    private static void bindView(View view, String title, int imageRes) {
        ImageView iv = view.findViewById(R.id.imageView19);
        TextView tv = view.findViewById(R.id.textView14);

        if (iv != null) iv.setImageResource(imageRes);

        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title == null ? "" : title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        if (start <= end) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ssb.setSpan(new RelativeSizeSpan(1.1f), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (tv != null) tv.setText(ssb);
    }

    private static MediaPlayer globalPlayer;

    private static MediaPlayer safeCreateAndStartPlayer(Context ctx) {
        try {
            Context appContext = ctx.getApplicationContext();

            if (globalPlayer != null) {
                try { globalPlayer.release(); } catch (Exception ignored) {}
                globalPlayer = null;
            }

            globalPlayer = MediaPlayer.create(appContext, R.raw.achievement_pop);
            if (globalPlayer != null) {
                globalPlayer.setVolume(1.0f, 1.0f);
                globalPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    globalPlayer = null;
                });
                globalPlayer.start();
            }
            return globalPlayer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
