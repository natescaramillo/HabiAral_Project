package com.example.habiaral.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.media.MediaPlayer;
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

import com.example.habiaral.R;

public class AchievementDialogUtils {

    public static void showAchievementUnlockedDialog(Context context, String title, int imageRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = dialogView.findViewById(R.id.imageView19);
        TextView tv = dialogView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.1f), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50;
            dialog.getWindow().setAttributes(params);
        }

        dialog.setOnShowListener(d -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.achievement_pop);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        });

        dialog.show();
    }
}

