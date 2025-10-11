package com.example.habiaral.Panitikan.Pabula;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.Panitikan.Pabula.Stories.PabulaKwento1;
import com.example.habiaral.Panitikan.Pabula.Stories.PabulaKwento2;
import com.example.habiaral.R;
import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Pabula extends AppCompatActivity {

    private ConstraintLayout btnKwento1, btnKwento2;
    private FrameLayout kwento1Lock, kwento2Lock;
    private FirebaseFirestore db;
    private String uid;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_pabula);

        initViews();
        lockAllButtons();

        ImageView pabulaBack = findViewById(R.id.back_button);

        pabulaBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        loadLessonProgressFromFirestore();

        if (isFirstTime()) {
            showDescriptionDialog();
            setFirstTimeShown();
        }

        ImageView helpBtn = findViewById(R.id.imageView14);
        helpBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showDescriptionDialog();
        });
    }

    private void showDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_description, null);
        builder.setView(dialogView);

        AlertDialog descriptionDialog = builder.create();
        descriptionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView titleText = dialogView.findViewById(R.id.description_dialog_title);
        TextView contentText = dialogView.findViewById(R.id.textView24);
        ImageView closeBtn = dialogView.findViewById(R.id.description_dialog_close);

        titleText.setText("Pabula");

        contentText.setText(
                "Ang Pabula ay isang maikling kuwento na karaniwang gumagamit ng mga hayop bilang tauhan " +
                        "na may kakayahang magsalita at kumilos na parang tao. " +
                        "Layunin ng pabula na magturo ng aral o mabuting asal sa pamamagitan ng mga simpleng kuwento. \n\n" +
                        "Karaniwang may malinaw na simula, gitna, at wakas, at nag-iiwan ng mahalagang aral para sa mga mambabasa."
        );

        closeBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            descriptionDialog.dismiss();
        });

        descriptionDialog.show();
    }


    private boolean isFirstTime() {
        return getSharedPreferences("PabulaPrefs", MODE_PRIVATE)
                .getBoolean("isFirstTime", true);
    }

    private void setFirstTimeShown() {
        getSharedPreferences("PabulaPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstTime", false)
                .apply();
    }

    private void markCategoryInProgressIfNeeded() {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> data = snapshot.getData();
                    if (data == null) return;

                    Map<String, Object> module3 = (Map<String, Object>) data.get("module_3");
                    if (module3 == null) return;

                    Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                    if (categories != null && categories.get("Pabula") instanceof Map) {
                        Map<String, Object> pabulaCat = (Map<String, Object>) categories.get("Pabula");
                        String status = (String) pabulaCat.get("status");
                        if ("completed".equals(status)) return;
                    }

                    Map<String, Object> categoryUpdate = new HashMap<>();
                    categoryUpdate.put("categoryname", "Pabula");
                    categoryUpdate.put("status", "in_progress");

                    db.collection("module_progress").document(uid)
                            .set(Map.of("module_3", Map.of("categories", Map.of("Pabula", categoryUpdate))),
                                    SetOptions.merge());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadLessonProgressFromFirestore() {
        db.collection("module_progress").document(uid)
                .get()
                .addOnSuccessListener(this::handleFirestoreData);
    }

    private void handleFirestoreData(DocumentSnapshot snapshot) {
        if (!snapshot.exists()) return;

        Map<String, Object> data = snapshot.getData();
        if (data == null) return;

        LessonProgressCache.setData(data);
        updateUIFromProgress(data);

        markCategoryInProgressIfNeeded();
    }

    private void updateUIFromProgress(Map<String, Object> data) {
        if (data == null) return;

        Map<String, Object> module3 = (Map<String, Object>) data.get("module_3");
        if (module3 == null) return;

        Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
        if (categories == null) return;

        Map<String, Object> pabulaCat = (Map<String, Object>) categories.get("Pabula");
        if (pabulaCat == null) return;

        Map<String, Object> stories = (Map<String, Object>) pabulaCat.get("stories");
        if (stories == null) return;

        boolean kwento1Done = isCompleted(stories, "PabulaKwento1");
        boolean kwento2Done = isCompleted(stories, "PabulaKwento2");

        unlockButton(btnKwento1, true, kwento1Lock);
        unlockButton(btnKwento2, kwento1Done, kwento2Lock);

        if (kwento1Done && kwento2Done) {
            markCategoryCompleted();
        }
    }

    private boolean isCompleted(Map<String, Object> stories, String key) {
        Object storyObj = stories.get(key);
        if (!(storyObj instanceof Map)) return false;

        Map<String, Object> storyData = (Map<String, Object>) storyObj;
        return "completed".equals(storyData.get("status"));
    }


    private void unlockButton(ConstraintLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }

    private void initViews() {
        btnKwento1 = findViewById(R.id.kwento1);
        btnKwento2 = findViewById(R.id.kwento2);

        kwento1Lock = findViewById(R.id.kwento1Lock);
        kwento2Lock = findViewById(R.id.kwento2Lock);

        btnKwento1.setOnClickListener(v -> openStory(PabulaKwento1.class));
        btnKwento2.setOnClickListener(v -> openStory(PabulaKwento2.class));
    }

    private void openStory(Class<?> cls) {
        SoundClickUtils.playClickSound(this, R.raw.button_click);
        startActivity(new Intent(this, cls));
    }

    private void lockAllButtons() {
        lockButton(btnKwento2);
    }

    private void lockButton(ConstraintLayout button) {
        button.setClickable(false);
        button.setAlpha(0.5f);
    }

    private void checkIfAllStoriesCompleted() {
        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> data = snapshot.getData();
                    if (data == null) return;

                    Map<String, Object> module3 = (Map<String, Object>) data.get("module_3");
                    if (module3 == null) return;

                    Map<String, Object> lessons = (Map<String, Object>) module3.get("lessons");
                    if (lessons == null) return;

                    boolean kwento1Done = isCompleted(lessons, "kwento1");
                    boolean kwento2Done = isCompleted(lessons, "kwento2");

                    if (kwento1Done && kwento2Done) {
                        markCategoryCompleted();
                    }
                });
    }

    private void markCategoryCompleted() {
        if (uid == null) return;

        Map<String, Object> categoryUpdate = new HashMap<>();
        categoryUpdate.put("categoryname", "Pabula");
        categoryUpdate.put("status", "completed");

        db.collection("module_progress").document(uid)
                .set(Map.of("module_3", Map.of("categories", Map.of("Pabula", categoryUpdate))),
                        SetOptions.merge());
    }
}
