package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
    ImageView imageView;
    TextView instructionText;
    private List<String> allLines;
    private TextToSpeech textToSpeech;
    private boolean isLessonDone = false;  // Track from Firebase

    private int currentPage = 0;

    private int[] pangngalanLesson = {
            R.drawable.pangngalan01,
            R.drawable.pangngalan02,
            R.drawable.pangngalan03,
            R.drawable.pangngalan04,
            R.drawable.pangngalan05,
            R.drawable.pangngalan06,
            R.drawable.pangngalan07,
            R.drawable.pangngalan08,
            R.drawable.pangngalan09,
            R.drawable.pangngalan10,
            R.drawable.pangngalan11,
            R.drawable.pangngalan12,
            R.drawable.pangngalan13,
            R.drawable.pangngalan14,
            R.drawable.pangngalan15,
            R.drawable.pangngalan16,
            R.drawable.pangngalan17,
            R.drawable.pangngalan18,
            R.drawable.pangngalan19,
            R.drawable.pangngalan20,
            R.drawable.pangngalan21,
            R.drawable.pangngalan22,
            R.drawable.pangngalan23,
            R.drawable.pangngalan24,
            R.drawable.pangngalan25,
            R.drawable.pangngalan26,
            R.drawable.pangngalan27,
            R.drawable.pangngalan28,
            R.drawable.pangngalan29,
            R.drawable.pangngalan30,
            R.drawable.pangngalan31
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        imageView = findViewById(R.id.imageViewPangngalan);
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

        // Load first page
        imageView.setImageResource(pangngalanLesson[currentPage]);

        // Detect left or right tap sa imageView
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float width = v.getWidth();

                if (x < width / 2) {
                    // Tap sa left side → previous page
                    previousPage();
                } else {
                    // Tap sa right side → next page
                    nextPage();
                }
            }
            return true;
        });

        // Check if lesson is already completed
        checkLessonStatus();

        // Unlock button → go to quiz
        unlockButton.setOnClickListener(view -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
            }

            Intent intent = new Intent(PangngalanLesson.this, PangngalanQuiz.class);
            startActivity(intent);
        });

        // Load lesson script
        loadCharacterLines();
    }

    private void nextPage() {
        if (currentPage < pangngalanLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangngalanLesson[currentPage]);

            if (currentPage == pangngalanLesson.length - 1) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
        }
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

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            imageView.setImageResource(pangngalanLesson[currentPage]);
        }
    }
}
