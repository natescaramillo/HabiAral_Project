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

public class PangAbayQuiz extends AppCompatActivity {

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangabay_quiz);

        nextButton = findViewById(R.id.pangabayNextButton);

        nextButton.setOnClickListener(view -> {
            unlockNextLesson();
            saveQuizResultToFirestore();
            showResultDialog();
        });
    }

    private void unlockNextLesson() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> nextLesson = new HashMap<>();
        nextLesson.put("unlocked", true);

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangatnig", nextLesson); // I-unlock ang "Pangatnig" dahil tapos na ang "Pang-abay"

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());

        Toast.makeText(this, "Pwede mo nang simulan ang susunod na aralin!: Pangatnig!", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(PangAbayQuiz.this, BahagiNgPananalita.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangAbayStatus = new HashMap<>();
        pangAbayStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangabay", pangAbayStatus);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "pangabay");

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", updateMap), SetOptions.merge());
    }
}
