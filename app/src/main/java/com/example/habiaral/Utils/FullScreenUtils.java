package com.example.habiaral.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.R;

public class FullScreenUtils {

    public static void toggleFullScreen(
            Activity activity,
            boolean[] isFullScreen,
            ImageView fullScreenOption,
            ImageView imageView,
            ImageView imageView2,
            Button unlockButton,
            ConstraintLayout bottomBar,
            LinearLayout optionBar
    ) {
        ConstraintLayout.LayoutParams imageParams = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        ConstraintLayout.LayoutParams bottomBarParams = (ConstraintLayout.LayoutParams) bottomBar.getLayoutParams();
        ConstraintLayout.LayoutParams optionBarParams = (ConstraintLayout.LayoutParams) optionBar.getLayoutParams();

        if (!isFullScreen[0]) {
            if (activity.getActionBar() != null) activity.getActionBar().hide();
            unlockButton.setVisibility(View.GONE);
            imageView2.setVisibility(View.GONE);

            imageParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            imageParams.height = 0;
            imageParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            imageParams.bottomToTop = optionBar.getId();
            imageParams.setMargins(0, 0, 0, 0);
            imageView.setLayoutParams(imageParams);

            bottomBarParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            bottomBarParams.setMargins(0, 0, 0, 0);
            bottomBar.setLayoutParams(bottomBarParams);

            optionBar.setVisibility(View.VISIBLE);
            optionBarParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            optionBarParams.height = (int) (60 * activity.getResources().getDisplayMetrics().density);
            optionBarParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            optionBar.setLayoutParams(optionBarParams);

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            fullScreenOption.setImageResource(R.drawable.not_full_screen);

        } else {
            if (activity.getActionBar() != null) activity.getActionBar().show();
            unlockButton.setVisibility(View.VISIBLE);
            imageView2.setVisibility(View.VISIBLE);

            imageParams.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
            imageParams.height = (int) (200 * activity.getResources().getDisplayMetrics().density);
            imageParams.setMargins(20, 20, 20, 0);
            imageView.setLayoutParams(imageParams);

            bottomBarParams.height = 0;
            bottomBar.setLayoutParams(bottomBarParams);

            optionBar.setVisibility(View.VISIBLE);
            optionBarParams.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
            optionBarParams.height = (int) (45 * activity.getResources().getDisplayMetrics().density);
            optionBarParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            optionBar.setLayoutParams(optionBarParams);

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            fullScreenOption.setImageResource(R.drawable.full_screen);
        }

        isFullScreen[0] = !isFullScreen[0];
    }
}