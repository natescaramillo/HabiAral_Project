package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.LessonProgressCache;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangUkolLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangUriLesson;
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

public class PangatnigQuiz extends AppCompatActivity {

    private List<Map<String, Object>> quizList = new ArrayList<>();
    private Button answer1, answer2, answer3, nextButton, introButton;
    private TextView questionText, questionTitle;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 30000;
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
    private MediaPlayer mediaPlayer;
    private View background;
    private int lastColorStage = 3;
    private MediaPlayer resultPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangatnig_quiz);

        questionTitle = findViewById(R.id.questionTitle);
        questionText = findViewById(R.id.pangatnig_questionText);
        nextButton = findViewById(R.id.pangatnigNextButton);
        timerBar = findViewById(R.id.timerBar);
        introButton = findViewById(R.id.intro_button);
        background = findViewById(R.id.bottomBar);

        answer1 = findViewById(R.id.answer1);
        answer2 = findViewById(R.id.answer2);
        answer3 = findViewById(R.id.answer3);

        db = FirebaseFirestore.getInstance();

        answer1.setVisibility(View.GONE);
        answer2.setVisibility(View.GONE);
        answer3.setVisibility(View.GONE);
        timerBar.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        background.setVisibility(View.GONE);

        introButton.setVisibility(View.VISIBLE);

        loadQuizDocument();

        introButton.setOnClickListener(v -> {
            playClickSound();

            showCountdownThenLoadQuestion();
        });

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
            playClickSound();

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
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

                    introButton.setVisibility(View.GONE);
                    answer1.setVisibility(View.VISIBLE);
                    answer2.setVisibility(View.VISIBLE);
                    answer3.setVisibility(View.VISIBLE);
                    timerBar.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                    background.setVisibility(View.VISIBLE);
                }, 1000);
            }, 1000);
        }, 1000);
    }

    private void loadQuizDocument() {
        db.collection("quiz").document("Q6")
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
                            nextButton.setEnabled(true);
                        }
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load quiz data.", Toast.LENGTH_SHORT).show());
    }

    private void loadQuestion(int index) {
        if (countDownTimer != null) countDownTimer.cancel();
        if (quizList == null || quizList.isEmpty()) return;

        timerBar.setVisibility(View.VISIBLE);

        Map<String, Object> qData = quizList.get(index);

        String question = (String) qData.get("question");
        List<String> choices = (List<String>) qData.get("choices");
        correctAnswer = (String) qData.get("correct_choice");

        if (choices != null && choices.size() >= 3) {
            List<String> shuffledChoices = new ArrayList<>(choices);
            Collections.shuffle(shuffledChoices);
            questionTitle.setText(getQuestionOrdinal(index + 1));
            questionText.setText(question);

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
        timeLeftInMillis = 30000;

        lastColorStage = 3;

        timerBar.setMax((int) timeLeftInMillis);
        timerBar.setProgress((int) timeLeftInMillis);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;

                int progress = (int) millisUntilFinished;

                ObjectAnimator animation = ObjectAnimator.ofInt(timerBar, "progress", timerBar.getProgress(), progress);
                animation.setDuration(50);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();

                int percent = (int) ((timeLeftInMillis * 100) / 30000);

                if (percent <= 25 && lastColorStage == 1) {
                    timerBar.setProgressDrawable(
                            ContextCompat.getDrawable(PangatnigQuiz.this, R.drawable.timer_color_red)
                    );
                    playTimerSound(R.raw.red_timer);
                    lastColorStage = 0;
                } else if (percent <= 50 && lastColorStage == 2) {
                    timerBar.setProgressDrawable(
                            ContextCompat.getDrawable(PangatnigQuiz.this, R.drawable.timer_color_orange)
                    );
                    playTimerSound(R.raw.orange_timer);
                    lastColorStage--;
                } else if (percent > 50 && lastColorStage == 3) {
                    timerBar.setProgressDrawable(
                            ContextCompat.getDrawable(PangatnigQuiz.this, R.drawable.timer_color_green)
                    );
                    playTimerSound(R.raw.green_timer);
                    lastColorStage--;
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

    private void stopTimerSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playTimerSound(int resId) {
        stopTimerSound();

        mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setVolume(0.5f, 0.5f);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
        });
        mediaPlayer.start();
    }

    private void playClickSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.quiz_click);
        mp.setVolume(0.5f, 0.5f);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);
        builder.setView(dialogView);

        AlertDialog exitDialog = builder.create();
        exitDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button yesBtn = dialogView.findViewById(R.id.button5);
        Button noBtn = dialogView.findViewById(R.id.button6);

        yesBtn.setOnClickListener(v -> {
            playClickSound();
            stopTimerSound();
            if (countDownTimer != null) countDownTimer.cancel();
            exitDialog.dismiss();
            finish();
        });

        noBtn.setOnClickListener(v -> {
            playClickSound();
            exitDialog.dismiss();
        });

        exitDialog.show();
    }

    private void showResultDialog() {
        stopTimerSound();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
        releaseResultPlayer();

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

        boolean passed = correctAnswers >= 6;
        if (passed) {
            resultText.setText("Ikaw ay nakapasa!");
            taposButton.setEnabled(true);
            taposButton.setAlpha(1.0f);
        } else {
            resultText.setText("Ikaw ay nabigo, subukan muli!");
            taposButton.setEnabled(false);
            taposButton.setAlpha(0.5f);
        }

        resultDialog = builder.create();
        if (resultDialog.getWindow() != null) {
            resultDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        resultDialog.setOnShowListener(d -> {
            releaseResultPlayer();
            int soundRes = passed ? R.raw.success : R.raw.game_over;
            resultPlayer = MediaPlayer.create(PangatnigQuiz.this, soundRes);
            if (resultPlayer != null) {
                resultPlayer.setVolume(0.6f, 0.6f);
                resultPlayer.setOnCompletionListener(mp -> releaseResultPlayer());
                try {
                    resultPlayer.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    releaseResultPlayer();
                }
            }
        });

        if (!isFinishing() && !isDestroyed()) {
            try {
                resultDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        retryButton.setOnClickListener(v -> {
            playClickSound();
            dismissAndReleaseResultDialog();
            resetQuizForRetry();
        });

        taposButton.setOnClickListener(v -> {
            playClickSound();
            dismissAndReleaseResultDialog();
            navigateToLesson(PangUkolLesson.class);
        });

        homeButton.setOnClickListener(v -> {
            playClickSound();
            dismissAndReleaseResultDialog();
            navigateToLesson(BahagiNgPananalita.class);
        });
    }

    private void releaseResultPlayer() {
        if (resultPlayer != null) {
            if (resultPlayer.isPlaying()) {
                resultPlayer.stop();
            }
            resultPlayer.release();
            resultPlayer = null;
        }
    }

    private void dismissAndReleaseResultDialog() {
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
        releaseResultPlayer();
        resultDialog = null;
    }

    private void resetQuizForRetry() {
        currentIndex = 0;
        correctAnswers = 0;
        isAnswered = false;
        quizFinished = false;

        if (quizList != null && !quizList.isEmpty()) {
            Collections.shuffle(quizList);
        } else {
            Toast.makeText(this, "Walang mga tanong, subukang i-restart ang app.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        introButton.setVisibility(View.GONE);
        answer1.setVisibility(View.VISIBLE);
        answer2.setVisibility(View.VISIBLE);
        answer3.setVisibility(View.VISIBLE);
        timerBar.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        background.setVisibility(View.VISIBLE);

        loadQuestion(currentIndex);
    }

    private void navigateToLesson(Class<?> lessonActivityClass) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopTimerSound();
        releaseResultPlayer();

        Intent intent = new Intent(PangatnigQuiz.this, lessonActivityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
        Toast.makeText(this, "Next Lesson Unlocked: Pang-ukol!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangatnigStatus = new HashMap<>();
        pangatnigStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangatnig", pangatnigStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangatnig");

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
            cachedModule1.put("current_lesson", "pangatnig");

            LessonProgressCache.setData(cachedData);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopTimerSound();
        releaseResultPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
        resultDialog = null;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        stopTimerSound();
        releaseResultPlayer();
    }
}
