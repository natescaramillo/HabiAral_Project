package com.example.habiaral.Komprehensyon.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Komprehensyon.Komprehensyon;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Kwento1 extends AppCompatActivity {
    Button unlockButton;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kwento1);

        unlockButton = findViewById(R.id.UnlockButtonKwento1);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlockLesson();
            }
        });
    }

    private void unlockLesson() {
        DocumentReference docRef = db.collection("module_progress").document(userId);

        Map<String, Object> update = new HashMap<>();
        update.put("Kwento1Done", true);

        docRef.update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Next Story Unlocked: Kwento2!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Kwento1.this, Komprehensyon.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to unlock story", Toast.LENGTH_SHORT).show());
    }
}
