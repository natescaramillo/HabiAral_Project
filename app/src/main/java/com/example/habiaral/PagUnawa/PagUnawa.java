package com.example.habiaral.PagUnawa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.PagUnawa.Stories.Kwento1;
import com.example.habiaral.PagUnawa.Stories.Kwento2;
import com.example.habiaral.PagUnawa.Stories.Kwento3;
import com.example.habiaral.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PagUnawa extends AppCompatActivity {

    // Match Java types with XML (ConstraintLayout)
    ConstraintLayout btnKwento1, btnKwento2, btnKwento3;
    FrameLayout kwento1Lock, kwento2Lock, kwento3Lock;

    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa);

        // Initialize views
        btnKwento1 = findViewById(R.id.kwento1);
        btnKwento2 = findViewById(R.id.kwento2);
        btnKwento3 = findViewById(R.id.kwento3);

        kwento1Lock = findViewById(R.id.kwento1Lock);
        kwento2Lock = findViewById(R.id.kwento2Lock);
        kwento3Lock = findViewById(R.id.kwento3Lock);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load progress from Firestore
        db.collection("module_progress").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                boolean kwento1Done = document.getBoolean("Kwento1Done") != null && document.getBoolean("Kwento1Done");
                                boolean kwento2Done = document.getBoolean("Kwento2Done") != null && document.getBoolean("Kwento2Done");
                                boolean kwento3Done = document.getBoolean("Kwento3Done") != null && document.getBoolean("Kwento3Done");

                                unlockButton(btnKwento1, true, kwento1Lock);
                                unlockButton(btnKwento2, kwento1Done, kwento2Lock);
                                unlockButton(btnKwento3, kwento2Done, kwento3Lock);
                            } else {
                                unlockButton(btnKwento1, true, kwento1Lock);
                                unlockButton(btnKwento2, false, kwento2Lock);
                                unlockButton(btnKwento3, false, kwento3Lock);
                            }
                        } else {
                            Log.e("Firebase", "Error getting document: ", task.getException());
                        }
                    }
                });

        // Click listeners for stories
        btnKwento1.setOnClickListener(v -> startActivity(new Intent(this, Kwento1.class)));
        btnKwento2.setOnClickListener(v -> startActivity(new Intent(this, Kwento2.class)));
        btnKwento3.setOnClickListener(v -> startActivity(new Intent(this, Kwento3.class)));
    }

    // Updated to use ConstraintLayout
    private void unlockButton(ConstraintLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }
}
