package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

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
    VideoView videoView;
    MediaController mediaController;
    private boolean isLessonDone = false; // Track from Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_panguri_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPanguri);
        videoView = findViewById(R.id.videoViewPanguri);

        // Disable unlock button until lesson is completed
        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Check if lesson is already completed
        checkLessonStatus();

        // =========================
        // VIDEO SETUP
        // =========================
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.panguri_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
        });

        // Unlock button → go to quiz
        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangUriLesson.this, PangUriQuiz.class);
            startActivity(intent);
        });

        // Start video immediately
        videoView.start();
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
                            if (lessons != null && lessons.containsKey("pang_uri")) {
                                Map<String, Object> pangUri = (Map<String, Object>) lessons.get("pang_uri");
                                if (pangUri != null && "completed".equals(pangUri.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to load progress", e));
    }

    // =========================
    // FIRESTORE - SAVE PROGRESS
    // =========================
    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangUriStatus = new HashMap<>();
        pangUriStatus.put("status", "completed");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pang_uri", pangUriStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pang_uri");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "✅ Progress saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to save progress", e));
    }
}
