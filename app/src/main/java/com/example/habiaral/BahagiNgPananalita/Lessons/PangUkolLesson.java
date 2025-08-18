package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangUkolQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangUkolLesson extends AppCompatActivity {

    Button unlockButton;
    ImageView imageView;
    private boolean isLessonDone = false; // Track from Firebase

    private int currentPage = 0;
    private int[] pangUkolLesson = {
            R.drawable.pangukol01,
            R.drawable.pangukol02,
            R.drawable.pangukol03,
            R.drawable.pangukol04,
            R.drawable.pangukol05
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangukol_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPangukol);
        imageView = findViewById(R.id.imageViewPangukol);

        // Disable unlock button until lesson is completed
        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Load first page
        imageView.setImageResource(pangUkolLesson[currentPage]);

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
            Intent intent = new Intent(PangUkolLesson.this, PangUkolQuiz.class);
            startActivity(intent);
        });
    }

    private void nextPage() {
        if (currentPage < pangUkolLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangUkolLesson[currentPage]);

            if (currentPage == pangUkolLesson.length - 1) {
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
            imageView.setImageResource(pangUkolLesson[currentPage]);
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
                            if (lessons != null && lessons.containsKey("pangukol")) {
                                Map<String, Object> pangukol = (Map<String, Object>) lessons.get("pangukol");
                                if (pangukol != null && "completed".equals(pangukol.get("status"))) {
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

        Map<String, Object> pangUkolStatus = new HashMap<>();
        pangUkolStatus.put("status", "in-progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pangukol", pangUkolStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pangukol");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }
}
