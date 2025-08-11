package com.example.habiaral.PagUnawa.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangAkopQuiz;
import com.example.habiaral.PagUnawa.PagUnawa;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Kwento3Quiz extends AppCompatActivity {
    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento3_quiz);

        nextButton = findViewById(R.id.nextButton);

        nextButton.setOnClickListener(view -> {
            unlockNextLesson();
            saveQuizResultToFirestore();
            showResultDialog();
        });
    }

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
            Intent intent = new Intent(Kwento3Quiz.this, PagUnawa.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // =========================
    // FIRESTORE UPDATES
    // =========================
    private void unlockNextLesson() {
        Toast.makeText(this, "Congratulations! You have completed all lessons!", Toast.LENGTH_LONG).show();
    }

    private void saveQuizResultToFirestore() {
        // wala pang code
    }
}
