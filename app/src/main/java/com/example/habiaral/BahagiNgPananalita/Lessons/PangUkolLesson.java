package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangUkolQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangUkolLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    private boolean isLessonDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangukol_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangukol);
        videoView = findViewById(R.id.videoViewPangukol);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        loadLessonStatusFromFirestore();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangukol_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                isLessonDone = true;
                saveProgressToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangUkolLesson.this, PangUkolQuiz.class);
            startActivity(intent);
        });
    }

    private void loadLessonStatusFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> moduleData = (Map<String, Object>) documentSnapshot.get("module_1");
                        if (moduleData != null) {
                            Map<String, Object> lessons = (Map<String, Object>) moduleData.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> pangukol = (Map<String, Object>) lessons.get("pangukol");
                                if (pangukol != null && "in_progress".equals(pangukol.get("status"))) {
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

        Map<String, Object> pangUkolStatus = new HashMap<>();
        pangUkolStatus.put("status", "in_progress");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangukol", pangUkolStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangukol");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }
}
