package com.example.habiaral.Komprehensyon.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Komprehensyon.Komprehensyon;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Kwento2 extends AppCompatActivity {

    Button unlockButton;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kwento2);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonKwento2);

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
        progress.put("Kwento2Done", true);

        db.collection("module_progress")
                .document(userId)
                .update(progress)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Next Story Unlocked: Kwento3!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Kwento2.this, Komprehensyon.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update progress", Toast.LENGTH_SHORT).show();
                });
    }
}
