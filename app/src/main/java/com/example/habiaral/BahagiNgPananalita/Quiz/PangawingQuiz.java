package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangawingQuiz extends AppCompatActivity {

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangawing_quiz);

        nextButton = findViewById(R.id.pangawingNextButton);

        nextButton.setOnClickListener(view -> {
            completeLesson();
            updateLessonStatusInFirestore();
            showResultDialog();
        });
    }

    private void completeLesson() {
        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("PangawingDone", true);
        editor.apply();

        Toast.makeText(this, "Congratulations! You have completed all lessons!", Toast.LENGTH_LONG).show();
    }

    private void updateLessonStatusInFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> pangawingStatus = new HashMap<>();
        pangawingStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangawing", pangawingStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangawing");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge())
                .addOnSuccessListener(unused -> checkAndUnlockAchievement());
    }

    private void checkAndUnlockAchievement() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String saCode = "SA11";
        String achievementId = "A11";
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
            if (module1 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
            if (lessons == null) return;

            String[] lessonKeys = {
                    "pangngalan", "pandiwa", "panguri", "panghalip", "pangabay",
                    "pangatnig", "pangukol", "pangakop", "padamdam", "pangawing"
            };

            for (String key : lessonKeys) {
                Map<String, Object> lesson = (Map<String, Object>) lessons.get(key);
                if (lesson == null || !"completed".equals(lesson.get("status"))) {
                    return;
                }
            }

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(saSnapshot -> {
                boolean alreadyUnlocked = false;

                if (saSnapshot.exists()) {
                    Map<String, Object> achievements = (Map<String, Object>) saSnapshot.get("achievements");
                    if (achievements != null && achievements.containsKey(saCode)) {
                        alreadyUnlocked = true;
                    }
                }

                // ✅ Even if already unlocked in Firestore, still mark locally
                if (alreadyUnlocked) {
                    if (!isAchievementDialogAlreadyShown(saCode)) {
                        markAchievementDialogAsShown(saCode);
                    }
                    return;
                }

                // ✅ Skip if already shown locally
                if (isAchievementDialogAlreadyShown(saCode)) {
                    return;
                }

                // ✅ Proceed to unlock
                continueUnlockingAchievement(db, uid, saCode, achievementId);
            });
        });
    }

    private void continueUnlockingAchievement(FirebaseFirestore db, String uid, String saCode, String achievementId) {
        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;
            String studentId = studentDoc.getString("studentId");

            db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists() || !achDoc.contains("title")) return;
                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementId", achievementId);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", Timestamp.now());

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements." + saCode, achievementData);

                db.collection("student_achievements").document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            // ✅ Mark as shown before displaying the dialog
                            markAchievementDialogAsShown(saCode);
                            showAchievementUnlockedDialog(title);
                        }));
            });
        });
    }

    private void showAchievementUnlockedDialog(String title) {
        new AlertDialog.Builder(this)
                .setTitle("Achievement Unlocked!")
                .setMessage("You unlocked: " + title)
                .setPositiveButton("OK", null)
                .show();
    }

    // ✅ Save & check local dialog shown flag
    private boolean isAchievementDialogAlreadyShown(String achievementCode) {
        SharedPreferences prefs = getSharedPreferences("AchievementDialogs", MODE_PRIVATE);
        return prefs.getBoolean(achievementCode, false);
    }

    private void markAchievementDialogAsShown(String achievementCode) {
        SharedPreferences prefs = getSharedPreferences("AchievementDialogs", MODE_PRIVATE);
        prefs.edit().putBoolean(achievementCode, true).apply();
    }

    private void showResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_box_option, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button retryButton = dialogView.findViewById(R.id.buttonRetry);
        Button homeButton = dialogView.findViewById(R.id.buttonHome);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        retryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PangawingQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
