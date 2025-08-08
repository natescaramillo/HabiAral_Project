package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Hugnayan extends AppCompatActivity {

    Button unlockButton;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hugnayan_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonHugnayan);

        // =========================
        // FIRESTORE INITIALIZATION
        // =========================
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Unlock button → save progress & return to main
        unlockButton.setOnClickListener(view -> unlockLesson());
    }

    // =========================
    // FIRESTORE - UNLOCK LESSON
    // =========================
    private void unlockLesson() {
        if (userId == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("HugnayanDone", true);

        db.collection("lesson_progress") // ✅ Tamang collection
                .document(userId)
                .update(progress)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(Hugnayan.this, "Next Story Unlocked: Langkapan!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Hugnayan.this, KayarianNgPangungusap.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Hugnayan.this, "Failed to update progress.", Toast.LENGTH_SHORT).show();
                });
    }
}
