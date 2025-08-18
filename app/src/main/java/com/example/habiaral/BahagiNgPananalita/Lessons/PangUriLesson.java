package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangUriQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangUriLesson extends AppCompatActivity {

    Button unlockButton;
    ImageView imageView;
    private boolean isLessonDone = false;

    private int currentPage = 0;
    private int[] pangUriLesson = {
            R.drawable.panguri01,
            R.drawable.panguri02,
            R.drawable.panguri03,
            R.drawable.panguri04,
            R.drawable.panguri05,
            R.drawable.panguri06,
            R.drawable.panguri07,
            R.drawable.panguri08
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_panguri_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanguri);
        imageView = findViewById(R.id.imageViewPanguri);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Load first page
        imageView.setImageResource(pangUriLesson[currentPage]);

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

        // Check lesson progress from Firestore
        checkLessonStatusFromFirestore();

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangUriLesson.this, PangUriQuiz.class);
            startActivity(intent);
        });
    }

    private void nextPage() {
        if (currentPage < pangUriLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangUriLesson[currentPage]);

            if (currentPage == pangUriLesson.length - 1) {
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
            imageView.setImageResource(pangUriLesson[currentPage]);
        }
    }

    private void checkLessonStatusFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> panguri = (Map<String, Object>) lessons.get("panguri");
                                if (panguri != null) {
                                    String status = (String) panguri.get("status");
                                    if ("in_progress".equals(status) || "completed".equals(status)) {
                                        isLessonDone = true;
                                        unlockButton.setEnabled(true);
                                        unlockButton.setAlpha(1f);
                                    }
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

        Map<String, Object> panguriStatus = new HashMap<>();
        panguriStatus.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("panguri", panguriStatus);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Bahagi ng Pananalita");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "panguri");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", progressMap), SetOptions.merge());
    }
}
