package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.Quiz.PandamdamQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PandamdamLesson extends AppCompatActivity {

    private final int[] pandamdamLesson = {
            R.drawable.pandamdam01, R.drawable.pandamdam02, R.drawable.pandamdam03,
            R.drawable.pandamdam04, R.drawable.pandamdam05, R.drawable.pandamdam06
    };
    private android.os.Handler textHandler = new android.os.Handler();
    private Map<Integer, List<String>> pageLines = new HashMap<>();
    private boolean waitForResumeChoice = false;
    private String currentUtterancePage = "";
    final boolean[] isFullScreen = {false};
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private TextToSpeech textToSpeech;
    private AlertDialog dialogOption;
    private TextView instructionText;
    private Runnable textRunnable;
    private Button unlockButton;
    private ImageView imageView;
    private int currentPage = 0;
    private ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pandamdam_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPandamdam);
        imageView = findViewById(R.id.imageViewPandamdam);
        instructionText = findViewById(R.id.instructionText);
        ImageView backOption = findViewById(R.id.back_option);
        ImageView nextOption = findViewById(R.id.next_option);
        ImageView fullScreenOption = findViewById(R.id.full_screen_option);
        ImageView imageView2 = findViewById(R.id.imageView2);
        constraintLayout.findViewById(R.id.instructionContainer);

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
            playClickSound();
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            startActivity(new Intent(PandamdamLesson.this, PandamdamQuiz.class));
        });

        backOption.setOnClickListener(v -> {
            playClickSound();
            previousPage();
        });

        nextOption.setOnClickListener(v -> {
            playClickSound();
            nextPage();
        });

        fullScreenOption.setOnClickListener(v -> {
            playClickSound();
            if (!isFullScreen[0]) {
                if (getSupportActionBar() != null) getSupportActionBar().hide();

                instructionText.setVisibility(View.GONE);
                imageView2.setVisibility(View.GONE);
                constraintLayout.setVisibility(View.GONE);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );

                fullScreenOption.setImageResource(R.drawable.not_full_screen);
                isFullScreen[0] = true;
            } else {
                if (getSupportActionBar() != null) getSupportActionBar().show();

                instructionText.setVisibility(View.VISIBLE);
                imageView2.setVisibility(View.VISIBLE);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                fullScreenOption.setImageResource(R.drawable.full_screen);
                isFullScreen[0] = false;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    textToSpeech.shutdown();
                }
                if (textRunnable != null) {
                    textHandler.removeCallbacks(textRunnable);
                }
                Intent intent = new Intent(PandamdamLesson.this, BahagiNgPananalita.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isFullScreen[0]) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void nextPage() {
        if (currentPage < pandamdamLesson.length - 1) {
            currentPage++;
            imageView.setImageResource(pandamdamLesson[currentPage]);
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

        if (currentPage == pandamdamLesson.length - 1) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            imageView.setImageResource(pandamdamLesson[currentPage]);
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
                            if (lessons != null && lessons.containsKey("pandamdam")) {
                                Map<String, Object> pandamdam = (Map<String, Object>) lessons.get("pandamdam");
                                if (pandamdam != null) {
                                    Long checkpoint = (Long) pandamdam.get("checkpoint");
                                    currentPage = (checkpoint != null) ? checkpoint.intValue() : 0;
                                    isFirstTime = false;

                                    if ("completed".equals(pandamdam.get("status"))) {
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
                        imageView.setImageResource(pandamdamLesson[currentPage]);
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

        Map<String, Object> pandamdamStatus = new HashMap<>();
        pandamdamStatus.put("status", statusToSave);
        pandamdamStatus.put("checkpoint", currentPage);

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("pandamdam", pandamdamStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status","in_progress");
        moduleMap.put("current_lesson", "pandamdam");
        moduleMap.put("lessons", lessonMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (completed) isLessonDone = true;
                });
    }

    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lesson_character_lines").document("LCL9")
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
                                        imageView.setImageResource(pandamdamLesson[currentPage]);
                                        if (pageLines.containsKey(currentPage)) {
                                            speakLines(pageLines.get(currentPage));
                                        }
                                    }, 1500);
                                });

                            } else {
                                imageView.setImageResource(pandamdamLesson[currentPage]);
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

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_ppt_option, null);
        builder.setView(dialogView);

        dialogOption = builder.create();
        dialogOption.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button buttonResume = dialogView.findViewById(R.id.button_resume);
        Button buttonBumalik = dialogView.findViewById(R.id.button_bumalik);

        buttonResume.setOnClickListener(v -> {
            playClickSound();

            currentPage = checkpoint;
            imageView.setImageResource(pandamdamLesson[currentPage]);

            if (pageLines.containsKey(currentPage)) {
                new android.os.Handler().postDelayed(() -> {
                    speakLines(pageLines.get(currentPage));
                }, 1500);
            }
            waitForResumeChoice = false;
            dialogOption.dismiss();
        });

        buttonBumalik.setOnClickListener(v -> {
            playClickSound();

            currentPage = 0;
            imageView.setImageResource(pandamdamLesson[currentPage]);

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

    private void playClickSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.button_click);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
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
