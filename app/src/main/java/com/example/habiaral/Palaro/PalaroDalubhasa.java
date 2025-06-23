package com.example.habiaral.Palaro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
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
    private Button btnTapos;
    private ProgressBar timerBar;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 60000;
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;

    private boolean hasSubmitted = false;

    private static final String DALUBHASA_ID = "D1"; // example, can be changed dynamically

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_dalubhasa);

        dalubhasaInstruction = findViewById(R.id.dalubhasa_instructionText);
        userSentenceInput = findViewById(R.id.dalubhasa_answer);
        btnTapos = findViewById(R.id.UnlockButtonPalaro);
        timerBar = findViewById(R.id.timerBar);

        db = FirebaseFirestore.getInstance();

        // Disable input and button at start
        userSentenceInput.setEnabled(false);
        btnTapos.setEnabled(false);

        loadCharacterLine("MDCL1");

        new Handler().postDelayed(this::showCountdownThenLoadInstruction, 7000);

        btnTapos.setOnClickListener(view -> {
            if (!hasSubmitted) {
                String sentence = userSentenceInput.getText().toString().trim();

                if (sentence.isEmpty()) {
                    Toast.makeText(this, "Paki sulat ang iyong pangungusap.", Toast.LENGTH_SHORT).show();
                } else if (sentence.split("\\s+").length < 4) {
                    // Too short, mark as wrong
                    saveWrongAnswer(sentence);
                } else {
                    saveCorrectAnswer(sentence);
                }

                hasSubmitted = true;
                userSentenceInput.setEnabled(false);
                btnTapos.setEnabled(false);
            }
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
                    userSentenceInput.setEnabled(true); // Enable input
                    btnTapos.setEnabled(true);          // Enable button
                    startTimer();
                }
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void startTimer() {
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

    private void saveCorrectAnswer(String sentence) {
        Map<String, Object> data = new HashMap<>();
        data.put("sentence", sentence);
        data.put("dalubhasaID", DALUBHASA_ID);
        data.put("correctAnswerID", "DCA1");

        db.collection("dalubhasa_correct_answers")
                .document("DCA1")
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Naipasa ang tamang sagot!", Toast.LENGTH_SHORT).show();
                    loadCharacterLine("MCL2");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi naisave ang tamang sagot.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveWrongAnswer(String sentence) {
        Map<String, Object> data = new HashMap<>();
        data.put("sentence", sentence);
        data.put("dalubhasaID", DALUBHASA_ID);
        data.put("wrongAnswerID", "DWA1");

        db.collection("dalubhasa_wrong_answers")
                .document("DWA1")
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Maling sagot. Naipasa pa rin.", Toast.LENGTH_SHORT).show();
                    loadCharacterLine("MCL3");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi naisave ang maling sagot.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
