package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangngalanQuiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.LessonGifUtils;
import com.example.habiaral.Utils.ResumeDialogUtils;
import com.example.habiaral.Utils.BahagiFirestoreUtils;
import com.example.habiaral.Utils.FullScreenUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.TTSUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PangngalanLesson extends AppCompatActivity {

    private final int[] lessonPPT = {
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
    private Map<Integer, List<String>> pageLines = new HashMap<>();
    private final boolean[] isFullScreen = {false};
    private boolean isNavigatingInsideApp = false;
    private boolean waitForResumeChoice = false;
    private ImageView btnBack, btnNext, btnFullscreen, image3D, imageView;
    private boolean isLessonDone = false;
    private boolean isFirstTime = true;
    private Button btnUnlock;
    private int currentPage = 0;
    private int resumePage = -1;
    private boolean isResumeDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_lesson);

        image3D = findViewById(R.id.lesson_idle_gif);
        LessonGifUtils.startLessonGifRandomizer(this, image3D);

        btnUnlock = findViewById(R.id.button_unlock);
        imageView = findViewById(R.id.lesson_image);

        btnBack = findViewById(R.id.button_back);
        btnNext = findViewById(R.id.button_next);

        btnFullscreen = findViewById(R.id.button_fullscreen);

        btnUnlock.setEnabled(false);
        btnUnlock.setAlpha(0.5f);

        TTSUtils.initTts(this, new TTSUtils.OnInitComplete() {
            @Override
            public void onReady() {
                loadCharacterLines();
            }

            @Override
            public void onFail() {}
        });

        checkLessonStatus();

        btnUnlock.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            isNavigatingInsideApp = true;
            TTSUtils.stopSpeaking();
            startActivity(new Intent(this, PangngalanQuiz.class));
        });

        btnBack.setOnClickListener(v -> { SoundClickUtils.playClickSound(this, R.raw.button_click); previousPage(); });
        btnNext.setOnClickListener(v -> { SoundClickUtils.playClickSound(this, R.raw.button_click); nextPage(); });

        ConstraintLayout bottomBar = findViewById(R.id.bottom_bar);
        ConstraintLayout optionBar = findViewById(R.id.option_bar);

        btnFullscreen.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            FullScreenUtils.toggleFullScreen(
                    this,
                    isFullScreen,
                    btnFullscreen,
                    imageView,
                    image3D,
                    btnUnlock,
                    bottomBar,
                    optionBar
            );
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (isFullScreen[0]) {
                    ConstraintLayout bottomBar = findViewById(R.id.bottom_bar);
                    ConstraintLayout optionBar = findViewById(R.id.option_bar);
                    ImageView btnFullscreen = findViewById(R.id.button_fullscreen);

                    FullScreenUtils.exitFullScreen(
                            PangngalanLesson.this,
                            isFullScreen,
                            btnFullscreen,
                            imageView,
                            image3D,
                            btnUnlock,
                            bottomBar,
                            optionBar
                    );
                    return;
                }

                TTSUtils.shutdown();
                startActivity(new Intent(PangngalanLesson.this, BahagiNgPananalita.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }

    private void nextPage() {
        if (currentPage < lessonPPT.length - 1 ) {
            currentPage++;
            updatePage();
        }
        if (currentPage == lessonPPT.length - 1) {
            btnUnlock.setEnabled(true);
            btnUnlock.setAlpha(1f);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    private void updatePage() {
        imageView.setImageResource(lessonPPT[currentPage]);
        BahagiFirestoreUtils.saveLessonProgress(BahagiFirestoreUtils.getCurrentUser().getUid(),
                "pangngalan", currentPage, isLessonDone);

        TTSUtils.stopSpeaking();
        updateNavigationButtons();

        if (isLessonDone || currentPage == lessonPPT.length - 1) {
            btnUnlock.setEnabled(true);
            btnUnlock.setAlpha(1f);
        } else {
            btnUnlock.setEnabled(false);
            btnUnlock.setAlpha(0.5f);
        }

        List<String> lines = pageLines.get(currentPage);
        if (lines != null && !lines.isEmpty()) {
            TTSUtils.speakSequentialLines(this, lines, "page_" + currentPage, null);
        }
    }

    private void updateNavigationButtons() {
        if (currentPage == 0) {
            btnBack.setEnabled(false);
            btnBack.setAlpha(0.5f);
        } else {
            btnBack.setEnabled(true);
            btnBack.setAlpha(1f);
        }

        if (currentPage == lessonPPT.length - 1) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1f);
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
                            if (lessons != null && lessons.containsKey("pangngalan")) {
                                Map<String, Object> lessonsData = (Map<String, Object>) lessons.get("pangngalan");
                                if (lessonsData != null) {
                                    Long checkpoint = (Long) lessonsData.get("checkpoint");
                                    currentPage = (checkpoint != null) ? checkpoint.intValue() : 0;
                                    isFirstTime = false;
                                    if ("completed".equals(lessonsData.get("status"))) {
                                        isLessonDone = true;
                                        btnUnlock.setEnabled(true);
                                        btnUnlock.setAlpha(1f);
                                    }
                                    if (!isResumeDialogShowing) {
                                        showResumeDialog(currentPage);
                                        waitForResumeChoice = true;
                                    }
                                }
                            }
                        }
                    } else {
                        currentPage = 0;
                        imageView.setImageResource(lessonPPT[currentPage]);
                        isFirstTime = true;
                    }
                    BahagiFirestoreUtils.saveLessonProgress(user.getUid(), "pangngalan", currentPage, isLessonDone);
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
                            TTSUtils.speakSequentialLines(this, introLines, "intro", () -> {
                                currentPage = 0;
                                updatePage();
                            });
                            btnBack.setEnabled(false);
                            btnBack.setAlpha(0.5f);
                        } else {
                            updatePage();
                        }
                    }
                });
    }


    private void showResumeDialog(int checkpoint) {
        if (isResumeDialogShowing) return;
        isResumeDialogShowing = true;

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

        dialog.setOnDismissListener(d -> isResumeDialogShowing = false);
    }

    @Override
    protected void onDestroy() {
        LessonGifUtils.stopIdleGifRandomizer(this, image3D);
        TTSUtils.shutdown();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        resumePage = currentPage;
        TTSUtils.stopSpeaking();
        LessonGifUtils.stopIdleGifRandomizer(this, image3D);

        if (isFullScreen[0]) {
            ConstraintLayout bottomBar = findViewById(R.id.bottom_bar);
            ConstraintLayout optionBar = findViewById(R.id.option_bar);

            FullScreenUtils.exitFullScreen(
                    this,
                    isFullScreen,
                    btnFullscreen,
                    imageView,
                    image3D,
                    btnUnlock,
                    bottomBar,
                    optionBar
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        isNavigatingInsideApp = false;

        if (resumePage != -1 && !isResumeDialogShowing) {
            showResumeDialog(resumePage);
            waitForResumeChoice = true;
            resumePage = -1;
        }
    }

}
