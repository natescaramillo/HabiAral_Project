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

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PalaroDalubhasa extends AppCompatActivity {

    private TextView dalubhasaInstruction;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_dalubhasa);

        dalubhasaInstruction = findViewById(R.id.dalubhasa_instructionText);
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
                } else if (!isValidSentence(sentence)) {
                    saveWrongAnswer(sentence);
                } else {
                    saveCorrectAnswer(sentence);
                }
                hasSubmitted = true;
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
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
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        dalubhasaInstruction.setText(line);
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
                        String instruction = documentSnapshot.getString("instruction");
                        dalubhasaInstruction.setText(instruction);
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
        if (countDownTimer != null) {
            countDownTimer.cancel(); // ❗ Stop existing timer
        }

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
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
                Toast.makeText(PalaroDalubhasa.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }


    private boolean isValidSentence(String input) {
        String[] words = input.trim().split("\\s+");
        return words.length >= 5 && input.endsWith(".");
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
                    nextQuestion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi naipasa. Subukan muli.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveWrongAnswer(String sentence) {
        Map<String, Object> data = new HashMap<>();
        data.put("sentence", sentence);
        data.put("dalubhasaID", DALUBHASA_ID);
        data.put("wrong_answersID", WRONG_ID);

        db.collection("dalubhasa_wrong_answers").document(WRONG_ID)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Maling sagot naipasa.", Toast.LENGTH_SHORT).show();
                    loadCharacterLine("MCL3");
                    nextQuestion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi naisave ang maling sagot.", Toast.LENGTH_SHORT).show();
                });
    }

    private void nextQuestion() {
        if (countDownTimer != null) countDownTimer.cancel(); // ❗ Cancel old timer
        userSentenceInput.setText("");
        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);
        hasSubmitted = false;

        // Load next question
        loadDalubhasaInstruction();
        startTimer();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
