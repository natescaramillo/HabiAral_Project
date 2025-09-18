package com.example.habiaral.Panitikan.Epiko.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.Epiko.Quiz.EpikoKwento2Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EpikoKwento2 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.agila_01, R.drawable.agila_02, R.drawable.agila_03,
            R.drawable.agila_04, R.drawable.agila_05, R.drawable.agila_06,
            R.drawable.agila_07, R.drawable.agila_08, R.drawable.agila_09,
            R.drawable.agila_10, R.drawable.agila_11, R.drawable.agila_12,
            R.drawable.agila_13, R.drawable.agila_14, R.drawable.agila_15,
            R.drawable.agila_16, R.drawable.agila_17, R.drawable.agila_18,
            R.drawable.agila_19, R.drawable.agila_20, R.drawable.agila_21,
            R.drawable.agila_21, R.drawable.agila_22, R.drawable.agila_23,
            R.drawable.agila_24, R.drawable.agila_25, R.drawable.agila_26,
            R.drawable.agila_27, R.drawable.agila_28, R.drawable.agila_29,
            R.drawable.agila_30
    };
    private boolean isLessonDone = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento2);

        storyImage = findViewById(R.id.imageViewComic2);
        unlockButton = findViewById(R.id.UnlockButtonKwento2);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        storyImage.setImageResource(comicPages[currentPage]);

        storyImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float width = storyImage.getWidth();

                if (x < width / 2) {
                    previousPage();
                } else {
                    nextPage();
                }
            }
            return true;
        });

        checkLessonStatusFromFirestore();

        unlockButton.setOnClickListener(v -> {
            if (isLessonDone) {
                Intent intent = new Intent(EpikoKwento2.this, EpikoKwento2Quiz.class);
                startActivity(intent);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                startActivity(new Intent(EpikoKwento2.this, Epiko.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }

    private void nextPage() {
        if (currentPage < comicPages.length - 1) {
            currentPage++;

            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            if (currentPage == comicPages.length - 1) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;

            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    private void checkLessonStatusFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                        if (module3 != null) {
                            Map<String, Object> lessons = (Map<String, Object>) module3.get("lessons");
                            if (lessons != null) {
                                Map<String, Object> kwento2 = (Map<String, Object>) lessons.get("kwento2");
                                if (kwento2 != null) {
                                    String status = (String) kwento2.get("status");
                                    if ("in_progress".equals(status) || "completed".equals(status)) {
                                        isLessonDone = true;
                                        unlockButton.setEnabled(true);
                                        unlockButton.setAlpha(1f);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> kwento2Status = new HashMap<>();
        kwento2Status.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("kwento2", kwento2Status);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Epiko");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "kwento2");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_3", progressMap), SetOptions.merge());
    }
}
