package com.example.habiaral.Panitikan.MaiklingKuwento.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.MaiklingKuwento.MaiklingKuwento;
import com.example.habiaral.Panitikan.MaiklingKuwento.Quiz.MaiklingKuwento3Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MaiklingKuwento3 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.agila_01, R.drawable.agila_02, R.drawable.agila_03,
            R.drawable.agila_04, R.drawable.agila_05, R.drawable.agila_06,
            R.drawable.agila_07, R.drawable.agila_08, R.drawable.agila_09,
            R.drawable.agila_10, R.drawable.agila_11, R.drawable.agila_12,
            R.drawable.agila_13, R.drawable.agila_14, R.drawable.agila_15,
            R.drawable.agila_16, R.drawable.agila_17, R.drawable.agila_18,
            R.drawable.agila_19, R.drawable.agila_20, R.drawable.agila_21,
            R.drawable.agila_22, R.drawable.agila_23, R.drawable.agila_24,
            R.drawable.agila_25, R.drawable.agila_26, R.drawable.agila_27,
            R.drawable.agila_28, R.drawable.agila_29, R.drawable.agila_30
    };

    private static final String STORY_ID = "MaiklingKuwento3";
    private static final String STORY_TITLE = "Sino ang Nagkaloob?";

    private boolean isLessonDone = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_alamat_kwento2);

        storyImage = findViewById(R.id.imageViewComic2);
        unlockButton = findViewById(R.id.UnlockButtonKwento2);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        storyImage.setImageResource(comicPages[currentPage]);

        storyImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float width = storyImage.getWidth();
                if (x < width / 2) previousPage();
                else nextPage();
            }
            return true;
        });

        unlockButton.setOnClickListener(v -> {
            if (isLessonDone) {
                startActivity(new Intent(MaiklingKuwento3.this, MaiklingKuwento3Quiz.class));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(MaiklingKuwento3.this, MaiklingKuwento.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        loadCurrentProgress();
    }

    private void nextPage() {
        if (currentPage < comicPages.length - 1) {
            currentPage++;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);

            if (currentPage == comicPages.length - 1) {
                isLessonDone = true;  // <-- Fix here
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
            }
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);
        }
    }

    private void loadCurrentProgress() {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                    if (module3 == null) return;

                    Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                    if (categories == null) return;

                    Map<String, Object> maiklingKuwento = (Map<String, Object>) categories.get("Maikling Kuwento");
                    if (maiklingKuwento == null) return;

                    Map<String, Object> stories = (Map<String, Object>) maiklingKuwento.get("stories");
                    if (stories == null) return;

                    Map<String, Object> story = (Map<String, Object>) stories.get(STORY_ID);
                    if (story == null) return;

                    Object checkpointObj = story.get("checkpoint");
                    Object statusObj = story.get("status");

                    if (checkpointObj instanceof Number) currentPage = ((Number) checkpointObj).intValue();
                    storyImage.setImageResource(comicPages[currentPage]);

                    if ("completed".equals(statusObj)) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);
                    }
                });
    }

    private void updateCheckpoint(int checkpoint) {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    String currentStatus = "in_progress";
                    if (snapshot.exists()) {
                        Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                        if (module3 != null) {
                            Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                            if (categories != null) {
                                Map<String, Object> maiklingKuwento = (Map<String, Object>) categories.get("Maikling Kuwento");
                                if (maiklingKuwento != null) {
                                    Map<String, Object> stories = (Map<String, Object>) maiklingKuwento.get("stories");
                                    if (stories != null) {
                                        Map<String, Object> story = (Map<String, Object>) stories.get(STORY_ID);
                                        if (story != null && story.get("status") != null) {
                                            currentStatus = story.get("status").toString();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Map<String, Object> storyData = new HashMap<>();
                    storyData.put("checkpoint", checkpoint);
                    storyData.put("title", STORY_TITLE);
                    storyData.put("status", currentStatus);

                    Map<String, Object> maiklingKuwentoData = new HashMap<>();
                    maiklingKuwentoData.put("stories", Map.of(STORY_ID, storyData));

                    Map<String, Object> categories = new HashMap<>();
                    categories.put("Maikling Kuwento", maiklingKuwentoData);

                    Map<String, Object> module3 = new HashMap<>();
                    module3.put("categories", categories);

                    db.collection("module_progress")
                            .document(uid)
                            .set(Map.of("module_3", module3), SetOptions.merge());

                    if (checkpoint == comicPages.length - 1) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);
                    }
                });
    }
}
