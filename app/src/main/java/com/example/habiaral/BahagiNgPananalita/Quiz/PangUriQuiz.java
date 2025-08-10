package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PangUriQuiz extends AppCompatActivity {

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_panguri_quiz);

        nextButton = findViewById(R.id.panguriNextButton);

        nextButton.setOnClickListener(view -> {
            unlockNextLesson();           // Firestore updates only
            saveQuizResultToFirestore(); // Save completed status
            showResultDialog();
        });
    }

    private void unlockNextLesson() {
        // Show toast that next lesson unlocked
        Toast.makeText(this, "Next Lesson Unlocked: Panghalip!", Toast.LENGTH_SHORT).show();

        // Optionally you can update the current_lesson here to "panghalip" if you want
        // This can be combined inside saveQuizResultToFirestore or separately
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangUriStatus = new HashMap<>();
        pangUriStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("panguri", pangUriStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "panguri"); // or "panghalip" if you want to reflect next lesson

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }

    private void showResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_box_option, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button retryButton = dialogView.findViewById(R.id.buttonRetry);
        Button homeButton = dialogView.findViewById(R.id.buttonHome);

        AlertDialog dialog = builder.create();
        dialog.show();

        retryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PangUriQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
