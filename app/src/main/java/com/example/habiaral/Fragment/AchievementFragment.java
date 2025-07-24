package com.example.habiaral.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.habiaral.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AchievementFragment extends Fragment {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final int[] boxIds = {
            R.id.achievement1, R.id.achievement2, R.id.achievement3,
            R.id.achievement4, R.id.achievement5, R.id.achievement6,
            R.id.achievement7, R.id.achievement8, R.id.achievement9,
            R.id.achievement10, R.id.achievement11, R.id.achievement12,
            R.id.achievement13
    };

    private final int[] textViewIds = {
            R.id.achievement1text, R.id.achievement2text, R.id.achievement3text,
            R.id.achievement4text, R.id.achievement5text, R.id.achievement6text,
            R.id.achievement7text, R.id.achievement8text, R.id.achievement9text,
            R.id.achievement10text, R.id.achievement11text, R.id.achievement12text,
            R.id.achievement13text
    };

    private final int[] imgIDs = {
            R.drawable.a1, R.drawable.a2, R.drawable.a3,
            R.drawable.a4, R.drawable.a5, R.drawable.a6,
            R.drawable.a7, R.drawable.a8, R.drawable.a9,
            R.drawable.a10, R.drawable.a11, R.drawable.a12,
            R.drawable.a13
    };

    public AchievementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_achievement, container, false);

        db.collection("achievements")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot result = task.getResult();
                        List<DocumentSnapshot> documents = new ArrayList<>(result.getDocuments());

                        // Sort by achievementID: A1, A2...A10, A11...
                        documents.sort(Comparator.comparing(doc -> {
                            String id = doc.getString("achievementID");
                            if (id != null && id.startsWith("A")) {
                                try {
                                    return Integer.parseInt(id.substring(1));
                                } catch (NumberFormatException e) {
                                    return 999; // push unknowns to the end
                                }
                            }
                            return 999;
                        }));

                        int count = Math.min(documents.size(), boxIds.length);

                        for (int i = 0; i < count; i++) {
                            DocumentSnapshot doc = documents.get(i);
                            String title = doc.getString("title");
                            String description = doc.getString("description");

                            View box = root.findViewById(boxIds[i]);
                            TextView textView = root.findViewById(textViewIds[i]);

                            if (textView != null) {
                                textView.setText(title != null ? title : "No Title");
                            }

                            int imgResId = imgIDs[i];
                            String finalTitle = title != null ? title : "No Title";
                            String finalDescription = description != null ? description : "No Description";

                            if (box != null) {
                                box.setOnClickListener(v ->
                                        showAchievementDialog(finalTitle, finalDescription, imgResId));
                            }
                        }
                    } else {
                        Log.e("Firestore", "❌ Error fetching achievements", task.getException());
                    }
                });

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
            params.y = 300;
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
