package com.example.habiaral.Palaro;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.habiaral.KayarianNgPangungusap.Quiz.LangkapanQuiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
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
    private MediaPlayer mediaPlayer, beepPlayer;
    private String lastTimerZone = "";
    private Handler handler = new Handler();
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private Button[] answerButtons;
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
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_baguhan);

        ImageView imageView = findViewById(R.id.characterIcon);
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(getApplicationContext())
                    .asGif()
                    .load(R.drawable.idle)
                    .into(imageView);
        }

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

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {

                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = tts.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this,
                                "Hindi ma-open ang installer ng TTS.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Voice selected = null;
                    for (Voice v : tts.getVoices()) {
                        Locale vLocale = v.getLocale();
                        if (vLocale != null && vLocale.getLanguage().equals("fil")) {
                            selected = v;
                            break;
                        } else if (v.getName().toLowerCase().contains("fil")) {
                            selected = v;
                            break;
                        }
                    }
                    if (selected != null) {
                        tts.setVoice(selected);
                    }

                    tts.setSpeechRate(1.0f);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    String uid = auth.getCurrentUser().getUid();

                    DocumentReference docRef = db.collection("minigame_progress").document(uid);
                    docRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isFirstTime = documentSnapshot.getBoolean("baguhan_intro");

                            if (isFirstTime == null || isFirstTime) {
                                String text = getString(R.string.mcl1_line);
                                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MCL1_UTTERANCE");

                                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                    @Override
                                    public void onStart(String utteranceId) {}

                                    @Override
                                    public void onDone(String utteranceId) {
                                        if ("MCL1_UTTERANCE".equals(utteranceId)) {
                                            runOnUiThread(() -> showCountdownThenLoadQuestion());
                                        }
                                    }

                                    @Override
                                    public void onError(String utteranceId) {}
                                });

                                docRef.update("baguhan_intro", false);

                            } else {
                                showCountdownThenLoadQuestion();
                            }
                        } else {
                            String text = getString(R.string.mdcl1_line);
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MCL1_UTTERANCE");

                            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {}

                                @Override
                                public void onDone(String utteranceId) {
                                    if ("MCL1_UTTERANCE".equals(utteranceId)) {
                                        runOnUiThread(() -> showCountdownThenLoadQuestion());
                                    }
                                }

                                @Override
                                public void onError(String utteranceId) {}
                            });

                            Map<String, Object> data = new HashMap<>();
                            data.put("dalubhasa_intro", false);
                            docRef.set(data, SetOptions.merge());
                        }
                    });
                }

            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
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

        setButtonsEnabled(false);

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

                                    if (!isFinishing() && !isDestroyed()) {
                                        Glide.with(this).asGif()
                                                .load(R.drawable.right_1)
                                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                                .into(imageView);
                                    }

                                    new Handler().postDelayed(() -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            Glide.with(this).asGif()
                                                    .load(R.drawable.idle)
                                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                                    .into(imageView);
                                        }
                                    }, 3000);

                                    MediaPlayer correctSound = MediaPlayer.create(this, R.raw.correct);
                                    correctSound.setOnCompletionListener(MediaPlayer::release);
                                    correctSound.start();

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    saveCorrectBaguhanAnswer(uid, studentID, correctDocId);

                                    timeLeft = Math.min(timeLeft + 5000, TOTAL_TIME);
                                    startTimer(timeLeft);
                                } else {
                                    correctStreak = 0;
                                    deductHeart();
                                    MediaPlayer wrongSound = MediaPlayer.create(this, R.raw.wrong);
                                    wrongSound.setOnCompletionListener(MediaPlayer::release);
                                    wrongSound.start();

                                    if (!isFinishing() && !isDestroyed()) {
                                        Glide.with(this).asGif()
                                                .load(R.drawable.wrong)
                                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                                .into(imageView);
                                    }

                                    new Handler().postDelayed(() -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            Glide.with(this).asGif()
                                                    .load(R.drawable.idle)
                                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                                    .into(imageView);
                                        }
                                    }, 2300);

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

    private void setButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;

        for (Button btn : answerButtons) {
            btn.setEnabled(enabled);
            btn.setAlpha(alpha);
        }

        if (unlockButton != null) {
            unlockButton.setEnabled(enabled);
            unlockButton.setAlpha(alpha);
        }

        if (unlockButton1 != null) {
            unlockButton1.setEnabled(enabled);
            unlockButton1.setAlpha(alpha);
        }
    }

    private void exitGame() {
        if (isGameOver) return;
        isGameOver = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler = null;
            countdownRunnable = null;
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (beepPlayer != null) {
            try {
                if (beepPlayer.isPlaying()) beepPlayer.stop();
            } catch (IllegalStateException ignored) {}
            try { beepPlayer.release(); } catch (IllegalStateException ignored) {}
            beepPlayer = null;
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

                    docRef.set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                unlockAchievementA8IfEligible();
                            });                }

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
                        db.collection("student_achievements").document(studentId).get().addOnSuccessListener(achSnapshot -> {
                            if (achSnapshot.exists()) {
                                Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                                if (achievements != null && achievements.containsKey(achievementCode)) {
                                    return;
                                }
                            }

                            continueUnlockingAchievement(studentId, achievementCode, achievementId);
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
                            AchievementDialogUtils.showAchievementUnlockedDialog(PalaroBaguhan.this, title, R.drawable.achievement08);
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
                                AchievementDialogUtils.showAchievementUnlockedDialog(PalaroBaguhan.this, title, R.drawable.achievement04);
                            }));
                });
            });
        });
    }


    private void saveBaguhanScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists()) {
                Toast.makeText(this, "Student document not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = studentDoc.getString("studentId");
            if (studentId == null || studentId.isEmpty()) {
                Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference docRef = db.collection("minigame_progress").document(uid);

            docRef.get().addOnSuccessListener(snapshot -> {
                int husay = snapshot.contains(FIELD_HUSAY) ? snapshot.getLong(FIELD_HUSAY).intValue() : 0;
                int dalubhasa = snapshot.contains(FIELD_DALUBHASA) ? snapshot.getLong(FIELD_DALUBHASA).intValue() : 0;
                int oldBaguhan = snapshot.contains(FIELD_BAGUHAN) ? snapshot.getLong(FIELD_BAGUHAN).intValue() : 0;

                int newBaguhanTotal = oldBaguhan + baguhanScore;

                Map<String, Object> updates = new HashMap<>();
                updates.put(FIELD_BAGUHAN, newBaguhanTotal);
                updates.put("studentId", studentId);
                updates.put(FIELD_TOTAL, newBaguhanTotal + husay + dalubhasa);

                docRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            if (newBaguhanTotal >= 400) {
                                checkAndUnlock(docRef, newBaguhanTotal + husay + dalubhasa, 400, FIELD_HUSAY_UNLOCKED, "Nabuksan na ang Husay!");
                            }
                            if (newBaguhanTotal + husay + dalubhasa >= 800) {
                                checkAndUnlock(docRef, newBaguhanTotal + husay + dalubhasa, 800, FIELD_DALUBHASA_UNLOCKED, "Nabuksan na ang Dalubhasa!");
                            }
                            baguhanScore = 0;
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error saving score", Toast.LENGTH_SHORT).show();
                        });
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

    private void showCountdownThenLoadQuestion() {

        db.collection("baguhan").get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    questionIds.add(doc.getId());
                }

                Collections.shuffle(questionIds);

                currentQuestionNumber = 0;

                countdownHandler = new Handler();
                final int[] countdown = {3};

                countdownHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isGameOver) return;

                        if (countdown[0] > 0) {
                            baguhanQuestion.setText(String.valueOf(countdown[0]));

                            try {
                                if (beepPlayer != null) {
                                    if (beepPlayer.isPlaying()) beepPlayer.stop();
                                    beepPlayer.release();
                                    beepPlayer = null;
                                }
                            } catch (IllegalStateException ignored) {}

                            beepPlayer = MediaPlayer.create(PalaroBaguhan.this, R.raw.beep);
                            if (beepPlayer != null) {
                                beepPlayer.setOnCompletionListener(mp -> {
                                    try { mp.release(); } catch (IllegalStateException ignored) {}
                                    beepPlayer = null;
                                });
                                beepPlayer.start();
                            };

                            countdown[0]--;
                            countdownHandler.postDelayed(this, 1000);
                        } else {
                            baguhanQuestion.setText("");

                            try {
                                if (beepPlayer != null) {
                                    if (beepPlayer.isPlaying()) beepPlayer.stop();
                                    beepPlayer.release();
                                    beepPlayer = null;
                                }
                            } catch (IllegalStateException ignored) {}

                            setButtonsEnabled(true);
                            loadBaguhanQuestion();
                            startTimer(timeLeft);
                        }
                    }
                });
                countdownHandler.post(countdownRunnable);
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
                    if (!questionIds.get(currentQuestionNumber).equals(questionDocId)) {
                        return;
                    }
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
                        if (isFinishing() || isDestroyed()) return;
                        runOnUiThread(() -> baguhanQuestion.setText(question));
                    }

                    if (choices.size() >= answerButtons.length) {
                        if (!isFinishing() && !isDestroyed()) {
                            runOnUiThread(() -> {
                                for (int i = 0; i < answerButtons.length; i++) {
                                    answerButtons[i].setVisibility(View.VISIBLE);
                                    answerButtons[i].setText(choices.get(i));
                                }
                            });
                        }

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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        TimerSoundUtils.stop();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler = null;
            countdownRunnable = null;
        }

        if (beepPlayer != null) {
            try {
                if (beepPlayer.isPlaying()) beepPlayer.stop();
            } catch (IllegalStateException ignored) {}
            try { beepPlayer.release(); } catch (IllegalStateException ignored) {}
            beepPlayer = null;
        }
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

        try { if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.setVolume(0f, 0f); } catch (IllegalStateException ignored) {}
        TimerSoundUtils.setVolume(0f);

        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }

        if (beepPlayer != null) {
            try {
                if (beepPlayer.isPlaying()) beepPlayer.stop();
            } catch (IllegalStateException ignored) {}
            try { beepPlayer.release(); } catch (IllegalStateException ignored) {}
            beepPlayer = null;
        }

        if (tts != null) tts.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1f, 1f);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        try {
            if (beepPlayer != null) {
                beepPlayer.setVolume(1f, 1f);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        TimerSoundUtils.setVolume(1f);
    }

    private void endAllAndFinish() {
        try {
            if (!isGameOver) {
                exitGame();
            } else {
                if (countDownTimer != null) {
                    try {
                        countDownTimer.cancel();
                    } catch (Exception ignored) {}
                    countDownTimer = null;
                }

                try { TimerSoundUtils.stop(); } catch (Exception ignored) {}
                stopAllSounds();

                try {
                    if (tts != null) {
                        tts.stop();
                    }
                } catch (Exception ignored) {}
            }


            try {
                if (connectivityManager != null && networkCallback != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                    networkCallback = null;
                }
            } catch (Exception ignored) {}

            if (!isFinishing()) {
                finish();
            }
        } catch (Exception e) {
            try { if (!isFinishing()) finish(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        endAllAndFinish();

        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler = null;
            countdownRunnable = null;
        }

        if (beepPlayer != null) {
            try {
                if (beepPlayer.isPlaying()) beepPlayer.stop();
            } catch (IllegalStateException ignored) {}
            try { beepPlayer.release(); } catch (IllegalStateException ignored) {}
            beepPlayer = null;
        }
    }
}