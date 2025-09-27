package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.KayarianFirestoreUtils;
import com.example.habiaral.Utils.LessonGifUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.TTSUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class sample extends AppCompatActivity {

    private static boolean introDone = false;
    private static boolean lessonDone = false;
    private static boolean exampleDone = false;
    private static boolean payakDone = false;

    private Button btnUnlock;
    private ImageView imgLesson, imgExample, image3D;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_lesson);

        image3D = findViewById(R.id.lesson_idle_gif);
        imgLesson = findViewById(R.id.lesson_image);
        imgExample = findViewById(R.id.example_image);
        btnUnlock = findViewById(R.id.button_unlock);
        btnUnlock.setAlpha(0.5f);

        prefs = getSharedPreferences("LessonPrefs", MODE_PRIVATE);

        introDone = prefs.getBoolean("payakIntroDone", false);
        lessonDone = prefs.getBoolean("payakLessonDone", false);
        exampleDone = prefs.getBoolean("payakExampleDone", false);
        payakDone = prefs.getBoolean("payakDone", false);

        LessonGifUtils.startLessonGifRandomizer(this, image3D);

        TTSUtils.initTts(this, new TTSUtils.OnInitComplete() {
            @Override
            public void onReady() {
                checkLessonStatus();
            }

            @Override
            public void onFail() {}
        });

        btnUnlock.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            TTSUtils.stopSpeaking();
            startActivity(new Intent(this, PayakQuiz.class));
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                TTSUtils.shutdown();
                startActivity(new Intent(sample.this, KayarianNgPangungusap.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }

    private void fadeInImage(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
        view.animate()
                .alpha(1f)
                .setDuration(800)
                .setListener(null);
    }

    private void checkLessonStatus() {
        FirebaseUser user = KayarianFirestoreUtils.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("module_progress")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_2");
                        if (module2 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                            if (lessons != null && lessons.containsKey("payak")) {
                                Map<String, Object> lessonData = (Map<String, Object>) lessons.get("payak");
                                if (lessonData != null) {
                                    String status = (String) lessonData.get("status");

                                    if ("completed".equals(status) || payakDone) {
                                        btnUnlock.setEnabled(true);
                                        btnUnlock.setAlpha(1f);
                                        bringToFront();
                                    } else {
                                        btnUnlock.setEnabled(false);
                                        btnUnlock.setAlpha(0.5f);
                                    }
                                }
                            }
                        }
                    }

                    runOnUiThread(this::loadCharacterLines);
                });
    }

    private void loadCharacterLines() {
        if (payakDone) {
            bringToFront();
        } else {
            if (!introDone) {
                playIntro();
            } else if (!lessonDone) {
                if (imgLesson.getVisibility() != View.VISIBLE) {
                    fadeInImage(imgLesson);
                }
                playLesson();
            } else if (!exampleDone) {
                if (imgExample.getVisibility() != View.VISIBLE) {
                    fadeInImage(imgExample);
                }
                playExample();
            }
        }
    }

    private void playIntro() {
        FirebaseFirestore.getInstance().collection("lesson_character_lines")
                .document("LCL11")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> introLines = (List<String>) doc.get("intro");
                        TTSUtils.kayarianSpeakSequentialLines(introLines, () -> {
                            introDone = true;
                            prefs.edit().putBoolean("payakIntroDone", true).apply();
                            runOnUiThread(() -> fadeInImage(imgLesson));
                            runOnUiThread(this::loadCharacterLines);
                        });
                    }
                });
    }

    private void playLesson() {
        FirebaseFirestore.getInstance().collection("lesson_character_lines")
                .document("LCL11")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> descriptionLines = (List<String>) doc.get("description_line");
                        TTSUtils.kayarianSpeakSequentialLines(descriptionLines, () -> {
                            lessonDone = true;
                            prefs.edit().putBoolean("payakLessonDone", true).apply();
                            runOnUiThread(() -> fadeInImage(imgExample));
                            runOnUiThread(this::loadCharacterLines);
                        });
                    }
                });
    }

    private void playExample() {
        FirebaseFirestore.getInstance().collection("lesson_character_lines")
                .document("LCL11")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> exampleLines = (List<String>) doc.get("example_line");
                        TTSUtils.kayarianSpeakSequentialLines(exampleLines, () -> {
                            exampleDone = true;
                            prefs.edit().putBoolean("payakExampleDone", true).apply();
                            runOnUiThread(() -> {
                                payakDone = true;
                                btnUnlock.setEnabled(true);
                                btnUnlock.setAlpha(1f);
                                prefs.edit().putBoolean("payakDone", true).apply();
                            });
                        });
                    }
                });
    }

    private void bringToFront() {
        imgLesson.setVisibility(View.VISIBLE);
        imgLesson.bringToFront();
        imgLesson.setAlpha(1f);
        imgExample.setVisibility(View.VISIBLE);
        imgExample.bringToFront();
        imgExample.setAlpha(1f);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LessonGifUtils.startLessonGifRandomizer(this, image3D);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLessonStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LessonGifUtils.stopIdleGifRandomizer(this, image3D);
        TTSUtils.stopSpeaking();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LessonGifUtils.stopIdleGifRandomizer(this, image3D);
        TTSUtils.shutdown();
    }
}
