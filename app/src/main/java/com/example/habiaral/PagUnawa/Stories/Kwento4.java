package com.example.habiaral.PagUnawa.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.PagUnawa.Quiz.Kwento3Quiz;
import com.example.habiaral.PagUnawa.Quiz.Kwento4Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Kwento4 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.sulayman_01, R.drawable.sulayman_02, R.drawable.sulayman_03,
            R.drawable.sulayman_04, R.drawable.sulayman_05, R.drawable.sulayman_06,
            R.drawable.sulayman_07, R.drawable.sulayman_08, R.drawable.sulayman_09,
            R.drawable.sulayman_10, R.drawable.sulayman_11, R.drawable.sulayman_12,
            R.drawable.sulayman_13, R.drawable.sulayman_14, R.drawable.sulayman_15,
            R.drawable.sulayman_16, R.drawable.sulayman_17, R.drawable.sulayman_18,
            R.drawable.sulayman_19, R.drawable.sulayman_20, R.drawable.sulayman_21,
            R.drawable.sulayman_22, R.drawable.sulayman_23
    };
    private boolean isLessonDone = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa_kwento4);

        storyImage = findViewById(R.id.imageViewComic4);
        unlockButton = findViewById(R.id.UnlockButtonKwento4);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        storyImage.setImageResource(comicPages[currentPage]);
        storyImage.setOnClickListener(v -> nextPage());

        checkLessonStatusFromFirestore();

        unlockButton.setOnClickListener(v -> {
            if (isLessonDone) {
                Intent intent = new Intent(Kwento4.this, Kwento4Quiz.class);
                startActivity(intent);
            }
        });
    }

    private void nextPage() {
        if (currentPage < comicPages.length - 1) {
            currentPage++;
            storyImage.setImageResource(comicPages[currentPage]);

            if (currentPage == comicPages.length - 1) {
                isLessonDone = true;
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
                saveProgressToFirestore();
            }
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
                                Map<String, Object> kwento4 = (Map<String, Object>) lessons.get("kwento4");
                                if (kwento4 != null) {
                                    String status = (String) kwento4.get("status");
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

        Map<String, Object> kwento4Status = new HashMap<>();
        kwento4Status.put("status", "in_progress");

        Map<String, Object> lessonMap = new HashMap<>();
        lessonMap.put("kwento4", kwento4Status);

        Map<String, Object> progressMap = new HashMap<>();
        progressMap.put("modulename", "Pag-Unawa");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "kwento4");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_3", progressMap), SetOptions.merge());
    }
}
