package com.example.habiaral.PagUnawa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.PagUnawa.Stories.Kwento1;
import com.example.habiaral.PagUnawa.Stories.Kwento2;
import com.example.habiaral.PagUnawa.Stories.Kwento3;
import com.example.habiaral.R;
import com.example.habiaral.BahagiNgPananalita.LessonProgressCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PagUnawa extends AppCompatActivity {

    ConstraintLayout btnKwento1, btnKwento2, btnKwento3;
    FrameLayout kwento1Lock, kwento2Lock, kwento3Lock;

    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa);

        initViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // Load from cache first
        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }
        db.collection("students").document(uid).get().addOnSuccessListener(studentSnap -> {
            if (studentSnap.exists()) {
                if (studentSnap.contains("studentId")) {
                    String studentID = studentSnap.getString("studentId");

                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentID);

                    db.collection("module_progress").document(uid)
                            .set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                loadLessonProgressFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                loadLessonProgressFromFirestore();
                            });
                } else {
                    loadLessonProgressFromFirestore();
                }
            } else {
                loadLessonProgressFromFirestore();
            }
        }).addOnFailureListener(e -> {
            loadLessonProgressFromFirestore();
        });
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
    }

    private void updateUIFromProgress(Map<String, Object> data) {
        if (data == null) return;

        Object module3Obj = data.get("module_3");
        if (!(module3Obj instanceof Map)) return;
        Map<String, Object> module3 = (Map<String, Object>) module3Obj;

        Object lessonsObj = module3.get("lessons");
        if (!(lessonsObj instanceof Map)) return;
        Map<String, Object> lessons = (Map<String, Object>) lessonsObj;

        boolean kwento1Done = isCompleted(lessons, "kwento1");
        boolean kwento2Done = isCompleted(lessons, "kwento2");
        boolean kwento3Done = isCompleted(lessons, "kwento3");

        unlockButton(btnKwento1, true, kwento1Lock, new Intent(this, Kwento1.class));
        unlockButton(btnKwento2, kwento1Done, kwento2Lock, new Intent(this, Kwento2.class));
        unlockButton(btnKwento3, kwento2Done, kwento3Lock, new Intent(this, Kwento3.class));

        checkAndCompleteModule(kwento1Done, kwento2Done, kwento3Done);
    }

    private boolean isCompleted(Map<String, Object> lessons, String key) {
        Object lessonObj = lessons.get(key);
        if (!(lessonObj instanceof Map)) return false;
        Map<String, Object> lessonData = (Map<String, Object>) lessonObj;
        return "completed".equals(lessonData.get("status"));
    }

    private void unlockButton(ConstraintLayout layout, boolean isUnlocked, FrameLayout lock, Intent intent) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);

        if (isUnlocked) {
            layout.setOnClickListener(v -> startActivity(intent));
        } else {
            layout.setOnClickListener(null);
        }
    }

    private void initViews() {
        btnKwento1 = findViewById(R.id.kwento1);
        btnKwento2 = findViewById(R.id.kwento2);
        btnKwento3 = findViewById(R.id.kwento3);

        kwento1Lock = findViewById(R.id.kwento1Lock);
        kwento2Lock = findViewById(R.id.kwento2Lock);
        kwento3Lock = findViewById(R.id.kwento3Lock);
    }

    private void checkAndCompleteModule(boolean kwento1Done, boolean kwento2Done, boolean kwento3Done) {
        boolean allDone = kwento1Done && kwento2Done && kwento3Done;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module3Updates = new HashMap<>();
        module3Updates.put("modulename", "Pag-unawa");
        module3Updates.put("status", allDone ? "completed" : "in_progress");
        update.put("module_3", module3Updates);

        db.collection("module_progress").document(uid).set(update, SetOptions.merge());
    }
}
