package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangAbayQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PangAbayLesson extends AppCompatActivity {

    private final int[] pangAbayLesson = {
            R.drawable.pangabay01, R.drawable.pangabay02, R.drawable.pangabay03,
            R.drawable.pangabay04, R.drawable.pangabay05, R.drawable.pangabay06,
            R.drawable.pangabay07, R.drawable.pangabay08, R.drawable.pangabay09,
            R.drawable.pangabay10, R.drawable.pangabay11, R.drawable.pangabay12
    };
    private android.os.Handler textHandler = new android.os.Handler();
    private Map<Integer, List<String>> pageLines = new HashMap<>();
    private boolean waitForResumeChoice = false;
    private String currentUtterancePage = "";
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private TextToSpeech textToSpeech;
    private AlertDialog dialogOption;
    private TextView instructionText;
    private Runnable textRunnable;
    private Button unlockButton;
    private ImageView imageView;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangabay_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangabay);
        imageView = findViewById(R.id.imageViewPangabay);
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
            startActivity(new Intent(PangAbayLesson.this, PangAbayQuiz.class));
        });
    }

    private void nextPage() {
        if (currentPage < pangAbayLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pangAbayLesson[currentPage]);
            saveProgressToFirestore(false);

            stopSpeakingAndAnimation();

            if (pageLines.containsKey(currentPage)) {
                instructionText.setText("");
                new android.os.Handler().postDelayed(() -> {
                    speakLines(pageLines.get(currentPage));
                }, 500);
            } else {
                instructionText.setText("");
            }
        }

        if (currentPage == pangAbayLesson.length - 1) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            imageView.setImageResource(pangAbayLesson[currentPage]);
            saveProgressToFirestore(false);

            stopSpeakingAndAnimation();

            if (pageLines.containsKey(currentPage)) {
                instructionText.setText("");
                new android.os.Handler().postDelayed(() -> {
                    speakLines(pageLines.get(currentPage));
                }, 500);
            } else {
                instructionText.setText("");
            }
        }
    }

    private void stopSpeakingAndAnimation() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        if (textRunnable != null) {
            textHandler.removeCallbacks(textRunnable);
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
                            if (lessons != null && lessons.containsKey("pangabay")) {
                                Map<String, Object> pangabay = (Map<String, Object>) lessons.get("pangabay");
                                if (pangabay != null) {
                                    Long checkpoint = (Long) pangabay.get("checkpoint");
                                    currentPage = (checkpoint != null) ? checkpoint.intValue() : 0;
                                    isFirstTime = false;

                                    if ("completed".equals(pangabay.get("status"))) {
                                        isLessonDone = true;
                                        unlockButton.setEnabled(true);
                                        unlockButton.setAlpha(1f);
                                    }
                                    showResumeDialog(currentPage);
                                    waitForResumeChoice = true;
                                }
                            }
                        }
                    } else {
                        currentPage = 0;
                        imageView.setImageResource(pangAbayLesson[currentPage]);
                        isFirstTime = true;
                    }
                    saveProgressToFirestore(false);
                });
    }

    private void saveProgressToFirestore(boolean completed) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        String statusToSave = isLessonDone ? "completed" : "in-progress";

        Map<String, Object> pangabayStatus = new HashMap<>();
        pangabayStatus.put("status", statusToSave);
        pangabayStatus.put("checkpoint", currentPage);

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pangabay", pangabayStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status","in_progress");
        moduleMap.put("current_lesson", "pangabay");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (completed) isLessonDone = true;
                });
    }

    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lesson_character_lines").document("LCL5")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        List<Map<String, Object>> pages = (List<Map<String, Object>>) documentSnapshot.get("pages");
                        if (pages != null) {
                            for (Map<String, Object> page : pages) {
                                Long pageNum = (Long) page.get("page");
                                List<String> lines = (List<String>) page.get("line");
                                if (pageNum != null && lines != null) {
                                    pageLines.put(pageNum.intValue() - 1, lines);
                                }
                            }
                        }

                        List<String> introLines = (List<String>) documentSnapshot.get("intro");

                        if (!waitForResumeChoice) {
                            if (introLines != null && isFirstTime && !introLines.isEmpty()) {
                                isFirstTime = false;

                                speakSequentialLines(introLines, () -> {
                                    new android.os.Handler().postDelayed(() -> {
                                        imageView.setImageResource(pangAbayLesson[currentPage]);
                                        if (pageLines.containsKey(currentPage)) {
                                            speakLines(pageLines.get(currentPage));
                                        }
                                    }, 1500);
                                });

                            } else {
                                imageView.setImageResource(pangAbayLesson[currentPage]);
                                if (pageLines.containsKey(currentPage)) {
                                    new android.os.Handler().postDelayed(() -> {
                                        speakLines(pageLines.get(currentPage));
                                    }, 1500);
                                }
                            }
                        }
                    }
                });
    }

    private void speakSequentialLines(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;

        final int[] index = {0};
        currentUtterancePage = "page_" + currentPage;

        animateText(lines.get(0));

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (!utteranceId.startsWith(currentUtterancePage)) return;

                    index[0]++;
                    if (index[0] < lines.size()) {
                        animateText(lines.get(index[0]));
                        speak(lines.get(index[0]), currentUtterancePage + "_" + index[0]);
                    } else {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {}
        });

        speak(lines.get(0), currentUtterancePage + "_0");
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
        if (textRunnable != null) {
            textHandler.removeCallbacks(textRunnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        stopSpeakingAndAnimation();
        super.onPause();
    }

    private void showResumeDialog(int checkpoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_ppt_option, null);
        builder.setView(dialogView);

        dialogOption = builder.create();
        dialogOption.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button buttonResume = dialogView.findViewById(R.id.button_resume);
        Button buttonBumalik = dialogView.findViewById(R.id.button_bumalik);

        buttonResume.setOnClickListener(v -> {
            currentPage = checkpoint;
            imageView.setImageResource(pangAbayLesson[currentPage]);

            if (pageLines.containsKey(currentPage)) {
                new android.os.Handler().postDelayed(() -> {
                    speakLines(pageLines.get(currentPage));
                }, 1500);
            }
            waitForResumeChoice = false;
            dialogOption.dismiss();
        });

        buttonBumalik.setOnClickListener(v -> {
            currentPage = 0;
            imageView.setImageResource(pangAbayLesson[currentPage]);

            if (pageLines.containsKey(currentPage)) {
                new android.os.Handler().postDelayed(() -> {
                    speakLines(pageLines.get(currentPage));
                }, 1500);
            }
            waitForResumeChoice = false;
            dialogOption.dismiss();
        });

        dialogOption.show();
    }

    private void animateText(String text) {
        instructionText.setText("");
        final int[] index = {0};
        final int delay = 50;

        if (textRunnable != null) {
            textHandler.removeCallbacks(textRunnable);
        }

        textRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    instructionText.append(String.valueOf(text.charAt(index[0])));
                    index[0]++;
                    textHandler.postDelayed(this, delay);
                }
            }
        };
        textHandler.post(textRunnable);
    }
}
