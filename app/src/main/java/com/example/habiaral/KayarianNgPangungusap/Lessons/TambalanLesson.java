package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Quiz.TambalanQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class TambalanLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    private boolean isLessonDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_tambalan_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonTambalan);
        videoView = findViewById(R.id.videoViewTambalan);

        // Disable unlock button until lesson is completed
        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Check if lesson is already completed
        checkLessonStatus();

        // =========================
        // VIDEO SETUP
        // =========================
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_lesson);
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
            Intent intent = new Intent(TambalanLesson.this, TambalanQuiz.class);
            startActivity(intent);
        });

        // Start video immediately
        videoView.start();
    }

    private void checkLessonStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_2");

                        if (module2 != null && module2.containsKey("lessons")) {
                            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                            if (lessons != null && lessons.containsKey("tambalan")) {
                                Map<String, Object> tambalan = (Map<String, Object>) lessons.get("tambalan");
                                if (tambalan != null && "completed".equals(tambalan.get("status"))) {
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

        Map<String, Object> tambalanStatus = new HashMap<>();
        tambalanStatus.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("tambalan", tambalanStatus);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Kayarian ng Pangungusap");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "tambalan");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_2", progressMap), SetOptions.merge());
    }
}
