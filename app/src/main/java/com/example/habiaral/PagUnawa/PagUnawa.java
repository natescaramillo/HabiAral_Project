package com.example.habiaral.PagUnawa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.PagUnawa.Stories.Kwento1;
import com.example.habiaral.PagUnawa.Stories.Kwento2;
import com.example.habiaral.PagUnawa.Stories.Kwento3;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        // Step 1: Fetch studentId
        db.collection("students").document(uid).get().addOnSuccessListener(studentSnap -> {
            if (studentSnap.exists()) {
                if (studentSnap.contains("studentId")) {
                    String studentID = studentSnap.getString("studentId");
                    Log.d("STUDENT_ID_FETCHED", "Fetched studentId: " + studentID);

                    // Step 2: Save to module_progress without overwriting other fields
                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentID);

                    db.collection("module_progress").document(uid)
                            .set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Log.d("STUDENT_ID_SAVED", "Saved to module_progress: " + studentID);
                                loadLessonProgress();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("SAVE_FAIL", "Failed saving studentId", e);
                                loadLessonProgress();
                            });
                } else {
                    Log.w("MISSING_FIELD", "studentId field missing in students/" + uid);
                    loadLessonProgress();
                }
            } else {
                Log.w("NO_DOC", "No student document found for uid: " + uid);
                loadLessonProgress();
            }
        }).addOnFailureListener(e -> {
            Log.e("FETCH_FAIL", "Error fetching student doc", e);
            loadLessonProgress();
        });
    }

    private void loadLessonProgress() {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
            if (module1 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
            if (lessons == null) return;

            boolean kwento1Done = isCompleted(lessons, "kwento1");
            boolean kwento2Done = isCompleted(lessons, "kwento2");
            boolean kwento3Done = isCompleted(lessons, "kwento3");

            unlockButton(btnKwento1, true, kwento1Lock);
            unlockButton(btnKwento2, kwento1Done, kwento2Lock);
            unlockButton(btnKwento3, kwento2Done, kwento3Lock);

            checkAndCompleteModule(kwento1Done, kwento2Done, kwento3Done);
        });
    }

    private boolean isCompleted(Map<String, Object> lessons, String key) {
        Map<String, Object> data = (Map<String, Object>) lessons.get(key);
        return data != null && "completed".equals(data.get("status"));
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
        btnKwento3 = findViewById(R.id.kwento3);

        kwento1Lock = findViewById(R.id.kwento1Lock);
        kwento2Lock = findViewById(R.id.kwento2Lock);
        kwento3Lock = findViewById(R.id.kwento3Lock);

        btnKwento1.setOnClickListener(v -> startActivity(new Intent(this, Kwento1.class)));
        btnKwento2.setOnClickListener(v -> startActivity(new Intent(this, Kwento2.class)));
        btnKwento3.setOnClickListener(v -> startActivity(new Intent(this, Kwento3.class)));
    }

    private void checkAndCompleteModule(boolean kwento1Done, boolean kwento2Done, boolean kwento3Done) {
        boolean allDone = kwento1Done && kwento2Done && kwento3Done;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module3Updates = new HashMap<>();

        module3Updates.put("modulename", "Pag-unawa");
        module3Updates.put("status", allDone ? "completed" : "in_progress");

        update.put("module_3", module3Updates);

        db.collection("module_progress").document(uid)
                .set(update, SetOptions.merge());
    }
}
