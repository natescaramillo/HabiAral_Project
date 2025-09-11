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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.example.habiaral.Utils.FinishDialogUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.TimerSoundUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;


public class PalaroBaguhan extends AppCompatActivity {

    private Button selectedAnswer;
    private TextView baguhanQuestion;
    private ProgressBar timerBar;
    private Button unlockButton, unlockButton1;
    private boolean isGameOver = false;
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;
    private FirebaseFirestore db;
    private int correctAnswerCount = 0;
    private int baguhanScore = 0;
    private int currentQuestionNumber = 1;
    private boolean isAnswered = false;
    private boolean isTimeUp = false;
    private String studentID;
    private TextToSpeech tts;
    private int remainingHearts = 5;
    private int correctStreak = 0;
    private ImageView[] heartIcons;
    private List<String> questionIds = new ArrayList<>();
    private long startTime;
    private MediaPlayer mediaPlayer;
    private String lastTimerZone = "";
    private Handler handler = new Handler();
    private Runnable loadLineRunnable, startCountdownRunnable;
    private Button[] answerButtons;
    public static final String LINE_START = "MCL1";
    public static final String LINE_ONE_CORRECT = "MCL2";
    public static final String LINE_TWO_CORRECT = "MCL3";
    public static final String LINE_STREAK = "MCL4";
    public static final String LINE_WRONG = "MCL6";
    private static final String LINE_WRONG_2 = "MCL5";
    private static final String FIELD_BAGUHAN = "baguhan_score";
    private static final String FIELD_HUSAY = "husay_score";
    private static final String FIELD_DALUBHASA = "dalubhasa_score";
    private static final String FIELD_TOTAL = "total_score";

