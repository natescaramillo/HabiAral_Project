package com.example.habiaral.Panitikan.Pabula.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.Panitikan.Pabula.Pabula;
import com.example.habiaral.Panitikan.Pabula.Stories.PabulaKwento3;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PabulaKwento2Quiz extends AppCompatActivity {
    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_kwento2_quiz);

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
            Intent intent = new Intent(PabulaKwento2Quiz.this, PabulaKwento3.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PabulaKwento2Quiz.this, Pabula.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void unlockNextLesson() {
        Toast.makeText(this, "Next Lesson Unlocked: Kwento3!", Toast.LENGTH_SHORT).show();
    }

    private void saveQuizResultToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> kwento2Status = new HashMap<>();
        kwento2Status.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("kwento2", kwento2Status);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lessons", lessonsMap);
        updateMap.put("current_lesson", "kwento2");

        Map<String, Object> moduleUpdate = Map.of("module_3", updateMap);

        db.collection("module_progress")
                .document(uid)
                .set(moduleUpdate, SetOptions.merge());

        if (LessonProgressCache.getData() != null) {
            Map<String, Object> cachedData = LessonProgressCache.getData();

            if (!cachedData.containsKey("module_3")) {
                cachedData.put("module_3", new HashMap<String, Object>());
            }

            Map<String, Object> cachedModule3 = (Map<String, Object>) cachedData.get("module_3");
            cachedModule3.put("lessons", lessonsMap);
            cachedModule3.put("current_lesson", "kwento2");

            LessonProgressCache.setData(cachedData);
        }
    }
}
