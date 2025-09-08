package com.example.habiaral.Palaro;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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

import java.util.Locale;

import com.example.habiaral.R;
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

public class PalaroHusay extends AppCompatActivity {

    private TextView husayInstruction;
    private Button answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9;
    private TextView fullAnswerView;
    private ProgressBar timerBar;
    private Button unlockButton;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private int correctAnswerCount = 0;

    private final List<TextView> selectedWords = new ArrayList<>();
    private boolean isTimeUp = false;
    private boolean isAnswered = false;
    private int currentQuestionNumber = 1;
    private int husayScore = 0;
    private int correctStreak = 0;
    private int remainingHearts = 5;
    private ImageView[] heartIcons;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private int attemptCount = 0;
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_husay);
        husayInstruction = findViewById(R.id.husay_instructionText);
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

        Button btnUmalis = findViewById(R.id.UnlockButtonPalaro1);
        btnUmalis.setOnClickListener(v -> showUmalisDialog());

        heartIcons = new ImageView[]{
                findViewById(R.id.heart01),
                findViewById(R.id.heart02),
                findViewById(R.id.heart03),
                findViewById(R.id.heart04),
                findViewById(R.id.heart05)
        };

        db = FirebaseFirestore.getInstance();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true;
                Locale tagalogLocale = new Locale("fil", "PH");
                tts.setLanguage(tagalogLocale);
                tts.setSpeechRate(1.3f);

                new Handler().postDelayed(() -> loadCharacterLine("MHCL1"), 200);
                new Handler().postDelayed(this::showCountdownThenLoadWords, 4000);
            }
        });

        setupAnswerSelection();
        setupBackConfirmation();
        setupUnlockButton();
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
            if (countDownTimer != null) countDownTimer.cancel();
            if (tts != null) tts.stop();
            dialog.dismiss();
            finish();
        });

        btnHindi.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupUnlockButton() {
        unlockButton.setOnClickListener(v -> {
            if (isTimeUp || isAnswered) return;

            String userAnswer = fullAnswerView.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Paki buuin muna ang pangungusap.", Toast.LENGTH_SHORT).show();
                return;
            }

            isAnswered = true;
            long startTime = System.currentTimeMillis();

            String husayDocId = "H" + currentQuestionNumber;
            db.collection("husay").document(husayDocId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String correctAnswer = documentSnapshot.getString("correctAnswer");


                            long endTime = System.currentTimeMillis();
                            long elapsedTimeInSeconds = (endTime - startTime) / 1000;

                            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                correctAnswerCount++;
                                husayScore += 3;
                                correctStreak++;

                                if (elapsedTimeInSeconds <= 5) {
                                    unlockFastAnswerAchievement();
                                }
                                if (correctStreak == 1) {
                                    loadCharacterLine("MCL2");
                                } else if (correctStreak == 2) {
                                    loadCharacterLine("MCL3");
                                } else {
                                    loadCharacterLine("MCL4");
                                }

                                Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                timeLeft = Math.min(timeLeft + 3000, TOTAL_TIME);
                                restartTimer(timeLeft);
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                saveCorrectHusayAnswer(userId, userId, husayDocId);
                            } else {
                                correctStreak = 0;
                                deductHeart();
                                String lineId = remainingHearts > 0 ? "MCL6" : "MCL5";
                                loadCharacterLine(lineId);
                                speakText("Mali ang sagot.");
                                Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();

                            }

                            currentQuestionNumber++;
                            if (currentQuestionNumber <= 10 && !isGameOver) {
                                new Handler().postDelayed(() -> {
                                    if (!isGameOver) {
                                        loadHusayWords("H" + currentQuestionNumber);
                                        fullAnswerView.setText("");
                                        isAnswered = false;
                                        isTimeUp = false;
                                        startTimer();
                                    }
                                }, 3000);
                            } else {
                                if (countDownTimer != null) countDownTimer.cancel();
                                finishQuiz();
                            }

                        }
                    });
        });
    }

    private void setupAnswerSelection() {
        TextView[] answers = {answer1, answer2, answer3, answer4, answer5, answer6};

        for (TextView answer : answers) {
            answer.setOnClickListener(view -> {
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
            if (countDownTimer != null) countDownTimer.cancel();
            if (tts != null) tts.stop();
            backDialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> backDialog.dismiss());

        backDialog.show();
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        husayInstruction.setText(line);
                        speakText(line);

                    }
                });
    }
