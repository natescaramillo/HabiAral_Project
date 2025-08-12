package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Quiz.HugnayanQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class HugnayanLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    private boolean isLessonDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_hugnayan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonHugnayan);
        videoView = findViewById(R.id.videoViewHugnayan);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Check lesson progress from Firestore
        checkLessonStatusFromFirestore();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.hugnayan_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);

                saveProgressToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(HugnayanLesson.this, HugnayanQuiz.class);
            startActivity(intent);
        });
    }

    private void checkLessonStatusFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_2");
                        if (module2 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> hugnayan = (Map<String, Object>) lessons.get("hugnayan");
                                if (hugnayan != null) {
                                    String status = (String) hugnayan.get("status");
                                    if ("in_progress".equals(status) || "completed".equals(status)) {
                                        isLessonDone = true;
                                        unlockButton.setEnabled(true);
                                        unlockButton.setAlpha(1f);
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to load Hugnayan lesson status", e));
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> hugnayanStatus = new HashMap<>();
        hugnayanStatus.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("hugnayan", hugnayanStatus);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Kayarian ng Pangungusap");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "hugnayan");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_2", progressMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "✅ PangUri lesson progress saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to save PangUri lesson progress", e));
    }
}
