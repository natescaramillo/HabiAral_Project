package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.KayarianNgPangungusap.Quiz.LangkapanQuiz;
import com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

public class LangkapanLesson extends AppCompatActivity {

    FirebaseFirestore db;
    String uid;
    TextToSpeech textToSpeech;

    TextView titleTextView;
    TextView descriptionTextView;
    TextView exampleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_langkapan_lesson);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        titleTextView = findViewById(R.id.title_Text_langkapan);
        descriptionTextView = findViewById(R.id.description_text_langkapan);
        exampleTextView = findViewById(R.id.example_Text_langkapan);

        descriptionTextView.setVisibility(View.GONE);
        exampleTextView.setVisibility(View.GONE);

        markLessonInProgress();

        Button quizButton = findViewById(R.id.UnlockButtonLangkapan);
        quizButton.setOnClickListener(v -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
            startActivity(new Intent(LangkapanLesson.this, LangkapanQuiz.class));
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(LangkapanLesson.this, KayarianNgPangungusap.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        initTextToSpeech();
    }

    private void markLessonInProgress() {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> module2 = (Map<String, Object>) documentSnapshot.get("module_2");
                        if (module2 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> langkapan = (Map<String, Object>) lessons.get("langkapan");
                                if (langkapan != null) {
                                    String status = (String) langkapan.get("status");
                                    if ("completed".equals(status)) return;
                                }
                            }
                        }
                    }

                    Map<String, Object> update = new HashMap<>();
                    Map<String, Object> langkapanMap = new HashMap<>();
                    langkapanMap.put("status", "in_progress");

                    Map<String, Object> lessonsMap = new HashMap<>();
                    lessonsMap.put("langkapan", langkapanMap);

                    Map<String, Object> module2Map = new HashMap<>();
                    module2Map.put("lessons", lessonsMap);
                    module2Map.put("current_lesson", "langkapan");

                    update.put("module_2", module2Map);

                    db.collection("module_progress").document(uid)
                            .set(update, SetOptions.merge());
                });
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this,
                                "Hindi ma-open ang installer ng TTS.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Voice selected = null;
                    for (Voice v : textToSpeech.getVoices()) {
                        Locale vLocale = v.getLocale();
                        if (vLocale != null && vLocale.getLanguage().equals("fil")) {
                            selected = v;
                            break;
                        } else if (v.getName().toLowerCase().contains("fil")) {
                            selected = v;
                            break;
                        }
                    }
                    if (selected != null) textToSpeech.setVoice(selected);

                    textToSpeech.setSpeechRate(1.0f);
                    loadCharacterLines();
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

                        speakIntroLines(introLines, () -> {
                            List<LineItem> remainingLines = new ArrayList<>();
                            if (descriptionLines != null)
                                for (String s : descriptionLines) remainingLines.add(new LineItem(s, descriptionTextView));
                            if (exampleLines != null)
                                for (String s : exampleLines) remainingLines.add(new LineItem(s, exampleTextView));

                            speakLinesSequentially(remainingLines);
                        });
                    }
                });
    }

    private void speakIntroLines(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) {
            onComplete.run();
            return;
        }

        Iterator<String> iterator = lines.iterator();
        speakNextIntroLine(iterator, onComplete);
    }

    private void speakNextIntroLine(Iterator<String> iterator, Runnable onComplete) {
        if (!iterator.hasNext()) {
            onComplete.run();
            return;
        }

        String line = iterator.next();
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { runOnUiThread(() -> speakNextIntroLine(iterator, onComplete)); }
            @Override public void onError(String utteranceId) {}
        });

        textToSpeech.speak(line, TextToSpeech.QUEUE_FLUSH, null, "INTRO_" + line);
    }

    private void speakLinesSequentially(List<LineItem> lines) {
        Iterator<LineItem> iterator = lines.iterator();
        speakNext(iterator);
    }

    private void speakNext(Iterator<LineItem> iterator) {
        if (!iterator.hasNext()) return;

        LineItem item = iterator.next();

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> animatePopUp(item.targetView, item.text));
            }
            @Override
            public void onDone(String utteranceId) { runOnUiThread(() -> speakNext(iterator)); }
            @Override public void onError(String utteranceId) {}
        });

        textToSpeech.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, item.text);
    }

    private void animatePopUp(TextView textView, String text) {
        String existingText = textView.getText().toString();
        if (!existingText.isEmpty()) existingText += "\n\n";
        textView.setText(existingText + text);

        textView.setAlpha(0f);
        textView.setScaleX(0.8f);
        textView.setScaleY(0.8f);
        textView.setVisibility(View.VISIBLE);

        textView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private static class LineItem {
        String text;
        TextView targetView;
        LineItem(String text, TextView targetView) {
            this.text = text;
            this.targetView = targetView;
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