//EDITED
    // Wala na ang TTS, hindi na magsasalita kapag buo ang sagot
    private void loadHusayWords(String docId) {
        db.collection("husay").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> words = (List<String>) documentSnapshot.get("words");
                        if (words != null && words.size() >= 9) {
                            TextView[] answers = {
                                    answer1, answer2, answer3,
                                    answer4, answer5, answer6,
                                    answer7, answer8, answer9
                            };

                            selectedWords.clear();
                            fullAnswerView.setText("");

                            for (int i = 0; i < 9; i++) {
                                TextView answer = answers[i];
                                answer.setText(words.get(i));
                                answer.setEnabled(true);

                                answer.setOnClickListener(view -> {
                                    if (view.isEnabled()) {
                                        String current = fullAnswerView.getText().toString().trim();
                                        fullAnswerView.setText((current + " " + ((TextView) view).getText()).trim());
                                        view.setEnabled(false);
                                        selectedWords.add((TextView) view);
                                        if (selectedWords.size() == 9) {
                                            String currentText = fullAnswerView.getText().toString();
                                            if (!currentText.endsWith(".")) {
                                                fullAnswerView.append(".");
                                                currentText = fullAnswerView.getText().toString();
                                            }
                                        }

                                    }
                                });

                                answer.setOnTouchListener(new View.OnTouchListener() {
                                    private long lastTapTime = 0;

                                    @Override
                                    public boolean onTouch(View view, MotionEvent motionEvent) {
                                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                            long currentTime = System.currentTimeMillis();
                                            if (currentTime - lastTapTime < 300) {
                                                TextView tapped = (TextView) view;
                                                if (selectedWords.contains(tapped)) {
                                                    selectedWords.remove(tapped);
                                                    tapped.setEnabled(true);
                                                    String word = tapped.getText().toString();
                                                    String currentText = fullAnswerView.getText().toString();
                                                    fullAnswerView.setText(currentText.replaceFirst("\\b" + word + "\\b", "").trim());
                                                }
                                            }
                                            lastTapTime = currentTime;
                                        }
                                        return false;
                                    }
                                });
                            }
                        }
                    }
                });
    }

    private void showCountdownThenLoadWords() {
        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    husayInstruction.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    loadCharacterLine("MHCL2");
                    new Handler().postDelayed(() -> {
                        loadHusayWords("H1");
                        startTimer();
                    }, 1400);
                }
            }
        });
    }

    private void startTimer() {
        restartTimer(timeLeft);
    }

    private void restartTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;

                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent <= 25) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_red));
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroHusay.this, R.drawable.timer_color_green));
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                isTimeUp = true;
                if (tts != null) tts.stop();
                finishQuiz();
            }
        }.start();
    }

    private void finishQuiz() {
        if (isGameOver) return;
        isGameOver = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (tts != null) {
            tts.stop();
        }

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
                            if (tts != null) tts.stop();
                            Toast.makeText(this, "Nabuksan na ang Dalubhasa!", Toast.LENGTH_LONG).show();
                        });
            }
        });
    }
    private void deductHeart() {
        remainingHearts--;

        if (heartIcons != null && remainingHearts >= 0 && remainingHearts < heartIcons.length) {
            heartIcons[remainingHearts].setVisibility(View.INVISIBLE);
        }

        if (remainingHearts <= 0) {
            if (countDownTimer != null) countDownTimer.cancel();
            if (tts != null) tts.stop();
            Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();
            loadCharacterLine("MCL5");

            finishQuiz();
        }
    }
    private void speakText(String text) {
        if (text == null || text.trim().isEmpty()) return;

        if (isTtsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            new Handler().postDelayed(() -> {
                if (isTtsReady && tts != null) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
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
                            showAchievementUnlockedDialog(title, R.drawable.achievement09);
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
                                        showAchievementUnlockedDialog(title, R.drawable.achievement02);
                                    }));
                        });
                    });
                });
    }

    private void showAchievementUnlockedDialog(String title, int imageRes) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = dialogView.findViewById(R.id.imageView19);
        TextView tv = dialogView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.1f), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

            // 👉 Gamitin LayoutParams para makuha yung offset na parang toast
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50; // offset mula sa taas (px)
            dialog.getWindow().setAttributes(params);
        }

        // 🎵 Play sound sabay sa pop up
        dialog.setOnShowListener(d -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.achievement_pop);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        });

        dialog.show();
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
                                showAchievementUnlockedDialog(title, R.drawable.achievement05);
                            }));
                });
            });
        });
    }

}