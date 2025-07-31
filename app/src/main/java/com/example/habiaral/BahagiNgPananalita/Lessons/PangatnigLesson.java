package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangatnigQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangatnigLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;
    boolean isLessonDone = false;
    FirebaseUser user;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangatnig_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangatnig);
        videoView = findViewById(R.id.videoViewPangatnig);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        loadLessonStatus();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangatnig_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveLessonDoneToFirestore();
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangatnigLesson.this, PangatnigQuiz.class);
            startActivity(intent);
        });
    }

    private void loadLessonStatus() {
        if (user == null) return;

        db.collection("module_progress")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) documentSnapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> pangatnig = (Map<String, Object>) lessons.get("pangatnig");
                                if (pangatnig != null) {
                                    String status = (String) pangatnig.get("status");
                                    if ("done".equals(status)) {
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

    private void saveLessonDoneToFirestore() {
        if (user == null) return;

        String uid = user.getUid();

        Map<String, Object> pangatnigStatus = new HashMap<>();
        pangatnigStatus.put("status", "done");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangatnig", pangatnigStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangatnig");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }
}
