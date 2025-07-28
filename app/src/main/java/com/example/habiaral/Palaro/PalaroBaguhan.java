package com.example.habiaral.Palaro;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;


public class PalaroBaguhan extends AppCompatActivity {

    private Button  answer1, answer2, answer3, answer4, answer5, answer6, selectedAnswer;
    private TextView baguhanQuestion;
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

    private String studentID;
    private TextToSpeech tts;

    private int remainingHearts = 5;
    private int correctStreak = 0;
    private ImageView[] heartIcons;
    private List<String> questionIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro_baguhan);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale tagalogLocale = new Locale("fil", "PH");
                int langResult = tts.setLanguage(tagalogLocale);
                tts.setSpeechRate(1.3f);

                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "❗ TTS: Wikang Tagalog hindi suportado.", Toast.LENGTH_SHORT).show();
                } else {
                    // ✅ TTS ready —> Call character line now
                    loadCharacterLine("MCL1");

                    // ✅ Start countdown after a short delay
                    new Handler().postDelayed(this::showCountdownThenLoadQuestion, 3000);
                }
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
                findViewById(R.id.imageView8),
                findViewById(R.id.imageView11),
                findViewById(R.id.imageView10),
                findViewById(R.id.imageView9),
                findViewById(R.id.imageView12)
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
                String correctDocId = questionIds.get(currentQuestionNumber).replace("B", "BCA");

                db.collection("baguhan_correct_answers").document(correctDocId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String correctAnswer = documentSnapshot.getString("correctAnswer");
                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;
                                    baguhanScore += 5;
                                    correctStreak++;

                                    // Set MCL based on streak
                                    if (correctStreak == 1) {
                                        loadCharacterLine("MCL2");
                                    } else if (correctStreak == 2) {
                                        loadCharacterLine("MCL3");
                                    } else if (correctStreak >= 3) {
                                        loadCharacterLine("MCL4");
                                    }

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();

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
                                currentQuestionNumber++;
                                resetForNextQuestion();
                                loadBaguhanQuestion();
                            }, 2000);
                        });
            } else {
                Toast.makeText(this, "Paki pili muna ng sagot.", Toast.LENGTH_SHORT).show();
            }
        });

        unlockButton1.setOnLongClickListener(v -> {
            saveBaguhanScore();
            Toast.makeText(this, "Naitala na ang progreso. Paalam muna!", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });

        Button umalisButton = findViewById(R.id.UnlockButtonPalaro1); // palitan kung ibang ID
        umalisButton.setOnClickListener(v -> showExitConfirmationDialog());
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid(); // Firebase UID

        // Step 1: Get studentId from students collection
        db.collection("students").document(userId).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists()) {
                Toast.makeText(this, "Student document not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentID = studentDoc.getString("studentId"); // ✅ Now this is properly fetched
            DocumentReference docRef = db.collection("minigame_progress").document(userId);

            // Step 2: Check if minigame_progress already exists
            docRef.get().addOnSuccessListener(snapshot -> {
                int husay = snapshot.contains("husay_score") ? snapshot.getLong("husay_score").intValue() : 0;
                int dalubhasa = snapshot.contains("dalubhasa_score") ? snapshot.getLong("dalubhasa_score").intValue() : 0;
                int oldBaguhan = snapshot.contains("baguhan_score") ? snapshot.getLong("baguhan_score").intValue() : 0;
                int newBaguhanTotal = oldBaguhan + baguhanScore;

                if (snapshot.exists() && snapshot.contains("minigame_progressID")) {
                    // Reuse existing ID
                    String existingProgressId = snapshot.getString("minigame_progressID");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("baguhan_score", newBaguhanTotal);
                    updates.put("studentID", studentID); // ✅ Correct student ID
                    updates.put("minigame_progressID", existingProgressId);
                    updates.put("total_score", newBaguhanTotal + husay + dalubhasa);

                    docRef.set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                if (newBaguhanTotal >= 400) unlockHusay(docRef);
                                baguhanScore = 0; // ✅ Reset after save

                            });

                } else {
                    // Generate new minigame_progressID
                    db.collection("minigame_progress").get().addOnSuccessListener(querySnapshot -> {
                        int nextNumber = querySnapshot.size() + 1;
                        String newProgressId = "MP" + nextNumber;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("baguhan_score", newBaguhanTotal);
                        updates.put("studentID", studentID); // ✅ Correct student ID
                        updates.put("minigame_progressID", newProgressId);
                        updates.put("total_score", newBaguhanTotal + husay + dalubhasa);

                        docRef.set(updates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    if (newBaguhanTotal >= 400) unlockHusay(docRef);
                                });
                    });
                }
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to fetch student info", Toast.LENGTH_SHORT).show();
        });
    }



    private void unlockHusay(DocumentReference docRef) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("husay_unlocked", true);

        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Nakaabot ka sa 400! Na-unlock ang Husay!", Toast.LENGTH_LONG).show();
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

                // Shuffle questions para randomized
                Collections.shuffle(questionIds);

                // Reset tracker
                currentQuestionNumber = 0;

                // Mag-countdown muna bago simulan
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

                // Change color based on time left
                if (percent <= 25) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.custom_progress_drawable_red));
                } else if (percent <= 50) {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.custom_progress_drawable_orange));
                } else {
                    timerBar.setProgressDrawable(ContextCompat.getDrawable(PalaroBaguhan.this, R.drawable.custom_progress_drawable));
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

    private void loadBaguhanQuestion() {
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
                    List<String> choices = (List<String>) documentSnapshot.get("choices");

                    if (question != null) {
                        baguhanQuestion.setText(question);
                        String spokenVersion = question.replaceAll("_+", "blanko");
                        speakText(spokenVersion);
                    }

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
                        if (line != null) {
                            baguhanQuestion.setText(line);
                            speakText(line);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (baguhanScore > 0) {
            saveBaguhanScore();
        }

        if (countDownTimer != null) countDownTimer.cancel();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
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
        View backDialogView = getLayoutInflater().inflate(R.layout.custom_dialog_box_exit_palaro, null);
        AlertDialog backDialog = new AlertDialog.Builder(this)
                .setView(backDialogView)
                .setCancelable(false)
                .create();

        // Para walang puting box
        if (backDialog.getWindow() != null) {
            backDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button yesButton = backDialogView.findViewById(R.id.button5); // "Oo" button
        Button noButton = backDialogView.findViewById(R.id.button6);  // "Hindi" button

        yesButton.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            backDialog.dismiss();
            finish(); // close the activity
        });

        noButton.setOnClickListener(v -> backDialog.dismiss());

        backDialog.show();
    }


}