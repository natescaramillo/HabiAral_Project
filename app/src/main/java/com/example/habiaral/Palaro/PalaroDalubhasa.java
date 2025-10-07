package com.example.habiaral.Palaro;

import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.View;
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
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.TimerSoundUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PalaroDalubhasa extends AppCompatActivity {

    private TextView dalubhasaInstruction, grammarFeedbackText, errorTooltip;
    private EditText userSentenceInput;
    private ProgressBar timerBar;
    private Button btnTapos, btnUmalis, btnSuriin;
    private ImageView[] heartIcons;
    private ImageView errorIcon, characterIcon;
    private FirebaseFirestore db;
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;
    private int dalubhasaScore = 0, currentErrorCount = 0, currentQuestionNumber = 0;
    private int remainingHearts = 5, perfectAnswerCount = 0;
    private boolean hasSubmitted = false, isTtsReady = false, isGameOver = false;
    private String currentDalubhasaID = "", lastTimerZone = "";
    private List<String> instructionList = new ArrayList<>();
    private List<List<String>> keywordList = new ArrayList<>();
    private List<String> currentKeywords = new ArrayList<>();
    private TextToSpeech tts;
    private MediaPlayer countdownBeepPlayer, timerSoundPlayer;
    private boolean isShowingErrorTooltip = false;
    private Handler countdownHandler, internetHandler = new Handler(), errorTooltipHandler = new Handler();
    private Runnable countdownRunnable, internetRunnable;
    private final Runnable hideErrorTooltipRunnable = () -> {
        if (errorTooltip != null) {
            errorTooltip.setVisibility(View.GONE);
        }
        if (errorIcon != null) {
            errorIcon.setVisibility(View.GONE);
        }
        isShowingErrorTooltip = false;
    };

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean hasSpokenMDCL1 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_dalubhasa);

        initNetworkCallback();
        initTTS();
        initViews();
        initButtons();
        initInputActions();
        initBackPressedHandler();
    }

    private void initNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(android.net.Network network) {
                    runOnUiThread(() -> finishQuiz());
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
                return;
            }

            isTtsReady = true;
            setupTTSLanguage();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();
            String uid = auth.getCurrentUser().getUid();
            DocumentReference docRef = db.collection("minigame_progress").document(uid);

            docRef.get().addOnSuccessListener(doc -> {
                boolean isFirstTime = doc.getBoolean("dalubhasa_intro") == null || doc.getBoolean("dalubhasa_intro");
                if (isFirstTime) {
                    speakIntroThenStart();
                    docRef.update("dalubhasa_intro", false);
                } else {
                    showCountdownThenLoadInstruction();
                }
            }).addOnFailureListener(e -> showCountdownThenLoadInstruction());
        });
    }

    private void setupTTSLanguage() {
        Locale tagalog = new Locale("fil", "PH");
        int result = tts.setLanguage(tagalog);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(new Locale("tl", "PH"));
        }

        for (Voice v : tts.getVoices()) {
            Locale l = v.getLocale();
            if (l != null && ("fil".equals(l.getLanguage()) || "tl".equals(l.getLanguage()))) {
                tts.setVoice(v);
                break;
            }
        }
        tts.setSpeechRate(1.0f);
    }

    private void speakIntroThenStart() {
        String text = getString(R.string.mdcl1_line);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MDCL1_UTTERANCE");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onError(String id) {}
            @Override
            public void onDone(String id) {
                if ("MDCL1_UTTERANCE".equals(id)) runOnUiThread(() -> showCountdownThenLoadInstruction());
            }
        });
    }

    private void initViews() {
        dalubhasaInstruction = findViewById(R.id.dalubhasa_instructionText);
        grammarFeedbackText = findViewById(R.id.grammar_feedback);
        userSentenceInput = findViewById(R.id.dalubhasa_answer);
        timerBar = findViewById(R.id.timerBar);
        btnTapos = findViewById(R.id.UnlockButtonPalaro);
        btnSuriin = findViewById(R.id.btnSuriin);
        btnUmalis = findViewById(R.id.UnlockButtonPalaro1);
        errorIcon = findViewById(R.id.error_icon);
        errorTooltip = findViewById(R.id.errorTooltip);
        characterIcon = findViewById(R.id.characterIcon);

        setButtonsEnabled(false);
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this).asGif().load(R.drawable.idle).into(characterIcon);
        }

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
        btnTapos.setAlpha(0.5f);
    }

    private void initButtons() {
        btnUmalis.setOnClickListener(v -> {
            playButtonClickSound();
            showUmalisDialog();
        });

        btnTapos.setOnClickListener(v -> {
            playButtonClickSound();
            if (hasSubmitted) {
                if (countDownTimer != null) countDownTimer.cancel();
                stopAllTimerSounds();
                startTimer();
                nextQuestion();
                if (userSentenceInput != null) {
                    userSentenceInput.setEnabled(true);
                    userSentenceInput.setFocusableInTouchMode(true);
                }
                userSentenceInput.setText("");
                btnTapos.setEnabled(true);
                btnTapos.setAlpha(1.0f);

                saveDalubhasaAnswer(FirebaseAuth.getInstance().getCurrentUser().getUid(), currentDalubhasaID);
                hasSubmitted = false;
            } else {
                showErrorTooltip("Pakipindot ang Enter para i-check ang grammar muna.");
            }
        });

        btnSuriin.setOnClickListener(v -> {
            playButtonClickSound();
            checkGrammar();
        });
    }

    private void initInputActions() {
        userSentenceInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                checkGrammar();
                return true;
            }
            return false;
        });
    }

    private void initBackPressedHandler() {
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
            showInputError("Pakisulat ang iyong pangungusap.");
            return;
        }
        if (!sentence.endsWith(".")) {
            showInputError("Siguraduhing nagtatapos ang pangungusap sa tuldok (.)");
            return;
        }

        int wordCount = countWordsExcludingKeywords(sentence, currentKeywords);
        if (wordCount > 10) {
            showInputError("Pinakamataas na bilang ay sampung salita, ngunit hindi kabilang ang panuto.");
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String keyword : currentKeywords)
            if (!sentence.toLowerCase().contains(keyword.toLowerCase()))
                missing.add(keyword);

        if (!missing.isEmpty()) {
            String details = "Kulang: " + String.join(", ", missing);
            showErrorTooltip(details);
            showWrongAnimation();
            loadCharacterLine("MDCL4");
            return;
        }

        String cleaned = sentence.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().trim();
        for (String keyword : currentKeywords)
            cleaned = cleaned.replace(keyword.toLowerCase().trim(), "").trim();

        if (cleaned.isBlank()) {
            showErrorTooltip("Hindi maaaring basta isulat lamang ang nakasaad sa panuto. Kailangang dagdagan ito ng mga pangungusap.");
            deductHeart();
            return;
        }

        showErrorTooltip("Sinusuri...");
        if (userSentenceInput != null) {
            userSentenceInput.setEnabled(false);
            userSentenceInput.setFocusable(false);
        }
        if (btnSuriin != null) {
            btnSuriin.setEnabled(false);
            btnSuriin.setAlpha(0.5f);
        }
        checkGrammarFromServer(sentence);
    }

    private void showGrammarResultTooltip(String res) {
        if (res == null || res.isEmpty()) {
            showErrorTooltip("Walang sagot mula sa server.");
            return;
        }

        boolean hasError = res.contains("MALI:");

        res = res.replace("TAMANG SAGOT:", "\nTAMANG SAGOT:");

        SpannableString spannable = new SpannableString(res);
        String[] headers = {"MALI:", "TAMANG SAGOT:"};

        for (String header : headers) {
            int start = res.indexOf(header);
            if (start >= 0) {
                int end = start + header.length();
                spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        errorTooltip.setText(spannable);
        errorTooltip.setTextSize(17);
        errorTooltip.setVisibility(View.VISIBLE);
        errorIcon.setVisibility(View.VISIBLE);

        errorTooltipHandler.removeCallbacksAndMessages(null);
        isShowingErrorTooltip = true;

        if (hasError) {
            playWrongSound();
            showWrongAnimation();

            if (countDownTimer != null) countDownTimer.cancel();
            stopAllTimerSounds();
            loadCharacterLine("MDCL3");

            deductHeart();

            if (isGameOver || remainingHearts == 0) {
                return;
            }

            if (userSentenceInput != null) {
                userSentenceInput.setEnabled(false);
                userSentenceInput.setFocusable(false);
            }

            btnSuriin.setVisibility(View.GONE);
            btnTapos.setVisibility(View.VISIBLE);
            btnTapos.setEnabled(true);
            btnTapos.setAlpha(1.0f);
            hasSubmitted = true;

            errorTooltipHandler.removeCallbacksAndMessages(null);
            errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 5000);
        } else {
            playCorrectSound();

            perfectAnswerCount++;
            if (perfectAnswerCount >= 5) {
                unlockSagotBayaniAchievement();
            }

            hasSubmitted = false;

            if (tts != null && isTtsReady) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onError(String id) {}
                    @Override
                    public void onDone(String id) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                errorTooltip.setVisibility(View.GONE);
                                errorIcon.setVisibility(View.GONE);
                                isShowingErrorTooltip = false;
                                nextQuestion();
                            }
                        });
                    }
                });
            } else {
                errorTooltipHandler.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        errorTooltip.setVisibility(View.GONE);
                        errorIcon.setVisibility(View.GONE);
                        isShowingErrorTooltip = false;
                        nextQuestion();
                    }
                }, 1200);
            }
        }

    }

    private void checkGrammarFromServer(String sentence) {
        OkHttpClient client = new OkHttpClient();
        String BACKEND_URL = "https://filipino-grammar-checker-backend.onrender.com/suriin-gramar";

        try {
            JSONObject body = new JSONObject();
            body.put("pangungusap", sentence);

            RequestBody rb = RequestBody.create(
                    body.toString(),
                    okhttp3.MediaType.get("application/json; charset=utf-8")
            );

            Request req = new Request.Builder()
                    .url(BACKEND_URL)
                    .post(rb)
                    .build();

            client.newCall(req).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        showErrorTooltip("Network error: " + e.getMessage());
                        if (btnSuriin != null) {
                            btnSuriin.setEnabled(true);
                            btnSuriin.setAlpha(1.0f);
                        }
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    if (response.code() == 204) {
                        runOnUiThread(() -> {
                            playCorrectSound();

                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                                countDownTimer = null;
                            }
                            stopAllTimerSounds();

                            errorTooltip.setText("Walang mali sa pangungusap.");
                            errorTooltip.setVisibility(View.VISIBLE);
                            errorIcon.setVisibility(View.VISIBLE);

                            if (tts != null && isTtsReady) {
                                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                    @Override public void onStart(String id) {}
                                    @Override public void onError(String id) {}
                                    @Override
                                    public void onDone(String id) {
                                        if ("MDCL2_UTTERANCE".equals(id)) {
                                            runOnUiThread(() -> {
                                                if (!isFinishing() && !isDestroyed()) {
                                                    errorTooltip.setVisibility(View.GONE);
                                                    errorIcon.setVisibility(View.GONE);
                                                    isShowingErrorTooltip = false;
                                                    nextQuestion();
                                                }
                                            });
                                        }
                                    }
                                });

                                loadCharacterLine("MDCL2");
                            } else {
                                errorTooltipHandler.postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        errorTooltip.setVisibility(View.GONE);
                                        errorIcon.setVisibility(View.GONE);
                                        isShowingErrorTooltip = false;
                                        nextQuestion();
                                    }
                                }, 1200);
                            }
                        });
                        return;
                    }


                    final String res = response.body().string().trim();
                    runOnUiThread(() -> showGrammarResultTooltip(res));
                }
            });
        } catch (Exception e) {
            showErrorTooltip("Error: " + e.getMessage());
        }
    }

    private void showInputError(String message) {
        showErrorTooltip(message);
        showWrongAnimation();
    }

    private void showWrongAnimation() {
        if (isFinishing() || isDestroyed()) return;

        Glide.with(this).asGif()
                .load(R.drawable.wrong)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(characterIcon);

        new Handler().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this).asGif()
                        .load(R.drawable.idle)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(characterIcon);
            }
        }, 2300);
    }

    private void showErrorTooltip(String message) {
        errorTooltip.setText(message);
        errorIcon.setVisibility(View.VISIBLE);
        errorTooltip.setVisibility(View.VISIBLE);

        isShowingErrorTooltip = true;

        errorTooltipHandler.removeCallbacksAndMessages(null);
        errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 3500);

        errorIcon.setOnClickListener(v -> {
            if (errorTooltip.getVisibility() == View.VISIBLE) {
                errorTooltip.setVisibility(View.GONE);
                errorIcon.setVisibility(View.GONE);
                isShowingErrorTooltip = false;
                errorTooltipHandler.removeCallbacksAndMessages(null);
            } else {
                errorTooltip.setVisibility(View.VISIBLE);
                errorIcon.setVisibility(View.VISIBLE);
                isShowingErrorTooltip = true;
                errorTooltipHandler.removeCallbacksAndMessages(null);
                errorTooltipHandler.postDelayed(hideErrorTooltipRunnable, 3500);
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
        if (lineId.equals("MDCL1") && hasSpokenMDCL1) return;

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
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MDCL2_UTTERANCE");
        }
    }

    private void nextQuestion() {
        if (isGameOver) return;

        if (errorTooltipHandler != null) errorTooltipHandler.removeCallbacksAndMessages(null);
        if (errorTooltip != null) errorTooltip.setVisibility(View.GONE);
        if (errorIcon != null) errorIcon.setVisibility(View.GONE);
        isShowingErrorTooltip = false;

        if (currentQuestionNumber < instructionList.size()) {
            String instruction = instructionList.get(currentQuestionNumber);
            dalubhasaInstruction.setText(instruction);
            currentKeywords = keywordList.get(currentQuestionNumber);
            currentDalubhasaID = "D" + (currentQuestionNumber + 1);

            userSentenceInput.setText("");

            if (!hasSubmitted) {
                userSentenceInput.setEnabled(true);
                userSentenceInput.setFocusableInTouchMode(true);
            } else {
                userSentenceInput.setEnabled(false);
                userSentenceInput.setFocusable(false);
            }

            btnSuriin.setVisibility(View.VISIBLE);
            btnTapos.setVisibility(View.GONE);

            setButtonsEnabled(true);

            btnTapos.setEnabled(true);
            btnTapos.setAlpha(1.0f);

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
        if (countdownHandler != null) countdownHandler.removeCallbacksAndMessages(null);
        countdownHandler = new Handler();
        final int[] countdown = {3};

        releaseBeepPlayer();

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed() || isGameOver) {
                    releaseBeepPlayer();
                    return;
                }

                if (countdown[0] > 0) {
                    dalubhasaInstruction.setText(String.valueOf(countdown[0]));
                    playBeep();
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    dalubhasaInstruction.setText("");
                    releaseBeepPlayer();
                    if (!isFinishing() && !isDestroyed() && !isGameOver) {
                        setButtonsEnabled(true);
                        loadAllDalubhasaInstructions();
                    }
                }
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void playBeep() {
        releaseBeepPlayer();
        countdownBeepPlayer = MediaPlayer.create(PalaroDalubhasa.this, R.raw.beep);
        countdownBeepPlayer.setOnCompletionListener(mp -> {
            try { mp.release(); } catch (Exception ignored) {}
            if (mp == countdownBeepPlayer) countdownBeepPlayer = null;
        });
        countdownBeepPlayer.start();
    }

    private void releaseBeepPlayer() {
        if (countdownBeepPlayer != null) {
            try { countdownBeepPlayer.stop(); } catch (Exception ignored) {}
            try { countdownBeepPlayer.release(); } catch (Exception ignored) {}
            countdownBeepPlayer = null;
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;

        if (btnUmalis != null) {
            btnUmalis.setEnabled(enabled);
            btnUmalis.setAlpha(alpha);
        }

        if (btnSuriin != null) {
            btnSuriin.setEnabled(enabled);
            btnSuriin.setAlpha(alpha);
        }
    }

    private void startTimer() {

        timeLeft = TOTAL_TIME;
        lastTimerZone = "";

        if (countDownTimer != null) countDownTimer.cancel();
        stopAllTimerSounds();

        playTimerSound(R.raw.sound_new);

        countDownTimer = new CountDownTimer(TOTAL_TIME, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent <= 26) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_red));
                } else if (percent <= 48) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_green));
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                btnTapos.setAlpha(0.5f);

                stopAllTimerSounds();

                loadCharacterLine("MCL5");
                saveDalubhasaScore();

                finishQuiz();
            }
        }.start();
    }

    private void playTimerSound(int soundResId) {
        stopAllTimerSounds();

        try {
            timerSoundPlayer = MediaPlayer.create(this, soundResId);
            if (timerSoundPlayer != null) {
                timerSoundPlayer.setLooping(false);
                timerSoundPlayer.start();
            }
        } catch (Exception ignored) {}
    }

    private void stopAllTimerSounds() {
        if (timerSoundPlayer != null) {
            try {
                if (timerSoundPlayer.isPlaying()) timerSoundPlayer.stop();
            } catch (Exception ignored) {}
            try { timerSoundPlayer.release(); } catch (Exception ignored) {}
            timerSoundPlayer = null;
        }
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

                if (countDownTimer != null) countDownTimer.cancel();
                stopAllTimerSounds();

                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                btnTapos.setAlpha(0.5f);

                loadCharacterLine("MCL5");

                saveDalubhasaScore();
                finishQuiz();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endAllAndFinish();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (tts != null) tts.stop();
        stopAllTimerSounds();

        if (internetHandler != null) internetHandler.removeCallbacks(internetRunnable);
        if (errorTooltipHandler != null) errorTooltipHandler.removeCallbacksAndMessages(null);
        if (connectivityManager != null && networkCallback != null)
            connectivityManager.unregisterNetworkCallback(networkCallback);

        if (countdownHandler != null) {
            try { countdownHandler.removeCallbacksAndMessages(null); } catch (Exception ignored) {}
            countdownHandler = null;
        }

        countdownRunnable = null;

        if (countdownBeepPlayer != null) {
            try {
                countdownBeepPlayer.stop();
                countdownBeepPlayer.release();
            } catch (Exception ignored) {}
            countdownBeepPlayer = null;
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

    private void playSound(int soundResId) {
        MediaPlayer mp = MediaPlayer.create(this, soundResId);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    private void playButtonClickSound() { playSound(R.raw.button_click); }
    private void playCorrectSound()     { playSound(R.raw.correct); }
    private void playWrongSound()       { playSound(R.raw.wrong); }

    @Override
    protected void onPause() {
        super.onPause();

        if (timerSoundPlayer != null) timerSoundPlayer.setVolume(0f, 0f);
        TimerSoundUtils.setVolume(0f);

        if (tts != null) tts.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (timerSoundPlayer != null) timerSoundPlayer.setVolume(1f, 1f);
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
        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
        stopAllTimerSounds();

        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Exception ignored) {}

        if (timerSoundPlayer != null) { timerSoundPlayer.stop(); timerSoundPlayer = null; }

        if (errorTooltipHandler != null) errorTooltipHandler.removeCallbacksAndMessages(null);
        if (internetHandler != null && internetRunnable != null) internetHandler.removeCallbacks(internetRunnable);

        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        endAllAndFinish();
    }

    private int countWordsExcludingKeywords(String sentence, List<String> keywords) {
        String cleaned = sentence.replaceAll("[^a-zA-ZÀ-ÿ0-9\\s]", "").toLowerCase();
        String[] words = cleaned.trim().split("\\s+");

        int count = 0;
        for (String word : words) {
            boolean isKeyword = false;
            for (String keyword : keywords) {
                if (word.equalsIgnoreCase(keyword)) {
                    isKeyword = true;
                    break;
                }
            }
            if (!isKeyword && !word.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
