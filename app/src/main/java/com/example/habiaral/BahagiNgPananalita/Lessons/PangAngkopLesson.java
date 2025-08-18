package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangAngkopQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangAngkopLesson extends AppCompatActivity {

    ImageView imageView;
    Button unlockButton;
    private boolean isLessonDone = false;

    private int currentPage = 0;
    private int[] pangAngkopLesson = {
            R.drawable.pangangkop01,
            R.drawable.pangangkop02,
            R.drawable.pangangkop03,
            R.drawable.pangangkop04,
            R.drawable.pangangkop05
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangangkop_lesson); // ensure this layout exists

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPangakop);
        imageView = findViewById(R.id.imageViewPangakop);

        // Disable unlock button until lesson is completed
        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Load first page
        imageView.setImageResource(pangAngkopLesson[currentPage]);

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
            Intent intent = new Intent(PangAngkopLesson.this, PangAngkopQuiz.class);
            startActivity(intent);
        });
    }

    private void nextPage() {
        if (currentPage < pangAngkopLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangAngkopLesson[currentPage]);

            if (currentPage == pangAngkopLesson.length - 1) {
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
            imageView.setImageResource(pangAngkopLesson[currentPage]);
        }
    }

    private void checkLessonStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                        if (module1 != null && module1.containsKey("lessons")) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("pangakop")) {
                                Map<String, Object> pangakop = (Map<String, Object>) lessons.get("pangakop");
                                if (pangakop != null && "completed".equals(pangakop.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                });
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangAkopStatus = new HashMap<>();
        pangAkopStatus.put("status", "in_progress");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangakop", pangAkopStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pangakop");
        moduleMap.put("lessons", lessonsMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }
}
