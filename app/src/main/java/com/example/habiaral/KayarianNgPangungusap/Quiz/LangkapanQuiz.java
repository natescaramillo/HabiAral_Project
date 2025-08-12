package com.example.habiaral.KayarianNgPangungusap.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LangkapanQuiz extends AppCompatActivity{

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_langkapan_quiz);

        nextButton = findViewById(R.id.langkapanNextButton);

        nextButton.setOnClickListener(view -> {
            unlockNextLesson();
            saveQuizResultToFirestore();
            showResultDialog();
        });
    }

    // =========================
    // DIALOGS & NAVIGATION
    // =========================
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
            Intent intent = new Intent(com.example.habiaral.KayarianNgPangungusap.Quiz.LangkapanQuiz.this, KayarianNgPangungusap.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // =========================
    // FIRESTORE UPDATES
    // =========================
    private void unlockNextLesson() {
        Toast.makeText(this, "Next Lesson Unlocked: Panghalip!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> langkapanStatus = new HashMap<>();
        langkapanStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("langkapan", langkapanStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "langkapan");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_2", updateMap), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Congratulations! You have completed all lessons!", Toast.LENGTH_LONG).show();
            checkAndUnlockAchievement();
        });
    }
    private void checkAndUnlockAchievement() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String saCode = "SA12";
        String achievementId = "A12";
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_2");
            if (module1 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
            if (lessons == null) return;

            String[] lessonKeys = {
                    "payak", "tambalan", "hugnayan", "langkapan"
            };

            for (String key : lessonKeys) {
                Map<String, Object> lesson = (Map<String, Object>) lessons.get(key);
                if (lesson == null || !"completed".equals(lesson.get("status"))) {
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

                continueUnlockingAchievement(db, uid, saCode, achievementId);
            });
        });
    }

    private void continueUnlockingAchievement(FirebaseFirestore db, String uid, String saCode, String achievementID) {
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
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            showAchievementUnlockedDialog(title, R.drawable.achievement12);
                        }));
            });
        });
    }
        private void showAchievementUnlockedDialog(String title, int imageRes) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View toastView = inflater.inflate(R.layout.achievement_unlocked, null);  // palitan ng pangalan ng XML file mo

            ImageView iv = toastView.findViewById(R.id.imageView19);
            TextView tv = toastView.findViewById(R.id.textView14);

            iv.setImageResource(imageRes);
            String line1 = "Nakamit mo na ang parangal:\n";
            String line2 = title;

            SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);

            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int start = line1.length();
            int end = line1.length() + line2.length();
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.setSpan(new RelativeSizeSpan(1.3f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tv.setText(ssb);
            Toast toast = new Toast(this);
            toast.setView(toastView);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
            toast.show();
        }
    }