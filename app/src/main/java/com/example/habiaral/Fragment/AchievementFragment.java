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
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.habiaral.R;

public class AchievementFragment extends Fragment {

    public AchievementFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_achievement, container, false);

        // IDs of the 12 clickable views (achievement boxes)
        int[] boxIds = {
                R.id.achievement1, R.id.achievement2, R.id.achievement3,
                R.id.achievement4, R.id.achievement5, R.id.achievement6,
                R.id.achievement7, R.id.achievement8, R.id.achievement9,
                R.id.achievement10, R.id.achievement11, R.id.achievement12
        };

        String[] achievement_names = {
                "Achievement 1", "Achievement 2", "Achievement 3",
                "Achievement 4", "Achievement 5", "Achievement 6",
                "Achievement 7", "Achievement 8", "Achievement 9",
                "Achievement 10", "Achievement 11", "Achievement 12"
        };

        String[] descriptions = {
                "Description for Achievement 1",
                "Description for Achievement 2",
                "Description for Achievement 3",
                "Description for Achievement 4",
                "Description for Achievement 5",
                "Description for Achievement 6",
                "Description for Achievement 7",
                "Description for Achievement 8",
                "Description for Achievement 9",
                "Description for Achievement 10",
                "Description for Achievement 11",
                "Description for Achievement 12"
        };
        int[] imgID = {
                R.drawable.a1, R.drawable.a2, R.drawable.a3,
                R.drawable.a4, R.drawable.a5, R.drawable.a6,
                R.drawable.a7, R.drawable.a8, R.drawable.a9,
                R.drawable.a10, R.drawable.a11, R.drawable.a12
        };


        // ✅ Correct loop using index
        for (int i = 0; i < boxIds.length; i++) {
            int index = i;
            View box = root.findViewById(boxIds[i]);
            if (box != null) {
                box.setOnClickListener(v -> showAchievementDialog(achievement_names[index], descriptions[index], imgID[index]));
            }
        }

        return root;
    }

    private void showAchievementDialog(String title, String description, int imgID) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.achievement_popup);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = 300; // vertical offset
            window.setAttributes(params);
        }

        ImageView close = dialog.findViewById(R.id.xmark);
        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }

        TextView titleView = dialog.findViewById(R.id.achievement_name);
        TextView descriptionView = dialog.findViewById(R.id.description);
        ImageView imageView = dialog.findViewById(R.id.imagepop_up);

        if (titleView != null) titleView.setText(title);
        if (descriptionView != null) descriptionView.setText(description);
        if (imageView != null) imageView.setImageResource(imgID);

        dialog.show();
    }
}
