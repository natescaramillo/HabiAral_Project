package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangHalipQuiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.ResumeDialogUtils;
import com.example.habiaral.Utils.BahagiFirestoreUtils;
import com.example.habiaral.Utils.FullScreenUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.SoundManager;
import com.example.habiaral.Utils.TextAnimationUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PangHalipLesson extends AppCompatActivity {

    private final int[] pangHalipLesson = {
            R.drawable.panghalip01, R.drawable.panghalip02, R.drawable.panghalip03,
            R.drawable.panghalip04, R.drawable.panghalip05, R.drawable.panghalip06,
            R.drawable.panghalip07
    };
    private android.os.Handler textHandler = new android.os.Handler();
    private Map<Integer, List<String>> pageLines = new HashMap<>();
    private boolean waitForResumeChoice = false;
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private TextToSpeech textToSpeech;
    private ImageView imageView;
    private Button unlockButton;
    private int currentPage = 0;
    private final boolean[] isFullScreen = {false};
    private ImageView backOption, nextOption;
    private int resumePage = -1;
    private int resumeLine = -1;
    private boolean isNavigatingInsideApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_panghalip_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanghalip);
        imageView = findViewById(R.id.imageViewPanghalip);

        backOption = findViewById(R.id.back_option);
        nextOption = findViewById(R.id.next_option);

        ImageView fullScreenOption = findViewById(R.id.full_screen_option);
        ImageView imageView2 = findViewById(R.id.imageView2);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.0f);
                SoundManager.setTts(textToSpeech);
                loadCharacterLines();
            }
        });

        checkLessonStatus();

        unlockButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            isNavigatingInsideApp = true;
            stopTTS();
            startActivity(new Intent(this, PangHalipQuiz.class));
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
                    unlockButton
            );
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                stopTTS();
                startActivity(new Intent(PangHalipLesson.this, BahagiNgPananalita.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }

    private void nextPage() {
        if (currentPage < pangHalipLesson.length - 1 ) {
            currentPage++;
            updatePage();
        }
        if (currentPage == pangHalipLesson.length - 1) {
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

        if (currentPage == pangHalipLesson.length - 1) {
            nextOption.setEnabled(false);
            nextOption.setAlpha(0.5f);
        } else {
            nextOption.setEnabled(true);
            nextOption.setAlpha(1f);
        }
    }

    private void updatePage() {
        imageView.setImageResource(pangHalipLesson[currentPage]);
        BahagiFirestoreUtils.saveLessonProgress(BahagiFirestoreUtils.getCurrentUser().getUid(),
                "panghalip", currentPage, isLessonDone);

        stopSpeaking();
        updateNavigationButtons();

        List<String> lines = pageLines.get(currentPage);
        if (lines != null && !lines.isEmpty()) {
            speakSequentialLines(lines, () -> {});
        }
    }

    private void checkLessonStatus() {
        FirebaseUser user = BahagiFirestoreUtils.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("module_progress").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                        if (module1 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("panghalip")) {
                                Map<String, Object> panghalip = (Map<String, Object>) lessons.get("panghalip");
                                if (panghalip != null) {
                                    Long checkpoint = (Long) panghalip.get("checkpoint");
                                    currentPage = (checkpoint != null) ? checkpoint.intValue() : 0;
                                    isFirstTime = false;
                                    if ("completed".equals(panghalip.get("status"))) {
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
                        imageView.setImageResource(pangHalipLesson[currentPage]);
                        isFirstTime = true;
                    }
                    BahagiFirestoreUtils.saveLessonProgress(user.getUid(), "panghalip", currentPage, isLessonDone);
                });
    }

    private void loadCharacterLines() {
        FirebaseFirestore.getInstance().collection("lesson_character_lines").document("LCL4").get()
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

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (!utteranceId.startsWith(utterancePage)) return;
                    index[0]++;
                    if (index[0] < lines.size()) {
                        if (!SoundManager.isMuted(PangHalipLesson.this)) {
                            speak(lines.get(index[0]), utterancePage + "_" + index[0]);
                        }
                    } else onComplete.run();
                });
            }

            @Override public void onError(String s) {}
        });

        if (!SoundManager.isMuted(this)) {
            speak(lines.get(0), utterancePage + "_0");
        }
    }

    private void speak(String text, String id) {
        if (textToSpeech != null && !text.isEmpty())
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    private void showResumeDialog(int checkpoint) {
        AlertDialog dialog = ResumeDialogUtils.showResumeDialog(this, new ResumeDialogUtils.ResumeDialogListener() {
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