package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

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

    private Button unlockButton;
    private ImageView imageView;
    private TextView instructionText;
    private TextToSpeech textToSpeech;
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private int currentPage = 0;

    private Map<Integer, List<String>> pageLines = new HashMap<>();

    private final int[] pangngalanLesson = {
            R.drawable.pangngalan01, R.drawable.pangngalan02, R.drawable.pangngalan03,
            R.drawable.pangngalan04, R.drawable.pangngalan05, R.drawable.pangngalan06,
            R.drawable.pangngalan07, R.drawable.pangngalan08, R.drawable.pangngalan09,
            R.drawable.pangngalan10, R.drawable.pangngalan11, R.drawable.pangngalan12,
            R.drawable.pangngalan13, R.drawable.pangngalan14, R.drawable.pangngalan15,
            R.drawable.pangngalan16, R.drawable.pangngalan17, R.drawable.pangngalan18,
            R.drawable.pangngalan19, R.drawable.pangngalan20, R.drawable.pangngalan21,
            R.drawable.pangngalan22, R.drawable.pangngalan23, R.drawable.pangngalan24,
            R.drawable.pangngalan25, R.drawable.pangngalan26, R.drawable.pangngalan27,
            R.drawable.pangngalan28, R.drawable.pangngalan29, R.drawable.pangngalan30,
            R.drawable.pangngalan31
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        imageView = findViewById(R.id.imageViewPangngalan);
        instructionText = findViewById(R.id.instructionText);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.0f);
                loadCharacterLines();
            }
        });

        checkLessonStatus();

        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                if (x < v.getWidth() / 2) previousPage();
                else nextPage();
            }
            return true;
        });

        unlockButton.setOnClickListener(view -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            startActivity(new Intent(PangngalanLesson.this, PangngalanQuiz.class));
        });
    }

    private void nextPage() {
        if (currentPage < pangngalanLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangngalanLesson[currentPage]);
            saveProgressToFirestore(false);

            if (pageLines.containsKey(currentPage)) {
                speakLines(pageLines.get(currentPage));
            } else {
                instructionText.setText("");
            }
        }

        if (currentPage == pangngalanLesson.length - 1) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            imageView.setImageResource(pangngalanLesson[currentPage]);
            saveProgressToFirestore(false);

            if (pageLines.containsKey(currentPage)) {
                speakLines(pageLines.get(currentPage));
            } else {
                instructionText.setText("");
            }
        }
    }

    private void checkLessonStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("pangngalan")) {
                                Map<String, Object> pangngalan = (Map<String, Object>) lessons.get("pangngalan");
                                if (pangngalan != null) {
                                    Long checkpoint = (Long) pangngalan.get("checkpoint");
                                    currentPage = (checkpoint != null) ? checkpoint.intValue() : 0;
                                    isFirstTime = false;

                                    if ("completed".equals(pangngalan.get("status"))) {
                                        isLessonDone = true;
                                        unlockButton.setEnabled(true);
                                        unlockButton.setAlpha(1f);
                                    }

                                    showResumeDialog(currentPage);
                                }
                            }
                        }
                    } else {
                        currentPage = 0;
                        imageView.setImageResource(pangngalanLesson[currentPage]);
                        isFirstTime = true;
                    }
                    saveProgressToFirestore(false);
                });
    }

    private void saveProgressToFirestore(boolean completed) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // alisin na ang isLessonDone check

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Panatilihin ang status na "completed" kung dati nang completed
        String statusToSave = isLessonDone ? "completed" : "in-progress";

        Map<String, Object> pangngalanStatus = new HashMap<>();
        pangngalanStatus.put("status", statusToSave);
        pangngalanStatus.put("checkpoint", currentPage);

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pangngalan", pangngalanStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status","in_progress");
        moduleMap.put("current_lesson", "pangngalan");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (completed) isLessonDone = true;
                });
    }


    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lesson_character_lines").document("LCL1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        // Load pages (convert 1-based → 0-based)
                        List<Map<String, Object>> pages = (List<Map<String, Object>>) documentSnapshot.get("pages");
                        if (pages != null) {
                            for (Map<String, Object> page : pages) {
                                Long pageNum = (Long) page.get("page"); // 1-based
                                List<String> lines = (List<String>) page.get("line");
                                if (pageNum != null && lines != null) {
                                    pageLines.put(pageNum.intValue() - 1, lines); // 0-based
                                }
                            }
                        }

                        List<String> introLines = (List<String>) documentSnapshot.get("intro");

                        if (introLines != null && isFirstTime && !introLines.isEmpty()) {
                            isFirstTime = false;

                            // Speak intro lines first
                            speakSequentialLines(introLines, () -> {
                                // After intro finishes, show checkpoint page and speak its lines
                                imageView.setImageResource(pangngalanLesson[currentPage]);
                                if (pageLines.containsKey(currentPage)) {
                                    speakLines(pageLines.get(currentPage));
                                }
                            });

                        } else {
                            // Not first time → directly show checkpoint page and speak lines
                            imageView.setImageResource(pangngalanLesson[currentPage]);
                            if (pageLines.containsKey(currentPage)) {
                                speakLines(pageLines.get(currentPage));
                            }
                        }
                    }
                });
    }

    private void speakSequentialLines(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;

        final int[] index = {0};
        instructionText.setText(lines.get(0));

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    index[0]++;
                    if (index[0] < lines.size()) {
                        instructionText.setText(lines.get(index[0]));
                        speak(lines.get(index[0]), String.valueOf(index[0]));
                    } else {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) { }
        });

        speak(lines.get(0), "0");
    }

    private void speakLines(List<String> lines) {
        speakSequentialLines(lines, () -> instructionText.setText(""));
    }

    private void speak(String text, String utteranceId) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
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
        if (textToSpeech != null) textToSpeech.stop();
        super.onPause();
    }

    private void showResumeDialog(int checkpoint) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(false);

        // Inflate the custom layout
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_ppt_option, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        Button buttonResume = dialogView.findViewById(R.id.button_resume);
        Button buttonBumalik = dialogView.findViewById(R.id.button_bumalik);

        // "Ipagpatuloy" button
        buttonResume.setOnClickListener(v -> {
            currentPage = checkpoint; // go to saved checkpoint
            imageView.setImageResource(pangngalanLesson[currentPage]);

            if (pageLines.containsKey(currentPage)) {
                speakLines(pageLines.get(currentPage));
            }
            dialog.dismiss();
        });

        // "Bumalik" button
        buttonBumalik.setOnClickListener(v -> {
            currentPage = 0; // start from first page
            imageView.setImageResource(pangngalanLesson[currentPage]);

            if (pageLines.containsKey(currentPage)) {
                speakLines(pageLines.get(currentPage));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

}
