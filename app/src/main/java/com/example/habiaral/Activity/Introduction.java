package com.example.habiaral.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.LessonProgressCache; // ✅ Added
import com.example.habiaral.InternetChecker;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Introduction extends AppCompatActivity {

    private EditText nicknameInput;
    private FirebaseFirestore db;
    private String userId;
    private boolean isSaving = false;
    private Handler handler = new Handler(); // Copy & Paste
    private Runnable internetCheckRunnable; // Copy & Paste
    private boolean activityInitialized = false; // Copy & Paste

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction);

        // Copy & Paste
        startInternetChecking();
    }

    // Copy & Paste
    private void startInternetChecking() {
        internetCheckRunnable = new Runnable() {
            @Override
            public void run() {
                InternetChecker.checkInternet(Introduction.this, () -> { // palitan
                    if (!activityInitialized) {
                        RunActivity();
                        activityInitialized = true;
                    }
                });

                handler.postDelayed(this, 3000);
            }
        };

        handler.post(internetCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(internetCheckRunnable); // Copy & Paste
        InternetChecker.resetDialogFlag();
    }

    // Create bagong method
    private void RunActivity() {
        nicknameInput = findViewById(R.id.nickname);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();
        db = FirebaseFirestore.getInstance();

        nicknameInput.setOnEditorActionListener((TextView v, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                saveNickname();
                return true;
            }
            return false;
        });
    }

    private void saveNickname() {
        if (isSaving) return;

        String nickname = nicknameInput.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "Paki-type ang iyong palayaw.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nickname.length() < 2) {
            Toast.makeText(this, "Ang palayaw ay dapat may 2 letra o higit pa.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSaving = true;
        nicknameInput.setEnabled(false);

        DocumentReference studentRef = db.collection("students").document(userId);

        Map<String, Object> update = new HashMap<>();
        update.put("nickname", nickname);
        update.put("updatedAt", System.currentTimeMillis());

        studentRef.set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Palayaw na-save!", Toast.LENGTH_SHORT).show();
                    preloadLessonProgressAndGoHome(); // ✅ preload before going home
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Hindi na-save ang palayaw", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    isSaving = false;
                    nicknameInput.setEnabled(true);
                });
    }

    // ✅ This fetches lesson progress into cache before homepage
    private void preloadLessonProgressAndGoHome() {
        db.collection("module_progress").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData());
            }
            goToHome();
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            goToHome(); // still go home even if progress fetch fails
        });
    }

    private void goToHome() {
        Intent intent = new Intent(Introduction.this, HomepageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
