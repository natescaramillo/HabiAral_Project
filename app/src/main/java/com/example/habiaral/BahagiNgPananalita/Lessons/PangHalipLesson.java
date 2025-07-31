package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangHalipQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangHalipLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;
    boolean isLessonDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panghalip_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanghalip);
        videoView = findViewById(R.id.videoViewPanghalip);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        // Load progress from Firebase
        loadProgressFromFirestore();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.panghalip_lesson);
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
            Intent intent = new Intent(PangHalipLesson.this, PangHalipQuiz.class);
            startActivity(intent);
        });
    }

    private void loadProgressFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("module_progress")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) documentSnapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("panghalip")) {
                                Map<String, Object> panghalip = (Map<String, Object>) lessons.get("panghalip");
                                if (panghalip != null && "done".equals(panghalip.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to load Panghalip progress", e));
    }


    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> lessonMap = new HashMap<>();
        Map<String, Object> pangHalipStatus = new HashMap<>();
        pangHalipStatus.put("status", "done");
        lessonMap.put("panghalip", pangHalipStatus);

        Map<String, Object> update = new HashMap<>();
        update.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", update), SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "✅ Panghalip progress saved"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Failed to save Panghalip progress", e));
    }
}
