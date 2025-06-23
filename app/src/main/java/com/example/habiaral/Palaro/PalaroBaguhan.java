package com.example.habiaral.Palaro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PalaroBaguhan extends AppCompatActivity {

    private TextView baguhanQuestion;
    private TextView answer1, answer2, answer3, answer4, answer5, answer6;
    private TextView selectedAnswer;
    private ProgressBar timerBar;
    private Button unlockButton;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 20000; // 20 seconds
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private int correctAnswerCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_baguhan);

        // Initialize views
        baguhanQuestion = findViewById(R.id.baguhan_instructionText);
        answer1 = findViewById(R.id.baguhan_answer1);
        answer2 = findViewById(R.id.baguhan_answer2);
        answer3 = findViewById(R.id.baguhan_answer3);
        answer4 = findViewById(R.id.baguhan_answer4);
        answer5 = findViewById(R.id.baguhan_answer5);
        answer6 = findViewById(R.id.baguhan_answer6);
        timerBar = findViewById(R.id.timerBar);
        unlockButton = findViewById(R.id.UnlockButtonPalaro);

        db = FirebaseFirestore.getInstance();

        // Show MCL1 first
        loadCharacterLine("MCL1");

        // After 3 seconds, show 3..2..1 then load the question
        new Handler().postDelayed(this::showCountdownThenLoadQuestion, 3000);

        // Answer click listener
        View.OnClickListener answerClickListener = view -> {
            resetAnswerBackgrounds();
            view.setBackgroundResource(R.drawable.answer_option_bg_selected);
            selectedAnswer = (TextView) view;
        };

        // Set click listeners
        answer1.setOnClickListener(answerClickListener);
        answer2.setOnClickListener(answerClickListener);
        answer3.setOnClickListener(answerClickListener);
        answer4.setOnClickListener(answerClickListener);
        answer5.setOnClickListener(answerClickListener);
        answer6.setOnClickListener(answerClickListener);

        // Unlock button logic
        unlockButton.setOnClickListener(v -> {
            if (selectedAnswer != null) {
                String userAnswer = selectedAnswer.getText().toString();

                db.collection("baguhan_correct_answers").document("BCA1")
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");
                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;

                                    if (correctAnswerCount == 1) {
                                        loadCharacterLine("MCL2");
                                    } else {
                                        loadCharacterLine("MCL3");
                                    }

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                } else {
                                    loadCharacterLine("MCL4");
                                    Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(this, "Paki pili muna ng sagot.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        baguhanQuestion.setText(line);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBaguhanQuestion() {
        db.collection("baguhan").document("B1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String question = documentSnapshot.getString("baguhan_question");
                        List<String> choices = (List<String>) documentSnapshot.get("choices");

                        if (question != null) {
                            baguhanQuestion.setText(question);
                        }

                        if (choices != null && choices.size() >= 6) {
                            answer1.setText(choices.get(0));
                            answer2.setText(choices.get(1));
                            answer3.setText(choices.get(2));
                            answer4.setText(choices.get(3));
                            answer5.setText(choices.get(4));
                            answer6.setText(choices.get(5));
                        }
                    } else {
                        Toast.makeText(this, "Question not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCountdownThenLoadQuestion() {
        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    baguhanQuestion.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    loadBaguhanQuestion();
                    startTimer();
                }
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void resetAnswerBackgrounds() {
        answer1.setBackgroundResource(R.drawable.answer_option_bg);
        answer2.setBackgroundResource(R.drawable.answer_option_bg);
        answer3.setBackgroundResource(R.drawable.answer_option_bg);
        answer4.setBackgroundResource(R.drawable.answer_option_bg);
        answer5.setBackgroundResource(R.drawable.answer_option_bg);
        answer6.setBackgroundResource(R.drawable.answer_option_bg);
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
                Toast.makeText(PalaroBaguhan.this, "Time's up!", Toast.LENGTH_SHORT).show();
                View showTotalPoints = getLayoutInflater().inflate(R.layout.time_up_dialog,null);
                AlertDialog dialog = new AlertDialog.Builder(PalaroBaguhan.this)
                        .setView(showTotalPoints)
                        .setCancelable(false)
                        .create();
                        dialog.show();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                Button balik = showTotalPoints.findViewById(R.id.btn_balik);
                balik.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(PalaroBaguhan.this, Palaro.class);
                    startActivity(intent);
                });
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
