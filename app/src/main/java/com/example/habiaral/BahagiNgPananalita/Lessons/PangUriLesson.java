package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangUriQuiz;
import com.example.habiaral.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangUriLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;
    FirebaseFirestore db;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panguri_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanguri);
        videoView = findViewById(R.id.videoViewPanguri);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        if (user != null) {
            db.collection("lesson_progress")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isLessonDone = documentSnapshot.getBoolean("panguri_done");
                            if (isLessonDone != null && isLessonDone) {
                                unlockButton.setEnabled(true);
                                unlockButton.setAlpha(1f);
                            }
                        }
                    });
        }

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.panguri_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(mp -> {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
            saveProgressToFirestore(); // Save to Firebase
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangUriLesson.this, PangUriQuiz.class);
            startActivity(intent);
        });
    }

    private void saveProgressToFirestore() {
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("panguri_done", true);

        db.collection("lesson_progress")
                .document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "✅ PangUri progress saved"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Failed to save PangUri progress", e));
    }
}
