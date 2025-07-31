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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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

    private static final int BAGUHAN_REQUEST_CODE = 1;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro);

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

        button1.setOnClickListener(v -> {
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
            if (userPoints >= 400) {
                startActivity(new Intent(Palaro.this, PalaroHusay.class));
            } else {
                Toast.makeText(this, "Unlock Husay at 400 points!", Toast.LENGTH_SHORT).show();
            }
        });

        button3.setOnClickListener(v -> {
            if (userPoints >= 800) {
                startActivity(new Intent(Palaro.this, PalaroDalubhasa.class));
            } else {
                Toast.makeText(this, "Unlock Dalubhasa at 800 points!", Toast.LENGTH_SHORT).show();
            }
        });

        Button debugAddPoints = findViewById(R.id.debug_add_points);
        debugAddPoints.setOnClickListener(v -> {
            userPoints += 100;
            editor.putInt(KEY_POINTS, userPoints).apply();
            updateUI();
        });
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
            trophyImage.setImageResource(R.drawable.gold_trophy);
            tierStart = 800;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else if (userPoints >= 800) {
            trophyImage.setImageResource(R.drawable.silver_trophy_1);
            tierStart = 800;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else if (userPoints >= 400) {
            trophyImage.setImageResource(R.drawable.bronze_trophy);
            tierStart = 400;
            progressPercent = calculatePercent(userPoints, tierStart);
        } else {
            trophyImage.setImageResource(R.drawable.unranked_trophy_1);
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
