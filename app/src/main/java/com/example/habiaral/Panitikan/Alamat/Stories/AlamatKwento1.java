package com.example.habiaral.Panitikan.Alamat.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Alamat.Alamat;
import com.example.habiaral.Panitikan.Alamat.Quiz.AlamatKwento1Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AlamatKwento1 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.kwento1_page01, R.drawable.kwento1_page02, R.drawable.kwento1_page03,
            R.drawable.kwento1_page04, R.drawable.kwento1_page05, R.drawable.kwento1_page06,
            R.drawable.kwento1_page07, R.drawable.kwento1_page08, R.drawable.kwento1_page09,
            R.drawable.kwento1_page10, R.drawable.kwento1_page11, R.drawable.kwento1_page12,
            R.drawable.kwento1_page13, R.drawable.kwento1_page14, R.drawable.kwento1_page15,
            R.drawable.kwento1_page16, R.drawable.kwento1_page17, R.drawable.kwento1_page18,
            R.drawable.kwento1_page19
    };

    private static final String STORY_ID = "AlamatKwento1";
    private static final String STORY_TITLE = "Alamat ng Rosas";

    private boolean isLessonDone = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento1);

        storyImage = findViewById(R.id.imageViewComic);
        unlockButton = findViewById(R.id.UnlockButtonKwento1);

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
                startActivity(new Intent(AlamatKwento1.this, AlamatKwento1Quiz.class));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(AlamatKwento1.this, Alamat.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        // Load current progress on start
        loadCurrentProgress();
    }

    private void nextPage() {
        if (currentPage < comicPages.length - 1) {
            currentPage++;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            // Update checkpoint, but do NOT mark story completed here
            updateCheckpoint(currentPage);

            // Enable quiz button only at the last page
            if (currentPage == comicPages.length - 1) {
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

                    Map<String, Object> alamat = (Map<String, Object>) categories.get("Alamat");
                    if (alamat == null) return;

                    Map<String, Object> stories = (Map<String, Object>) alamat.get("stories");
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
                                Map<String, Object> alamat = (Map<String, Object>) categories.get("Alamat");
                                if (alamat != null) {
                                    Map<String, Object> stories = (Map<String, Object>) alamat.get("stories");
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

                    Map<String, Object> alamatData = new HashMap<>();
                    alamatData.put("stories", Map.of(STORY_ID, storyData));

                    Map<String, Object> categories = new HashMap<>();
                    categories.put("Alamat", alamatData);

                    Map<String, Object> module3 = new HashMap<>();
                    module3.put("categories", categories);

                    db.collection("module_progress")
                            .document(uid)
                            .set(Map.of("module_3", module3), SetOptions.merge());

                    if ("completed".equals(currentStatus) || checkpoint == comicPages.length - 1) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);
                    }
                });
    }

}
