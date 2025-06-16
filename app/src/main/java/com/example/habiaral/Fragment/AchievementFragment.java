package com.example.habiaral.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.example.habiaral.R;

public class AchievementFragment extends Fragment {

    public AchievementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_achievement, container, false);

        // IDs of the 9 clickable views (achievement boxes)
        int[] boxIds = {
                R.id.view5, R.id.view6, R.id.view7,
                R.id.view8, R.id.view9, R.id.view10,
                R.id.view11, R.id.view12, R.id.view13
        };

        // Assign dialog behavior to each
        for (int id : boxIds) {
            View box = root.findViewById(id);
            if (box != null) {
                box.setOnClickListener(v -> showAchievementDialog());
            }
        }

        return root;
    }

    private void showAchievementDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.achievement_popup); // make sure this layout exists
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = 300; // vertical offset from the top
            window.setAttributes(params);
        }

        ImageView close = dialog.findViewById(R.id.xmark);
        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
