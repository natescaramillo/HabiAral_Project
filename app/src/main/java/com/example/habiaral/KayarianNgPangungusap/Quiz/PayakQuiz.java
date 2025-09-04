package com.example.habiaral.KayarianNgPangungusap.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.KayarianNgPangungusap.Lessons.TambalanLesson;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PayakQuiz extends AppCompatActivity{

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_payak_quiz);

        nextButton = findViewById(R.id.payakNextButton);

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
        Button taposButton = dialogView.findViewById(R.id.finishButton);
        Button homeButton = dialogView.findViewById(R.id.returnButton);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        retryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        taposButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz.this, TambalanLesson.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz.this, KayarianNgPangungusap.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // =========================
    // FIRESTORE UPDATES
    // =========================
    private void unlockNextLesson() {
        Toast.makeText(this, "Next Lesson Unlocked: Tambalan!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> payakStatus = new HashMap<>();
        payakStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("payak", payakStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "payak");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_2", updateMap), SetOptions.merge());
    }
}
