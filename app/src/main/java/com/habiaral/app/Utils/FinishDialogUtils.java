package com.habiaral.app.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.habiaral.app.R;

public class FinishDialogUtils {

    public static void showFinishDialog(Context context, int score, String message, Runnable onFinish) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_box_time_up, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView titleText = view.findViewById(R.id.textView11);
        TextView scoreText = view.findViewById(R.id.scoreText);

        scoreText.setText(String.valueOf(score));
        titleText.setText(message);

        Button balik = view.findViewById(R.id.btn_balik);
        balik.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(context, R.raw.button_click);
            dialog.dismiss();
            if (onFinish != null) onFinish.run();
        });
    }
}