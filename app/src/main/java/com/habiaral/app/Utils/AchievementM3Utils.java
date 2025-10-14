package com.habiaral.app.Utils;

import android.content.Context;

import com.habiaral.app.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AchievementM3Utils {

    public static void checkAndUnlockAchievement(Context context, FirebaseFirestore db, String uid) {
        String saCode = "SA13";
        String achievementId = "A13";

        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
            if (module3 == null) return;

            Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
            if (categories == null) return;

            String[] requiredCategories = {"Alamat", "Epiko", "Maikling Kuwento", "Pabula", "Parabula"};
            for (String category : requiredCategories) {
                Map<String, Object> cat = (Map<String, Object>) categories.get(category);
                if (cat == null || !"completed".equals(cat.get("status"))) {
                    return;
                }
            }

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(saSnapshot -> {
                if (saSnapshot.exists()) {
                    Map<String, Object> achievements = (Map<String, Object>) saSnapshot.get("achievements");
                    if (achievements != null && achievements.containsKey(saCode)) {
                        return;
                    }
                }

                continueUnlockingAchievement(context, db, uid, saCode, achievementId);
            });
        });
    }

    private static void continueUnlockingAchievement(Context context, FirebaseFirestore db, String uid, String saCode, String achievementID) {
        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;
            String studentId = studentDoc.getString("studentId");

            db.collection("achievements").document(achievementID).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists() || !achDoc.contains("title")) return;
                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementID);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", Timestamp.now());

                Map<String, Object> achievementsMap = new HashMap<>();
                achievementsMap.put(saCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements", achievementsMap);

                db.collection("student_achievements")
                        .document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            AchievementDialogUtils.showAchievementUnlockedDialog(context, title, R.drawable.achievement12);
                        });
            });
        });
    }
}

