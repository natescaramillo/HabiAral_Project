package com.example.habiaral.PagUnawa.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.PagUnawa.PagUnawa;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Kwento3 extends AppCompatActivity {

    Button unlockButton;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento3);

        // =========================
        // UI INITIALIZATION
        // =========================
        unlockButton = findViewById(R.id.UnlockButtonKwento3);

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
        progress.put("Kwento3Done", true);

        db.collection("module_progress")
                .document(userId)
                .update(progress)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Congratulations! You have completed all the stories!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(Kwento3.this, PagUnawa.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update progress", Toast.LENGTH_SHORT).show();
                });
    }
}
