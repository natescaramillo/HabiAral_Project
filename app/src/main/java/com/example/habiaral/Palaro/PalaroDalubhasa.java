package com.example.habiaral.Palaro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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
    private static final String DALUBHASA_ID = "D1";
    private static final String CORRECT_ID = "DCA1";
    private static final String WRONG_ID = "DWA1";

    private int correctAnswerCount = 0;
    private static final String DOCUMENT_ID = "MP1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_dalubhasa);

        dalubhasaInstruction = findViewById(R.id.dalubhasa_instructionText);
        grammarFeedbackText = findViewById(R.id.grammar_feedback);
        userSentenceInput = findViewById(R.id.dalubhasa_answer);
        timerBar = findViewById(R.id.timerBar);
        btnTapos = findViewById(R.id.UnlockButtonPalaro);

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
                } else if (!isValidSentenceStructureOnly(sentence)) {
                    Toast.makeText(this, "Pakilagyan ng tamang estruktura.", Toast.LENGTH_SHORT).show();
                } else {
                    grammarFeedbackText.setText("⏳ Sinusuri ang gramatika...");
                    // 🔍 Perform grammar checking
                    GrammarChecker.checkGrammar(this, sentence, new GrammarChecker.GrammarCallback() {
                        @Override
                        public void onResult(String response) {
                            parseGrammarResult(response);
                            saveCorrectAnswer(sentence);
                            hasSubmitted = true;
                            userSentenceInput.setEnabled(false);
                            btnTapos.setEnabled(false);
                        }

                        @Override
                        public void onError(String error) {
                            grammarFeedbackText.setText("⚠️ Error sa pagsusuri: " + error);
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

    private boolean isValidSentenceStructureOnly(String input) {
        String[] words = input.trim().split("\\s+");
        return words.length >= 5 && input.endsWith(".");
    }

    private void showBackConfirmationDialog() {
        View backDialogView = getLayoutInflater().inflate(R.layout.custom_dialog_box_exit_palaro, null);
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
                        dalubhasaInstruction.setText(documentSnapshot.getString("line"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDalubhasaInstruction() {
        db.collection("dalubhasa").document(DALUBHASA_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        dalubhasaInstruction.setText(documentSnapshot.getString("instruction"));
                        userSentenceInput.setEnabled(true);
                        btnTapos.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Dalubhasa instruction not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load Dalubhasa data.", Toast.LENGTH_SHORT).show();
                });
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
                    loadDalubhasaInstruction();
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
                int progress = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(progress);
                if (millisUntilFinished <= 5000 && millisUntilFinished >= 4900) {
                    loadCharacterLine("MCL5");
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                saveDalubhasaScore();
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                Toast.makeText(PalaroDalubhasa.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void saveCorrectAnswer(String sentence) {
        Map<String, Object> data = new HashMap<>();
        data.put("sentence", sentence);
        data.put("dalubhasaID", DALUBHASA_ID);
        data.put("correct_answersID", CORRECT_ID);

        db.collection("dalubhasa_correct_answers").document(CORRECT_ID)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tamang sagot naipasa!", Toast.LENGTH_SHORT).show();
                    loadCharacterLine("MCL2");
                    correctAnswerCount++;
                    nextQuestion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi naipasa. Subukan muli.", Toast.LENGTH_SHORT).show();
                });
    }

    private void nextQuestion() {
        if (countDownTimer != null) countDownTimer.cancel();
        userSentenceInput.setText("");
        grammarFeedbackText.setText("");
        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);
        hasSubmitted = false;
        loadDalubhasaInstruction();
        startTimer();
    }

    private void saveDalubhasaScore() {
        int score = correctAnswerCount * 5;
        DocumentReference docRef = db.collection("minigame_progress").document(DOCUMENT_ID);

        Map<String, Object> updates = new HashMap<>();
        updates.put("dalubhasa_score", score);
        updates.put("total_score", score);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(PalaroDalubhasa.this, "Score saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(PalaroDalubhasa.this, "Failed to save score.", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void parseGrammarResult(String json) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(json);
            org.json.JSONArray matches = jsonObject.getJSONArray("matches");

            if (matches.length() == 0) {
                grammarFeedbackText.setText("✅ Walang nakitang grammatical errors.");
                return;
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < matches.length(); i++) {
                org.json.JSONObject match = matches.getJSONObject(i);
                String message = match.getString("message");
                org.json.JSONArray replacements = match.getJSONArray("replacements");

                result.append("🔸 ").append(message);
                if (replacements.length() > 0) {
                    result.append("\n👉 Mungkahi: ").append(replacements.getJSONObject(0).getString("value"));
                }
                result.append("\n\n");
            }

            grammarFeedbackText.setText(result.toString());

        } catch (Exception e) {
            grammarFeedbackText.setText("⚠️ May problema sa pag-parse ng resulta.");
        }
    }
}
