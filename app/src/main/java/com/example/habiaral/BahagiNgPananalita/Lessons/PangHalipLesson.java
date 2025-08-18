package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangHalipQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangHalipLesson extends AppCompatActivity {

    Button unlockButton;
    ImageView imageView;
    private boolean isLessonDone = false; // Track from Firebase

    private int currentPage = 0;
    private int[] pangHalipLesson = {
            R.drawable.panghalip01,
            R.drawable.panghalip02,
            R.drawable.panghalip03,
            R.drawable.panghalip04,
            R.drawable.panghalip05,
            R.drawable.panghalip06,
            R.drawable.panghalip07
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_panghalip_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPanghalip);
        imageView = findViewById(R.id.imageViewPanghalip);

        // Disable unlock button until lesson is completed
        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Load first page
        imageView.setImageResource(pangHalipLesson[currentPage]);

        // Detect left or right tap sa imageView
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float width = v.getWidth();

                if (x < width / 2) {
                    // Tap sa left side → previous page
                    previousPage();
                } else {
                    // Tap sa right side → next page
                    nextPage();
                }
            }
            return true;
        });

        // Check if lesson is already completed
        checkLessonStatus();

        // Unlock button → go to quiz
        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangHalipLesson.this, PangHalipQuiz.class);
            startActivity(intent);
        });
    }

    private void nextPage() {
        if (currentPage < pangHalipLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangHalipLesson[currentPage]);

            if (currentPage == pangHalipLesson.length - 1) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            imageView.setImageResource(pangHalipLesson[currentPage]);
        }
    }

    // =========================
    // FIRESTORE - CHECK LESSON STATUS
    // =========================
    private void checkLessonStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");

                        if (module1 != null && module1.containsKey("lessons")) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("panghalip")) {
                                Map<String, Object> panghalip = (Map<String, Object>) lessons.get("panghalip");
                                if (panghalip != null && "completed".equals(panghalip.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                });
    }

    // =========================
    // FIRESTORE - SAVE PROGRESS
    // =========================
    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangHalipStatus = new HashMap<>();
        pangHalipStatus.put("status", "in-progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("panghalip", pangHalipStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "panghalip");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }
}
