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
import com.example.habiaral.Cache.LessonProgressCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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
        lockAllButtons();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // Load from cache first
        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentSnap -> {
                    if (studentSnap.exists()) {
                        if (studentSnap.contains("studentId")) {
                            String studentID = studentSnap.getString("studentId");
                            android.util.Log.d("STUDENT_ID_FETCHED", "Fetched studentId: " + studentID);

                            Map<String, Object> update = new HashMap<>();
                            update.put("studentId", studentID);

                            db.collection("module_progress").document(uid)
                                    .set(update, SetOptions.merge())
                                    .addOnSuccessListener(unused -> {
                                        android.util.Log.d("STUDENT_ID_SAVED", "Saved to module_progress: " + studentID);
                                        loadLessonProgressFromFirestore();
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("SAVE_FAIL", "Failed saving studentId", e);
                                        loadLessonProgressFromFirestore();
                                    });
                        } else {
                            android.util.Log.w("MISSING_FIELD", "studentId field missing in students/" + uid);
                            loadLessonProgressFromFirestore();
                        }
                    } else {
                        android.util.Log.w("NO_DOC", "No student document found for uid: " + uid);
                        loadLessonProgressFromFirestore();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FETCH_FAIL", "Error fetching student doc", e);
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

        // Update cache
        LessonProgressCache.setData(data);

        // Update UI
        updateUIFromProgress(data);
    }

    private void updateUIFromProgress(Map<String, Object> data) {
        if (data == null) return;

        Object module2Obj = data.get("module_2");
        if (!(module2Obj instanceof Map)) return;

        Map<String, Object> module2 = (Map<String, Object>) module2Obj;
        Object lessonsObj = module2.get("lessons");
        if (!(lessonsObj instanceof Map)) return;

        Map<String, Object> lessons = (Map<String, Object>) lessonsObj;

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
    }

    private boolean isCompleted(Map<String, Object> lessons, String key) {
        Object lessonObj = lessons.get(key);
        if (!(lessonObj instanceof Map)) return false;

        Map<String, Object> lessonData = (Map<String, Object>) lessonObj;
        return "completed".equals(lessonData.get("status"));
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

        db.collection("module_progress").document(uid).set(update, SetOptions.merge());
    }

    private void lockAllButtons() {
        lockButton(btnTambalan);
        lockButton(btnHugnayan);
        lockButton(btnLangkapan);
    }

    private void lockButton(LinearLayout button) {
        button.setClickable(false);
        button.setAlpha(0.5f);
    }
}



