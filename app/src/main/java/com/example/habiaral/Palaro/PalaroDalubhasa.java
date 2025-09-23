package com.example.habiaral.Palaro;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.habiaral.GrammarChecker.GrammarChecker;
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.InternetCheckerUtils;
import com.example.habiaral.Utils.TimerSoundUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class PalaroDalubhasa extends AppCompatActivity {

    private TextView dalubhasaInstruction;
    private TextView grammarFeedbackText;
    private EditText userSentenceInput;
    private ProgressBar timerBar;
    private Button btnTapos;
    private Button btnSuriin;
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;
    private FirebaseFirestore db;
    private boolean hasSubmitted = false;
    private int currentErrorCount = 0;
    private int dalubhasaScore = 0;
    private List<String> instructionList = new ArrayList<>();
    private List<List<String>> keywordList = new ArrayList<>();
    private int currentQuestionNumber = 0;
    private List<String> currentKeywords = new ArrayList<>();
    private String currentDalubhasaID = "";
    private int remainingHearts = 5;
    private ImageView[] heartIcons;
    private boolean isTtsReady = false;
    private TextToSpeech tts;
    private int perfectAnswerCount = 0;
    private boolean isGameOver = false;
    private boolean greenSound = false;
    private boolean redSound = false;
    private boolean orangeSound = false;
    private ImageView errorIcon;
    private TextView errorTooltip;
    private MediaPlayer greenTimerSoundPlayer;
    private MediaPlayer orangeTimerSoundPlayer;
    private MediaPlayer redTimerSoundPlayer;
    private String lastTimerZone = "";
    private ImageView characterIcon;
    private Handler internetHandler = new Handler();
    private Runnable internetRunnable;
    private Handler errorTooltipHandler = new Handler();
    private Runnable hideErrorTooltipRunnable = () -> errorTooltip.setVisibility(View.GONE);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_dalubhasa);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true;
                Locale tagalogLocale = new Locale("fil", "PH");
                tts.setLanguage(tagalogLocale);
                tts.setSpeechRate(1.3f);

                new Handler().postDelayed(() -> loadCharacterLine("MDCL1"), 300);
                new Handler().postDelayed(this::showCountdownThenLoadInstruction, 4000);
            }
        });

        dalubhasaInstruction = findViewById(R.id.dalubhasa_instructionText);
        grammarFeedbackText = findViewById(R.id.grammar_feedback);
        userSentenceInput = findViewById(R.id.dalubhasa_answer);
        timerBar = findViewById(R.id.timerBar);
        btnTapos = findViewById(R.id.UnlockButtonPalaro);
        btnSuriin = findViewById(R.id.btnSuriin);
        Button btnUmalis = findViewById(R.id.UnlockButtonPalaro1);
        errorIcon = findViewById(R.id.error_icon);
        errorTooltip = findViewById(R.id.errorTooltip);
        characterIcon = findViewById(R.id.characterIcon);

        Glide.with(this).asGif().load(R.drawable.idle).into(characterIcon);

        btnUmalis.setOnClickListener(v -> {
            playButtonClickSound();
            showUmalisDialog();
        });

        heartIcons = new ImageView[]{
                findViewById(R.id.heart01),
                findViewById(R.id.heart02),
                findViewById(R.id.heart03),
                findViewById(R.id.heart04),
                findViewById(R.id.heart05)
        };

        db = FirebaseFirestore.getInstance();

        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);

        btnTapos.setOnClickListener(v -> {
            if (hasSubmitted) {

                nextQuestion();
                userSentenceInput.setEnabled(true);
                userSentenceInput.setText("");
                btnTapos.setEnabled(true);

                String firebaseUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String questionId = currentDalubhasaID;
                saveDalubhasaAnswer(firebaseUID, questionId);

                hasSubmitted = false;
            } else {
                showErrorTooltip("Pakipindot ang Enter para i-check ang grammar muna.");
            }
        });

        btnSuriin.setOnClickListener(v -> {
            playButtonClickSound();
            checkGrammar();
        });

        userSentenceInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                checkGrammar();
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                playButtonClickSound();
                showBackConfirmationDialog();
            }
        });

    }

    private void checkGrammar() {
        String sentence = userSentenceInput.getText().toString().trim();
        if (sentence.isEmpty()) {
            showErrorTooltip("Pakisulat ang iyong pangungusap.");
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.wrong)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(characterIcon);
            new Handler().postDelayed(() -> {
                Glide.with(this)
                        .asGif()
                        .load(R.drawable.idle)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
            }, 2300);

            return;
        } else if (!sentence.endsWith(".")) {
            showErrorTooltip("Siguraduhing nagtatapos ang pangungusap sa tuldok (.)");

            Glide.with(this)
                    .asGif()
                    .load(R.drawable.wrong)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(characterIcon);
            new Handler().postDelayed(() -> {
                Glide.with(this)
                        .asGif()
                        .load(R.drawable.idle)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
            }, 2300);

            return;
        }

        List<String> missingKeywords = new ArrayList<>();
        for (String keyword : currentKeywords) {
            if (!sentence.toLowerCase().contains(keyword.toLowerCase())) {
                missingKeywords.add(keyword);
            }
        }

        if (!missingKeywords.isEmpty()) {
            String details = "Kulang: " + String.join(", ", missingKeywords);
            errorIcon.setVisibility(View.VISIBLE);
            errorTooltip.setVisibility(View.VISIBLE);
            errorTooltip.setText(details);
            speakLine("Wala ka ng kinakailangang bahagi sa iyong sagot. Pakibasa muli ang mga panuto.");
            loadCharacterLine(currentDalubhasaID);

            Glide.with(this)
                    .asGif()
                    .load(R.drawable.wrong)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(characterIcon);
            new Handler().postDelayed(() -> {
                Glide.with(this)
                        .asGif()
                        .load(R.drawable.idle)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
            }, 2300);

            return;
        }

        GrammarChecker.checkGrammar(this, sentence, new GrammarChecker.GrammarCallback() {
            @Override
            public void onResult(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray matches = jsonObject.getJSONArray("matches");
                        currentErrorCount = matches.length();

                        if (matches.length() > 0 && !hasSubmitted) {
                            Toast.makeText(PalaroDalubhasa.this, "Mali ang grammar! Heart deducted.", Toast.LENGTH_SHORT).show();
                            deductHeart();

                            // Stop timer sounds after error
                            stopAllTimerSounds(); // <--- ADD THIS
                        }

                        highlightGrammarIssues(response, sentence);
                        hasSubmitted = true;
                        userSentenceInput.setEnabled(false);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        grammarFeedbackText.setText("Invalid grammar response format.");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PalaroDalubhasa.this, "Grammar Check Failed: " + error, Toast.LENGTH_SHORT).show();

                    Glide.with(PalaroDalubhasa.this)
                            .asGif()
                            .load(R.drawable.wrong)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(characterIcon);
                    new Handler().postDelayed(() -> {
                        Glide.with(PalaroDalubhasa.this)
                                .asGif()
                                .load(R.drawable.idle)
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .into(characterIcon);
                    }, 2300);
                });
            }
        });
    }

    // Add this helper to restart the timer and sound
    private void restartTimer() {
        // Stop any existing timer and sound
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopAllTimerSounds();
        startTimer();
    }

    private void showErrorTooltip(String message) {
        errorTooltip.setText(message);
        errorIcon.setVisibility(View.VISIBLE);
        errorTooltip.setVisibility(View.VISIBLE);

        // Cancel any previous hide requests
        errorTooltipHandler.removeCallbacks(hideErrorTooltipRunnable);

        // Schedule hide after 2.5 seconds
        errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 2500);

        errorIcon.setOnClickListener(v -> {
            if (errorTooltip.getVisibility() == View.VISIBLE) {
                errorTooltip.setVisibility(View.GONE);
            } else {
                errorTooltip.setVisibility(View.VISIBLE);
                // Re-schedule auto-hide
                errorTooltipHandler.removeCallbacks(hideErrorTooltipRunnable);
                errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 2500);
            }
        });
    }


    private void showUmalisDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PalaroDalubhasa.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);

        Button btnOo = dialogView.findViewById(R.id.button5);
        Button btnHindi = dialogView.findViewById(R.id.button6);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnOo.setOnClickListener(v -> {
            playButtonClickSound();

            if (countDownTimer != null) countDownTimer.cancel();
            if (tts != null) tts.stop();
            dialog.dismiss();
            finish();
        });

        btnHindi.setOnClickListener(v -> {
            playButtonClickSound();
            dialog.dismiss();
        });

        dialog.show();
    }


    private void highlightGrammarIssues(String response, String originalSentence) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray matches = jsonObject.getJSONArray("matches");
            currentErrorCount = matches.length();
            int scoreForThisSentence;

            if (matches.length() == 0) {
                scoreForThisSentence = 15;
                dalubhasaScore += scoreForThisSentence;
                perfectAnswerCount++;
                playCorrectSound();

                // **Stop timer and sounds**
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                stopAllTimerSounds();

                // **Toggle buttons**
                btnSuriin.setVisibility(View.GONE);
                btnTapos.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .asGif()
                        .load(R.drawable.right_1)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
                new Handler().postDelayed(() -> {
                    Glide.with(this)
                            .asGif()
                            .load(R.drawable.idle)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(characterIcon);
                }, 3000);

                loadCharacterLine("MDCL2");
            } else {
                if (matches.length() == 1) {
                    scoreForThisSentence = 13;
                } else {
                    scoreForThisSentence = 10;
                }

                dalubhasaScore += scoreForThisSentence;

                playWrongSound();
                Glide.with(this)
                        .asGif()
                        .load(R.drawable.wrong)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
                new Handler().postDelayed(() -> {
                    Glide.with(this)
                            .asGif()
                            .load(R.drawable.idle)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(characterIcon);
                }, 2300);

                if (scoreForThisSentence == 13 || scoreForThisSentence == 15) {
                    perfectAnswerCount++;
                } else {
                    perfectAnswerCount = 0;
                }

                if (perfectAnswerCount >= 5) {
                    unlockSagotBayaniAchievement();
                }

                loadCharacterLine("MDCL3");

                new Handler().postDelayed(() -> {
                    try {
                        SpannableStringBuilder builder = new SpannableStringBuilder();
                        builder.append("Maling salita o grammar:\n\n");

// Highlight mali sa sentence
                        SpannableString highlightedSentence = new SpannableString(originalSentence);
                        for (int i = 0; i < matches.length(); i++) {
                            JSONObject match = matches.getJSONObject(i);
                            int offset = match.getInt("offset");
                            int length = match.getInt("length");
                            highlightedSentence.setSpan(new UnderlineSpan(), offset, offset + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            highlightedSentence.setSpan(new ForegroundColorSpan(Color.RED), offset, offset + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        builder.append(highlightedSentence);
                        builder.append("\n\n");

// Add per-word explanations
                        for (int i = 0; i < matches.length(); i++) {
                            JSONObject match = matches.getJSONObject(i);
                            int offset = match.getInt("offset");
                            int length = match.getInt("length");
                            String wrongWord = originalSentence.substring(offset, offset + length);
                            String message = match.optString("message", "Error sa grammar");
                            builder.append("- ").append(wrongWord).append(": ").append(message).append("\n");
                        }

                        errorTooltip.setText(builder);
                        errorTooltip.setVisibility(View.VISIBLE);
                        errorIcon.setVisibility(View.VISIBLE);

                        errorTooltipHandler.removeCallbacks(hideErrorTooltipRunnable);
                        errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 3500);

                        errorIcon.setOnClickListener(v -> {
                            if (errorTooltip.getVisibility() == View.VISIBLE) {
                                errorTooltip.setVisibility(View.GONE);
                            } else {
                                errorTooltip.setVisibility(View.VISIBLE);
                            }
                        });

                        new Handler().postDelayed(this::nextQuestion, 4000);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        grammarFeedbackText.setText("Error habang hinahati ang mga mali.");
                    }
                }, 2000);
            }


        } catch (JSONException e) {
            e.printStackTrace();
            grammarFeedbackText.setText("Invalid grammar response format.");
        }
    }

    private void showBackConfirmationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button yesButton = dialogView.findViewById(R.id.button5);
        Button noButton = dialogView.findViewById(R.id.button6);

        yesButton.setOnClickListener(v -> {
            playButtonClickSound();
            if (countDownTimer != null) countDownTimer.cancel();
            if (tts != null) tts.stop();
            dialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> {
            playButtonClickSound();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");

                        if (line != null && !line.isEmpty()) {
                            speakLine(line);
                        }
                    }
                });
    }

    private void loadAllDalubhasaInstructions() {
        db.collection("dalubhasa")
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        instructionList.clear();
                        keywordList.clear();

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String instruction = document.getString("instruction");
                            List<String> keywords = (List<String>) document.get("keywords");

                            instructionList.add(instruction);
                            keywordList.add(keywords);
                        }

                        // **Shuffle questions**
                        shuffleQuestions();

                        nextQuestion();
                    }
                });
    }

    private void shuffleQuestions() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < instructionList.size(); i++) {
            indices.add(i);
        }
        java.util.Collections.shuffle(indices);

        List<String> shuffledInstructions = new ArrayList<>();
        List<List<String>> shuffledKeywords = new ArrayList<>();

        for (int idx : indices) {
            shuffledInstructions.add(instructionList.get(idx));
            shuffledKeywords.add(keywordList.get(idx));
        }

        instructionList = shuffledInstructions;
        keywordList = shuffledKeywords;
    }

    private void speakLine(String text) {
        if (text == null || text.trim().isEmpty()) return;

        if (isTtsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void nextQuestion() {
        if (isGameOver) return;

        if (currentQuestionNumber < instructionList.size()) {
            String instruction = instructionList.get(currentQuestionNumber);
            dalubhasaInstruction.setText(instruction);
            currentKeywords = keywordList.get(currentQuestionNumber);
            currentDalubhasaID = "D" + (currentQuestionNumber + 1);

            userSentenceInput.setText("");
            userSentenceInput.setEnabled(true);

            // **Reset buttons visibility**
            btnSuriin.setVisibility(View.VISIBLE);
            btnTapos.setVisibility(View.GONE);

            btnTapos.setEnabled(true);
            hasSubmitted = false;
            grammarFeedbackText.setText("");
            currentQuestionNumber++;

            startTimer();
        } else {
            if (countDownTimer != null) countDownTimer.cancel();
            saveDalubhasaScore();
            finishQuiz();
        }
    }

    private void showCountdownThenLoadInstruction() {
        // Use a short beep sound for each countdown tick
        final MediaPlayer[] beepPlayer = {null};

        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    dalubhasaInstruction.setText(String.valueOf(countdown[0]));

                    // Play beep sound
                    if (beepPlayer[0] != null) {
                        beepPlayer[0].release();
                    }
                    beepPlayer[0] = MediaPlayer.create(PalaroDalubhasa.this, R.raw.beep);
                    beepPlayer[0].setOnCompletionListener(mp -> mp.release());
                    beepPlayer[0].start();

                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    dalubhasaInstruction.setText("");
                    // Release beep player if still exists
                    if (beepPlayer[0] != null) {
                        beepPlayer[0].release();
                        beepPlayer[0] = null;
                    }
                    loadAllDalubhasaInstructions();
                    startTimer();
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void startTimer() {

        timeLeft = TOTAL_TIME;
        lastTimerZone = "";

        if (countDownTimer != null) countDownTimer.cancel();
        stopAllTimerSounds();

        countDownTimer = new CountDownTimer(TOTAL_TIME, 100) {
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
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);

                stopAllTimerSounds();

                loadCharacterLine("MCL5");
                saveDalubhasaScore();
            }
        }.start();
    }

    private void updateTimerZone(String zone, int drawableRes, int soundRes) {
        if (!lastTimerZone.equals(zone)) {
            stopAllTimerSounds();
            timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, drawableRes));
            playTimerSound(soundRes);
            lastTimerZone = zone;
        }
    }

    private void stopAllTimerSounds() {
        if (greenTimerSoundPlayer != null) { greenTimerSoundPlayer.stop(); greenTimerSoundPlayer.release(); greenTimerSoundPlayer = null; }
        if (orangeTimerSoundPlayer != null) { orangeTimerSoundPlayer.stop(); orangeTimerSoundPlayer.release(); orangeTimerSoundPlayer = null; }
        if (redTimerSoundPlayer != null) { redTimerSoundPlayer.stop(); redTimerSoundPlayer.release(); redTimerSoundPlayer = null; }

        greenSound = false;
        orangeSound = false;
        redSound = false;
    }

    private void playTimerSound(int soundResId) {
        stopAllTimerSounds();

        MediaPlayer mp = MediaPlayer.create(this, soundResId);

        if (soundResId == R.raw.green_timer) greenTimerSoundPlayer = mp;
        else if (soundResId == R.raw.orange_timer) orangeTimerSoundPlayer = mp;
        else if (soundResId == R.raw.red_timer) redTimerSoundPlayer = mp;

        mp.setOnCompletionListener(mediaPlayer -> {
            mediaPlayer.release();
            if (soundResId == R.raw.green_timer) greenTimerSoundPlayer = null;
            else if (soundResId == R.raw.orange_timer) orangeTimerSoundPlayer = null;
            else if (soundResId == R.raw.red_timer) redTimerSoundPlayer = null;
        });

        mp.start();
    }

    private void finishQuiz() {
        if (isGameOver) return;
        isGameOver = true;

        MediaPlayer gameOverSound = MediaPlayer.create(this, R.raw.game_over);
        gameOverSound.setOnCompletionListener(MediaPlayer::release);
        gameOverSound.start();

        View showTotalPoints = getLayoutInflater().inflate(R.layout.dialog_box_time_up, null);
        AlertDialog dialog = new AlertDialog.Builder(PalaroDalubhasa.this)
                .setView(showTotalPoints)
                .setCancelable(false)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView scoreTextDialog = showTotalPoints.findViewById(R.id.scoreText);
        scoreTextDialog.setText(String.valueOf(dalubhasaScore));

        Button balik = showTotalPoints.findViewById(R.id.btn_balik);
        balik.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }

    private void saveDalubhasaScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || dalubhasaScore <= 0) return;

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

            Map<String, Object> updates = new HashMap<>();
            updates.put("dalubhasa_score", com.google.firebase.firestore.FieldValue.increment(dalubhasaScore));
            updates.put("total_score", com.google.firebase.firestore.FieldValue.increment(dalubhasaScore));
            updates.put("studentId", studentId);

            docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        dalubhasaScore = 0;

                        docRef.get().addOnSuccessListener(snapshot -> {
                            int totalScore = snapshot.contains("total_score") ? snapshot.getLong("total_score").intValue() : 0;
                        });
                    });
        });
    }

    private void deductHeart() {
        if (remainingHearts > 0) {
            remainingHearts--;
            MediaPlayer heartPop = MediaPlayer.create(this, R.raw.heart_pop);
            heartPop.setOnCompletionListener(MediaPlayer::release);
            heartPop.start();

            if (heartIcons != null && remainingHearts >= 0 && remainingHearts < heartIcons.length) {
                heartIcons[remainingHearts].setVisibility(View.INVISIBLE);
            }

            if (remainingHearts == 0) {
                Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();

                // Stop timer and sounds
                if (countDownTimer != null) countDownTimer.cancel();
                stopAllTimerSounds(); // <--- ADD THIS

                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);

                loadCharacterLine("MCL5");

                saveDalubhasaScore();
                finishQuiz();
            }
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        stopAllTimerSounds();

        if (internetHandler != null && internetRunnable != null) {
            internetHandler.removeCallbacks(internetRunnable);
        }

        if (errorTooltipHandler != null) {
            errorTooltipHandler.removeCallbacksAndMessages(null);
        }
    }

    private void saveDalubhasaAnswer(String firebaseUID, String questionId) {
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
                    Map<String, Object> dalubhasaMap = (Map<String, Object>) answerDoc.get("dalubhasa");
                    if (dalubhasaMap != null && Boolean.TRUE.equals(dalubhasaMap.get(questionId))) {
                        alreadyAnswered = true;
                    }
                }

                if (!alreadyAnswered) {
                    Map<String, Object> nestedDalubhasa = new HashMap<>();
                    nestedDalubhasa.put(questionId, true);

                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentId);
                    update.put("dalubhasa", nestedDalubhasa);

                    docRef.set(update, SetOptions.merge());
                    checkAndUnlockQuestionBankMaster();
                }
            });
        });
    }

    private void unlockQuestionBankMasterAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA10";
        String achievementId = "A10";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                                AchievementDialogUtils.showAchievementUnlockedDialog(PalaroDalubhasa.this, title, R.drawable.achievement10);
                            }));
                });
            });
        });
    }

    private void checkAndUnlockQuestionBankMaster() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("dalubhasa").get().addOnSuccessListener(questionSnapshot -> {
            int totalQuestions = questionSnapshot.size();
            if (totalQuestions == 0) return;

            db.collection("palaro_answered").document(uid).get().addOnSuccessListener(answerDoc -> {
                if (!answerDoc.exists()) return;

                Map<String, Object> dalubhasaMap = (Map<String, Object>) answerDoc.get("dalubhasa");
                if (dalubhasaMap == null) return;

                long answeredCount = dalubhasaMap.values().stream()
                        .filter(val -> val instanceof Boolean && (Boolean) val)
                        .count();

                if (answeredCount >= totalQuestions) {
                    unlockQuestionBankMasterAchievement();
                }
            });
        });
    }


    private void unlockSagotBayaniAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA3";
        String achievementId = "A3";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                            .addOnSuccessListener(unused -> runOnUiThread(() ->{
                                AchievementDialogUtils.showAchievementUnlockedDialog(PalaroDalubhasa.this, title, R.drawable.achievement03);
                            }));
                });
            });
        });

    }
    private void playButtonClickSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.button_click);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }
    private void playCorrectSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    private void playWrongSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.wrong);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (greenTimerSoundPlayer != null) greenTimerSoundPlayer.setVolume(0f, 0f);
        if (orangeTimerSoundPlayer != null) orangeTimerSoundPlayer.setVolume(0f, 0f);
        if (redTimerSoundPlayer != null) redTimerSoundPlayer.setVolume(0f, 0f);

        TimerSoundUtils.setVolume(0f);

        if (tts != null) tts.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (greenTimerSoundPlayer != null) greenTimerSoundPlayer.setVolume(1f, 1f);
        if (orangeTimerSoundPlayer != null) orangeTimerSoundPlayer.setVolume(1f, 1f);
        if (redTimerSoundPlayer != null) redTimerSoundPlayer.setVolume(1f, 1f);

        TimerSoundUtils.setVolume(0.2f);
    }


}