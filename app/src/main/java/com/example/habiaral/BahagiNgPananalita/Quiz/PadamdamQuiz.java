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

public class PadamdamQuiz extends AppCompatActivity {

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_padamdam_quiz);

        nextButton = findViewById(R.id.padamdamNextButton);

        nextButton.setOnClickListener(view -> {
            unlockNextLesson();
            saveQuizResultToFirestore();
            showResultDialog();
        });
    }

    // =========================
    // DIALOGS & NAVIGATION
    // =========================
    private void showResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_box_quiz_score, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        Button retryButton = dialogView.findViewById(R.id.retryButton);
        Button homeButton = dialogView.findViewById(R.id.finishButton);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        retryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PadamdamQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // =========================
    // FIRESTORE UPDATES
    // =========================
    private void unlockNextLesson() {
        Toast.makeText(this, "Next Lesson Unlocked: Pangawing!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> padamdamStatus = new HashMap<>();
        padamdamStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("padamdam", padamdamStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "padamdam");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }
}
