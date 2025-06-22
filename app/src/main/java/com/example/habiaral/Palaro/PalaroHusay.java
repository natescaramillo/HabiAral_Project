package com.example.habiaral.Palaro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PalaroHusay extends AppCompatActivity {

    private TextView husayInstruction;
    private TextView answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9;
    private TextView fullAnswerView;
    private ProgressBar timerBar;
    private Button unlockButton;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 20000; // 20 seconds
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private int correctAnswerCount = 0;

    private final List<TextView> selectedWords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_husay);

        husayInstruction = findViewById(R.id.husay_instructionText);
        answer1 = findViewById(R.id.husay_answer1);
        answer2 = findViewById(R.id.husay_answer2);
        answer3 = findViewById(R.id.husay_answer3);
        answer4 = findViewById(R.id.husay_answer4);
        answer5 = findViewById(R.id.husay_answer5);
        answer6 = findViewById(R.id.husay_answer6);
        answer7 = findViewById(R.id.husay_answer7);
        answer8 = findViewById(R.id.husay_answer8);
        answer9 = findViewById(R.id.husay_answer9);
        fullAnswerView = findViewById(R.id.whole_answer);
        timerBar = findViewById(R.id.timerBar);
        unlockButton = findViewById(R.id.UnlockButtonPalaro);

        db = FirebaseFirestore.getInstance();

        loadCharacterLine("MHCL1");

        new Handler().postDelayed(this::showCountdownThenLoadWords, 7000);

        unlockButton.setOnClickListener(v -> {
            String userAnswer = fullAnswerView.getText().toString().trim();

            if (!userAnswer.isEmpty()) {
                db.collection("husay_correct_answers").document("HCA1")
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");

                                if (normalize(userAnswer).equals(normalize(correctAnswer))) {
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
                Toast.makeText(this, "Paki buuin muna ang pangungusap.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        husayInstruction.setText(line);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show());
    }

    private void loadHusayWords() {
        db.collection("husay").document("H1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> words = (List<String>) documentSnapshot.get("words");

                        if (words != null && words.size() >= 9) {
                            answer1.setText(words.get(0));
                            answer2.setText(words.get(1));
                            answer3.setText(words.get(2));
                            answer4.setText(words.get(3));
                            answer5.setText(words.get(4));
                            answer6.setText(words.get(5));
                            answer7.setText(words.get(6));
                            answer8.setText(words.get(7));
                            answer9.setText(words.get(8));

                            TextView[] answers = {answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9};

                            for (TextView answer : answers) {
                                answer.setOnClickListener(view -> {
                                    TextView tapped = (TextView) view;
                                    if (tapped.isEnabled()) {
                                        String current = fullAnswerView.getText().toString().trim();
                                        fullAnswerView.setText((current + " " + tapped.getText().toString()).trim());
                                        tapped.setEnabled(false);
                                        selectedWords.add(tapped);

                                        if (selectedWords.size() == 9) {
                                            String finalSentence = fullAnswerView.getText().toString().trim();
                                            if (!finalSentence.endsWith(".")) {
                                                fullAnswerView.setText(finalSentence + ".");
                                            }
                                        }
                                    }
                                });

                                answer.setOnTouchListener(new View.OnTouchListener() {
                                    private long lastTapTime = 0;

                                    @Override
                                    public boolean onTouch(View view, MotionEvent motionEvent) {
                                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                            long currentTime = System.currentTimeMillis();
                                            if (currentTime - lastTapTime < 300) {
                                                // Double tap detected
                                                TextView tapped = (TextView) view;
                                                String word = tapped.getText().toString();
                                                String currentText = fullAnswerView.getText().toString();

                                                if (selectedWords.contains(tapped)) {
                                                    selectedWords.remove(tapped);
                                                    tapped.setEnabled(true);
                                                    currentText = currentText.replaceFirst("\\b" + word + "\\b", "").trim();
                                                    fullAnswerView.setText(currentText);
                                                }
                                            }
                                            lastTapTime = currentTime;
                                        }
                                        return false;
                                    }
                                });
                            }
                        }
                    } else {
                        Toast.makeText(this, "Words not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load words.", Toast.LENGTH_SHORT).show());
    }

    private void showCountdownThenLoadWords() {
        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    husayInstruction.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    loadCharacterLine("MHCL2");

                    new Handler().postDelayed(() -> {
                        loadHusayWords();
                        startTimer();
                    }, 1500);
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
                Toast.makeText(PalaroHusay.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9 ]", "") // remove punctuation
                .replaceAll("\\s+", " ")         // normalize multiple spaces
                .trim();
    }
}