    private static final String FIELD_HUSAY_UNLOCKED = "husay_unlocked";
    private static final String FIELD_DALUBHASA_UNLOCKED = "dalubhasa_unlocked";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_baguhan);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("fil", "PH"));
                tts.setSpeechRate(1.3f);

                loadLineRunnable = () -> loadCharacterLine(LINE_START);
                startCountdownRunnable = this::showCountdownThenLoadQuestion;

                handler.postDelayed(loadLineRunnable, 300);
                handler.postDelayed(startCountdownRunnable, 3000);
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) studentID = currentUser.getUid();

        baguhanQuestion = findViewById(R.id.baguhan_instructionText);

        answerButtons = new Button[]{
                findViewById(R.id.baguhan_answer1),
                findViewById(R.id.baguhan_answer2),
                findViewById(R.id.baguhan_answer3),
                findViewById(R.id.baguhan_answer4),
                findViewById(R.id.baguhan_answer5),
                findViewById(R.id.baguhan_answer6)
        };

        timerBar = findViewById(R.id.timerBar);
        unlockButton = findViewById(R.id.UnlockButtonPalaro);
        unlockButton1 = findViewById(R.id.UnlockButtonPalaro1);

        heartIcons = new ImageView[]{
                findViewById(R.id.heart01),
                findViewById(R.id.heart02),
                findViewById(R.id.heart03),
                findViewById(R.id.heart04),
                findViewById(R.id.heart05)
        };

        db = FirebaseFirestore.getInstance();
        currentQuestionNumber = 1;
        correctAnswerCount = 0;
        baguhanScore = 0;

        View.OnClickListener answerClickListener = view -> {
            if (isTimeUp || isAnswered) return;
            resetAnswerBackgrounds();
            view.setBackgroundResource(R.drawable.answer_option_bg_selected);
            selectedAnswer = (Button) view;
        };

        for (Button btn : answerButtons) {
            btn.setOnClickListener(answerClickListener);
        }

        unlockButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (isTimeUp || isAnswered) return;

            if (selectedAnswer != null) {
                isAnswered = true;
                String userAnswer = selectedAnswer.getText().toString();
                String correctDocId = questionIds.get(currentQuestionNumber);

                db.collection("baguhan").document(correctDocId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");

                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;
                                    baguhanScore += 5;
                                    correctStreak++;
                                    handleCorrectStreak(correctStreak);

                                    MediaPlayer correctSound = MediaPlayer.create(this, R.raw.correct);
                                    correctSound.setOnCompletionListener(MediaPlayer::release);
                                    correctSound.start();

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    saveCorrectBaguhanAnswer(uid, studentID, correctDocId);

                                    timeLeft = Math.min(timeLeft + 3000, TOTAL_TIME);
                                    startTimer(timeLeft);
                                } else {
                                    correctStreak = 0;
                                    deductHeart();
                                    MediaPlayer wrongSound = MediaPlayer.create(this, R.raw.wrong);
                                    wrongSound.setOnCompletionListener(MediaPlayer::release);
                                    wrongSound.start();

                                    loadCharacterLine(remainingHearts > 0 ? LINE_WRONG : LINE_WRONG_2);
                                    Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            new Handler().postDelayed(() -> {
                                if (!isGameOver && !isFinishing()) {
                                    currentQuestionNumber++;
                                    resetForNextQuestion();
                                    loadBaguhanQuestion();
                                }
                            }, 2000);
                        });
            } else {
                Toast.makeText(this, "Paki pili muna ng sagot.", Toast.LENGTH_SHORT).show();
            }
        });

        unlockButton1.setOnLongClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            saveBaguhanScore();
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        Button umalisButton = findViewById(R.id.UnlockButtonPalaro1);
        umalisButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showExitConfirmationDialog();
        });
    }

    private void exitGame() {
        if (isGameOver) return;
        isGameOver = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (handler != null) {
            if (loadLineRunnable != null) handler.removeCallbacks(loadLineRunnable);
            if (startCountdownRunnable != null) handler.removeCallbacks(startCountdownRunnable);
        }

        stopAllSounds();
        TimerSoundUtils.stop();

        if (tts != null) {
            tts.stop();
        }

        saveBaguhanScore();
    }


    private void handleCorrectStreak(int streak) {
        if (streak == 1) loadCharacterLine(LINE_ONE_CORRECT);
        else if (streak == 2) loadCharacterLine(LINE_TWO_CORRECT);
        else if (streak >= 3) loadCharacterLine(LINE_STREAK);
    }

    private void finishQuiz() {
        if (isGameOver) return;
        isGameOver = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (tts != null) tts.stop();
        TimerSoundUtils.stop();

        saveBaguhanScore();
        unlockAchievementA8IfEligible();
        if (correctAnswerCount >= 20) unlockPerfectStreakAchievement();

        showFinishDialog();
    }

    private void showFinishDialog() {
        String message = getFinishMessage();
        FinishDialogUtils.showFinishDialog(this, baguhanScore, message, () -> {
            if (!isFinishing()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("baguhanPoints", baguhanScore);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        playGameOverSound();
    }

    private String getFinishMessage() {
        if (isTimeUp) return "Ubos na ang iyong oras";
        if (remainingHearts <= 0) return "Ubos na ang iyong buhay";
        return "Natapos na ang palaro";
    }

    private void saveCorrectBaguhanAnswer(String uid, String firebaseUID, String questionId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(firebaseUID).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) {
                return;
            }

            String studentId = studentDoc.getString("studentId");
            DocumentReference docRef = db.collection("palaro_answered").document(firebaseUID);

            docRef.get().addOnSuccessListener(answerDoc -> {
                boolean alreadyAnswered = false;

                if (answerDoc.exists()) {
                    Map<String, Object> baguhanMap = (Map<String, Object>) answerDoc.get("baguhan");
                    if (baguhanMap != null && Boolean.TRUE.equals(baguhanMap.get(questionId))) {
                        alreadyAnswered = true;
                    }
                }

                if (!alreadyAnswered) {
                    Map<String, Object> nestedBaguhan = new HashMap<>();
                    nestedBaguhan.put(questionId, true);

                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentId);
                    update.put("baguhan", nestedBaguhan);

                    docRef.set(update, SetOptions.merge());
                }

            });
        });
    }

    private void unlockAchievementA8IfEligible() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA8";
        String achievementId = "A8";

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");
            String firebaseUID = studentDoc.getId();

            db.collection("baguhan").get().addOnSuccessListener(allQuestionsSnapshot -> {
                List<String> allQuestionIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : allQuestionsSnapshot) {
                    allQuestionIds.add(doc.getId());
                }

                db.collection("palaro_answered").document(firebaseUID).get().addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    Map<String, Object> answeredMap = (Map<String, Object>) userDoc.get("baguhan");
                    if (answeredMap == null) return;

                    List<String> answeredIds = new ArrayList<>(answeredMap.keySet());

                    if (answeredIds.containsAll(allQuestionIds)) {
                        db.collection("student_achievements").document(firebaseUID).get().addOnSuccessListener(achSnapshot -> {
                            Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                            if (achievements != null && achievements.containsKey(achievementCode)) {
                                return;
                            }

                            continueUnlockingAchievement(firebaseUID, achievementCode, achievementId);
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
                            showAchievementUnlockedDialog(title, R.drawable.achievement08);
                        }));
            });
        });
    }


    private void unlockPerfectStreakAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA4";
        String achievementId = "A4";

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(achSnapshot -> {
                Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                if (achievements != null && achievements.containsKey(achievementCode)) {
                    return;
                }

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
                                showAchievementUnlockedDialog(title,R.drawable.achievement04);
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

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50;
            dialog.getWindow().setAttributes(params);
        }

        dialog.setOnShowListener(d -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.achievement_pop);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        });

        dialog.show();
    }

    private void saveBaguhanScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        DocumentReference docRef = db.collection("minigame_progress").document(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_BAGUHAN, FieldValue.increment(baguhanScore));
        updates.put(FIELD_TOTAL, FieldValue.increment(baguhanScore));

        docRef.set(updates, SetOptions.merge()).addOnSuccessListener(aVoid -> {
            baguhanScore = 0;

            docRef.get().addOnSuccessListener(snapshot -> {
                int baguhan = getScore(snapshot, FIELD_BAGUHAN);
                int husay = getScore(snapshot, FIELD_HUSAY);
                int dalubhasa = getScore(snapshot, FIELD_DALUBHASA);

                int totalScore = baguhan + husay + dalubhasa;

                checkAndUnlock(docRef, totalScore, 400, FIELD_HUSAY_UNLOCKED, "Nabuksan na ang Husay!");
                checkAndUnlock(docRef, totalScore, 800, FIELD_DALUBHASA_UNLOCKED, "Nabuksan na ang Dalubhasa!");
            });
        });
    }

    private int getScore(DocumentSnapshot snapshot, String field) {
        return snapshot.contains(field) ? snapshot.getLong(field).intValue() : 0;
    }

    private void checkAndUnlock(DocumentReference docRef, int totalScore, int threshold, String unlockField, String message) {
        if (totalScore < threshold) return;

        docRef.get().addOnSuccessListener(snapshot -> {
            boolean alreadyUnlocked = snapshot.contains(unlockField) && Boolean.TRUE.equals(snapshot.getBoolean(unlockField));
            if (!alreadyUnlocked) {
                Map<String, Object> updates = new HashMap<>();
                updates.put(unlockField, true);

                docRef.set(updates, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                    if (countDownTimer != null) countDownTimer.cancel();
                    if (tts != null) tts.stop();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private MediaPlayer beepPlayer;

    private void showCountdownThenLoadQuestion() {
        db.collection("baguhan").get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    questionIds.add(doc.getId());
                }

                Collections.shuffle(questionIds);

                currentQuestionNumber = 0;

                if (beepPlayer == null) {
                    beepPlayer = MediaPlayer.create(this, R.raw.ready_go_new);
                }

                final Handler countdownHandler = new Handler();
                final int[] countdown = {3};

                countdownHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isGameOver) return;

                        if (countdown[0] > 0) {
                            baguhanQuestion.setText(String.valueOf(countdown[0]));

                            if (beepPlayer != null) {
                                beepPlayer.start();
                            }

                            countdown[0]--;
                            countdownHandler.postDelayed(this, 1000);
                        } else {
                            baguhanQuestion.setText("");
                            loadBaguhanQuestion();
                            startTimer(timeLeft);
                        }
                    }
                });
            }
        });
    }

    private void startTimer(long startTime) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(startTime, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;

                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent <= 25) {
                    updateTimerZone("RED", R.drawable.timer_color_red, R.raw.red_timer);
                } else if (percent <= 50) {
                    updateTimerZone("ORANGE", R.drawable.timer_color_orange, R.raw.orange_timer);
                } else {
                    updateTimerZone("GREEN", R.drawable.timer_color_green, R.raw.green_timer);
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                isTimeUp = true;
                disableAnswerSelection();
                loadCharacterLine(LINE_WRONG_2);
                TimerSoundUtils.stop();
                finishQuiz();
            }

        }.start();
    }

    private void updateTimerZone(String zone, int drawableRes, int soundRes) {
        if (!lastTimerZone.equals(zone)) {
            TimerSoundUtils.stop();
            timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, drawableRes));
            TimerSoundUtils.playTimerSound(PalaroBaguhan.this, soundRes, null);
            lastTimerZone = zone;
        }
    }

    private void resetForNextQuestion() {
        isAnswered = false;
        isTimeUp = false;
        selectedAnswer = null;
        unlockButton.setEnabled(true);
        resetAnswerBackgrounds();
    }

    private void resetAnswerBackgrounds() {
        for (Button btn : answerButtons) {
            btn.setBackgroundResource(R.drawable.answer_option_bg);
        }
    }

    private void disableAnswerSelection() {
        for (Button btn : answerButtons) {
            btn.setOnClickListener(null);
        }
        unlockButton.setEnabled(false);
    }


    private void deductHeart() {
        if (remainingHearts > 0) {
            remainingHearts--;
            heartIcons[remainingHearts].setVisibility(View.INVISIBLE);
            MediaPlayer heartPop = MediaPlayer.create(this, R.raw.heart_pop);
            heartPop.setOnCompletionListener(MediaPlayer::release);
            heartPop.start();

            if (remainingHearts == 0) {
                if (countDownTimer != null) countDownTimer.cancel();
                if (tts != null) tts.stop();
                disableAnswerSelection();
                finishQuiz();
            }
        }
    }

    private void stopAllSounds() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (beepPlayer != null) {
            if (beepPlayer.isPlaying()) {
                beepPlayer.stop();
            }
            beepPlayer.release();
            beepPlayer = null;
        }
    }

    private void loadBaguhanQuestion() {
        if (isGameOver) return;
        startTime = System.currentTimeMillis();

        if (currentQuestionNumber >= questionIds.size()) {
            if (tts != null) tts.stop();
            finishQuiz();
            return;
        }

        String questionDocId = questionIds.get(currentQuestionNumber);

        db.collection("baguhan").document(questionDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        finishQuiz();
                        return;
                    }

                    String question = documentSnapshot.getString("baguhan_question");

                    List<Object> rawchoices = (List<Object>) documentSnapshot.get("choices");
                    List<String> choices = new ArrayList<>();

                    if (rawchoices != null) {
                        for (Object obj : rawchoices) {
                            if (obj instanceof String) {
                                choices.add((String) obj);
                            }
                        }
                    }

                    if (question != null) {
                        runOnUiThread(() -> {
                            baguhanQuestion.setText(question);
                        });
                    }

                    if (choices.size() >= answerButtons.length) {
                        runOnUiThread(() -> {
                            for (int i = 0; i < answerButtons.length; i++) {
                                answerButtons[i].setVisibility(View.VISIBLE);
                                answerButtons[i].setText(choices.get(i));
                            }
                        });
                    }

                    isAnswered = false;
                    selectedAnswer = null;
                    resetAnswerBackgrounds();
                });
    }


    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        if (line != null) {
                            speakText(line);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        exitGame();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        TimerSoundUtils.stop();
        super.onDestroy();
    }

    private void speakText(String text) {
        if (tts != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showExitConfirmationDialog() {
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
            exitGame();
            backDialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            backDialog.dismiss();
        });

        backDialog.show();
    }

    private void playGameOverSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.game_over);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAllSounds();
        TimerSoundUtils.stop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (tts != null) tts.stop();
    }

}