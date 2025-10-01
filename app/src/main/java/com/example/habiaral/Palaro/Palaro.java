package com.example.habiaral.Palaro;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Palaro extends AppCompatActivity {

    private View button1, button2, button3;
    private ImageView gameMechanicsIcon;
    private TextView userPointText, currentEnergyText, energyTimerText;
    private ProgressBar palaroProgress;

    private int userPoints = 0;
    private int userEnergy = 100;

    private final int ENERGY_COST = 20;
    private final int ENERGY_MAX = 100;
    private final long ENERGY_INTERVAL = 3 * 60 * 1000; // 3 minutes

    private CountDownTimer energyTimer;
    private FirebaseFirestore db;
    private boolean isUnlocking = false;
    private MediaPlayer mediaPlayer;

    private static final int BAGUHAN_REQUEST_CODE = 1;

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
        ImageView palaroBack = findViewById(R.id.palaro_back);

        updateUI();
        checkLocks();
        loadTotalScoreAndEnergyFromFirestore();

        palaroBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

        gameMechanicsIcon.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showGameMechanics();
        });

        button1.setOnClickListener(v -> playBaguhan());
        button2.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (userPoints >= 400) startActivity(new Intent(Palaro.this, PalaroHusay.class));
            else Toast.makeText(this, "Unlock Husay at 400 points!", Toast.LENGTH_SHORT).show();
        });
        button3.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            if (userPoints >= 800) startActivity(new Intent(Palaro.this, PalaroDalubhasa.class));
            else Toast.makeText(this, "Unlock Dalubhasa at 800 points!", Toast.LENGTH_SHORT).show();
        });
    }

    private void playBaguhan() {
        SoundClickUtils.playClickSound(this, R.raw.button_click);
        if (userEnergy >= ENERGY_COST) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;
            String userId = currentUser.getUid();

            // Bawasan ang energy at i-save sa Firebase
            userEnergy -= ENERGY_COST;
            long now = System.currentTimeMillis();

            Map<String, Object> update = new HashMap<>();
            update.put("userEnergy", userEnergy);
            db.collection("minigame_progress").document(userId)
                    .set(update, SetOptions.merge());

            updateUI();
            checkLocks();

            // Restart energy regeneration
            startEnergyRegeneration(userId, now);

            Intent intent = new Intent(this, PalaroBaguhan.class);
            intent.putExtra("resetProgress", true);
            startActivityForResult(intent, BAGUHAN_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Not enough energy!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTotalScoreAndEnergyFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DocumentReference docRef = db.collection("minigame_progress").document(userId);

        docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading progress", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                int firebasePoints = snapshot.getLong("total_score") != null ? snapshot.getLong("total_score").intValue() : 0;
                long firebaseEnergy = snapshot.getLong("userEnergy") != null ? snapshot.getLong("userEnergy") : ENERGY_MAX;
                long lastEnergyTime = snapshot.getLong("lastEnergyTime") != null ? snapshot.getLong("lastEnergyTime") : System.currentTimeMillis();

                // Prevent score rollback
                userPoints = Math.max(userPoints, firebasePoints);
                userEnergy = (int) firebaseEnergy;

                updateUI();
                checkLocks();
                startEnergyRegeneration(userId, lastEnergyTime);

                unlockGanapNaKaalamanAchievement();
                unlockBatangHenyoAchievement(userPoints);

            } else {
                Map<String, Object> initData = new HashMap<>();
                initData.put("total_score", 0);
                initData.put("userEnergy", ENERGY_MAX);
                initData.put("lastEnergyTime", System.currentTimeMillis());
                docRef.set(initData);

                userPoints = 0;
                userEnergy = ENERGY_MAX;

                updateUI();
                checkLocks();
            }
        });
    }

    private boolean energyTimerRunning = false;

    private void startEnergyRegeneration(String userId, long lastTime) {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastTime;

        int regenCount = (int) (elapsed / ENERGY_INTERVAL);
        if (regenCount > 0) {
            userEnergy = Math.min(userEnergy + regenCount, ENERGY_MAX);
            lastTime += regenCount * ENERGY_INTERVAL;

            // I-update lang sa Firebase
            Map<String, Object> update = new HashMap<>();
            update.put("userEnergy", userEnergy);
            update.put("lastEnergyTime", lastTime);
            db.collection("minigame_progress").document(userId).set(update, SetOptions.merge());
        }

        updateUI();
        checkLocks();

        // Kung energy full, itago ang timer
        if (userEnergy >= ENERGY_MAX) {
            energyTimerText.setVisibility(View.GONE);
            energyTimerRunning = false;
        } else {
            // Restart timer lang kung hindi pa tumatakbo
            if (!energyTimerRunning) {
                long timeSinceLastEnergy = currentTime - lastTime;
                long timeUntilNext = ENERGY_INTERVAL - timeSinceLastEnergy;
                startEnergyCountDown(userId, timeUntilNext);
            }
        }
    }

    private void startEnergyCountDown(String userId, long millisUntilFinished) {
        if (energyTimer != null) energyTimer.cancel();

        energyTimerRunning = true;
        energyTimerText.setVisibility(View.VISIBLE); // Siguraduhin na visible kapag countdown

        energyTimer = new CountDownTimer(millisUntilFinished, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                energyTimerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
            }

            public void onFinish() {
                long now = System.currentTimeMillis();
                if (userEnergy < ENERGY_MAX) {
                    userEnergy = Math.min(userEnergy + 1, ENERGY_MAX);

                    Map<String, Object> update = new HashMap<>();
                    update.put("userEnergy", userEnergy);
                    update.put("lastEnergyTime", now);
                    db.collection("minigame_progress").document(userId).set(update, SetOptions.merge());

                    updateUI();
                    checkLocks();

                    if (userEnergy < ENERGY_MAX) {
                        startEnergyCountDown(userId, ENERGY_INTERVAL);
                    } else {
                        // Kapag full na, hide timer
                        energyTimerText.setText("FULL");
                        energyTimerText.setVisibility(View.GONE);
                        energyTimerRunning = false;
                    }
                } else {
                    energyTimerText.setVisibility(View.GONE);
                    energyTimerRunning = false;
                }
            }
        }.start();
    }


    private void updateUI() {
        String displayPoints;
        if (userPoints < 400) displayPoints = userPoints + "/400";
        else if (userPoints < 800) displayPoints = userPoints + "/800";
        else if (userPoints < 1200) displayPoints = userPoints + "/1200";
        else displayPoints = String.valueOf(userPoints);

        userPointText.setText(displayPoints);
        currentEnergyText.setText(String.valueOf(userEnergy));

        ImageView trophyImage = findViewById(R.id.trophy_image);
        int tierStart = 0;
        int progressPercent = 0;

        if (userPoints >= 1200) { trophyImage.setImageResource(R.drawable.trophy_gold); tierStart = 800; progressPercent = calculatePercent(userPoints, tierStart);}
        else if (userPoints >= 800) { trophyImage.setImageResource(R.drawable.trophy_silver); tierStart = 800; progressPercent = calculatePercent(userPoints, tierStart);}
        else if (userPoints >= 400) { trophyImage.setImageResource(R.drawable.trophy_bronze); tierStart = 400; progressPercent = calculatePercent(userPoints, tierStart);}
        else { trophyImage.setImageResource(R.drawable.trophy_unranked); progressPercent = calculatePercent(userPoints, tierStart);}

        palaroProgress.setMax(100);
        palaroProgress.setProgress(progressPercent);
    }

    private void checkLocks() {
        button2.setEnabled(userPoints >= 400);
        button2.setAlpha(userPoints >= 400 ? 1f : 0.5f);
        button3.setEnabled(userPoints >= 800);
        button3.setAlpha(userPoints >= 800 ? 1f : 0.5f);
    }

    private void showGameMechanics() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_box_game_mechanics, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ImageView imgIsara = dialogView.findViewById(R.id.img_Isara);
        imgIsara.setOnClickListener(v -> { SoundClickUtils.playClickSound(this, R.raw.button_click); dialog.dismiss(); });

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }

    private int calculatePercent(int points, int tierStart) {
        int raw = points - tierStart;
        if (raw < 0) raw = 0;
        if (raw > 400) raw = 400;
        return Math.round((raw / 400f) * 100);
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
                                        AchievementDialogUtils.showAchievementUnlockedDialog(Palaro.this, title, R.drawable.achievement01);
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

                            AchievementDialogUtils.showAchievementUnlockedDialog(Palaro.this, title, R.drawable.achievement07);
                        }));
            });
        });
    }

}
