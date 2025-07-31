package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangAbayQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangAbayLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;
    boolean isLessonDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangabay_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangabay);
        videoView = findViewById(R.id.videoViewPangabay);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        checkProgressFromFirestore();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangabay_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangAbayLesson.this, PangAbayQuiz.class);
            startActivity(intent);
        });
    }

    private void checkProgressFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) documentSnapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> pangabay = (Map<String, Object>) lessons.get("pangabay");
                                if (pangabay != null && "in_progress".equals(pangabay.get("status"))) {
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

        Map<String, Object> pangAbayStatus = new HashMap<>();
        pangAbayStatus.put("status", "in_progress");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangabay", pangAbayStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangabay");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }
}
