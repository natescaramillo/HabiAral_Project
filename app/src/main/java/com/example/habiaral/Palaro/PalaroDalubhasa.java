package com.example.habiaral.Palaro;

import android.graphics.Color;
import android.graphics.Typeface;
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

import com.example.habiaral.R;
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

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 300000;
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private boolean hasSubmitted = false;

    private int correctAnswerCount = 0;
    private int currentErrorCount = 0;
    private int dalubhasaScore = 0;
    private List<String> instructionList = new ArrayList<>();
    private List<List<String>> keywordList = new ArrayList<>();
    private int currentQuestionNumber = 0;
    private List<String> currentKeywords = new ArrayList<>();
    private String currentDalubhasaID = "";
    private int remainingHearts = 5;
    private ImageView[] heartIcons;

    private TextToSpeech textToSpeech;

    private boolean isTtsReady = false;
    private TextToSpeech tts;
    private int perfectAnswerCount = 0;

    private boolean isGameOver = false;


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

        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);


        btnTapos.setOnClickListener(v -> {
            if (!hasSubmitted) {
                String sentence = userSentenceInput.getText().toString().trim();
                if (sentence.isEmpty()) {
                    Toast.makeText(this, "Pakisulat ang iyong pangungusap.", Toast.LENGTH_SHORT).show();
                } else if (!sentence.endsWith(".")) {
                    Toast.makeText(this, "Siguraduhing nagtatapos ang pangungusap sa tuldok (.)", Toast.LENGTH_SHORT).show();
                } else {
                    List<String> missingKeywords = new ArrayList<>();
                    for (String keyword : currentKeywords) {
                        if (!sentence.toLowerCase().contains(keyword.toLowerCase())) {
                            missingKeywords.add(keyword);
                        }
                    }

                    if (!missingKeywords.isEmpty()) {
                        Toast.makeText(this, "Kulang ng salitang: " + missingKeywords, Toast.LENGTH_LONG).show();
                        loadCharacterLine(currentDalubhasaID);
                        return;
                    }

                    GrammarChecker.checkGrammar(this, sentence, new GrammarChecker.GrammarCallback() {
                        @Override
                        public void onResult(String response) {
                            runOnUiThread(() -> {
                                highlightGrammarIssues(response, sentence);
                                hasSubmitted = true;
                                userSentenceInput.setEnabled(false);
                                btnTapos.setEnabled(false);

                                String firebaseUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                String questionId = currentDalubhasaID;
                                saveDalubhasaAnswer(firebaseUID, questionId);

                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(PalaroDalubhasa.this, "Grammar Check Failed: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            }
        });

        userSentenceInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                btnTapos.performClick();
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });
    }

    private void showUmalisDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PalaroDalubhasa.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit_palaro, null);

        Button btnOo = dialogView.findViewById(R.id.button5);
        Button btnHindi = dialogView.findViewById(R.id.button6);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnOo.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            dialog.dismiss();
            finish();
        });

        btnHindi.setOnClickListener(v -> dialog.dismiss());

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

                loadCharacterLine("MDCL2");

                new Handler().postDelayed(this::nextQuestion, 4000);
            } else {
                if (matches.length() == 1) {
                    scoreForThisSentence = 13;
                } else {
                    scoreForThisSentence = 10;
                }

                dalubhasaScore += scoreForThisSentence;

                if (scoreForThisSentence == 13 || scoreForThisSentence == 15) {
                    perfectAnswerCount++;
                } else {
                    perfectAnswerCount = 0;
                }

                if (perfectAnswerCount >= 5) {
                    unlockSagotBayaniAchievement();
                }


                loadCharacterLine("MDCL3");

                if (matches.length() >= 2) {
                    deductHeart();
                }

                new Handler().postDelayed(() -> {
                    try {
                        SpannableString spannable = new SpannableString(originalSentence);
                        for (int i = 0; i < matches.length(); i++) {
                            JSONObject match = matches.getJSONObject(i);
                            int offset = match.getInt("offset");
                            int length = match.getInt("length");

                            spannable.setSpan(new UnderlineSpan(), offset, offset + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spannable.setSpan(new ForegroundColorSpan(Color.RED), offset, offset + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        dalubhasaInstruction.setText(spannable);
                        new Handler().postDelayed(this::nextQuestion, 5000);

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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit_palaro, null);
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
            if (countDownTimer != null) countDownTimer.cancel();
            dialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        dalubhasaInstruction.setText(line);

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

                        nextQuestion();
                    }
                });
    }
    private void speakLine(String text) {
        if (isGameOver) return;
        if (text == null || text.trim().isEmpty()) return;

        if (isTtsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void nextQuestion() {
        if (isGameOver) return;

        if (currentQuestionNumber < instructionList.size()) {
            String instruction = instructionList.get(currentQuestionNumber);
            dalubhasaInstruction.setText(instructionList.get(currentQuestionNumber));
            currentKeywords = keywordList.get(currentQuestionNumber);
            currentDalubhasaID = "D" + (currentQuestionNumber + 1);
            speakLine(instruction);

            userSentenceInput.setText("");
            userSentenceInput.setEnabled(true);
            btnTapos.setEnabled(true);
            hasSubmitted = false;
            grammarFeedbackText.setText("");
            currentQuestionNumber++;
        } else {
            if (countDownTimer != null) countDownTimer.cancel();
            saveDalubhasaScore();
            finishQuiz();
        }
    }

    private void showCountdownThenLoadInstruction() {
        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    dalubhasaInstruction.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    loadAllDalubhasaInstructions();
                    startTimer();
                }
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(TOTAL_TIME, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent <= 25) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_red));
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.timer_color_green));
                }

                if (millisUntilFinished <= 5000 && millisUntilFinished >= 4900) {
                    loadCharacterLine("MCL5");
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                saveDalubhasaScore();
                finishQuiz();
            }

        }.start();
    }

    private void finishQuiz() {
        if (isGameOver) return;
        isGameOver = true;

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
            heartIcons[remainingHearts].setVisibility(View.INVISIBLE);

            if (remainingHearts == 0) {
                Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();
                if (countDownTimer != null) countDownTimer.cancel();
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                saveDalubhasaScore();
                finishQuiz();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
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
                                showAchievementUnlockedDialog(title, R.drawable.achievement10);
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
                                showAchievementUnlockedDialog(title, R.drawable.achievement03);
                            }));
                });
            });
        });

    }

    private void showAchievementUnlockedDialog(String title, int imageRes) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View toastView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = toastView.findViewById(R.id.imageView19);
        TextView tv = toastView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);

        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ssb.setSpan(new RelativeSizeSpan(1.3f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);
        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

}