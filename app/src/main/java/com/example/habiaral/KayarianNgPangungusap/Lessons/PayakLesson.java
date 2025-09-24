package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

public class PayakLesson extends AppCompatActivity {

    FirebaseFirestore db;
    String uid;
    TextToSpeech textToSpeech;

    ImageView descriptionImageView;
    ImageView exampleImageView;
    Button quizButton;
    private boolean introCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_payak_lesson);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        descriptionImageView = findViewById(R.id.description_image_payak);
        exampleImageView = findViewById(R.id.example_image_payak);
        quizButton = findViewById(R.id.UnlockButtonPayak);

        descriptionImageView.setVisibility(View.GONE);
        exampleImageView.setVisibility(View.GONE);

        markLessonInProgress();

        quizButton.setEnabled(false);
        quizButton.setAlpha(0.5f);
        quizButton.setOnClickListener(v -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
            }

            startActivity(new Intent(PayakLesson.this, PayakQuiz.class));
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(PayakLesson.this, KayarianNgPangungusap.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        initTextToSpeech();
    }

    private void markLessonInProgress() {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean alreadyCompleted = false;

                    if (documentSnapshot.exists()) {
                        Map<String, Object> module2 = (Map<String, Object>) documentSnapshot.get("module_2");
                        if (module2 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> payak = (Map<String, Object>) lessons.get("payak");
                                if (payak != null) {
                                    String status = (String) payak.get("status");
                                    if ("completed".equals(status)) {
                                        alreadyCompleted = true;
                                    }
                                }
                            }
                        }
                    }

                    if (!alreadyCompleted) {
                        Map<String, Object> update = new HashMap<>();
                        Map<String, Object> payakMap = new HashMap<>();
                        payakMap.put("status", "in_progress");

                        Map<String, Object> lessonsMap = new HashMap<>();
                        lessonsMap.put("payak", payakMap);

                        Map<String, Object> module2Map = new HashMap<>();
                        module2Map.put("lessons", lessonsMap);
                        module2Map.put("current_lesson", "payak");

                        update.put("module_2", module2Map);

                        db.collection("module_progress").document(uid)
                                .set(update, SetOptions.merge());
                    }

                });
    }

    private void initTextToSpeech() {
        SharedPreferences prefs = getSharedPreferences("lesson_prefs", MODE_PRIVATE);
        introCompleted = prefs.getBoolean("payak_quiz_unlocked", false);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Hindi ma-open ang installer ng TTS.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    for (Voice v : textToSpeech.getVoices()) {
                        if (v.getLocale() != null && v.getLocale().getLanguage().equals("fil")) {
                            textToSpeech.setVoice(v);
                            break;
                        }
                    }
                    textToSpeech.setSpeechRate(1.0f);

                    if (!introCompleted) {
                        loadCharacterLines();
                    }
                }
            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCharacterLines() {
        db.collection("lesson_character_lines").document("LCL11")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> introLines = (List<String>) document.get("intro");
                        List<String> descriptionLines = (List<String>) document.get("description_line");
                        List<String> exampleLines = (List<String>) document.get("example_line");

                        speakLinesSequentially(introLines, () -> {
                            runOnUiThread(() -> fadeInImage(descriptionImageView));

                            speakLinesSequentially(descriptionLines, () -> {
                                runOnUiThread(() -> fadeInImage(exampleImageView));

                                speakLinesSequentially(exampleLines, () -> {
                                    runOnUiThread(() -> {
                                        quizButton.setEnabled(true);
                                        quizButton.setAlpha(1.0f);

                                        introCompleted = true;
                                        SharedPreferences prefs = getSharedPreferences("lesson_prefs", MODE_PRIVATE);
                                        prefs.edit().putBoolean("payak_quiz_unlocked", true).apply();
                                    });
                                });
                            });
                        });

                    }
                });
    }

    private void fadeInImage(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(800)
                .setListener(null);
    }

    private void speakLinesSequentially(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Iterator<String> iterator = lines.iterator();
        speakNext(iterator, onComplete);
    }

    private void speakNext(Iterator<String> iterator, Runnable onComplete) {
        if (!iterator.hasNext()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        String line = iterator.next();
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { runOnUiThread(() -> speakNext(iterator, onComplete)); }
            @Override public void onError(String utteranceId) {}
        });

        textToSpeech.speak(line, TextToSpeech.QUEUE_ADD, null, line);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        SharedPreferences prefs = getSharedPreferences("lesson_prefs", MODE_PRIVATE);
        introCompleted = prefs.getBoolean("payak_quiz_unlocked", false);

        if (introCompleted) {
            descriptionImageView.setVisibility(View.VISIBLE);
            exampleImageView.setVisibility(View.VISIBLE);
            quizButton.setEnabled(true);
            quizButton.setAlpha(1.0f);
        } else {
            descriptionImageView.setVisibility(View.GONE);
            exampleImageView.setVisibility(View.GONE);
            quizButton.setEnabled(false);
            quizButton.setAlpha(0.5f);

            loadCharacterLines();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
}
