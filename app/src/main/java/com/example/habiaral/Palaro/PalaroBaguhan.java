package com.example.habiaral.Palaro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PalaroBaguhan extends AppCompatActivity {

    private TextView baguhanQuestion, answer1, answer2, answer3, answer4, answer5, answer6, selectedAnswer;
    private ProgressBar timerBar;
    private Button unlockButton, unlockButton1;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 20000;
    private long timeLeft = TOTAL_TIME;

    private FirebaseFirestore db;
    private int correctAnswerCount = 0;
    private int baguhanScore = 0;
    private int currentQuestionNumber = 1;
    private static final String DOCUMENT_ID = "MP1";

    private boolean isAnswered = false;
    private boolean isTimeUp = false;

    private SharedPreferences preferences;
    private static final String PREF_NAME = "BaguhanPrefs";
    private static final String KEY_QUESTION_NUM = "currentQuestionNumber";
    private static final String KEY_CORRECT_COUNT = "correctAnswerCount";
    private static final String KEY_SCORE = "baguhanScore";
    private String studentID;

    private int remainingHearts = 5;
    private ImageView[] heartIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_baguhan);

        // Reset progress if needed
        if (getIntent().getBooleanExtra("resetProgress", false)) {
            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            studentID = currentUser.getUid();
        }

        baguhanQuestion = findViewById(R.id.baguhan_instructionText);
        answer1 = findViewById(R.id.baguhan_answer1);
        answer2 = findViewById(R.id.baguhan_answer2);
        answer3 = findViewById(R.id.baguhan_answer3);
        answer4 = findViewById(R.id.baguhan_answer4);
        answer5 = findViewById(R.id.baguhan_answer5);
        answer6 = findViewById(R.id.baguhan_answer6);
        timerBar = findViewById(R.id.timerBar);
        unlockButton = findViewById(R.id.UnlockButtonPalaro);
        unlockButton1 = findViewById(R.id.UnlockButtonPalaro1);

        heartIcons = new ImageView[]{
                findViewById(R.id.imageView8),
                findViewById(R.id.imageView11),
                findViewById(R.id.imageView10),
                findViewById(R.id.imageView9),
                findViewById(R.id.imageView12)
        };

        db = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentQuestionNumber = preferences.getInt(KEY_QUESTION_NUM, 1);
        correctAnswerCount = preferences.getInt(KEY_CORRECT_COUNT, 0);
        baguhanScore = preferences.getInt(KEY_SCORE, 0);

        loadCharacterLine("MCL1");
        new Handler().postDelayed(this::showCountdownThenLoadQuestion, 3000);

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

            if (selectedAnswer != null) {
                isAnswered = true;
                String userAnswer = selectedAnswer.getText().toString();
                String correctDocId = "BCA" + currentQuestionNumber;

                db.collection("baguhan_correct_answers").document(correctDocId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");
                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;
                                    baguhanScore += 5;
                                    loadCharacterLine("MCL2");
                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                    timeLeft = Math.min(timeLeft + 3000, TOTAL_TIME);
                                    startTimer(timeLeft);
                                } else {
                                    loadCharacterLine("MCL4");
                                    Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                                    deductHeart();
                                }
                            }

                            new Handler().postDelayed(() -> {
                                currentQuestionNumber++;
                                saveProgress();
                                resetForNextQuestion();
                                loadBaguhanQuestion();
                            }, 2000);
                        });
            } else {
                Toast.makeText(this, "Paki pili muna ng sagot.", Toast.LENGTH_SHORT).show();
            }
        });

        unlockButton1.setOnLongClickListener(v -> {
            saveProgressToFirebase();
            Toast.makeText(this, "Naitala na ang progreso. Paalam muna!", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
    }

    private void finishQuiz() {
        saveBaguhanScore();

        View showTotalPoints = getLayoutInflater().inflate(R.layout.time_up_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(PalaroBaguhan.this)
                .setView(showTotalPoints)
                .setCancelable(false)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView scoreTextDialog = showTotalPoints.findViewById(R.id.scoreText);
        scoreTextDialog.setText(String.valueOf(baguhanScore));

        Button balik = showTotalPoints.findViewById(R.id.btn_balik);
        balik.setOnClickListener(v -> {
            dialog.dismiss();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("baguhanPoints", baguhanScore);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        Toast.makeText(this, "Tapos na ang laro!", Toast.LENGTH_SHORT).show();
    }

    private void saveBaguhanScore() {
        DocumentReference docRef = db.collection("minigame_progress").document(DOCUMENT_ID);
        Map<String, Object> updates = new HashMap<>();
        updates.put("baguhan_score", baguhanScore);
        updates.put("total_score", baguhanScore);
        updates.put("studentID", studentID);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (baguhanScore >= 400) unlockHusay(docRef);
                });
    }

    private void unlockHusay(DocumentReference docRef) {
        docRef.update("husay_unlocked", true)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "🎉 Husay unlocked!", Toast.LENGTH_LONG).show());
    }

    private void showCountdownThenLoadQuestion() {
        final Handler countdownHandler = new Handler();
        final int[] countdown = {3};

        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    baguhanQuestion.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    loadBaguhanQuestion();
                    startTimer(timeLeft);
                }
            }
        });
    }

    private void startTimer(long startTime) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(startTime, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                timerBar.setProgress((int) (timeLeft * 100 / TOTAL_TIME));
                if (millisUntilFinished <= 5000 && millisUntilFinished >= 4900) {
                    loadCharacterLine("MCL5");
                }
            }

            @Override
            public void onFinish() {
                timerBar.setProgress(0);
                isTimeUp = true;
                disableAnswerSelection();
                finishQuiz();
            }
        }.start();
    }

    private void resetForNextQuestion() {
        isAnswered = false;
        isTimeUp = false;
        selectedAnswer = null;
        unlockButton.setEnabled(true);
        resetAnswerBackgrounds();
    }

    private void resetAnswerBackgrounds() {
        answer1.setBackgroundResource(R.drawable.answer_option_bg);
        answer2.setBackgroundResource(R.drawable.answer_option_bg);
        answer3.setBackgroundResource(R.drawable.answer_option_bg);
        answer4.setBackgroundResource(R.drawable.answer_option_bg);
        answer5.setBackgroundResource(R.drawable.answer_option_bg);
        answer6.setBackgroundResource(R.drawable.answer_option_bg);
    }

    private void disableAnswerSelection() {
        answer1.setOnClickListener(null);
        answer2.setOnClickListener(null);
        answer3.setOnClickListener(null);
        answer4.setOnClickListener(null);
        answer5.setOnClickListener(null);
        answer6.setOnClickListener(null);
        unlockButton.setEnabled(false);
    }

    private void deductHeart() {
        if (remainingHearts > 0) {
            remainingHearts--;
            heartIcons[remainingHearts].setVisibility(View.INVISIBLE);

            if (remainingHearts == 0) {
                Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();
                disableAnswerSelection();
                finishQuiz();
            }
        }
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_QUESTION_NUM, currentQuestionNumber);
        editor.putInt(KEY_CORRECT_COUNT, correctAnswerCount);
        editor.putInt(KEY_SCORE, baguhanScore);
        editor.apply();
    }

    private void saveProgressToFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> progress = new HashMap<>();
            progress.put("currentQuestionNumber", currentQuestionNumber);
            progress.put("correctAnswerCount", correctAnswerCount);
            progress.put("baguhanScore", baguhanScore);

            db.collection("student_progress").document(currentUser.getUid()).set(progress);
        }
    }

    private void loadBaguhanQuestion() {
        String questionDocId = "B" + currentQuestionNumber;

        db.collection("baguhan").document(questionDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        finishQuiz(); // No more questions
                        return;
                    }

                    String question = documentSnapshot.getString("baguhan_question");
                    List<String> choices = (List<String>) documentSnapshot.get("choices");

                    if (question != null) baguhanQuestion.setText(question);
                    if (choices != null && choices.size() >= 6) {
                        answer1.setText(choices.get(0));
                        answer2.setText(choices.get(1));
                        answer3.setText(choices.get(2));
                        answer4.setText(choices.get(3));
                        answer5.setText(choices.get(4));
                        answer6.setText(choices.get(5));
                    }

                    isAnswered = false;
                    selectedAnswer = null;
                    resetAnswerBackgrounds();
                });
    }

    private void loadCharacterLine(String lineId) {
        db.collection("minigame_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        if (line != null) baguhanQuestion.setText(line);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        saveProgressToFirebase();
    }
}
