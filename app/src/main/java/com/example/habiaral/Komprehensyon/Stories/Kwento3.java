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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Kwento3 extends AppCompatActivity {
    Button unlockButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kwento3);

        unlockButton = findViewById(R.id.UnlockButtonKwento3);
        db = FirebaseFirestore.getInstance();

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlockLesson();
            }
        });
    }

    private void unlockLesson() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> progress = new HashMap<>();
        progress.put("Kwento3Done", true);

        db.collection("module_progress").document(userId)
                .update(progress)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Congratulations! You have completed all the stories!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Kwento3.this, Komprehensyon.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update progress", Toast.LENGTH_SHORT).show());
    }
}
