package com.example.habiaral.PagUnawa.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.PagUnawa.PagUnawa;
import com.example.habiaral.PagUnawa.Quiz.Kwento3Quiz;
import com.example.habiaral.PagUnawa.Quiz.Kwento4Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Kwento3 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.juan_01, R.drawable.juan_02, R.drawable.juan_03,
            R.drawable.juan_04, R.drawable.juan_05, R.drawable.juan_06,
            R.drawable.juan_07, R.drawable.juan_08, R.drawable.juan_09,
            R.drawable.juan_10, R.drawable.juan_11, R.drawable.juan_12,
            R.drawable.juan_13, R.drawable.juan_14, R.drawable.juan_15,
            R.drawable.juan_16, R.drawable.juan_17, R.drawable.juan_18,
            R.drawable.juan_19, R.drawable.juan_20, R.drawable.juan_21,
            R.drawable.juan_22, R.drawable.juan_23, R.drawable.juan_24,
            R.drawable.juan_25, R.drawable.juan_26, R.drawable.juan_27,
            R.drawable.juan_28, R.drawable.juan_29, R.drawable.juan_30,
            R.drawable.juan_31
    };
    private boolean isLessonDone = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento3);

        storyImage = findViewById(R.id.imageViewComic3);
        unlockButton = findViewById(R.id.UnlockButtonKwento3);

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
                Intent intent = new Intent(Kwento3.this, Kwento3Quiz.class);
                startActivity(intent);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                startActivity(new Intent(Kwento3.this, PagUnawa.class)
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
                                Map<String, Object> kwento3 = (Map<String, Object>) lessons.get("kwento3");
                                if (kwento3 != null) {
                                    String status = (String) kwento3.get("status");
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

        Map<String, Object> kwento3Status = new HashMap<>();
        kwento3Status.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("kwento3", kwento3Status);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Pag-Unawa");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "kwento3");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_3", progressMap), SetOptions.merge());
    }
}
