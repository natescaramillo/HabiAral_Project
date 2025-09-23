package com.example.habiaral.Panitikan.Parabula.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Parabula.Parabula;
import com.example.habiaral.Panitikan.Parabula.Stories.ParabulaKwento2;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;
import com.example.habiaral.Utils.TimerSoundUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ParabulaKwento1Quiz extends AppCompatActivity {

    private TextView questionTitle, questionText;
    private ProgressBar timerBar;
    private Button answer1, answer2, answer3, introButton, nextButton;
    private View bottomBar;

    private FirebaseFirestore db;
    private String uid;

    private List<Map<String, Object>> allQuizList = new ArrayList<>();
    private List<Map<String, Object>> quizList = new ArrayList<>();
    private int currentIndex = -1;
    private int correctAnswers = 0;
    private int totalQuestions = 0;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 30000;
    private boolean quizFinished = false;
    private boolean isAnswered = false;

    private static final String STORY_ID = "ParabulaKwento1";
    private static final String STORY_TITLE = "Alibughang Anak";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_parabula_kwento1_quiz);

        questionTitle = findViewById(R.id.questionTitle);
        questionText = findViewById(R.id.kwento4_questionText);
        timerBar = findViewById(R.id.timerBar);
        answer1 = findViewById(R.id.answer1);
        answer2 = findViewById(R.id.answer2);
        answer3 = findViewById(R.id.answer3);
        introButton = findViewById(R.id.intro_button);
        nextButton = findViewById(R.id.kwento4NextButton);
        bottomBar = findViewById(R.id.bottomBar);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        answer1.setVisibility(View.GONE);
        answer2.setVisibility(View.GONE);
        answer3.setVisibility(View.GONE);
        timerBar.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
        introButton.setVisibility(View.VISIBLE);

        loadQuizDocument();

        introButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showCountdownThenLoadQuestion();
        });

        View.OnClickListener choiceClickListener = v -> {
            if (isAnswered) return;
            isAnswered = true;
            nextButton.setEnabled(true);
            Button b = (Button) v;
            evaluateSelectedAnswer(b.getText().toString());
        };

        answer1.setOnClickListener(choiceClickListener);
        answer2.setOnClickListener(choiceClickListener);
        answer3.setOnClickListener(choiceClickListener);

        nextButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);

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

                if (correctAnswers >= Math.min(6, quizList.size())) {
                    markStoryCompleted();
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

    private void loadQuizDocument() {
        db.collection("quiz").document("Q23")
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String intro = doc.getString("intro");
                        if (intro != null && !intro.isEmpty()) {
                            questionTitle.setText("Simula");
                            questionText.setText(intro);
                            nextButton.setEnabled(true);
                        }

                        allQuizList = (List<Map<String, Object>>) doc.get("Quizzes");
                        if (allQuizList != null && !allQuizList.isEmpty()) {
                            Collections.shuffle(allQuizList);
                            int limit = Math.min(10, allQuizList.size());
                            quizList = new ArrayList<>(allQuizList.subList(0, limit));
                            totalQuestions = quizList.size();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Nabigong kunin ang quiz data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCountdownThenLoadQuestion() {
        questionTitle.setText("Simula");
        questionText.setText("3");
        playReadySoundIfAvailable();

        new Handler().postDelayed(() -> {
            questionText.setText("2");
            playReadySoundIfAvailable();
        }, 1000);

        new Handler().postDelayed(() -> {
            questionText.setText("1");
            playReadySoundIfAvailable();
        }, 2000);

        new Handler().postDelayed(() -> {
            questionTitle.setText("Unang tanong");
            currentIndex = 0;
            correctAnswers = 0;
            isAnswered = false;
            quizFinished = false;

            introButton.setVisibility(View.GONE);
            answer1.setVisibility(View.VISIBLE);
            answer2.setVisibility(View.VISIBLE);
            answer3.setVisibility(View.VISIBLE);
            timerBar.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);

            if (quizList == null || quizList.isEmpty()) {
                Toast.makeText(this, "Walang tanong para sa quiz na ito.", Toast.LENGTH_LONG).show();
                return;
            }

            loadQuestion(currentIndex);
        }, 3000);
    }

    private void loadQuestion(int index) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (quizList == null || index < 0 || index >= quizList.size()) return;

        Map<String, Object> qData = quizList.get(index);
        String question = (String) qData.get("question");
        List<String> choices = (List<String>) qData.get("choices");

        questionTitle.setText(getQuestionOrdinal(index + 1));
        questionText.setText(question);

        if (choices == null) choices = new ArrayList<>();
        while (choices.size() < 3) choices.add("—");

        List<String> shuffled = new ArrayList<>(choices);
        Collections.shuffle(shuffled);

        answer1.setText(shuffled.get(0));
        answer2.setText(shuffled.get(1));
        answer3.setText(shuffled.get(2));

        answer1.setEnabled(true);
        answer2.setEnabled(true);
        answer3.setEnabled(true);
        nextButton.setEnabled(false);
        isAnswered = false;

        timerBar.setMax((int) timeLeftInMillis);
        timerBar.setProgress((int) timeLeftInMillis);

        startTimer();
    }

    private void evaluateSelectedAnswer(String selectedAnswer) {
        answer1.setEnabled(false);
        answer2.setEnabled(false);
        answer3.setEnabled(false);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        Map<String, Object> qData = quizList.get(currentIndex);
        Object correctObj = qData.get("correct_choice");
        String correctChoice = correctObj != null ? correctObj.toString() : "";

        if (selectedAnswer.equals(correctChoice)) {
            correctAnswers++;
        } else {
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeLeftInMillis = 30000;

        timerBar.setMax((int) timeLeftInMillis);
        timerBar.setProgress((int) timeLeftInMillis);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int progress = (int) Math.min(millisUntilFinished, Integer.MAX_VALUE);
                timerBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                if (quizFinished) return;
                isAnswered = true;
                answer1.setEnabled(false);
                answer2.setEnabled(false);
                answer3.setEnabled(false);
                nextButton.setEnabled(true);

                new Handler().postDelayed(() -> {
                    currentIndex++;
                    if (currentIndex < quizList.size()) {
                        loadQuestion(currentIndex);
                    } else {
                        quizFinished = true;
                        showResultDialog();
                    }
                }, 600);
            }
        }.start();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit, null);
        builder.setView(dialogView);

        AlertDialog exitDialog = builder.create();
        if (exitDialog.getWindow() != null)
            exitDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button yesBtn = dialogView.findViewById(R.id.button5);
        Button noBtn = dialogView.findViewById(R.id.button6);

        yesBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (countDownTimer != null) countDownTimer.cancel();
            exitDialog.dismiss();
            finish();
        });

        noBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            exitDialog.dismiss();
        });

        exitDialog.show();
    }

    private void showResultDialog() {
        if (countDownTimer != null) countDownTimer.cancel();
        quizFinished = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_box_quiz_score, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        TextView resultText = dialogView.findViewById(R.id.textView7);
        TextView scoreNumber = dialogView.findViewById(R.id.textView6);
        if (scoreNumber != null) scoreNumber.setText(correctAnswers + "/" + totalQuestions);
        if (resultText != null) {
            boolean passed = correctAnswers >= Math.min(6, totalQuestions);
            resultText.setText(passed ? "Ikaw ay nakapasa!" : "Ikaw ay nabigo, subukan muli!");
        }

        Button retryButton = dialogView.findViewById(R.id.retryButton);
        Button taposButton = dialogView.findViewById(R.id.finishButton);
        Button homeButton = dialogView.findViewById(R.id.returnButton);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        retryButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            dialog.dismiss();
            resetQuizForRetry();
        });

        taposButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            dialog.dismiss();
            boolean passed = correctAnswers >= Math.min(6, totalQuestions);
            if (passed) {
                markStoryCompleted();
                startActivity(new Intent(ParabulaKwento1Quiz.this, ParabulaKwento2.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            } else {
                Toast.makeText(this, "Kailangan pumasa para makapunta sa Kwento 2!", Toast.LENGTH_SHORT).show();
            }
        });

        homeButton.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            dialog.dismiss();
            startActivity(new Intent(this, Parabula.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
    }

    private void resetQuizForRetry() {
        if (allQuizList == null || allQuizList.isEmpty()) {
            Toast.makeText(this, "Walang mga tanong, subukang i-restart ang app.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        correctAnswers = 0;
        quizFinished = false;
        Collections.shuffle(allQuizList);
        int limit = Math.min(10, allQuizList.size());
        quizList = new ArrayList<>(allQuizList.subList(0, limit));
        totalQuestions = quizList.size();
        currentIndex = 0;

        introButton.setVisibility(View.GONE);
        answer1.setVisibility(View.VISIBLE);
        answer2.setVisibility(View.VISIBLE);
        answer3.setVisibility(View.VISIBLE);
        timerBar.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        loadQuestion(currentIndex);
    }

    private void markStoryCompleted() {
        if (uid == null) return;

        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("module_3.categories.Parabula.stories." + STORY_ID + ".title", STORY_TITLE);
        updates.put("module_3.categories.Parabula.stories." + STORY_ID + ".status", "completed");

        db.collection("module_progress").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Next Lesson Unlocked: Kwento2!", Toast.LENGTH_SHORT).show();
                    checkIfCategoryCompleted("Parabula");
                })
                .addOnFailureListener(e ->
                        db.collection("module_progress").document(uid).set(updates, SetOptions.merge()));
    }

    private void checkIfCategoryCompleted(String categoryName) {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> stories = (Map<String, Object>)
                            snapshot.get("module_3.categories." + categoryName + ".stories");
                    if (stories == null) return;

                    String[] requiredStories = {"ParabulaKwento1", "ParabulaKwento2"};

                    boolean allCompleted = true;
                    for (String storyKey : requiredStories) {
                        Object storyObj = stories.get(storyKey);
                        if (!(storyObj instanceof Map)) {
                            allCompleted = false;
                            break;
                        }
                        Map<String, Object> storyData = (Map<String, Object>) storyObj;
                        if (!"completed".equals(storyData.get("status"))) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        db.collection("module_progress").document(uid)
                                .set(Map.of("module_3",
                                        Map.of("categories",
                                                Map.of(categoryName, Map.of("status", "completed"))
                                        )), SetOptions.merge())
                                .addOnSuccessListener(unused -> checkIfModuleCompleted());
                    }
                });
    }

    private void checkIfModuleCompleted() {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> categories =
                            (Map<String, Object>) snapshot.get("module_3.categories");
                    if (categories == null) return;

                    String[] requiredCategories = {"Alamat", "Epiko", "Maikling Kuwento", "Pabula", "Parabula"};

                    boolean allCompleted = true;
                    for (String categoryKey : requiredCategories) {
                        Object catObj = categories.get(categoryKey);
                        if (!(catObj instanceof Map)) {
                            allCompleted = false;
                            break;
                        }
                        Map<String, Object> catData = (Map<String, Object>) catObj;
                        if (!"completed".equals(catData.get("status"))) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        db.collection("module_progress").document(uid)
                                .set(Map.of("module_3", Map.of(
                                        "status", "completed",
                                        "modulename", "Panitikan"
                                )), SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    com.example.habiaral.Utils.AchievementM3Utils.checkAndUnlockAchievement(this, db, uid);
                                });
                    }
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

    private void playReadySoundIfAvailable() {
        try {
            com.example.habiaral.Utils.SoundClickUtils.playClickSound(this, R.raw.ready_start);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        TimerSoundUtils.setVolume(0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimerSoundUtils.setVolume(1f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
