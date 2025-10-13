package com.example.habiaral.Palaro;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.Voice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.speech.tts.TextToSpeech;

import java.util.Collections;
import java.util.Locale;

import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.SoundEffectsManager;
import com.example.habiaral.Utils.TimerSoundUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

public class PalaroHusay extends AppCompatActivity {

    private Button answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9, unlockButton, btnUmalis;
    private TextView fullAnswerView;
    private ProgressBar timerBar;
    private FirebaseFirestore db;
    private TextToSpeech textToSpeech;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private CountDownTimer countDownTimer;
    private Handler handler = new Handler(), countdownHandler;
    private Runnable countdownRunnable;
    private MediaPlayer timerSound, countdownBeep;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;
    private String currentHusayDocId = null;
    private int correctAnswerCount = 0, currentQuestionNumber = 1, husayScore = 0,
            correctStreak = 0, remainingHearts = 5, attemptCount = 0,
            currentQuestionIndex = 0, countdownValue = 3;

    private boolean isTimeUp = false, isAnswered = false, isTtsReady = false,
            isGameOver = false, playMHCL2 = false;
    private ImageView[] heartIcons;
    private ImageView characterIcon;
    private final List<TextView> selectedWords = new ArrayList<>();
    private final List<String> questionIds = new ArrayList<>();
    private String lastTimerZone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_husay);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(android.net.Network network) {
                    super.onLost(network);
                    runOnUiThread(() -> {
                        finishQuiz();
                    });
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        SoundEffectsManager.init(this);

        answer1 = findViewById(R.id.husay_answer1);
        answer2 = findViewById(R.id.husay_answer2);
        answer3 = findViewById(R.id.husay_answer3);
        answer4 = findViewById(R.id.husay_answer4);
        answer5 = findViewById(R.id.husay_answer5);
        answer6 = findViewById(R.id.husay_answer6);
        answer7 = findViewById(R.id.husay_answer7);
        answer8 = findViewById(R.id.husay_answer8);
        answer9 = findViewById(R.id.husay_answer9);

        fullAnswerView = findViewById(R.id.whole_answer);
        timerBar = findViewById(R.id.timerBar);
        unlockButton = findViewById(R.id.UnlockButtonPalaro);

        btnUmalis = findViewById(R.id.UnlockButtonPalaro1);

        if (unlockButton != null) unlockButton.setEnabled(false);
        if (btnUmalis != null) btnUmalis.setEnabled(false);

        btnUmalis.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showUmalisDialog();
        });

        heartIcons = new ImageView[]{
                findViewById(R.id.heart01),
                findViewById(R.id.heart02),
                findViewById(R.id.heart03),
                findViewById(R.id.heart04),
                findViewById(R.id.heart05)
        };

        timerSound = MediaPlayer.create(this, R.raw.sound_new);
        timerSound.setLooping(false);

        db = FirebaseFirestore.getInstance();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.", Toast.LENGTH_LONG).show();
                    try {
                        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Hindi ma-open ang installer ng TTS.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    isTtsReady = true;
                    Voice selected = null;
                    for (Voice v : textToSpeech.getVoices()) {
                        Locale vLocale = v.getLocale();
                        if (vLocale != null && vLocale.getLanguage().equals("fil")) {
                            selected = v;
                            break;
                        }
                    }
                    if (selected != null) {
                        textToSpeech.setVoice(selected);
                    }
                    textToSpeech.setSpeechRate(1.0f);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    String uid = auth.getCurrentUser().getUid();

                    DocumentReference docRef = db.collection("minigame_progress").document(uid);
                    docRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isFirstTime = documentSnapshot.getBoolean("husay_intro");

                            if (isFirstTime == null || isFirstTime) {
                                playMHCL2 = true;
                                loadCharacterLineWithCallback("MHCL1", () -> {
                                    runOnUiThread(this::showCountdownThenLoadWords);
                                });
                                docRef.update("husay_intro", false);

                            } else {
                                playMHCL2 = false;
                                showCountdownThenLoadWords();
                            }
                        } else {
                            playMHCL2 = true;
                            loadCharacterLineWithCallback("MHCL1", () -> {
                                runOnUiThread(this::showCountdownThenLoadWords);
                            });

                            Map<String, Object> data = new HashMap<>();
                            data.put("husay_intro", false);
                            docRef.set(data, SetOptions.merge());
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
            }
        });

        characterIcon = findViewById(R.id.characterIcon);
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(getApplicationContext())
                    .asGif()
                    .load(R.drawable.idle)
                    .into(characterIcon);
        }

        setupAnswerSelection();
        setupBackConfirmation();
        setupUnlockButton();

        for (int i = 1; i <= 100; i++) {
            questionIds.add("H" + i);
        }

        Collections.shuffle(questionIds);

    }

    private void showUmalisDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PalaroHusay.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);

        Button btnOo = dialogView.findViewById(R.id.button5);
        Button btnHindi = dialogView.findViewById(R.id.button6);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnOo.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);

            stopAllSounds();
            stopCountdownSequence();

            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            handler.removeCallbacksAndMessages(null);

            dialog.dismiss();
            finish();
        });


        btnHindi.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            dialog.dismiss();
        });
        dialog.show();

    }

    private void stopAllSounds() {
        if (timerSound != null && timerSound.isPlaying()) timerSound.pause();
        if (textToSpeech != null) textToSpeech.stop();
    }

    private void setupUnlockButton() {
        unlockButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (isTimeUp || isAnswered) return;

            String userAnswer = fullAnswerView.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Paki buuin muna ang pangungusap.", Toast.LENGTH_SHORT).show();
                return;
            }

            isAnswered = true;
            long startTime = System.currentTimeMillis();

            String husayDocId = currentHusayDocId;
            if (husayDocId == null) {
                Toast.makeText(this, "Walang kasalukuyang tanong.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("husay").document(husayDocId).get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                String correctAnswer = doc.getString("correctAnswer");
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                if (normalize(userAnswer).equals(normalize(correctAnswer))) {
                    handleCorrectAnswer(elapsed, husayDocId);
                } else {
                    handleWrongAnswer();
                }

                proceedToNextQuestion();
            });
        });
    }

    private String normalize(String s) {
        if (s == null) return "";
        String n = s.trim().replaceAll("\\s+", " ").toLowerCase();
        n = n.replaceAll("[\\p{Punct}&&[^']]+$", "");
        return n;
    }

    private void handleCorrectAnswer(long elapsedTime, String docId) {
        SoundEffectsManager.play("CORRECT");
        playGif(R.drawable.right_1, 300);

        new Handler().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                playGif(R.drawable.idle, 300);
            }
        }, 4000);

        correctAnswerCount++;
        husayScore += 3;
        correctStreak++;

        if (elapsedTime <= 5) unlockFastAnswerAchievement();

        loadCharacterLine(
                correctStreak == 1 ? "MCL2" :
                correctStreak == 2 ? "MCL3" : "MCL4"
        );

        Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        saveCorrectHusayAnswer(userId, userId, docId);
    }

    private void handleWrongAnswer() {
        SoundEffectsManager.play("WRONG");
        playGif(R.drawable.wrong, 300);
        new Handler().postDelayed(() -> playGif(R.drawable.idle, 300), 2300);

        correctStreak = 0;
        deductHeart();
        loadCharacterLine(remainingHearts > 0 ? "MCL6" : "MCL5");
        new Handler().postDelayed(() -> speakText("Mali ang sagot."), 400);
        Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
    }

    private void playGif(int drawableId, int fadeMs) {
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this)
                    .asGif()
                    .load(drawableId)
                    .transition(DrawableTransitionOptions.withCrossFade(fadeMs))
                    .into(characterIcon);
        }
    }

    private void proceedToNextQuestion() {
        currentQuestionNumber++;

        if (currentQuestionIndex >= questionIds.size() || isGameOver) {
            if (countDownTimer != null) countDownTimer.cancel();
            finishQuiz();
            return;
        }

        new Handler().postDelayed(() -> {
            if (!isGameOver && currentQuestionIndex < questionIds.size()) {
                currentQuestionIndex++;
                if (currentQuestionIndex < questionIds.size()) {
                    loadHusayWords(questionIds.get(currentQuestionIndex));

                    fullAnswerView.setText("");
                    isAnswered = false;
                    isTimeUp = false;

                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }

                    timeLeft = TOTAL_TIME;

                    timerBar.setProgress(100);
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_green));

                } else {
                    finishQuiz();
                }
            } else {
                finishQuiz();
            }
        }, 3000);
    }


    private void setupAnswerSelection() {
        TextView[] answers = {answer1, answer2, answer3, answer4, answer5, answer6};

        for (TextView answer : answers) {
            answer.setOnClickListener(view -> {
                SoundClickUtils.playClickSound(this, R.raw.button_click);
                if (isTimeUp || isAnswered) return;
                resetAnswerBackgrounds();
                view.setBackgroundResource(R.drawable.answer_option_bg_selected);
            });
        }
    }

    private void setupBackConfirmation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });
    }

    private void resetAnswerBackgrounds() {
        TextView[] allAnswers = {answer1, answer2, answer3, answer4, answer5, answer6};
        for (TextView answer : allAnswers) {
            answer.setBackgroundResource(R.drawable.answer_option_bg);
        }
    }

    private void showBackConfirmationDialog() {
        View backDialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);
        AlertDialog backDialog = new AlertDialog.Builder(this)
                .setView(backDialogView)
                .setCancelable(false)
                .create();

        if (backDialog.getWindow() != null) {
            backDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button yesButton = backDialogView.findViewById(R.id.button5);
        Button noButton = backDialogView.findViewById(R.id.button6);

        yesButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (countDownTimer != null) countDownTimer.cancel();
            if (textToSpeech != null) textToSpeech.stop();
            backDialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            backDialog.dismiss();
        });

        backDialog.show();
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        speakText(line);

                    }
                });
    }

    private void loadHusayWords(String docId) {
        currentHusayDocId = docId;
        db.collection("husay").document(docId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            List<String> words = (List<String>) doc.get("words");
            if (words == null || words.size() < 9) return;

            TextView[] answers = {answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9};
            selectedWords.clear();
            fullAnswerView.setText("");

            for (int i = 0; i < 9; i++) {
                TextView answer = answers[i];
                answer.setText(words.get(i));
                answer.setEnabled(true);
                answer.setOnClickListener(view -> onWordClicked((TextView) view));
                answer.setOnTouchListener(new DoubleTapListener((TextView) answer));
            }

            unlockInputs();

            startTimer();
        });
    }


    private void onWordClicked(TextView view) {
        SoundClickUtils.playClickSound(this, R.raw.button_click);
        if (!view.isEnabled()) return;

        String current = fullAnswerView.getText().toString().trim();
        fullAnswerView.setText((current + " " + view.getText()).trim());
        view.setEnabled(false);
        selectedWords.add(view);

        if (selectedWords.size() == 9 && !fullAnswerView.getText().toString().endsWith(".")) {
            fullAnswerView.append(".");
        }
    }

    private class DoubleTapListener implements View.OnTouchListener {
        private long lastTap = 0;
        private final TextView view;

        DoubleTapListener(TextView view) { this.view = view; }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - lastTap < 300 && selectedWords.contains(view)) {
                    selectedWords.remove(view);
                    view.setEnabled(true);
                    String word = view.getText().toString();
                    fullAnswerView.setText(fullAnswerView.getText().toString()
                            .replaceFirst("\\b" + word + "\\b", "").trim());
                }
                lastTap = now;
            }
            return false;
        }
    }

    private void lockInputs() {
        TextView[] allAnswers = {answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9};
        for (TextView a : allAnswers) {
            if (a != null) {
                a.setEnabled(false);
                a.setBackgroundResource(R.drawable.answer_option_bg);
            }
        }
        if (unlockButton != null) unlockButton.setEnabled(false);
        if (btnUmalis != null) btnUmalis.setEnabled(false);
    }

    private void unlockInputs() {
        TextView[] allAnswers = {answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9};
        for (TextView a : allAnswers) {
            if (a != null) a.setEnabled(true);
        }
        if (unlockButton != null) unlockButton.setEnabled(true);
        if (btnUmalis != null) btnUmalis.setEnabled(true);
    }

    private void showCountdownThenLoadWords() {
        stopCountdownSequence();
        lockInputs();
        countdownHandler = new Handler();
        countdownValue = 3;

        countdownRunnable = () -> {
            if (isGameOver || isFinishing() || isDestroyed()) {
                stopCountdownSequence();
                return;
            }

            if (countdownValue > 0) {
                releaseBeepPlayer();

                countdownBeep = MediaPlayer.create(this, R.raw.beep);
                if (countdownBeep != null) {
                    countdownBeep.setOnCompletionListener(mp -> releasePlayer(mp));
                    try { countdownBeep.start(); } catch (IllegalStateException ignored) {}
                }

                countdownValue--;
                countdownHandler.postDelayed(countdownRunnable, 1000);
            } else {
                releaseBeepPlayer();

                if (!isGameOver && !isFinishing() && !isDestroyed()) {
                    if (playMHCL2) {
                        loadCharacterLineWithCallback("MHCL2", () -> {
                            if (!questionIds.isEmpty() && !isGameOver)
                                loadHusayWords(questionIds.get(currentQuestionIndex));
                        });
                    } else {
                        if (!questionIds.isEmpty() && !isGameOver)
                            loadHusayWords(questionIds.get(currentQuestionIndex));
                    }
                }
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void releaseBeepPlayer() {
        if (countdownBeep != null) {
            try {
                if (countdownBeep.isPlaying()) countdownBeep.stop();
                countdownBeep.release();
            } catch (Exception ignored) {}
            countdownBeep = null;
        }
    }

    private void releasePlayer(MediaPlayer mp) {
        try { mp.release(); } catch (Exception ignored) {}
    }

    private void stopCountdownSequence() {
        try {
            if (countdownHandler != null && countdownRunnable != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
        } catch (Exception ignored) {}

        try {
            if (countdownBeep != null) {
                try { if (countdownBeep.isPlaying()) countdownBeep.stop(); } catch (Exception ignored) {}
                try { countdownBeep.release(); } catch (Exception ignored) {}
                countdownBeep = null;
            }
        } catch (Exception ignored) {}

        countdownHandler = null;
        countdownRunnable = null;
        countdownValue = 3;
    }


    private void loadCharacterLineWithCallback(String docId, Runnable onDone) {
        db.collection("minigame_character_lines").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        if (isTtsReady && textToSpeech != null) {
                            textToSpeech.speak(line, TextToSpeech.QUEUE_FLUSH, null, docId + "_UTTERANCE");

                            textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {}

                                @Override
                                public void onDone(String utteranceId) {
                                    if ((docId + "_UTTERANCE").equals(utteranceId)) {
                                        runOnUiThread(onDone);
                                    }
                                }

                                @Override
                                public void onError(String utteranceId) {}
                            });
                        }
                    }
                });
    }

    private void startTimer() {
        restartTimer(timeLeft);
    }

    private void restartTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();

        if (timerSound != null) {
            timerSound.seekTo(0);
            timerSound.start();
        }

        countDownTimer = new CountDownTimer(duration, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (remainingHearts <= 0) {
                    cancel();
                    return;
                }
                timeLeft = millisUntilFinished;
                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent > 48) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_green));
                } else if (percent > 26) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_red));
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                isTimeUp = true;
                stopTimerSound();
                loadCharacterLine("MCL5");
                finishQuiz();
            }
        }.start();
    }

    private void stopTimerSound() {
        if (timerSound != null && timerSound.isPlaying()) {
            timerSound.stop();
            try {
                timerSound.prepare();
            } catch (Exception ignored) {}
        }
    }

    private void finishQuiz() {
        if (isGameOver) return;
        isGameOver = true;

        stopCountdownSequence();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        MediaPlayer gameOverSound = MediaPlayer.create(this, R.raw.game_over);
        gameOverSound.setOnCompletionListener(MediaPlayer::release);
        gameOverSound.start();

        saveHusayScore();

        View showTotalPoints = getLayoutInflater().inflate(R.layout.dialog_box_time_up, null);
        AlertDialog dialog = new AlertDialog.Builder(PalaroHusay.this)
                .setView(showTotalPoints)
                .setCancelable(false)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView titleText = showTotalPoints.findViewById(R.id.textView11);
        TextView scoreTextDialog = showTotalPoints.findViewById(R.id.scoreText);
        scoreTextDialog.setText(String.valueOf(husayScore));

        if (isTimeUp) {
            titleText.setText("Ubos na ang iyong oras");
        } else if (remainingHearts <= 0) {
            titleText.setText("Ubos na ang iyong buhay");
        } else {
            titleText.setText("Natapos na ang palaro");
        }

        Button balik = showTotalPoints.findViewById(R.id.btn_balik);
        balik.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            dialog.dismiss();
            husayScore = 0;
            Intent resultIntent = new Intent();
            resultIntent.putExtra("husayPoints", husayScore);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void saveHusayScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || husayScore <= 0) return;

        String uid = currentUser.getUid();
        DocumentReference docRef = db.collection("minigame_progress").document(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("husay_score", com.google.firebase.firestore.FieldValue.increment(husayScore));
        updates.put("total_score", com.google.firebase.firestore.FieldValue.increment(husayScore));

        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    husayScore = 0;

                    docRef.get().addOnSuccessListener(snapshot -> {
                        int totalScore = snapshot.contains("total_score") ? snapshot.getLong("total_score").intValue() : 0;

                        if (totalScore >= 800) {
                            unlockDalubhasa(docRef);
                        }
                    });
                });
    }

    private void unlockDalubhasa(DocumentReference docRef) {
        docRef.get().addOnSuccessListener(snapshot -> {
            boolean alreadyUnlocked = snapshot.contains("dalubhasa_unlocked") && snapshot.getBoolean("dalubhasa_unlocked");
            if (!alreadyUnlocked) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("dalubhasa_unlocked", true);

                docRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            if (countDownTimer != null) countDownTimer.cancel();
                            if (textToSpeech != null) textToSpeech.stop();
                            Toast.makeText(this, "Nabuksan na ang Dalubhasa!", Toast.LENGTH_LONG).show();
                        });
            }
        });
    }
    private void deductHeart() {
        remainingHearts--;

        MediaPlayer heartPopSound = MediaPlayer.create(this, R.raw.heart_pop);
        heartPopSound.setOnCompletionListener(MediaPlayer::release);
        heartPopSound.start();

        if (heartIcons != null && remainingHearts >= 0 && remainingHearts < heartIcons.length) {
            heartIcons[remainingHearts].setVisibility(View.INVISIBLE);
        }

        if (remainingHearts <= 0) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            stopTimerSound();

            Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();

            loadCharacterLine("MCL5");
            finishQuiz();
        }
    }


    private void speakText(String text) {
        if (text == null || text.trim().isEmpty()) return;

        if (isTtsReady && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            new Handler().postDelayed(() -> {
                if (isTtsReady && textToSpeech != null) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        endAllAndFinish();
        if (countDownTimer != null) countDownTimer.cancel();

        if (timerSound != null) {
            timerSound.stop();
            timerSound.release();
        }

        handler.removeCallbacksAndMessages(null);
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    private void saveCorrectHusayAnswer(String uid, String firebaseUID, String questionId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(firebaseUID).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) {
                Toast.makeText(this, "Walang studentId na nahanap.", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = studentDoc.getString("studentId");
            DocumentReference docRef = db.collection("palaro_answered").document(firebaseUID);

            docRef.get().addOnSuccessListener(answerDoc -> {
                boolean alreadyAnswered = false;

                if (answerDoc.exists()) {
                    Map<String, Object> husayMap = (Map<String, Object>) answerDoc.get("husay");
                    if (husayMap != null && Boolean.TRUE.equals(husayMap.get(questionId))) {
                        alreadyAnswered = true;
                    }
                }

                if (!alreadyAnswered) {
                    Map<String, Object> nestedHusay = new HashMap<>();
                    nestedHusay.put(questionId, true);

                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentId);
                    update.put("husay", nestedHusay);

                    docRef.set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                unlockAchievementA9IfEligible();
                            });

                    docRef.set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                unlockAchievementA9IfEligible();
                                int wrongAnswers = attemptCount - correctAnswerCount;
                                if (correctAnswerCount >= 50 && wrongAnswers <= 3) {
                                    unlockBihasangMagsanayAchievement(firebaseUID);
                                }
                            });

                }
            });
        });
    }

    private void unlockAchievementA9IfEligible() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA9";
        String achievementId = "A9";

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");
            String firebaseUID = studentDoc.getId();

            db.collection("husay").get().addOnSuccessListener(allQuestionsSnapshot -> {
                List<String> allQuestionIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : allQuestionsSnapshot) {
                    allQuestionIds.add(doc.getId());
                }

                db.collection("palaro_answered").document(firebaseUID).get().addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    Map<String, Object> answeredMap = (Map<String, Object>) userDoc.get("husay");
                    if (answeredMap == null) return;

                    List<String> answeredIds = new ArrayList<>(answeredMap.keySet());

                    if (answeredIds.containsAll(allQuestionIds)) {
                        db.collection("student_achievements").document(firebaseUID).get().addOnSuccessListener(achSnapshot -> {
                            Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");

                            if (achievements != null && achievements.containsKey(achievementCode)) {
                                unlockBihasangMagsanayAchievement(studentId);
                                return;
                            }

                            continueUnlockingAchievement(firebaseUID, achievementCode, achievementId);
                            unlockBihasangMagsanayAchievement(studentId);
                        });
                    }

                });
            });
        });
    }

    private void continueUnlockingAchievement(String uid, String saCode, String achievementID) {
        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            db.collection("achievements").document(achievementID).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists()) return;

                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementID);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", Timestamp.now());

                Map<String, Object> achievementMap = new HashMap<>();
                achievementMap.put(saCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements", achievementMap);


                db.collection("student_achievements").document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            AchievementDialogUtils.showAchievementUnlockedDialog(PalaroHusay.this, title, R.drawable.achievement09);
                        }));
            });
        });
    }

    private void unlockBihasangMagsanayAchievement(String firebaseUID) {
        String achievementCode = "SA2";
        String achievementId = "A2";

        db.collection("student_achievements").document(firebaseUID).get()
                .addOnSuccessListener(achSnapshot -> {
                    Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                    if (achievements != null && achievements.containsKey(achievementCode)) {
                        return;
                    }

                    db.collection("students").document(firebaseUID).get().addOnSuccessListener(studentDoc -> {
                        if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

                        String studentId = studentDoc.getString("studentId");

                        db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                            if (!achDoc.exists()) return;

                            String title = achDoc.getString("title");

                            Map<String, Object> achievementData = new HashMap<>();
                            achievementData.put("achievementID", achievementId);
                            achievementData.put("title", title);
                            achievementData.put("unlockedAt", Timestamp.now());

                            Map<String, Object> achievementMap = new HashMap<>();
                            achievementMap.put(achievementCode, achievementData);

                            Map<String, Object> wrapper = new HashMap<>();
                            wrapper.put("studentId", studentId);
                            wrapper.put("achievements", achievementMap);

                            db.collection("student_achievements").document(firebaseUID)
                                    .set(wrapper, SetOptions.merge())
                                    .addOnSuccessListener(unused -> runOnUiThread(() ->{
                                        AchievementDialogUtils.showAchievementUnlockedDialog(PalaroHusay.this, title, R.drawable.achievement02);
                                    }));
                        });
                    });
                });
    }

    private void unlockFastAnswerAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA5";
        String achievementId = "A5";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(achSnapshot -> {
                boolean alreadyUnlocked = false;

                if (achSnapshot.exists()) {
                    Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                    if (achievements != null && achievements.containsKey(achievementCode)) {
                        alreadyUnlocked = true;
                    }
                }

                if (alreadyUnlocked) return;

                db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                    if (!achDoc.exists()) return;

                    String title = achDoc.getString("title");

                    Map<String, Object> achievementData = new HashMap<>();
                    achievementData.put("achievementID", achievementId);
                    achievementData.put("title", title);
                    achievementData.put("unlockedAt", Timestamp.now());

                    Map<String, Object> achievementMap = new HashMap<>();
                    achievementMap.put(achievementCode, achievementData);

                    Map<String, Object> wrapper = new HashMap<>();
                    wrapper.put("studentId", studentId);
                    wrapper.put("achievements", achievementMap);

                    db.collection("student_achievements").document(uid)
                            .set(wrapper, SetOptions.merge())
                            .addOnSuccessListener(unused -> runOnUiThread(() -> {
                                AchievementDialogUtils.showAchievementUnlockedDialog(PalaroHusay.this, title, R.drawable.achievement05);
                            }));
                });
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (timerSound != null) timerSound.setVolume(0f, 0f);

        TimerSoundUtils.setVolume(0f);

        if (textToSpeech != null) textToSpeech.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (timerSound != null) timerSound.setVolume(1f, 1f);

        TimerSoundUtils.setVolume(0.2f);
    }

    private void endAllAndFinish() {
        if (isGameOver) {
            cleanupResources();
            finish();
            return;
        }

        isGameOver = true;
        cleanupResources();
        finish();
    }

    private void cleanupResources() {
        stopCountdownSequence();

        safeRun(() -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
        });

        safeRun(() -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
                isTtsReady = false;
            }
        });

        safeRun(() -> {
            if (handler != null) handler.removeCallbacksAndMessages(null);
        });

        safeRun(() -> {
            if (connectivityManager != null && networkCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                } catch (IllegalArgumentException | SecurityException ignored) {}
                networkCallback = null;
            }
        });

        lastTimerZone = "";
    }

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        endAllAndFinish();
    }
}