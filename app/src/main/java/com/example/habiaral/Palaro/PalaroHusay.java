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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PalaroHusay extends AppCompatActivity {

    private TextView husayInstruction;
    private TextView answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9;
    private TextView fullAnswerView;
    private ProgressBar timerBar;
    private Button unlockButton;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 20000;
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private int correctAnswerCount = 0;
    private static final String DOCUMENT_ID = "MP1";

    private final List<TextView> selectedWords = new ArrayList<>();
    private TextView selectedAnswer;
    private boolean isTimeUp = false;
    private boolean isAnswered = false;
    private int currentQuestionNumber = 1;

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

        View.OnClickListener answerClickListener = view -> {
            if (isTimeUp || isAnswered) return;
            resetAnswerBackgrounds();
            view.setBackgroundResource(R.drawable.answer_option_bg_selected);
            selectedAnswer = (TextView) view;
        };

        answer1.setOnClickListener(answerClickListener);
        answer2.setOnClickListener(answerClickListener);
        answer3.setOnClickListener(answerClickListener);
        answer4.setOnClickListener(answerClickListener);
        answer5.setOnClickListener(answerClickListener);
        answer6.setOnClickListener(answerClickListener);

        unlockButton.setOnClickListener(v -> {
            if (isTimeUp || isAnswered) return;

            String userAnswer = fullAnswerView.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Paki buuin muna ang pangungusap.", Toast.LENGTH_SHORT).show();
                return;
            }

            isAnswered = true;
            String correctDocId = "HCA" + currentQuestionNumber;
            db.collection("husay_correct_answers").document(correctDocId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String correctAnswer = documentSnapshot.getString("correctAnswer");
                            if (normalize(userAnswer).equals(normalize(correctAnswer))) {
                                correctAnswerCount++;
                                loadCharacterLine(correctAnswerCount == 1 ? "MCL2" : "MCL3");
                                Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                            } else {
                                loadCharacterLine("MCL4");
                                Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                            }

                            // 👉 Add this to proceed to next question
                            currentQuestionNumber++;
                            if (currentQuestionNumber <= 10) {
                                new Handler().postDelayed(() -> {
                                    loadHusayWords("H" + currentQuestionNumber);
                                    fullAnswerView.setText("");
                                    isAnswered = false;
                                    isTimeUp = false;
                                    startTimer(); // restart timer for next question
                                }, 3000);
                            } else {
                                countDownTimer.cancel();
                                saveHusayScore(); // end
                            }
                        }
                    });
        });

        // Handle back press with confirmation dialog
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });
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
                        husayInstruction.setText(documentSnapshot.getString("line"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show());
    }

    private void loadHusayWords(String docId) {
        db.collection("husay").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> words = (List<String>) documentSnapshot.get("words");

                        if (words != null && words.size() >= 9) {
                            TextView[] answers = {
                                    answer1, answer2, answer3,
                                    answer4, answer5, answer6,
                                    answer7, answer8, answer9
                            };

                            for (int i = 0; i < 9; i++) {
                                TextView answer = answers[i];
                                answer.setText(words.get(i));
                                answer.setEnabled(true);

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

                            selectedWords.clear();
                            fullAnswerView.setText("");
                        }
                    } else {
                        Toast.makeText(this, "Words not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load words.", Toast.LENGTH_SHORT).show());
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
                        loadHusayWords("H1");
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
                isTimeUp = true;
                timerBar.setProgress(0);
                saveHusayScore();
                Toast.makeText(PalaroHusay.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void saveHusayScore() {
        int husayScore = correctAnswerCount * 3;

        DocumentReference docRef = db.collection("minigame_progress").document(DOCUMENT_ID);

        Map<String, Object> updates = new HashMap<>();
        updates.put("husay_score", husayScore);
        updates.put("total_score", husayScore);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PalaroHusay.this, "Score saved!", Toast.LENGTH_SHORT).show();
                    if (husayScore >= 800) {
                        unlockDalubhasa(docRef);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(PalaroHusay.this, "Failed to save score.", Toast.LENGTH_SHORT).show());
    }

    private void unlockDalubhasa(DocumentReference docRef) {
        docRef.update("husay_unlocked", true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Dalubhasa unlocked!", Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void resetAnswerBackgrounds() {
        answer1.setBackgroundResource(R.drawable.answer_option_bg);
        answer2.setBackgroundResource(R.drawable.answer_option_bg);
        answer3.setBackgroundResource(R.drawable.answer_option_bg);
        answer4.setBackgroundResource(R.drawable.answer_option_bg);
        answer5.setBackgroundResource(R.drawable.answer_option_bg);
        answer6.setBackgroundResource(R.drawable.answer_option_bg);
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
