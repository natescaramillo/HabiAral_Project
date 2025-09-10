package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangngalanQuiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.DialogUtils;
import com.example.habiaral.Utils.FirestoreUtils;
import com.example.habiaral.Utils.FullScreenUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.TextAnimationUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PangngalanLesson extends AppCompatActivity {

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

    private android.os.Handler textHandler = new android.os.Handler();
    private Map<Integer, List<String>> pageLines = new HashMap<>();
    private boolean waitForResumeChoice = false;
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private TextToSpeech textToSpeech;
    private TextView instructionText;
    private ImageView imageView;
    private Button unlockButton;
    private int currentPage = 0;
    private final boolean[] isFullScreen = {false};
    private ImageView backOption, nextOption;
    private ConstraintLayout constraintLayout;
    private int resumePage = -1;
    private int resumeLine = -1;
    private boolean isNavigatingInsideApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        imageView = findViewById(R.id.imageViewPangngalan);
        instructionText = findViewById(R.id.instructionText);

        backOption = findViewById(R.id.back_option);
        nextOption = findViewById(R.id.next_option);

        ImageView fullScreenOption = findViewById(R.id.full_screen_option);
        ImageView imageView2 = findViewById(R.id.imageView2);

        constraintLayout = findViewById(R.id.instructionContainer);

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

        unlockButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            isNavigatingInsideApp = true;
            stopTTS();
            startActivity(new Intent(this, PangngalanQuiz.class));
        });

        backOption.setOnClickListener(v -> { SoundClickUtils.playClickSound(this, R.raw.button_click); previousPage(); });
        nextOption.setOnClickListener(v -> { SoundClickUtils.playClickSound(this, R.raw.button_click); nextPage(); });

        fullScreenOption.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            FullScreenUtils.toggleFullScreen(
                    this,
                    isFullScreen,
                    fullScreenOption,
                    imageView,
                    imageView2,
                    constraintLayout,
                    instructionText,
                    unlockButton
            );
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                stopTTS();
                startActivity(new Intent(PangngalanLesson.this, BahagiNgPananalita.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }

    private void nextPage() {
        if (currentPage < pangngalanLesson.length - 1 ) {
            currentPage++;
            updatePage();
        }
        if (currentPage == pangngalanLesson.length - 1) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    private void updateNavigationButtons() {
        if (currentPage == 0) {
            backOption.setEnabled(false);
            backOption.setAlpha(0.5f);
        } else {
            backOption.setEnabled(true);
            backOption.setAlpha(1f);
        }

        if (currentPage == pangngalanLesson.length - 1) {
            nextOption.setEnabled(false);
            nextOption.setAlpha(0.5f);
        } else {
            nextOption.setEnabled(true);
            nextOption.setAlpha(1f);
        }
    }

    private void updatePage() {
        imageView.setImageResource(pangngalanLesson[currentPage]);
        FirestoreUtils.saveLessonProgress(FirestoreUtils.getCurrentUser().getUid(),
                "pangngalan", currentPage, isLessonDone);

        stopSpeaking();

        TextAnimationUtils.cancelAnimation(instructionText);

        if (pageLines.containsKey(currentPage)) {
            instructionText.setText("");
            new android.os.Handler().postDelayed(() ->
                    speakLines(pageLines.get(currentPage)), 500);
        } else {
            instructionText.setText("");
        }

        updateNavigationButtons();
    }

    private void checkLessonStatus() {
        FirebaseUser user = FirestoreUtils.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("module_progress").document(user.getUid()).get()
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
                                    waitForResumeChoice = true;
                                }
                            }
                        }
                    } else {
                        currentPage = 0;
                        imageView.setImageResource(pangngalanLesson[currentPage]);
                        isFirstTime = true;
                    }
                    FirestoreUtils.saveLessonProgress(user.getUid(), "pangngalan", currentPage, isLessonDone);
                });
    }

    private void loadCharacterLines() {
        FirebaseFirestore.getInstance().collection("lesson_character_lines").document("LCL1").get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<Map<String, Object>> pages = (List<Map<String, Object>>) doc.get("pages");
                    if (pages != null) {
                        for (Map<String, Object> page : pages) {
                            Long pageNum = (Long) page.get("page");
                            List<String> lines = (List<String>) page.get("line");
                            if (pageNum != null && lines != null) {
                                pageLines.put(pageNum.intValue() - 1, lines);
                            }
                        }
                    }
                    List<String> introLines = (List<String>) doc.get("intro");
                    if (!waitForResumeChoice) {
                        if (introLines != null && isFirstTime && !introLines.isEmpty()) {
                            isFirstTime = false;
                            speakSequentialLines(introLines, this::updatePage);
                        } else updatePage();
                    }
                });
    }

    private void speakSequentialLines(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;
        final int[] index = {0};
        String utterancePage = "page_" + currentPage;

        TextAnimationUtils.animateText(instructionText, lines.get(0), 50);

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (!utteranceId.startsWith(utterancePage)) return;
                    index[0]++;
                    if (index[0] < lines.size()) {
                        TextAnimationUtils.animateText(instructionText, lines.get(index[0]), 50);
                        speak(lines.get(index[0]), utterancePage + "_" + index[0]);
                    } else onComplete.run();
                });
            }
            @Override public void onError(String s) {}
        });

        speak(lines.get(0), utterancePage + "_0");
    }

    private void speakLines(List<String> lines) {
        speakSequentialLines(lines, () -> instructionText.setText(""));
    }

    private void speak(String text, String id) {
        if (textToSpeech != null && !text.isEmpty())
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    private void showResumeDialog(int checkpoint) {
        AlertDialog dialog = DialogUtils.showResumeDialog(this, new DialogUtils.ResumeDialogListener() {
            @Override public void onResumeLesson() {
                currentPage = checkpoint;
                updatePage();
                waitForResumeChoice = false;
            }
            @Override public void onRestartLesson() {
                currentPage = 0;
                updatePage();
                waitForResumeChoice = false;
            }
        });
    }

    private void stopSpeaking() {
        if (textToSpeech != null) textToSpeech.stop();
        textHandler.removeCallbacksAndMessages(null);
        TextAnimationUtils.cancelAnimation(instructionText);
    }

    private void stopTTS() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        textHandler.removeCallbacksAndMessages(null);
    }

    @Override protected void onDestroy() { stopTTS(); super.onDestroy(); }

    @Override
    protected void onPause() {
        super.onPause();

        if (!isNavigatingInsideApp) {
            resumePage = currentPage;
        }

        stopSpeaking();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (resumePage != -1) {
            currentPage = resumePage;
            updatePage();
            resumePage = -1;
            resumeLine = -1;
        }
    }
}
