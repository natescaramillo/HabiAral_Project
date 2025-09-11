package com.example.habiaral.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.example.habiaral.R;

public class ResumeDialogUtils {

    public interface ResumeDialogListener {
        void onResumeLesson();
        void onRestartLesson();
    }

    public static AlertDialog showResumeDialog(Context context, ResumeDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = View.inflate(context, R.layout.dialog_box_ppt_option, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button buttonResume = dialogView.findViewById(R.id.button_resume);
        Button buttonBumalik = dialogView.findViewById(R.id.button_bumalik);

        buttonResume.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(context, R.raw.button_click);
            listener.onResumeLesson();
            dialog.dismiss();
        });

        buttonBumalik.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(context, R.raw.button_click);
            listener.onRestartLesson();
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }
}