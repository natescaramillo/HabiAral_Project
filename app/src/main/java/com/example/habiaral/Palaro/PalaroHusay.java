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
        private boolean isTimeUp = false;
        private boolean isAnswered = false;
        private int currentQuestionNumber = 1;
        private int husayScore = 0;
        private int correctStreak = 0;
        private int remainingHearts = 5;
        private ImageView[] heartIcons;

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
                            husayInstruction.setText(documentSnapshot.getString("line"));
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
                                            if (selectedWords.size() == 9 && !fullAnswerView.getText().toString().endsWith(".")) {
                                                fullAnswerView.append(".");
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
            int finalScore = correctAnswerCount * 3;
            DocumentReference docRef = db.collection("minigame_progress").document(DOCUMENT_ID);

            Map<String, Object> updates = new HashMap<>();
            updates.put("husay_score", finalScore);
            updates.put("total_score", finalScore);

            docRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PalaroHusay.this, "Score saved!", Toast.LENGTH_SHORT).show();
                        if (finalScore >= 800) {
                            unlockDalubhasa(docRef);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(PalaroHusay.this, "Failed to save score.", Toast.LENGTH_SHORT).show());
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

    }
