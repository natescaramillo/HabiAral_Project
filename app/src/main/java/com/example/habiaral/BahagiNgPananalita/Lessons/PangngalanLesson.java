package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalitaProgress;
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
    private TextToSpeech textToSpeech;  // 👈 TTS


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangngalan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        videoView = findViewById(R.id.videoViewPangngalan);
        instructionText = findViewById(R.id.instructionText);

        // 🔊 Initialize TTS
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(new Locale("tl", "PH")); // Tagalog
                textToSpeech.setSpeechRate(1.1f);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "❌ Language not supported or missing.");
                }
            } else {
                Log.e("TTS", "❌ Initialization failed.");
            }
        });

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

        videoView.setOnCompletionListener(mp -> {
            if (!isLessonDone) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);

                SharedPreferences.Editor editor = getSharedPreferences("LessonProgress", MODE_PRIVATE).edit();
                editor.putBoolean("PangngalanDone", true);
                editor.apply();

                saveProgressToFirestore();
            }
            if (linesAfterVideo != null && !linesAfterVideo.isEmpty()) {
                displayLinesAfterVideo(linesAfterVideo); // 👉 Show lines 3+
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangngalanLesson.this, PangngalanQuiz.class);
            startActivity(intent);
        });

        // Start displaying lines from Firestore
        loadCharacterLines();
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> lessonMap = new HashMap<>();
        Map<String, Object> pangngalanStatus = new HashMap<>();
        pangngalanStatus.put("status", "in_progress");
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

    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lesson_character_lines").document("LCL1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> lines = (List<String>) documentSnapshot.get("line");
                        if (lines != null && lines.size() > 3) {
                            allLines = lines;

                            // Separate intro and post-video lines
                            List<String> introLines = lines.subList(0, 3);
                            linesAfterVideo = lines.subList(3, lines.size());

                            displayIntroLines(introLines);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "❌ Failed to fetch lines", e));
    }

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

                    // 🔊 Speak line
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

    private void displayLinesAfterVideo(List<String> lines) {
        Handler handler = new Handler();
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < lines.size()) {
                    String line = lines.get(index[0]);
                    instructionText.setText(line);

                    // 🔊 Speak line
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

    // 🔊 Speak line
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

}
