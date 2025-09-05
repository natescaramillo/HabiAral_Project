package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.LessonProgressCache;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangUkolLesson;
import com.example.habiaral.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PangawingQuiz extends AppCompatActivity {

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
    private String introText = "";

    private int correctAnswers = 0;
    private int totalQuestions = 0;
    private int currentIndex = -1;
    private ProgressBar timerBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangawing_quiz);

        questionTitle = findViewById(R.id.questionTitle);
        questionText = findViewById(R.id.pangawing_questionText);
        answer1 = findViewById(R.id.answer1);
        answer2 = findViewById(R.id.answer2);
        answer3 = findViewById(R.id.answer3);
        nextButton = findViewById(R.id.pangawingNextButton);
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
        db.collection("quiz").document("Q10")
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
        Button finishButton = dialogView.findViewById(R.id.finishButton);
        Button homeButton = dialogView.findViewById(R.id.returnButton);

        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView scoreNumber = dialogView.findViewById(R.id.textView6);
        TextView resultText = dialogView.findViewById(R.id.textView7);

        progressBar.setMax(totalQuestions);
        progressBar.setProgress(correctAnswers);
        scoreNumber.setText(correctAnswers + "/" + totalQuestions);

        if (correctAnswers >= 6) {
            resultText.setText("Ikaw ay nakapasa!");
            finishButton.setEnabled(true);
        } else {
            resultText.setText("Ikaw ay nabigo, subukan muli!");
            finishButton.setEnabled(false);
            finishButton.setAlpha(0.5f);
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

        finishButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            Intent intent = new Intent(PangawingQuiz.this, BahagiNgPananalita.class); // or next lesson
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            if (resultDialog.isShowing()) resultDialog.dismiss();
            Intent intent = new Intent(PangawingQuiz.this, BahagiNgPananalita.class);
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

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        Map<String, Object> pangawingStatus = new HashMap<>();
        pangawingStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangawing", pangawingStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangawing");

        Map<String, Object> moduleUpdate = Map.of("module_1", updateMap);

        db.collection("module_progress")
                .document(uid)
                .set(moduleUpdate, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    checkAndUnlockAchievement();
                });

        if (LessonProgressCache.getData() != null) {
            Map<String, Object> cachedData = LessonProgressCache.getData();
            if (!cachedData.containsKey("module_1")) cachedData.put("module_1", new HashMap<String, Object>());
            Map<String, Object> cachedModule1 = (Map<String, Object>) cachedData.get("module_1");
            cachedModule1.put("lessons", lessonsMap);
            cachedModule1.put("current_lesson", "pangawing");
            LessonProgressCache.setData(cachedData);
        }
    }

    private void showAchievementUnlockedDialog(String title, int imageRes) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = dialogView.findViewById(R.id.imageView19);
        TextView tv = dialogView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.1f), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

            // 👉 Gamitin LayoutParams para makuha yung offset na parang toast
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50; // offset mula sa taas (px)
            dialog.getWindow().setAttributes(params);
        }

        // 🎵 Play sound sabay sa pop up
        dialog.setOnShowListener(d -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.achievement_pop);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        });

        dialog.show();
    }
    private void checkAndUnlockAchievement() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String saCode = "SA11";
        String achievementId = "A11";

        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
            if (module1 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
            if (lessons == null) return;

            String[] lessonKeys = {
                    "pangngalan", "pandiwa", "panguri", "panghalip", "pangabay",
                    "pangatnig", "pangukol", "pangakop", "padamdam", "pangawing"
            };

            for (String key : lessonKeys) {
                Map<String, Object> lesson = (Map<String, Object>) lessons.get(key);
                if (lesson == null || !"completed".equals(lesson.get("status"))) {
                    return;
                }
            }

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(saSnapshot -> {
                if (saSnapshot.exists()) {
                    Map<String, Object> achievements = (Map<String, Object>) saSnapshot.get("achievements");
                    if (achievements != null && achievements.containsKey(saCode)) return;
                }
                continueUnlockingAchievement(db, uid, saCode, achievementId);
            });
        });
    }

    private void continueUnlockingAchievement(FirebaseFirestore db, String uid, String saCode, String achievementID) {
        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;
            String studentId = studentDoc.getString("studentId");

            db.collection("achievements").document(achievementID).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists() || !achDoc.contains("title")) return;
                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementID);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", Timestamp.now());

                Map<String, Object> achievementsMap = new HashMap<>();
                achievementsMap.put(saCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements", achievementsMap);

                db.collection("student_achievements")
                        .document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            showAchievementUnlockedDialog(title, R.drawable.achievement11);
                        }));
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
