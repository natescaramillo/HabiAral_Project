package com.example.habiaral.PagUnawa.Stories;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.PagUnawa.Quiz.Kwento2Quiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Kwento2 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.kwento1_page01, R.drawable.kwento1_page02, R.drawable.kwento1_page03,
            R.drawable.kwento1_page04, R.drawable.kwento1_page05, R.drawable.kwento1_page06,
            R.drawable.kwento1_page07, R.drawable.kwento1_page08, R.drawable.kwento1_page09,
            R.drawable.kwento1_page10, R.drawable.kwento1_page11, R.drawable.kwento1_page12,
            R.drawable.kwento1_page13, R.drawable.kwento1_page14, R.drawable.kwento1_page15,
            R.drawable.kwento1_page16, R.drawable.kwento1_page17, R.drawable.kwento1_page18,
            R.drawable.kwento1_page19
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
        storyImage.setOnClickListener(v -> nextPage());

        checkLessonStatusFromFirestore();

        unlockButton.setOnClickListener(v -> {
            if (isLessonDone) {
                Intent intent = new Intent(Kwento2.this, Kwento2Quiz.class);
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
        progressMap.put("modulename", "Pag-Unawa");
        progressMap.put("status", "in_progress");
        progressMap.put("current_lesson", "kwento2");
        progressMap.put("lessons", lessonMap);

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_3", progressMap), SetOptions.merge());
    }
}
