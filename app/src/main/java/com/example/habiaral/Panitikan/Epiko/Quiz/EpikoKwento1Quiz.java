package com.example.habiaral.Panitikan.Epiko.Quiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.Epiko.Stories.EpikoKwento2;
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementM3Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EpikoKwento1Quiz extends AppCompatActivity {
    Button nextButton;
    private FirebaseFirestore db;
    private String uid;

    private static final String STORY_ID = "EpikoKwento1";
    private static final String STORY_TITLE = "Indarapatra at Sulayman";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_alamat_kwento1_quiz);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        nextButton = findViewById(R.id.kwento1NextButton);
        nextButton.setOnClickListener(view -> {
            markStoryCompleted();
            showResultDialog();

        });
    }

    private void showResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_box_quiz_score, null);
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
            startActivity(new Intent(this, EpikoKwento2.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        homeButton.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, Epiko.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
    }

    private void markStoryCompleted() {
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("module_3.categories.Epiko.stories." + STORY_ID + ".title", STORY_TITLE);
        updates.put("module_3.categories.Epiko.stories." + STORY_ID + ".status", "completed");

        db.collection("module_progress").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Next Lesson Unlocked: Kwento2!", Toast.LENGTH_SHORT).show();
                    checkIfCategoryCompleted("Epiko");
                })
                .addOnFailureListener(e ->
                        db.collection("module_progress").document(uid).set(updates, SetOptions.merge()));
    }

    private void checkIfCategoryCompleted(String categoryName) {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> stories = (Map<String, Object>)
                            snapshot.get("module_3.categories." + categoryName + ".stories");
                    if (stories == null) return;

                    String[] requiredStories = {"EpikoKwento1", "EpikoKwento2"};

                    boolean allCompleted = true;
                    for (String storyKey : requiredStories) {
                        Object storyObj = stories.get(storyKey);
                        if (!(storyObj instanceof Map)) {
                            allCompleted = false;
                            break;
                        }
                        Map<String, Object> storyData = (Map<String, Object>) storyObj;
                        if (!"completed".equals(storyData.get("status"))) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        db.collection("module_progress").document(uid)
                                .set(Map.of("module_3",
                                        Map.of("categories",
                                                Map.of(categoryName, Map.of("status", "completed"))
                                        )), SetOptions.merge())
                                .addOnSuccessListener(unused -> checkIfModuleCompleted());
                    }
                });
    }

    private void checkIfModuleCompleted() {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> categories =
                            (Map<String, Object>) snapshot.get("module_3.categories");
                    if (categories == null) return;

                    String[] requiredCategories = {"Alamat", "Epiko", "Maikling Kuwento", "Pabula", "Parabula"};

                    boolean allCompleted = true;
                    for (String categoryKey : requiredCategories) {
                        Object catObj = categories.get(categoryKey);
                        if (!(catObj instanceof Map)) {
                            allCompleted = false;
                            break;
                        }
                        Map<String, Object> catData = (Map<String, Object>) catObj;
                        if (!"completed".equals(catData.get("status"))) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        db.collection("module_progress").document(uid)
                                .set(Map.of("module_3", Map.of(
                                        "status", "completed",
                                        "modulename", "Panitikan"
                                )), SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    AchievementM3Utils.checkAndUnlockAchievement(this, db, uid);
                                });
                    }
                });
    }
}
