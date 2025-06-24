package com.example.habiaral.Palaro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

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

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro);

        button1 = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        gameMechanicsIcon = findViewById(R.id.game_mechanic_icon);

        userPointText = findViewById(R.id.user_point);
        currentEnergyText = findViewById(R.id.current_energy2);
        energyTimerText = findViewById(R.id.time_energy);
        palaroProgress = findViewById(R.id.palaro_progress);

        palaroProgress.setMax(2000);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = prefs.edit();

        db = FirebaseFirestore.getInstance();

        userEnergy = prefs.getInt(KEY_ENERGY, 100);

        gameMechanicsIcon.setOnClickListener(v -> showGameMechanics());

        loadUserScoresFromFirestore();
        startEnergyRegeneration();

        button1.setOnClickListener(v -> {
            if (userEnergy >= ENERGY_COST) {
                button1.setEnabled(false); // Prevent multiple rapid clicks
                userEnergy -= ENERGY_COST;
                long now = System.currentTimeMillis();
                editor.putInt(KEY_ENERGY, userEnergy);
                editor.putLong(KEY_LAST_ENERGY_TIME, now);
                editor.apply();

                updateUI();
                checkLocks();
                startEnergyRegeneration();

                startActivity(new Intent(Palaro.this, PalaroBaguhan.class));
                button1.postDelayed(() -> button1.setEnabled(true), 1000);
            } else {
                Toast.makeText(this, "Not enough energy!", Toast.LENGTH_SHORT).show();
            }
        });

        button2.setOnClickListener(v -> {
            if (userPoints >= 400) {
                button2.setEnabled(false);
                startActivity(new Intent(Palaro.this, PalaroHusay.class));
                button2.postDelayed(() -> button2.setEnabled(true), 1000);
            } else {
                Toast.makeText(this, "Unlock Husay at 400 points!", Toast.LENGTH_SHORT).show();
            }
        });

        button3.setOnClickListener(v -> {
            if (userPoints >= 800) {
                button3.setEnabled(false);
                startActivity(new Intent(Palaro.this, PalaroDalubhasa.class));
                button3.postDelayed(() -> button3.setEnabled(true), 1000);
            } else {
                Toast.makeText(this, "Unlock Dalubhasa at 800 points!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserScoresFromFirestore() {
        db.collection("minigame_progress").document("MP1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int baguhanScore = documentSnapshot.getLong("baguhan_score") != null ? documentSnapshot.getLong("baguhan_score").intValue() : 0;
                        int husayScore = documentSnapshot.getLong("husay_score") != null ? documentSnapshot.getLong("husay_score").intValue() : 0;
                        int dalubhasaScore = documentSnapshot.getLong("dalubhasa_score") != null ? documentSnapshot.getLong("dalubhasa_score").intValue() : 0;

                        userPoints = baguhanScore + husayScore + dalubhasaScore;
                        updateUI();
                        checkLocks();
                    } else {
                        Toast.makeText(this, "No score data found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load scores: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        userPointText.setText(String.valueOf(userPoints));
        currentEnergyText.setText(String.valueOf(userEnergy));
        palaroProgress.setProgress(userPoints);
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
                startEnergyCountDown(timeUntilNext);
            }
        } else {
            if (energyTimer != null) energyTimer.cancel();
            if (energyTimerText != null) {
                energyTimerText.setText("FULL");
                energyTimerText.setVisibility(View.GONE);
            }
        }
    }

    private void startEnergyCountDown(long millisUntilFinished) {
        if (energyTimer != null) energyTimer.cancel();

        energyTimer = new CountDownTimer(millisUntilFinished, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                if (energyTimerText != null) {
                    energyTimerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
                }
            }

            public void onFinish() {
                if (energyTimerText != null) {
                    energyTimerText.setText("0:00");
                }
                startEnergyRegeneration();
            }
        }.start();
    }

    private void showGameMechanics() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.game_mechanics_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnIsara = dialogView.findViewById(R.id.btn_isara);
        btnIsara.setOnClickListener(v -> dialog.dismiss());

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (energyTimer != null) {
            energyTimer.cancel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        editor.putInt(KEY_ENERGY, userEnergy);
        editor.putLong(KEY_LAST_ENERGY_TIME, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userEnergy = prefs.getInt(KEY_ENERGY, 100);
        loadUserScoresFromFirestore();
        startEnergyRegeneration();
    }
}
