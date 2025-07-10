    package com.example.habiaral.Palaro;

    import android.os.Bundle;
    import android.os.CountDownTimer;
    import android.os.Handler;
    import android.view.MotionEvent;
    import android.view.View;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.OnBackPressedCallback;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import android.speech.tts.TextToSpeech;
    import java.util.Locale;

    import com.example.habiaral.R;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.firestore.SetOptions;


    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class PalaroHusay extends AppCompatActivity {

        private TextView husayInstruction;
        private Button answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9;
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
        private boolean isTimeUp = false;
        private boolean isAnswered = false;
        private int currentQuestionNumber = 1;
        private int husayScore = 0;
        private int correctStreak = 0;
        private int remainingHearts = 5;
        private ImageView[] heartIcons;
        private TextToSpeech tts;
        private boolean isTtsReady = false;


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

            heartIcons = new ImageView[]{
                    findViewById(R.id.imageView8),
                    findViewById(R.id.imageView11),
                    findViewById(R.id.imageView10),
                    findViewById(R.id.imageView9),
                    findViewById(R.id.imageView12)
            };

            db = FirebaseFirestore.getInstance();

            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    Locale tagalogLocale = new Locale("fil", "PH");
                    int langResult = tts.setLanguage(tagalogLocale);
                    tts.setSpeechRate(1.3f);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(this, "❗ TTS: Wikang Tagalog hindi suportado.", Toast.LENGTH_SHORT).show();
                    } else {
                        isTtsReady = true; // ✅ now ready
                    }
                } else {
                    Toast.makeText(this, "❗ TTS Initialization failed.", Toast.LENGTH_SHORT).show();
                }
            });


            loadCharacterLine("MHCL1");
            new Handler().postDelayed(this::showCountdownThenLoadWords, 7000);

            setupAnswerSelection();
            setupBackConfirmation();
            setupUnlockButton();
        }

        private void setupUnlockButton() {
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

                                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                                    correctAnswerCount++;
                                    husayScore += 5;
                                    correctStreak++;

                                    if (correctStreak == 1) {
                                        loadCharacterLine("MCL2");
                                    } else if (correctStreak == 2) {
                                        loadCharacterLine("MCL3");
                                    } else {
                                        loadCharacterLine("MCL4");
                                    }

                                    Toast.makeText(this, "Tama!", Toast.LENGTH_SHORT).show();
                                    timeLeft = Math.min(timeLeft + 3000, TOTAL_TIME);
                                    restartTimer(timeLeft);
                                } else {
                                    correctStreak = 0;
                                    deductHeart();
                                    loadCharacterLine(remainingHearts > 0 ? "MCL6" : "MCL5");
                                    Toast.makeText(this, "Mali.", Toast.LENGTH_SHORT).show();
                                }

                                currentQuestionNumber++;
                                if (currentQuestionNumber <= 10) {
                                    new Handler().postDelayed(() -> {
                                        loadHusayWords("H" + currentQuestionNumber);
                                        fullAnswerView.setText("");
                                        isAnswered = false;
                                        isTimeUp = false;
                                        startTimer();
                                    }, 3000);
                                } else {
                                    if (countDownTimer != null) countDownTimer.cancel();
                                    saveHusayScore();
                                }
                            }
                        });
            });
        }


        private void setupAnswerSelection() {
            TextView[] answers = {answer1, answer2, answer3, answer4, answer5, answer6};

            for (TextView answer : answers) {
                answer.setOnClickListener(view -> {
                    if (isTimeUp || isAnswered) return;
                    resetAnswerBackgrounds();
                    view.setBackgroundResource(R.drawable.answer_option_bg_selected);
                });
            }
        }

        private void setupBackConfirmation() {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    showBackConfirmationDialog();
                }
            });
        }

        private void resetAnswerBackgrounds() {
            TextView[] allAnswers = {answer1, answer2, answer3, answer4, answer5, answer6};
            for (TextView answer : allAnswers) {
                answer.setBackgroundResource(R.drawable.answer_option_bg); // assuming this is your default bg
            }
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
                            String line = documentSnapshot.getString("line");
                            husayInstruction.setText(line);
                            speakText(line);

                        }
                    });
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

                                selectedWords.clear();
                                fullAnswerView.setText("");

                                for (int i = 0; i < 9; i++) {
                                    TextView answer = answers[i];
                                    answer.setText(words.get(i));
                                    answer.setEnabled(true);

                                    answer.setOnClickListener(view -> {
                                        if (view.isEnabled()) {
                                            String current = fullAnswerView.getText().toString().trim();
                                            fullAnswerView.setText((current + " " + ((TextView) view).getText()).trim());
                                            view.setEnabled(false);
                                            selectedWords.add((TextView) view);
                                            if (selectedWords.size() == 9) {
                                                String currentText = fullAnswerView.getText().toString();
                                                if (!currentText.endsWith(".")) {
                                                    fullAnswerView.append(".");
                                                    currentText = fullAnswerView.getText().toString(); // update text after appending
                                                }
                                                // ✅ Speak sentence automatically
                                                speakText(currentText.trim());
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
                                                    if (selectedWords.contains(tapped)) {
                                                        selectedWords.remove(tapped);
                                                        tapped.setEnabled(true);
                                                        String word = tapped.getText().toString();
                                                        String currentText = fullAnswerView.getText().toString();
                                                        fullAnswerView.setText(currentText.replaceFirst("\\b" + word + "\\b", "").trim());
                                                    }
                                                }
                                                lastTapTime = currentTime;
                                            }
                                            return false;
                                        }
                                    });
                                }
                            }
                        }
                    });
        }

        private void showCountdownThenLoadWords() {
            final Handler countdownHandler = new Handler();
            final int[] countdown = {3};

            countdownHandler.post(new Runnable() {
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
            });
        }

        private void startTimer() {
            restartTimer(timeLeft); // gamitin ang natitirang oras, HINDI TOTAL_TIME
        }


        private void restartTimer(long duration) {
            if (countDownTimer != null) countDownTimer.cancel();

            countDownTimer = new CountDownTimer(duration, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeft = millisUntilFinished;
                    int progress = (int) (timeLeft * 100 / TOTAL_TIME);
                    timerBar.setProgress(progress);
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
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            String userId = currentUser.getUid();
            int finalScore = correctAnswerCount * 3;

            // Step 1: Get studentID from 'students' collection
            db.collection("students").document(userId).get().addOnSuccessListener(studentDoc -> {
                if (!studentDoc.exists()) {
                    Toast.makeText(PalaroHusay.this, "❌ Student record not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String studentID = studentDoc.getString("studentId");
                if (studentID == null || studentID.isEmpty()) {
                    Toast.makeText(PalaroHusay.this, "❌ Missing studentId field.", Toast.LENGTH_SHORT).show();
                    return;
                }

                DocumentReference docRef = db.collection("minigame_progress").document(userId);

                docRef.get().addOnSuccessListener(snapshot -> {
                    int baguhan = snapshot.contains("baguhan_score") ? snapshot.getLong("baguhan_score").intValue() : 0;
                    int dalubhasa = snapshot.contains("dalubhasa_score") ? snapshot.getLong("dalubhasa_score").intValue() : 0;

                    if (snapshot.exists() && snapshot.contains("minigame_progressID")) {
                        // Reuse existing minigame_progressID
                        String existingProgressId = snapshot.getString("minigame_progressID");

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("husay_score", finalScore);
                        updates.put("studentID", studentID); // ✅ correct student ID
                        updates.put("minigame_progressID", existingProgressId);
                        updates.put("total_score", finalScore + baguhan + dalubhasa);

                        docRef.set(updates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(PalaroHusay.this, "✅ Husay score saved!", Toast.LENGTH_SHORT).show();
                                    if (finalScore >= 800) unlockDalubhasa(docRef);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(PalaroHusay.this, "❌ Failed to save Husay score.", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        // No progress doc yet — generate new progress ID
                        db.collection("minigame_progress").get().addOnSuccessListener(querySnapshot -> {
                            int nextNumber = querySnapshot.size() + 1;
                            String newProgressId = "MP" + nextNumber;

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("husay_score", finalScore);
                            updates.put("studentID", studentID);
                            updates.put("minigame_progressID", newProgressId);
                            updates.put("total_score", finalScore + baguhan + dalubhasa);

                            docRef.set(updates, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(PalaroHusay.this, "✅ Husay score saved!", Toast.LENGTH_SHORT).show();
                                        if (finalScore >= 800) unlockDalubhasa(docRef);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(PalaroHusay.this, "❌ Failed to save Husay score.", Toast.LENGTH_SHORT).show();
                                    });
                        });
                    }
                });

            }).addOnFailureListener(e -> {
                Toast.makeText(PalaroHusay.this, "❌ Error fetching student info.", Toast.LENGTH_SHORT).show();
            });
        }



        private void unlockDalubhasa(DocumentReference docRef) {
            Map<String, Object> update = new HashMap<>();
            update.put("dalubhasa_unlocked", true);
            docRef.update(update)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Dalubhasa Unlocked!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to unlock Dalubhasa.", Toast.LENGTH_SHORT).show());
        }

        private void deductHeart() {
            remainingHearts--;

            if (heartIcons != null && remainingHearts >= 0 && remainingHearts < heartIcons.length) {
                heartIcons[remainingHearts].setVisibility(View.INVISIBLE);
            }

            if (remainingHearts <= 0) {
                if (countDownTimer != null) countDownTimer.cancel();
                Toast.makeText(this, "Ubos na ang puso!", Toast.LENGTH_SHORT).show();
                loadCharacterLine("MCL5");
            }
        }
        private void speakText(String text) {
            if (tts != null && isTtsReady && !text.isEmpty()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(this, "⛔ TTS not ready yet.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (countDownTimer != null) countDownTimer.cancel();
            saveHusayScore();
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }

        }
        }


