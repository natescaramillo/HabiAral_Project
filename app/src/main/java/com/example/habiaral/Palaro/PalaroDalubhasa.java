package com.example.habiaral.Palaro;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
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

import com.example.habiaral.R;
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
    private static final long TOTAL_TIME = 60000;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_dalubhasa);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(new Locale("fil", "PH")); // Filipino
                textToSpeech.setSpeechRate(1.2f); // Optional
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Walang suporta ang TTS sa wikang Filipino", Toast.LENGTH_SHORT).show();
                } else {
                    isTtsReady = true;
                }
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
                findViewById(R.id.imageView8),
                findViewById(R.id.imageView11),
                findViewById(R.id.imageView10),
                findViewById(R.id.imageView9),
                findViewById(R.id.imageView12)
        };

        db = FirebaseFirestore.getInstance();

        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);

        loadCharacterLine("MDCL1");
        new Handler().postDelayed(this::showCountdownThenLoadInstruction, 7000);

        btnTapos.setOnClickListener(v -> {
            if (!hasSubmitted) {
                String sentence = userSentenceInput.getText().toString().trim();
                if (sentence.isEmpty()) {
                    Toast.makeText(this, "Pakisulat ang iyong pangungusap.", Toast.LENGTH_SHORT).show();
                } else if (!sentence.endsWith(".")) {
                    Toast.makeText(this, "Siguraduhing nagtatapos ang pangungusap sa tuldok (.)", Toast.LENGTH_SHORT).show();
                } else {
                    // I-check kung ilang keyword ang nawawala sa sentence
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
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_box_exit_palaro, null);

        Button btnOo = dialogView.findViewById(R.id.button5);    // OO button
        Button btnHindi = dialogView.findViewById(R.id.button6); // Hindi button

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: for no rounded box

        btnOo.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            saveDalubhasaScore(); // Optional
            dialog.dismiss();
            finish(); // Exit activity
        });

        btnHindi.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void highlightGrammarIssues(String response, String originalSentence) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray matches = jsonObject.getJSONArray("matches");

            currentErrorCount = matches.length();

            if (matches.length() == 0) {
                loadCharacterLine("MDCL2");
                dalubhasaScore += 15;
                new Handler().postDelayed(this::nextQuestion, 4000);
            } else {
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

                if (matches.length() == 1) {
                    dalubhasaScore += 13;
                } else {
                    dalubhasaScore += 10;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            grammarFeedbackText.setText("Invalid grammar response format.");
        }
    }

    private void showBackConfirmationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_box_exit_palaro, null);
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
                            speakLine(line); // 🔊 Speak the line
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show();
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
                    } else {
                        Toast.makeText(this, "No Dalubhasa questions found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load Dalubhasa questions.", Toast.LENGTH_SHORT).show();
                });
    }

    private void nextQuestion() {
        if (currentQuestionNumber < instructionList.size()) {
            dalubhasaInstruction.setText(instructionList.get(currentQuestionNumber));
            currentKeywords = keywordList.get(currentQuestionNumber);
            currentDalubhasaID = "D" + (currentQuestionNumber + 1);

            userSentenceInput.setText("");
            userSentenceInput.setEnabled(true);
            btnTapos.setEnabled(true);
            hasSubmitted = false;
            grammarFeedbackText.setText("");
            currentQuestionNumber++;
        } else {
            if (countDownTimer != null) countDownTimer.cancel();
            saveDalubhasaScore();
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

                // 🔴 Change color based on percentage
                if (percent <= 25) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.custom_progress_drawable_red));
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.custom_progress_drawable_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroDalubhasa.this, R.drawable.custom_progress_drawable));
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
                finishQuiz(); // Tawagin ang custom dialog
            }

        }.start();
    }

    private void finishQuiz() {
        View showTotalPoints = getLayoutInflater().inflate(R.layout.time_up_dialog, null);
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
            finish(); // Wala tayong `setResult()` kasi walang bumabalik na score sa previous activity
        });

        Toast.makeText(this, "Tapos na ang laro!", Toast.LENGTH_SHORT).show();
    }


    private void saveDalubhasaScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || dalubhasaScore <= 0) return;


        String userId = currentUser.getUid();

        // Step 1: Get studentID from 'students' collection
        db.collection("students").document(userId).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists()) {
                Toast.makeText(this, "Student document not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentID = studentDoc.getString("studentId"); // ✅ Now this is properly fetched
            DocumentReference docRef = db.collection("minigame_progress").document(userId);

            docRef.get().addOnSuccessListener(snapshot -> {
                int baguhan = snapshot.contains("baguhan_score") ? snapshot.getLong("baguhan_score").intValue() : 0;
                int husay = snapshot.contains("husay_score") ? snapshot.getLong("husay_score").intValue() : 0;
                int oldDalubhasa = snapshot.contains("dalubhasa_score") ? snapshot.getLong("dalubhasa_score").intValue() : 0;
                int newDalubhasaTotal = oldDalubhasa + dalubhasaScore;

                if (snapshot.exists() && snapshot.contains("minigame_progressID")) {
                    // Reuse existing minigame_progressID
                    String existingProgressId = snapshot.getString("minigame_progressID");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("dalubhasa_score", newDalubhasaTotal);
                    updates.put("studentID", studentID); // ✅ correct student ID
                    updates.put("minigame_progressID", existingProgressId);
                    updates.put("total_score", newDalubhasaTotal + baguhan + husay);

                    docRef.set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(PalaroDalubhasa.this, "Natapos na ang buong palaro", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // No progress doc yet — generate new progress ID
                    db.collection("minigame_progress").get().addOnSuccessListener(querySnapshot -> {
                        int nextNumber = querySnapshot.size() + 1;
                        String newProgressId = "MP" + nextNumber;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("dalubhasa_score", newDalubhasaTotal);
                        updates.put("studentID", studentID); // ✅ correct student ID
                        updates.put("minigame_progressID", newProgressId);
                        updates.put("total_score", newDalubhasaTotal + baguhan + husay);

                        docRef.set(updates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(PalaroDalubhasa.this, "Natapos na ang buong palaro", Toast.LENGTH_SHORT).show();

                                });
                    });
                }
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
            }
        }
    }

    private void speakLine(String line) {
        if (isTtsReady && textToSpeech != null && !line.isEmpty()) {
            textToSpeech.speak(line, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}