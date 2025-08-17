package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangngalanQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PangngalanLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;
    TextView instructionText;
    private List<String> allLines;
    private List<String> linesAfterVideo;
    private TextToSpeech textToSpeech;
    private boolean isLessonDone = false;  // Track from Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        videoView = findViewById(R.id.videoViewPangngalan);
        instructionText = findViewById(R.id.instructionText);

        // =========================
        // TEXT TO SPEECH SETUP
        // =========================
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.1f);
            }
        });

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

            if (linesAfterVideo != null && !linesAfterVideo.isEmpty()) {
                displayLinesAfterVideo(linesAfterVideo);
            }
        });

        // Unlock button → go to quiz
        unlockButton.setOnClickListener(view -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;  // prevent further use
            }
            Intent intent = new Intent(PangngalanLesson.this, PangngalanQuiz.class);
            startActivity(intent);
        });

        // Load lesson script
        loadCharacterLines();
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
                            if (lessons != null && lessons.containsKey("pangngalan")) {
                                Map<String, Object> pangngalan = (Map<String, Object>) lessons.get("pangngalan");
                                if (pangngalan != null && "completed".equals(pangngalan.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                });
    }

    // =========================
    // FIRESTORE - SAVE PROGRESS
    // =========================
    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangngalanStatus = new HashMap<>();
        pangngalanStatus.put("status", "in-progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pangngalan", pangngalanStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pangngalan");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }

    // =========================
    // LOAD LESSON SCRIPT
    // =========================
    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lesson_character_lines").document("LCL1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> lines = (List<String>) documentSnapshot.get("line");
                        if (lines != null && lines.size() > 3) {
                            allLines = lines;
                            List<String> introLines = lines.subList(0, 3);
                            linesAfterVideo = lines.subList(3, lines.size());

                            displayIntroLines(introLines);
                        }
                    }
                });
    }

    // =========================
    // DISPLAY LINES BEFORE VIDEO
    // =========================
    private void displayIntroLines(List<String> introLines) {
        Handler handler = new Handler();
        final int[] index = {0};

        instructionText.setText("");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < introLines.size()) {
                    String line = introLines.get(index[0]);
                    instructionText.setText(line);
                    speak(line);
                    index[0]++;
                    handler.postDelayed(this, 4000);
                } else {
                    instructionText.setText("");
                    videoView.start();
                }
            }
        };

        handler.postDelayed(runnable, 3000);
    }

    // =========================
    // DISPLAY LINES AFTER VIDEO
    // =========================
    private void displayLinesAfterVideo(List<String> lines) {
        Handler handler = new Handler();
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < lines.size()) {
                    String line = lines.get(index[0]);
                    instructionText.setText(line);
                    speak(line);
                    index[0]++;
                    handler.postDelayed(this, 5000);
                } else {
                    instructionText.setText("");
                }
            }
        };

        handler.postDelayed(runnable, 1000);
    }

    // =========================
    // TEXT TO SPEECH
    // =========================
    private void speak(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        super.onPause();
    }
}
