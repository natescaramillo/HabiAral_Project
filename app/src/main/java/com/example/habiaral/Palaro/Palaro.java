package com.example.habiaral.Palaro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Palaro extends AppCompatActivity {

    View button1, button2, button3;
    ImageView gameMechanicsIcon;
    TextView userPointText, currentEnergyText, energyTimerText;
    ProgressBar palaroProgress;

    int userPoints;
    int userEnergy;

    final int ENERGY_COST = 20;
    final int ENERGY_MAX = 100;
    final long ENERGY_INTERVAL = 3 * 60 * 1000;

    CountDownTimer energyTimer;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    private static final String PREF_NAME = "PalaroPrefs";
    private static final String KEY_ENERGY = "userEnergy";
    private static final String KEY_POINTS = "userPoints";
    private static final String KEY_LAST_ENERGY_TIME = "lastEnergyTime";

    private static final int BAGUHAN_REQUEST_CODE = 1;

    private FirebaseFirestore db;
    private boolean isUnlocking = false;
    private MediaPlayer mediaPlayer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palaro);

        db = FirebaseFirestore.getInstance();

        button1 = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        gameMechanicsIcon = findViewById(R.id.game_mechanic_icon);

        userPointText = findViewById(R.id.user_point);
        currentEnergyText = findViewById(R.id.current_energy2);
        energyTimerText = findViewById(R.id.time_energy);
        palaroProgress = findViewById(R.id.palaro_progress);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = prefs.edit();

        userEnergy = prefs.getInt(KEY_ENERGY, 100);
        userPoints = prefs.getInt(KEY_POINTS, 0);

        if (!prefs.contains(KEY_LAST_ENERGY_TIME)) {
            editor.putLong(KEY_LAST_ENERGY_TIME, System.currentTimeMillis());
            editor.apply();
        }

        gameMechanicsIcon.setOnClickListener(v -> showGameMechanics());

        updateUI();
        checkLocks();
        startEnergyRegeneration();
        loadTotalScoreFromFirestore();

        // ✅ Button click listeners with sound
        gameMechanicsIcon.setOnClickListener(v -> {
            playClickSound();
            showGameMechanics();
        });
        button1.setOnClickListener(v -> {
            playClickSound();
            if (userEnergy >= ENERGY_COST) {
                userEnergy -= ENERGY_COST;

                if (userEnergy == ENERGY_MAX - ENERGY_COST) {
                    editor.putLong(KEY_LAST_ENERGY_TIME, System.currentTimeMillis());
                }

                editor.putInt(KEY_ENERGY, userEnergy).apply();

                updateUI();
                checkLocks();
                startEnergyRegeneration();

                Intent intent = new Intent(this, PalaroBaguhan.class);
                intent.putExtra("resetProgress", true);
                startActivityForResult(intent, BAGUHAN_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Not enough energy!", Toast.LENGTH_SHORT).show();
            }
        });

        button2.setOnClickListener(v -> {
            playClickSound();
            if (userPoints >= 400) {
                startActivity(new Intent(Palaro.this, PalaroHusay.class));
            } else {
                Toast.makeText(this, "Unlock Husay at 400 points!", Toast.LENGTH_SHORT).show();
            }
        });

        button3.setOnClickListener(v -> {
            playClickSound();
            if (userPoints >= 800) {
                startActivity(new Intent(Palaro.this, PalaroDalubhasa.class));
            } else {
                Toast.makeText(this, "Unlock Dalubhasa at 800 points!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // ✅ Helper method for sound
    private void playClickSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.button_click);
        mediaPlayer.setOnCompletionListener(mp -> mp.release());
        mediaPlayer.start();
    }

    private void loadTotalScoreFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DocumentReference docRef = db.collection("minigame_progress").document(userId);

        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                int baguhan = snapshot.contains("baguhan_score") ? snapshot.getLong("baguhan_score").intValue() : 0;
                int husay = snapshot.contains("husay_score") ? snapshot.getLong("husay_score").intValue() : 0;
                int dalubhasa = snapshot.contains("dalubhasa_score") ? snapshot.getLong("dalubhasa_score").intValue() : 0;

                int totalScore = baguhan + husay + dalubhasa;
                userPoints = totalScore;
                editor.putInt(KEY_POINTS, userPoints).apply();
                updateUI();
                checkLocks();
                unlockGanapNaKaalamanAchievement();
                unlockBatangHenyoAchievement(totalScore); // ✅ Call this here


            } else {
                userPoints = 0;
                editor.putInt(KEY_POINTS, userPoints).apply();
                updateUI();
                checkLocks();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load progress", Toast.LENGTH_SHORT).show();
        });
    }
    private void unlockGanapNaKaalamanAchievement() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA1"; // Display key
        String achievementId = "A1";    // Firestore doc ID

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
            if (!studentDoc.exists() || !studentDoc.contains("studentId")) return;

            String studentId = studentDoc.getString("studentId");

            // Check if all palaro questions are answered
            db.collection("palaro_answered").document(uid).get().addOnSuccessListener(progressDoc -> {
                if (!progressDoc.exists()) return;

                Map<String, Object> baguhanMap = progressDoc.contains("baguhan") ? (Map<String, Object>) progressDoc.get("baguhan") : new HashMap<>();
                Map<String, Object> husayMap = progressDoc.contains("husay") ? (Map<String, Object>) progressDoc.get("husay") : new HashMap<>();
                Map<String, Object> dalubhasaMap = progressDoc.contains("dalubhasa") ? (Map<String, Object>) progressDoc.get("dalubhasa") : new HashMap<>();

                boolean allBaguhanAnswered = baguhanMap.size() >= 20;
                boolean allHusayAnswered = husayMap.size() >= 5;
                boolean allDalubhasaAnswered = dalubhasaMap.size() >= 5;

                if (allBaguhanAnswered && allHusayAnswered && allDalubhasaAnswered) {
                    // Check if already unlocked
                    db.collection("student_achievements").document(uid).get().addOnSuccessListener(achSnapshot -> {
                        Map<String, Object> achievements = (Map<String, Object>) achSnapshot.get("achievements");
                        if (achievements != null && achievements.containsKey(achievementCode)) {
                            return; // Already unlocked
                        }

                        // Get data from achievements collection
                        db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                            if (!achDoc.exists()) return;

                            String title = achDoc.getString("title");

                            Map<String, Object> achievementData = new HashMap<>();
                            achievementData.put("achievementID", achievementId);
                            achievementData.put("title", title);
                            achievementData.put("unlockedAt", com.google.firebase.Timestamp.now());

                            Map<String, Object> achievementMap = new HashMap<>();
                            achievementMap.put(achievementCode, achievementData);

                            Map<String, Object> wrapper = new HashMap<>();
                            wrapper.put("studentId", studentId);
                            wrapper.put("achievements", achievementMap);

                            db.collection("student_achievements").document(uid)
                                    .set(wrapper, SetOptions.merge())
                                    .addOnSuccessListener(unused -> runOnUiThread(() -> {
                                        showAchievementUnlockedDialog(title, R.drawable.achievement01);
                                    }));
                        });
                    });
                }
            });
        });
    }
    private void unlockBatangHenyoAchievement(int totalScore) {
        if (totalScore < 2000) return; // Only unlock if score is 2000+

        if (isUnlocking) return; // Prevent re-entrance
        isUnlocking = true;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA7";
        String achievementId = "A7";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("student_achievements").document(uid).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> achievements = (Map<String, Object>) snapshot.get("achievements");

            if (achievements != null && achievements.containsKey(achievementCode)) {
                return; // Already unlocked
            }

            // Get data from achievements collection
            db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists()) return;

                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementId);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", com.google.firebase.Timestamp.now());

                Map<String, Object> achievementMap = new HashMap<>();
                achievementMap.put(achievementCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", uid); // Optional kung hindi required
                wrapper.put("achievements", achievementMap);

                db.collection("student_achievements").document(uid)
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {

                            showAchievementUnlockedDialog(title, R.drawable.achievement07);
                        }));
            });
        });
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

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
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



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BAGUHAN_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            int baguhanScore = data.getIntExtra("baguhanPoints", 0);

            if (baguhanScore > 0) {
                userPoints += baguhanScore;
                editor.putInt(KEY_POINTS, userPoints);
                editor.apply();

                updateUI();
                checkLocks();

                Toast.makeText(this, "Nagdagdag ng " + baguhanScore + " puntos!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI() {
        userPointText.setText(String.valueOf(userPoints));
        currentEnergyText.setText(String.valueOf(userEnergy));

        ImageView trophyImage = findViewById(R.id.trophy_image);

        int tierStart = 0;
        int tierEnd = 400;
        int progressPercent = 0;

        if (userPoints >= 1200) {
            trophyImage.setImageResource(R.drawable.trophy_gold);
            tierStart = 800;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else if (userPoints >= 800) {
            trophyImage.setImageResource(R.drawable.trophy_silver);
            tierStart = 800;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else if (userPoints >= 400) {
            trophyImage.setImageResource(R.drawable.trophy_bronze);
            tierStart = 400;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else {
            trophyImage.setImageResource(R.drawable.trophy_unranked);
            progressPercent = calculatePercent(userPoints, tierStart);
        }

        palaroProgress.setMax(100);
        palaroProgress.setProgress(progressPercent);
    }

    private void checkLocks() {
        button2.setEnabled(userPoints >= 400);
        button2.setAlpha(userPoints >= 400 ? 1f : 0.5f);
        button3.setEnabled(userPoints >= 800);
        button3.setAlpha(userPoints >= 800 ? 1f : 0.5f);
    }

    private void startEnergyRegeneration() {
        long currentTime = System.currentTimeMillis();
        long lastTime = prefs.getLong(KEY_LAST_ENERGY_TIME, currentTime);

        long elapsed = currentTime - lastTime;
        int regenCount = (int) (elapsed / ENERGY_INTERVAL);

        if (regenCount > 0 && userEnergy < ENERGY_MAX) {
            userEnergy = Math.min(userEnergy + regenCount, ENERGY_MAX);
            lastTime = currentTime - (elapsed % ENERGY_INTERVAL);
            editor.putInt(KEY_ENERGY, userEnergy);
            editor.putLong(KEY_LAST_ENERGY_TIME, lastTime);
            editor.apply();
        }

        updateUI();
        checkLocks();

        if (userEnergy < ENERGY_MAX) {
            long timeSinceLastEnergy = currentTime - lastTime;
            long timeUntilNext = ENERGY_INTERVAL - timeSinceLastEnergy;

            if (energyTimerText != null) {
                energyTimerText.setVisibility(View.VISIBLE);
            }

            startEnergyCountDown(timeUntilNext);
        } else {
            if (energyTimer != null) {
                energyTimer.cancel();
            }

            if (energyTimerText != null) {
                energyTimerText.setText("FULL");
                energyTimerText.setVisibility(View.GONE);
            }
        }
    }

    private void startEnergyCountDown(long millisUntilFinished) {
        if (energyTimer != null) {
            energyTimer.cancel();
        }

        energyTimer = new CountDownTimer(millisUntilFinished, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String time = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
                energyTimerText.setText(time);
            }

            public void onFinish() {
                energyTimerText.setText("0:00");
                startEnergyRegeneration();
            }
        }.start();
    }

    private void showGameMechanics() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_box_game_mechanics, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnIsara = dialogView.findViewById(R.id.btn_isara);
        btnIsara.setOnClickListener(v -> {
            playClickSound(); // 🔊 tutunog pag-click
            dialog.dismiss(); // ❌ isara dialog
        });

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (energyTimer != null) {
            energyTimer.cancel();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        editor.putInt(KEY_ENERGY, userEnergy);
        editor.putInt(KEY_POINTS, userPoints);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userEnergy = prefs.getInt(KEY_ENERGY, 100);
        userPoints = prefs.getInt(KEY_POINTS, 0);
        updateUI();
        loadTotalScoreFromFirestore();
        checkLocks();
    }

    private int calculatePercent(int points, int tierStart) {
        int raw = points - tierStart;
        if (raw < 0) raw = 0;
        if (raw > 400) raw = 400;

        return Math.round((raw / 400f) * 100);
    }

}
