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
import com.example.habiaral.BahagiNgPananalita.Lessons.PandiwaLesson;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.habiaral.BahagiNgPananalita.LessonProgressCache;

public class PangngalanQuiz extends AppCompatActivity {

    private List<Map<String, Object>> quizList = new ArrayList<>();
    private Button answer1, answer2, answer3, nextButton;
    private TextView questionText, questionTitle;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 10000;
    private boolean quizFinished = false;
    private boolean isAnswered = false;
    private String correctAnswer = "";
    private AlertDialog resultDialog;
    private String lessonName = "";
    private int correctAnswers = 0;
    private int totalQuestions = 0;
    private String introText = "";
    private int currentIndex = -1;
    private ProgressBar timerBar;
    private FirebaseFirestore db;

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

        loadQuizDocument();

        View.OnClickListener choiceClickListener = view -> {
            if (isAnswered) return;
            isAnswered = true;
            nextButton.setEnabled(true);
            disableAnswers();

            Button selected = (Button) view;
            String selectedAnswer = selected.getText().toString();

            if (selectedAnswer.equals(correctAnswer)) {
                correctAnswers++;
            }
        };

        answer1.setOnClickListener(choiceClickListener);
        answer2.setOnClickListener(choiceClickListener);
        answer3.setOnClickListener(choiceClickListener);

        nextButton.setOnClickListener(v -> {
            if (currentIndex == -1) {
                showCountdownThenLoadQuestion();
                return;
            }

            if (!isAnswered) {
                Toast.makeText(this, "Pumili muna ng sagot bago mag-next!", Toast.LENGTH_SHORT).show();
                return;
            }

            currentIndex++;

            if (currentIndex < quizList.size()) {
                loadQuestion(currentIndex);
            } else {
                quizFinished = true;
                if (countDownTimer != null) countDownTimer.cancel();

                if (correctAnswers >= 6) {
                    unlockNextLesson();
                    saveQuizResultToFirestore();
                }

                showResultDialog();
            }
        });
    }

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
                    currentIndex = 0;
                    loadQuestion(currentIndex);
                }, 1000);
            }, 1000);
        }, 1000);
    }

    private void loadQuizDocument() {
        db.collection("quiz").document("Q1")
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        introText = doc.getString("intro");
                        lessonName = doc.getString("lesson");
                        quizList = (List<Map<String, Object>>) doc.get("Quizzes");

                        if (quizList != null && !quizList.isEmpty()) {
                            Collections.shuffle(quizList);
                        }

                        if (introText != null) {
                            questionTitle.setText("Simula");
                            questionText.setText(introText);
                            answer1.setVisibility(View.GONE);
                            answer2.setVisibility(View.GONE);
                            answer3.setVisibility(View.GONE);
                            nextButton.setEnabled(true);
                        }
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load quiz data.", Toast.LENGTH_SHORT).show());
    }

    private void loadQuestion(int index) {
        if (countDownTimer != null) countDownTimer.cancel();
        if (quizList == null || quizList.isEmpty()) return;

        Map<String, Object> qData = quizList.get(index);

        String question = (String) qData.get("question");
        List<String> choices = (List<String>) qData.get("choices");
        correctAnswer = (String) qData.get("correct_choice");

        if (choices != null && choices.size() >= 3) {
            List<String> shuffledChoices = new ArrayList<>(choices);
            Collections.shuffle(shuffledChoices);
            questionTitle.setText(getQuestionOrdinal(index + 1));
            questionText.setText(question);

            answer1.setVisibility(View.VISIBLE);
            answer2.setVisibility(View.VISIBLE);
            answer3.setVisibility(View.VISIBLE);

            answer1.setText(shuffledChoices.get(0));
            answer2.setText(shuffledChoices.get(1));
            answer3.setText(shuffledChoices.get(2));

            resetButtons();
            startTimer();

            totalQuestions = quizList.size();
        }
    }

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

                // ✅ Compute percentage
                int percent = (int) ((timeLeftInMillis * 100) / 10000);

                if (percent <= 25) {
                    timerBar.setProgressDrawable(
                            androidx.core.content.ContextCompat.getDrawable(PangngalanQuiz.this, R.drawable.timer_color_red)
                    );
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(
                            androidx.core.content.ContextCompat.getDrawable(PangngalanQuiz.this, R.drawable.timer_color_orange)
                    );
                } else {
                    timerBar.setProgressDrawable(
                            androidx.core.content.ContextCompat.getDrawable(PangngalanQuiz.this, R.drawable.timer_color_green)
                    );
                }
            }

            @Override
            public void onFinish() {
                if (quizFinished) return;
                isAnswered = true;
                disableAnswers();
                nextButton.setEnabled(true);

                new Handler().postDelayed(() -> {
                    if (!quizFinished) nextButton.performClick();
                }, 500);
            }
        }.start();
    }

    private void showResultDialog() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_quiz_score, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button retryButton = dialogView.findViewById(R.id.retryButton);
        Button taposButton = dialogView.findViewById(R.id.finishButton);
        Button homeButton = dialogView.findViewById(R.id.returnButton);

        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView scoreNumber = dialogView.findViewById(R.id.textView6);
        TextView resultText = dialogView.findViewById(R.id.textView7);

        progressBar.setMax(totalQuestions);
        progressBar.setProgress(correctAnswers);
        scoreNumber.setText(correctAnswers + "/" + totalQuestions);

        if (correctAnswers >= 6) {
            resultText.setText("Ikaw ay nakapasa!");
            taposButton.setEnabled(true);
        } else {
            resultText.setText("Ikaw ay nabigo, subukan muli!");
            taposButton.setEnabled(false);
            taposButton.setAlpha(0.5f);
        }

        resultDialog = builder.create();
        resultDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (!isFinishing()) resultDialog.show();

        retryButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            currentIndex = 0;
            correctAnswers = 0;
            Collections.shuffle(quizList);
            loadQuestion(currentIndex);
        });

        taposButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            Intent intent = new Intent(PangngalanQuiz.this, PandiwaLesson.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            Intent intent = new Intent(PangngalanQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

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

        Map<String, Object> moduleUpdate = Map.of("module_1", updateMap);

        db.collection("module_progress")
                .document(uid)
                .set(moduleUpdate, SetOptions.merge());

        if (LessonProgressCache.getData() != null) {
            Map<String, Object> cachedData = LessonProgressCache.getData();

            if (!cachedData.containsKey("module_1")) {
                cachedData.put("module_1", new HashMap<String, Object>());
            }

            Map<String, Object> cachedModule1 = (Map<String, Object>) cachedData.get("module_1");
            cachedModule1.put("lessons", lessonsMap);
            cachedModule1.put("current_lesson", "pangngalan");

            LessonProgressCache.setData(cachedData);
        }
    }

    @Override
    protected void onDestroy() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
