package com.example.habiaral.Palaro;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;


public class PalaroBaguhan extends AppCompatActivity {

    private Button answer1, answer2, answer3, answer4, answer5, answer6, selectedAnswer;
    private TextView baguhanQuestion;
    private ProgressBar timerBar;
    private Button unlockButton, unlockButton1;
    private boolean isGameOver = false;
    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 100000;
    private long timeLeft = TOTAL_TIME;
    private FirebaseFirestore db;
    private int correctAnswerCount = 0;
    private int baguhanScore = 0;
    private int currentQuestionNumber = 1;
    private static final String DOCUMENT_ID = "MP1";
    private boolean isAnswered = false;
    private boolean isTimeUp = false;
    private String studentID;
    private TextToSpeech tts;
    private int remainingHearts = 5;
    private int correctStreak = 0;
    private ImageView[] heartIcons;
    private List<String> questionIds = new ArrayList<>();
    private List<Map<String, Object>> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private long startTime;
    private boolean husayUnlocked = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro_baguhan);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale tagalogLocale = new Locale("fil", "PH");
                tts.setLanguage(tagalogLocale);
                tts.setSpeechRate(1.3f);

                new Handler().postDelayed(() -> loadCharacterLine("MCL1"), 300);
                new Handler().postDelayed(this::showCountdownThenLoadQuestion, 3000);
            }
        });

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
                findViewById(R.id.heart01),
                findViewById(R.id.heart02),
                findViewById(R.id.heart03),
                findViewById(R.id.heart04),
                findViewById(R.id.heart05)
        };

        db = FirebaseFirestore.getInstance();
        currentQuestionNumber = 1;
        correctAnswerCount = 0;
        baguhanScore = 0;

        View.OnClickListener answerClickListener = view -> {
            if (isTimeUp || isAnswered) return;
            resetAnswerBackgrounds();
            view.setBackgroundResource(R.drawable.answer_option_bg_selected);
            selectedAnswer = (Button) view;
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
                String correctDocId = questionIds.get(currentQuestionNumber);


                db.collection("baguhan").document(correctDocId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");

                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;
                                    baguhanScore += 5;
                                    correctStreak++;

                                    if (correctStreak == 1) {
                                        loadCharacterLine("MCL2");
                                    } else if (correctStreak == 2) {
                                        loadCharacterLine("MCL3");
                                    } else if (correctStreak >= 3) {
                                        loadCharacterLine("MCL4");
                                    }

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    saveCorrectBaguhanAnswer(uid, studentID, correctDocId);


                                    timeLeft = Math.min(timeLeft + 3000, TOTAL_TIME);
                                    startTimer(timeLeft);
                                } else {
                                    correctStreak = 0;
                                    deductHeart();
                                    if (remainingHearts > 0) {
                                        loadCharacterLine("MCL6");
                                    } else {
                                        loadCharacterLine("MCL5");
                                    }

                                    Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                                }
                            }


                            new Handler().postDelayed(() -> {
                                if (!isGameOver) {
                                    currentQuestionNumber++;
                                    resetForNextQuestion();
                                    loadBaguhanQuestion();
                                }
                            }, 2000);
                        });
            } else {
                Toast.makeText(this, "Paki pili muna ng sagot.", Toast.LENGTH_SHORT).show();
            }
        });

        unlockButton1.setOnLongClickListener(v -> {
            saveBaguhanScore();
            Toast.makeText(this, "Naitala na ang progreso. Paalam muna!", Toast.LENGTH_SHORT).show();
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });

        Button umalisButton = findViewById(R.id.UnlockButtonPalaro1);
        umalisButton.setOnClickListener(v -> showExitConfirmationDialog());
    }

    private void finishQuiz() {
        isGameOver = true;
        if (tts != null) {
            tts.stop();
        }
        saveBaguhanScore();
        unlockAchievementA8IfEligible();

        if (correctAnswerCount >= 20) {
            unlockPerfectStreakAchievement();
        }


        View showTotalPoints = getLayoutInflater().inflate(R.layout.dialog_box_time_up, null);
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

    private void saveCorrectBaguhanAnswer(String uid, String firebaseUID, String questionId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(firebaseUID).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) {
                Toast.makeText(this, "Walang studentId na nahanap.", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = studentDoc.getString("studentId");
            DocumentReference docRef = db.collection("palaro_answered").document(firebaseUID);

            docRef.get().addOnSuccessListener(answerDoc -> {
                boolean alreadyAnswered = false;

                if (answerDoc.exists()) {
                    Map<String, Object> baguhanMap = (Map<String, Object>) answerDoc.get("baguhan");
                    if (baguhanMap != null && Boolean.TRUE.equals(baguhanMap.get(questionId))) {
                        alreadyAnswered = true;
                    }
                }

                if (!alreadyAnswered) {
                    Map<String, Object> nestedBaguhan = new HashMap<>();
                    nestedBaguhan.put(questionId, true);

                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentId);
                    update.put("baguhan", nestedBaguhan);

                    docRef.set(update, SetOptions.merge());
                }

            });
        });
    }


    private void unlockAchievementA8IfEligible() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA8";
        String achievementId = "A8";

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");
            String firebaseUID = studentDoc.getId();

            db.collection("baguhan").get().addOnSuccessListener(allQuestionsSnapshot -> {
                List<String> allQuestionIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : allQuestionsSnapshot) {
                    allQuestionIds.add(doc.getId());
                }

                db.collection("palaro_answered").document(firebaseUID).get().addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    Map<String, Object> answeredMap = (Map<String, Object>) userDoc.get("baguhan");
                    if (answeredMap == null) return;

                    List<String> answeredIds = new ArrayList<>(answeredMap.keySet());

                    if (answeredIds.containsAll(allQuestionIds)) {
                        db.collection("student_achievements").document(firebaseUID).get().addOnSuccessListener(achSnapshot -> {
                            Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                            if (achievements != null && achievements.containsKey(achievementCode)) {
                                return;
                            }

                            continueUnlockingAchievement(firebaseUID, achievementCode, achievementId);
                        });
                    }

                });
            });
        });
    }

    private void continueUnlockingAchievement(String uid, String saCode, String achievementID) {
        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            db.collection("achievements").document(achievementID).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists()) return;

                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementID);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", Timestamp.now());

                Map<String, Object> achievementMap = new HashMap<>();
                achievementMap.put(saCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements", achievementMap);


                db.collection("student_achievements").document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            showAchievementUnlockedDialog(title, R.drawable.achievement08);
                        }));
            });
        });
    }


    private void unlockPerfectStreakAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA4";
        String achievementId = "A4";

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            db.collection("student_achievements").document(uid).get().addOnSuccessListener(achSnapshot -> {
                Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                if (achievements != null && achievements.containsKey(achievementCode)) {
                    return;
                }

                db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                    if (!achDoc.exists()) return;

                    String title = achDoc.getString("title");

                    Map<String, Object> achievementData = new HashMap<>();
                    achievementData.put("achievementID", achievementId);
                    achievementData.put("title", title);
                    achievementData.put("unlockedAt", Timestamp.now());

                    Map<String, Object> achievementMap = new HashMap<>();
                    achievementMap.put(achievementCode, achievementData);

                    Map<String, Object> wrapper = new HashMap<>();
                    wrapper.put("studentId", studentId);
                    wrapper.put("achievements", achievementMap);

                    db.collection("student_achievements").document(uid)
                            .set(wrapper, SetOptions.merge())
                            .addOnSuccessListener(unused -> runOnUiThread(() -> {
                                showAchievementUnlockedDialog(title,R.drawable.achievement04);
                            }));
                });
            });
        });
    }




    private void showAchievementUnlockedDialog(String title, int imageRes){
        LayoutInflater inflater = LayoutInflater.from(this);
        View toastView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = toastView.findViewById(R.id.imageView19);
        TextView tv = toastView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);

        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ssb.setSpan(new RelativeSizeSpan(1.3f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);
        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    private void saveBaguhanScore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists()) {
                Toast.makeText(this, "Student document not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = studentDoc.getString("studentId");
            if (studentId == null || studentId.isEmpty()) {
                Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference docRef = db.collection("minigame_progress").document(uid);

            docRef.get().addOnSuccessListener(snapshot -> {
                int husay = snapshot.contains("husay_score") ? snapshot.getLong("husay_score").intValue() : 0;
                int dalubhasa = snapshot.contains("dalubhasa_score") ? snapshot.getLong("dalubhasa_score").intValue() : 0;
                int oldBaguhan = snapshot.contains("baguhan_score") ? snapshot.getLong("baguhan_score").intValue() : 0;
                int newBaguhanTotal = oldBaguhan + baguhanScore;

                Map<String, Object> updates = new HashMap<>();
                updates.put("baguhan_score", newBaguhanTotal);
                updates.put("studentId", studentId);
                updates.put("total_score", newBaguhanTotal + husay + dalubhasa);

                docRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            if (newBaguhanTotal >= 400) {
                                unlockHusay(docRef);
                            }
                            baguhanScore = 0;
                        })
                        .addOnFailureListener(e -> Log.e("Firestore", "Error saving score", e));
            });
        });
    }

    private void unlockHusay(DocumentReference docRef) {
        if (husayUnlocked) return;
        husayUnlocked = true;

        Map<String, Object> updates = new HashMap<>();
        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Nabuksan na ang Husay!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Palaro.class));
                    finish();
                });
    }



    private void showCountdownThenLoadQuestion() {
        db.collection("baguhan").get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    questionIds.add(doc.getId());
                }

                Collections.shuffle(questionIds);

                currentQuestionNumber = 0;

                final Handler countdownHandler = new Handler();
                final int[] countdown = {3};

                countdownHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isGameOver) return;

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
            } else {
                Toast.makeText(this, "❗ Walang tanong sa database!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer(long startTime) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(startTime, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;

                int percent = (int) (timeLeft * 100 / TOTAL_TIME);
                timerBar.setProgress(percent);

                if (percent <= 25) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.timer_color_red));
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.timer_color_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.timer_color_green));
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
                if (tts != null) tts.stop();
                disableAnswerSelection();
                finishQuiz();
            }
        }
    }

    private void loadBaguhanQuestion() {
        if (isGameOver) return;
        startTime = System.currentTimeMillis();

        if (currentQuestionNumber >= questionIds.size()) {
            finishQuiz();
            return;
        }

        String questionDocId = questionIds.get(currentQuestionNumber);

        db.collection("baguhan").document(questionDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        finishQuiz();
                        return;
                    }

                    String question = documentSnapshot.getString("baguhan_question");

                    List<Object> rawchoices = (List<Object>) documentSnapshot.get("choices");
                    List<String> choices = new ArrayList<>();

                    if (rawchoices != null) {
                        for (Object obj : rawchoices) {
                            if (obj instanceof String) {
                                choices.add((String) obj);
                            }
                        }
                    }

                    if (question != null) {
                        runOnUiThread(() -> {
                            baguhanQuestion.setText(question);
                            String spokenVersion = question.replaceAll("_+", "blanko");
                            speakText(spokenVersion);
                        });
                    }

                    if (choices.size() >= 6) {
                        runOnUiThread(() -> {
                            answer1.setVisibility(View.VISIBLE);
                            answer2.setVisibility(View.VISIBLE);
                            answer3.setVisibility(View.VISIBLE);
                            answer4.setVisibility(View.VISIBLE);
                            answer5.setVisibility(View.VISIBLE);
                            answer6.setVisibility(View.VISIBLE);

                            answer1.setText(choices.get(0));
                            answer2.setText(choices.get(1));
                            answer3.setText(choices.get(2));
                            answer4.setText(choices.get(3));
                            answer5.setText(choices.get(4));
                            answer6.setText(choices.get(5));
                        });
                    } else {
                        Toast.makeText(this, "Invalid choices sa tanong: " + questionDocId, Toast.LENGTH_SHORT).show();
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
                        if (line != null) {
                            baguhanQuestion.setText(line);
                            speakText(line);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void showBackConfirmationDialog() {
        View backDialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit_palaro, null);

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
            saveBaguhanScore();
            backDialog.dismiss();
            finish();
        });

        noButton.setOnClickListener(v -> backDialog.dismiss());

        backDialog.show();
    }

    private void speakText(String text) {
        if (tts != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showExitConfirmationDialog() {
        View backDialogView = getLayoutInflater().inflate(R.layout.dialog_box_exit_palaro, null);
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

}

