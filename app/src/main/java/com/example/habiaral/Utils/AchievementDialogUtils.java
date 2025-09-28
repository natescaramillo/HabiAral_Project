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

    /**
     * Shows an achievement popup that will not crash when the user navigates away.
     * Visible for ~1 second (1000 ms). Uses overlay when overlay-permission is granted;
     * otherwise falls back to a Toast (safe).
     */
    public static void showAchievementUnlockedDialog(Context context, String title, int imageRes) {
        final Context appCtx = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        // Ensure UI work runs on main thread
        main.post(() -> {
            LayoutInflater inflater = LayoutInflater.from(appCtx);

            // Create the view we'll try to show via WindowManager first
            final View overlayView = inflater.inflate(R.layout.achievement_unlocked, null);
            bindView(overlayView, title, imageRes);

            // Try WindowManager overlay path (only if we have overlay permission when required)
            try {
                WindowManager wm = (WindowManager) appCtx.getSystemService(Context.WINDOW_SERVICE);
                if (wm == null) throw new RuntimeException("WindowManager is null");

                int type;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // On modern Android, TYPE_APPLICATION_OVERLAY requires permission
                    if (Settings.canDrawOverlays(appCtx)) {
                        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    } else {
                        // No overlay permission -> force fallback to Toast
                        throw new SecurityException("No overlay permission (Settings.canDrawOverlays==false)");
                    }
                } else {
                    // For older devices, TYPE_TOAST works more reliably without special permission
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
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.y = 50;

                // Add the view (may throw BadTokenException / SecurityException on some devices)
                wm.addView(overlayView, params);

                // Start the slide in / out animation and lifecycle (1 second visible)
                overlayView.post(() -> {
                    int height = overlayView.getHeight();
                    overlayView.setTranslationY(-height);

                    overlayView.animate()
                            .translationY(0)
                            .setDuration(600)
                            .withEndAction(() -> {
                                MediaPlayer mp = safeCreateAndStartPlayer(appCtx);
                                // 1 second visible
                                main.postDelayed(() -> {
                                    overlayView.animate()
                                            .translationY(-height)
                                            .setDuration(600)
                                            .withEndAction(() -> {
                                                try {
                                                    wm.removeView(overlayView);
                                                } catch (IllegalArgumentException ignored) {
                                                    // already removed or never attached
                                                }
                                            })
                                            .start();
                                }, 3000);
                            })
                            .start();
                });

                // success path return
                return;

            } catch (Exception e) {
                // Fall through to Toast fallback if any problem occurs (permission, BadToken, etc).
                // (We intentionally don't throw so it won't crash the app.)
            }

            // --- FALLBACK: use a Toast with a freshly inflated view (safer) ---
            try {
                // Inflate fresh view for the Toast to avoid "view already has parent"
                final View toastView = inflater.inflate(R.layout.achievement_unlocked, null);
                bindView(toastView, title, imageRes);

                Toast toast = new Toast(appCtx);
                toast.setView(toastView);
                // We'll cancel it manually after 1 second
                toast.setDuration(Toast.LENGTH_LONG); // duration is overridden/cancelled manually
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 50);
                toast.show();

                // play sound
                safeCreateAndStartPlayer(appCtx);

                // cancel after 1 second
                main.postDelayed(toast::cancel, 3000);

            } catch (Exception ex) {
                // last-resort: don't crash — silently ignore
            }
        });
    }

    // Helper to bind image + spannable text into the inflated layout
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

    // Safe MediaPlayer creation and start (returns null if cannot create)
    // Keep a static reference para hindi made-destroy agad pag lumipat ng activity
    private static MediaPlayer globalPlayer;

    private static MediaPlayer safeCreateAndStartPlayer(Context ctx) {
        try {
            // Gumamit ng ApplicationContext para hindi sumama sa Activity lifecycle
            Context appContext = ctx.getApplicationContext();

            // Kung may dati pang tumutunog, i-release muna
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
