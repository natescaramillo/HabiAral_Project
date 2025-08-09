package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PangngalanQuiz extends AppCompatActivity {

    TextView questionText, questionTitle;
    Button answer1, answer2, answer3, nextButton;
    ProgressBar timerBar;

    FirebaseFirestore db;
    ArrayList<String> quizIDs;
    int currentIndex = 0;
    String correctAnswer = "";
    boolean isAnswered = false;

    CountDownTimer countDownTimer;
    long timeLeftInMillis = 10000;
    private AlertDialog resultDialog;
    boolean quizFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_quiz);

        questionTitle = findViewById(R.id.questionTitle);
        questionText = findViewById(R.id.pangngalan_questionText);
        answer1 = findViewById(R.id.answer1);
        answer2 = findViewById(R.id.answer2);
        answer3 = findViewById(R.id.answer3);
        nextButton = findViewById(R.id.pangngalanNextButton);
        timerBar = findViewById(R.id.timerBar);

        db = FirebaseFirestore.getInstance();
        quizIDs = new ArrayList<>();
        Collections.addAll(quizIDs, "QQ1", "QQ2", "QQ3", "QQ4", "QQ5", "QQ6", "QQ7", "QQ8", "QQ9", "QQ10");
        Collections.shuffle(quizIDs);

        // Intro
        loadCharacterLine("QCL1");
        new Handler().postDelayed(() -> {
            loadCharacterLine("QCL2");
            new Handler().postDelayed(this::showCountdownThenLoadQuestion, 3000);
        }, 5000);

        // Answer click
        View.OnClickListener choiceClickListener = view -> {
            if (isAnswered) return;
            isAnswered = true;
            nextButton.setEnabled(true);
            disableAnswers();
        };

        answer1.setOnClickListener(choiceClickListener);
        answer2.setOnClickListener(choiceClickListener);
        answer3.setOnClickListener(choiceClickListener);

        // Next click
        nextButton.setOnClickListener(v -> {
            if (!isAnswered) {
                Toast.makeText(this, "Pumili muna ng sagot bago mag-next!", Toast.LENGTH_SHORT).show();
                return;
            }

            currentIndex++;

            if (currentIndex < quizIDs.size()) {
                loadQuestion(quizIDs.get(currentIndex));
            } else {
                quizFinished = true;
                if (countDownTimer != null) countDownTimer.cancel();
                unlockNextLesson();
                saveQuizResultToFirestore();
                showResultDialog();
            }
        });
    }

    // ===== UI =====
    private void resetButtons() {
        int defaultBg = R.drawable.answer_option_bg;
        answer1.setBackgroundResource(defaultBg);
        answer2.setBackgroundResource(defaultBg);
        answer3.setBackgroundResource(defaultBg);

        answer1.setEnabled(true);
        answer2.setEnabled(true);
        answer3.setEnabled(true);

        isAnswered = false;
        nextButton.setEnabled(false);
    }

    private void disableAnswers() {
        answer1.setEnabled(false);
        answer2.setEnabled(false);
        answer3.setEnabled(false);
    }

    private void showCountdownThenLoadQuestion() {
        questionText.setText("3");
        new Handler().postDelayed(() -> {
            questionText.setText("2");
            new Handler().postDelayed(() -> {
                questionText.setText("1");
                new Handler().postDelayed(() -> {
                    questionText.setText("");
                    loadQuestion(quizIDs.get(currentIndex));
                }, 1000);
            }, 1000);
        }, 1000);
    }

    // ===== Data =====
    private void loadCharacterLine(String lineId) {
        db.collection("quiz_character_lines").document(lineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String line = documentSnapshot.getString("line");
                        questionText.setText(line);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load line: " + lineId, Toast.LENGTH_SHORT).show());
    }

    private void loadQuestion(String documentID) {
        if (countDownTimer != null) countDownTimer.cancel();

        db.collection("quiz_question").document(documentID)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        String question = doc.getString("question");
                        ArrayList<String> choices = (ArrayList<String>) doc.get("choices");
                        correctAnswer = doc.getString("correct_choice");

                        if (choices != null && choices.size() >= 3) {
                            Collections.shuffle(choices);
                            questionTitle.setText(getQuestionOrdinal(currentIndex + 1));
                            questionText.setText(question);
                            answer1.setText(choices.get(0));
                            answer2.setText(choices.get(1));
                            answer3.setText(choices.get(2));

                            resetButtons();
                            startTimer();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load question.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ===== Timer =====
    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeLeftInMillis = 10000;

        timerBar.setMax((int) timeLeftInMillis);
        timerBar.setProgress((int) timeLeftInMillis);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                timerBar.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if (quizFinished) return;
                isAnswered = true;
                disableAnswers();
                nextButton.setEnabled(true); // allow next kahit timeout
                Toast.makeText(PangngalanQuiz.this, "Time's up!", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(() -> {
                    if (!quizFinished) nextButton.performClick();
                }, 500);
            }
        }.start();
    }

    // ===== Result Dialog =====
    private void showResultDialog() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_option, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button retryButton = dialogView.findViewById(R.id.buttonRetry);
        Button homeButton = dialogView.findViewById(R.id.buttonHome);

        resultDialog = builder.create();
        resultDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (!isFinishing()) resultDialog.show();

        retryButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            currentIndex = 0;
            Collections.shuffle(quizIDs);
            loadQuestion(quizIDs.get(currentIndex));
        });

        homeButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            Intent intent = new Intent(PangngalanQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ===== Utilities =====
    private String getQuestionOrdinal(int number) {
        switch (number) {
            case 1: return "Unang tanong";
            case 2: return "Pangalawang tanong";
            case 3: return "Ikatlong tanong";
            case 4: return "Ikaapat na tanong";
            case 5: return "Ikalimang tanong";
            case 6: return "Ikaanim na tanong";
            case 7: return "Ikapitong tanong";
            case 8: return "Ikawalong tanong";
            case 9: return "Ikasiyam na tanong";
            case 10: return "Ikasampung tanong";
            default: return "Tanong";
        }
    }

    // ===== Firestore Save =====
    private void unlockNextLesson() {
        Toast.makeText(this, "Next Lesson Unlocked: Pandiwa!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangngalanStatus = new HashMap<>();
        pangngalanStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangngalan", pangngalanStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangngalan");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }

    @Override
    protected void onDestroy() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
