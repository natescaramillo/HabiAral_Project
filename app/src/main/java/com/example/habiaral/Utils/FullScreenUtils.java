package com.example.habiaral.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class FullScreenUtils {

    public static void toggleFullScreen(
            Activity activity,
            boolean[] isFullScreen,
            ImageView fullScreenOption,
            ImageView imageView,
            ImageView imageView2,
            Button unlockButton
    ) {
        if (!isFullScreen[0]) {
            if (activity.getActionBar() != null) activity.getActionBar().hide();

            unlockButton.setVisibility(View.GONE);
            imageView2.setVisibility(View.GONE);

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
            params.height = (int) (310 * activity.getResources().getDisplayMetrics().density);
            imageView.setLayoutParams(params);

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            fullScreenOption.setImageResource(com.example.habiaral.R.drawable.not_full_screen);

        } else {
            if (activity.getActionBar() != null) activity.getActionBar().show();

            unlockButton.setVisibility(View.VISIBLE);
            imageView2.setVisibility(View.VISIBLE);

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
            params.height = (int) (200 * activity.getResources().getDisplayMetrics().density);
            imageView.setLayoutParams(params);

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            fullScreenOption.setImageResource(com.example.habiaral.R.drawable.full_screen);
        }

        isFullScreen[0] = !isFullScreen[0];
    }
}
