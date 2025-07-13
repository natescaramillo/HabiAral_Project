package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangHalipLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panghalip_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanghalip);
        videoView = findViewById(R.id.videoViewPanghalip);

        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        boolean isLessonDone = sharedPreferences.getBoolean("PangHalipDone", false);

        if (isLessonDone) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        } else {
            unlockButton.setEnabled(false);
            unlockButton.setAlpha(0.5f);
        }

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

                // ✅ Save to SharedPreferences
                SharedPreferences.Editor editor = getSharedPreferences("LessonProgress", MODE_PRIVATE).edit();
                editor.putBoolean("PangHalipDone", true);
                editor.apply();

                // ✅ Save to Firestore
                saveProgressToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangHalipLesson.this, PangHalipQuiz.class);
            startActivity(intent);
        });
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Create lessons map for panghalip
        Map<String, Object> lessonMap = new HashMap<>();
        Map<String, Object> pangHalipStatus = new HashMap<>();
        pangHalipStatus.put("status", "in_progress");
        lessonMap.put("panghalip", pangHalipStatus);

        Map<String, Object> progress = new HashMap<>();
        progress.put("modulename", "Bahagi ng Pananalita");
        progress.put("status", "in_progress");
        progress.put("current_lesson", "panghalip");
        progress.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", progress), SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "✅ Progress saved for Panghalip lesson"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Failed to save progress", e));
    }
}
