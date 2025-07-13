package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalitaProgress;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangngalanQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangngalanLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangngalan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        videoView = findViewById(R.id.videoViewPangngalan);

        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        boolean isLessonDone = sharedPreferences.getBoolean("PangngalanDone", false);

        if (isLessonDone) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        } else {
            unlockButton.setEnabled(false);
            unlockButton.setAlpha(0.5f);
        }

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangngalan_lesson);
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
                editor.putBoolean("PangngalanDone", true);
                editor.apply();

                // ✅ Save to Firestore
                saveProgressToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangngalanLesson.this, PangngalanQuiz.class);
            startActivity(intent);
        });
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Create lessons map for pangngalan only for now
        Map<String, Object> lessonMap = new HashMap<>();
        Map<String, Object> pangngalanStatus = new HashMap<>();
        pangngalanStatus.put("status", "in_progress"); // change to "completed" after quiz
        lessonMap.put("pangngalan", pangngalanStatus);

        BahagiNgPananalitaProgress progress = new BahagiNgPananalitaProgress();
        progress.setModulename("Bahagi ng Pananalita");
        progress.setStatus("in_progress");
        progress.setCurrent_lesson("pangngalan");
        progress.setLessons(lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", progress), SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "✅ Progress saved for Pangngalan lesson"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Failed to save progress", e));
    }
}
