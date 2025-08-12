package com.example.habiaral.KayarianNgPangungusap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Lessons.PayakLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.TambalanLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.HugnayanLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.LangkapanLesson;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class KayarianNgPangungusap extends AppCompatActivity {

    LinearLayout btnPayak, btnTambalan, btnHugnayan, btnLangkapan;
    FrameLayout payakLock, tambalanLock, hugnayanLock, langkapanLock;
    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap);

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
                    android.util.Log.d("STUDENT_ID_FETCHED", "Fetched studentId: " + studentID);

                    // Step 2: Save to module_progress without overwriting other fields
                    Map<String, Object> update = new HashMap<>();
                    update.put("studentId", studentID);

                    db.collection("module_progress").document(uid)
                            .set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                android.util.Log.d("STUDENT_ID_SAVED", "Saved to module_progress: " + studentID);
                                loadLessonProgress();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("SAVE_FAIL", "Failed saving studentId", e);
                                loadLessonProgress();
                            });
                } else {
                    android.util.Log.w("MISSING_FIELD", "studentId field missing in students/" + uid);
                    loadLessonProgress();
                }
            } else {
                android.util.Log.w("NO_DOC", "No student document found for uid: " + uid);
                loadLessonProgress();
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("FETCH_FAIL", "Error fetching student doc", e);
            loadLessonProgress();
        });
    }

    private void loadLessonProgress() {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_2");
            if (module2 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
            if (lessons == null) return;

            boolean payakDone = isCompleted(lessons, "payak");
            boolean tambalanDone = isCompleted(lessons, "tambalan");
            boolean hugnayanDone = isCompleted(lessons, "hugnayan");
            boolean langkapanDone = isCompleted(lessons, "langkapan");

            // Unlock logic
            unlockButton(btnPayak, true, payakLock);
            unlockButton(btnTambalan, payakDone, tambalanLock);
            unlockButton(btnHugnayan, tambalanDone, hugnayanLock);
            unlockButton(btnLangkapan, hugnayanDone, langkapanLock);

            checkAndCompleteModule(payakDone, tambalanDone, hugnayanDone, langkapanDone);
        });
    }

    private boolean isCompleted(Map<String, Object> lessons, String key) {
        Map<String, Object> data = (Map<String, Object>) lessons.get(key);
        return data != null && "completed".equals(data.get("status"));
    }

    private void unlockButton(LinearLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }

    private void initViews() {
        btnPayak = findViewById(R.id.payak);
        btnTambalan = findViewById(R.id.tambalan);
        btnHugnayan = findViewById(R.id.hugnayan);
        btnLangkapan = findViewById(R.id.langkapan);

        payakLock = findViewById(R.id.payakLock);
        tambalanLock = findViewById(R.id.tambalanLock);
        hugnayanLock = findViewById(R.id.hugnayanLock);
        langkapanLock = findViewById(R.id.langkapanLock);

        btnPayak.setOnClickListener(v -> startActivity(new Intent(this, PayakLesson.class)));
        btnTambalan.setOnClickListener(v -> startActivity(new Intent(this, TambalanLesson.class)));
        btnHugnayan.setOnClickListener(v -> startActivity(new Intent(this, HugnayanLesson.class)));
        btnLangkapan.setOnClickListener(v -> startActivity(new Intent(this, LangkapanLesson.class)));
    }

    private void checkAndCompleteModule(boolean payakDone, boolean tambalanDone, boolean hugnayanDone, boolean langkapanDone) {
        boolean allDone = payakDone && tambalanDone && hugnayanDone && langkapanDone;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module2Updates = new HashMap<>();

        module2Updates.put("modulename", "Kayarian ng Pangungusap");
        module2Updates.put("status", allDone ? "completed" : "in_progress");

        update.put("module_2", module2Updates);

        db.collection("module_progress").document(uid)
                .set(update, SetOptions.merge());
    }
}
