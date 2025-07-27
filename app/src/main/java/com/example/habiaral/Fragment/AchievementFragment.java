package com.example.habiaral.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.*;

public class AchievementFragment extends Fragment{

    private FirebaseFirestore db;
    private String uid;
    private View root;

    int[] boxIds = {
            R.id.achievement1, R.id.achievement2, R.id.achievement3, R.id.achievement4, R.id.achievement5,
            R.id.achievement6, R.id.achievement7, R.id.achievement8, R.id.achievement9, R.id.achievement10,
            R.id.achievement11, R.id.achievement12, R.id.achievement13
    };

    int[] imageViewIds = {
            R.id.achievement_image1, R.id.achievement_image2, R.id.achievement_image3, R.id.achievement_image4,
            R.id.achievement_image5, R.id.achievement_image6, R.id.achievement_image7, R.id.achievement_image8,
            R.id.achievement_image9, R.id.achievement_image10, R.id.achievement_image11, R.id.achievement_image12,
            R.id.achievement_image13
    };

    int[] textViewIds = {
            R.id.achievement1text, R.id.achievement2text, R.id.achievement3text, R.id.achievement4text, R.id.achievement5text,
            R.id.achievement6text, R.id.achievement7text, R.id.achievement8text, R.id.achievement9text, R.id.achievement10text,
            R.id.achievement11text, R.id.achievement12text, R.id.achievement13text
    };

    int[] imgIDs = {
            R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4, R.drawable.a5,
            R.drawable.a6, R.drawable.a7, R.drawable.a8, R.drawable.a9, R.drawable.a10,
            R.drawable.a11, R.drawable.a12, R.drawable.a13
    };

    List<DocumentSnapshot> achievementDocs = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_achievement, container, false);
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadAchievements();

        return root;
    }

    private void loadAchievements() {
        db.collection("achievements").get().addOnSuccessListener(task -> {
            achievementDocs = new ArrayList<>(task.getDocuments());

            achievementDocs.sort(Comparator.comparing(doc -> {
                String id = doc.getString("achievementID");
                try {
                    return Integer.parseInt(id != null ? id.replaceAll("\\D+", "") : "999");
                } catch (Exception e) {
                    return 999;
                }
            }));

            for (int i = 0; i < Math.min(achievementDocs.size(), textViewIds.length); i++) {
                DocumentSnapshot doc = achievementDocs.get(i);
                String title = doc.getString("title");
                TextView textView = root.findViewById(textViewIds[i]);
                if (textView != null) {
                    textView.setText(title != null ? title : "No Title");
                }
            }

            loadUnlockedAchievements(uid);
        }).addOnFailureListener(e -> Log.e("AchievementFragment", "Error loading achievements", e));
    }

    private void loadUnlockedAchievements(String uid) {
        db.collection("student_achievements").document(uid).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> achievementsMap = new HashMap<>();

            if (snapshot.exists()) {
                Map<String, Object> data = snapshot.getData();
                if (data != null && data.containsKey("achievements")) {
                    achievementsMap = (Map<String, Object>) data.get("achievements");
                }
            }

            for (int i = 0; i < boxIds.length && i < achievementDocs.size(); i++) {
                View box = root.findViewById(boxIds[i]);
                ImageView image = root.findViewById(imageViewIds[i]);

                if (box == null || image == null) continue;

                String saKey = "SA" + (i + 1);
                DocumentSnapshot doc = achievementDocs.get(i);
                String title = doc.getString("title");
                String description = doc.getString("description");
                int finalImgId = imgIDs[i];

                if (achievementsMap.containsKey(saKey)) {
                    image.setAlpha(1f);
                    removeLockIconIfExists(box);

                    // Extract unlockedAt date
                    Map<String, Object> saData = (Map<String, Object>) achievementsMap.get(saKey);
                    String unlockedAtStr = "";

                    if (saData != null && saData.get("unlockedAt") instanceof Timestamp) {
                        Timestamp ts = (Timestamp) saData.get("unlockedAt");
                        unlockedAtStr = new SimpleDateFormat("MMMM d, yyyy", new Locale("fil", "PH")).format(ts.toDate());
                    }

                    String finalUnlockedAtStr = unlockedAtStr;
                    box.setOnClickListener(v -> showAchievementDialog(title, description, finalImgId, finalUnlockedAtStr));
                } else {
                    image.setAlpha(0.3f);
                    addLockIcon(image);
                    box.setOnClickListener(v -> Toast.makeText(getContext(), "This achievement is locked.", Toast.LENGTH_SHORT).show());
                }
            }
        }).addOnFailureListener(e -> Log.e("AchievementFragment", "Error loading unlocked achievements", e));
    }

    private void addLockIcon(ImageView achievementImage) {
        if (getContext() == null) return;

        ViewParent parent = achievementImage.getParent();
        if (!(parent instanceof FrameLayout)) return;

        FrameLayout frame = (FrameLayout) parent;

        for (int i = 0; i < frame.getChildCount(); i++) {
            View child = frame.getChildAt(i);
            if ("lock_icon".equals(child.getTag())) {
                frame.removeView(child);
                break;
            }
        }

        ImageView lockIcon = new ImageView(getContext());
        lockIcon.setImageResource(R.drawable.ic_lock);
        lockIcon.setTag("lock_icon");

        int sizeInDp = 32;
        float scale = getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (sizeInDp * scale + 0.5f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);
        params.gravity = Gravity.CENTER;
        lockIcon.setLayoutParams(params);

        frame.addView(lockIcon);
    }

    private void removeLockIconIfExists(View box) {
        if (!(box instanceof ViewGroup)) return;

        ViewGroup container = (ViewGroup) box;
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if ("lock_icon".equals(child.getTag())) {
                container.removeViewAt(i);
            }
        }
    }

    private void showAchievementDialog(String title, String description, int imgID, String unlockedAt) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
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

        TextView titleView = dialog.findViewById(R.id.achievement_name);
        TextView descriptionView = dialog.findViewById(R.id.description);
        ImageView imageView = dialog.findViewById(R.id.imagepop_up);
        ImageView close = dialog.findViewById(R.id.xmark);
        TextView dateView = dialog.findViewById(R.id.unlocked_date);

        if (titleView != null) titleView.setText(title);
        if (descriptionView != null) descriptionView.setText(description);
        if (imageView != null) imageView.setImageResource(imgID);
        if (dateView != null && unlockedAt != null) {
            dateView.setText("Nakamit noong " + unlockedAt);
        } else if (dateView != null) {
            dateView.setText("");
        }
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
