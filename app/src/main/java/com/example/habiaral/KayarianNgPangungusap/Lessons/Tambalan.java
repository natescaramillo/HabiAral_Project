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

public class Tambalan extends AppCompatActivity {

    Button unlockButton;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambalan_lesson);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonTambalan);

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

        Map<String, Object> update = new HashMap<>();
        update.put("TambalanDone", true);

        db.collection("lesson_progress") // ✅ Tamang collection
                .document(userId)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(Tambalan.this, "Next Story Unlocked: Hugnayan!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Tambalan.this, KayarianNgPangungusap.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Tambalan.this, "Failed to update progress", Toast.LENGTH_SHORT).show();
                });
    }
}
