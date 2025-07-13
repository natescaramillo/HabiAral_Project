package com.example.habiaral.BahagiNgPananalita;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Lessons.*;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class BahagiNgPananalita extends AppCompatActivity {

    LinearLayout btnPangngalan, btnPandiwa, btnPangUri, btnPangHalip, btnPangAbay, btnPangatnig, btnPangukol, btnPangAkop, btnPadamdam, btnPangawing;
    FrameLayout pangngalanLock, pandiwaLock, pangUriLock, pangHalipLock, pangAbayLock, pangatnigLock, pangUkolLock, pangAkopLock, padamdamLock, pangawingLock;
    SharedPreferences sharedPreferences;
    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bahagi_ng_pananalita);

        initViews();
        sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);

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

            Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
            if (module1 == null) return;

            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
            if (lessons == null) return;

            boolean pangngalanDone = isCompleted(lessons, "pangngalan");
            boolean pandiwaDone = isCompleted(lessons, "pandiwa");
            boolean pangUriDone = isCompleted(lessons, "panguri");
            boolean pangHalipDone = isCompleted(lessons, "panghalip");
            boolean pangAbayDone = isCompleted(lessons, "pangabay");
            boolean pangatnigDone = isCompleted(lessons, "pangatnig");
            boolean pangUkolDone = isCompleted(lessons, "pangukol");
            boolean pangAkopDone = isCompleted(lessons, "pangakop");
            boolean padamdamDone = isCompleted(lessons, "padamdam");
            boolean pangawingDone = isCompleted(lessons, "pangawing");

            // Unlock logic
            unlockButton(btnPangngalan, true, pangngalanLock);
            unlockButton(btnPandiwa, pangngalanDone, pandiwaLock);
            unlockButton(btnPangUri, pandiwaDone, pangUriLock);
            unlockButton(btnPangHalip, pangUriDone, pangHalipLock);
            unlockButton(btnPangAbay, pangHalipDone, pangAbayLock);
            unlockButton(btnPangatnig, pangAbayDone, pangatnigLock);
            unlockButton(btnPangukol, pangatnigDone, pangUkolLock);
            unlockButton(btnPangAkop, pangUkolDone, pangAkopLock);
            unlockButton(btnPadamdam, pangAkopDone, padamdamLock);
            unlockButton(btnPangawing, padamdamDone, pangawingLock);

            // Save to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("PangngalanDone", pangngalanDone);
            editor.putBoolean("PandiwaDone", pandiwaDone);
            editor.putBoolean("PangUriDone", pangUriDone);
            editor.putBoolean("PangHalipDone", pangHalipDone);
            editor.putBoolean("PangAbayDone", pangAbayDone);
            editor.putBoolean("PangatnigDone", pangatnigDone);
            editor.putBoolean("PangUkolDone", pangUkolDone);
            editor.putBoolean("PangAkopDone", pangAkopDone);
            editor.putBoolean("PadamdamDone", padamdamDone);
            editor.putBoolean("PangawingDone", pangawingDone);
            editor.apply();

            checkAndCompleteModule();
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
        btnPangngalan = findViewById(R.id.pangngalan);
        btnPandiwa = findViewById(R.id.pandiwa);
        btnPangUri = findViewById(R.id.panguri);
        btnPangHalip = findViewById(R.id.panghalip);
        btnPangAbay = findViewById(R.id.pangabay);
        btnPangatnig = findViewById(R.id.pangatnig);
        btnPangukol = findViewById(R.id.pangukol);
        btnPangAkop = findViewById(R.id.pangakop);
        btnPadamdam = findViewById(R.id.padamdam);
        btnPangawing = findViewById(R.id.pangawing);

        pangngalanLock = findViewById(R.id.pangngalanLock);
        pandiwaLock = findViewById(R.id.pandiwaLock);
        pangUriLock = findViewById(R.id.pangUriLock);
        pangHalipLock = findViewById(R.id.pangHalipLock);
        pangAbayLock = findViewById(R.id.pangAbayLock);
        pangatnigLock = findViewById(R.id.pangatnigLock);
        pangUkolLock = findViewById(R.id.pangUkolLock);
        pangAkopLock = findViewById(R.id.pangAkopLock);
        padamdamLock = findViewById(R.id.padamdamLock);
        pangawingLock = findViewById(R.id.pangawingLock);

        btnPangngalan.setOnClickListener(v -> startActivity(new Intent(this, PangngalanLesson.class)));
        btnPandiwa.setOnClickListener(v -> startActivity(new Intent(this, PandiwaLesson.class)));
        btnPangUri.setOnClickListener(v -> startActivity(new Intent(this, PangUriLesson.class)));
        btnPangHalip.setOnClickListener(v -> startActivity(new Intent(this, PangHalipLesson.class)));
        btnPangAbay.setOnClickListener(v -> startActivity(new Intent(this, PangAbayLesson.class)));
        btnPangatnig.setOnClickListener(v -> startActivity(new Intent(this, PangatnigLesson.class)));
        btnPangukol.setOnClickListener(v -> startActivity(new Intent(this, PangUkolLesson.class)));
        btnPangAkop.setOnClickListener(v -> startActivity(new Intent(this, PangAkopLesson.class)));
        btnPadamdam.setOnClickListener(v -> startActivity(new Intent(this, PadamdamLesson.class)));
        btnPangawing.setOnClickListener(v -> startActivity(new Intent(this, PangawingLesson.class)));
    }

    private void checkAndCompleteModule() {
        boolean allDone = sharedPreferences.getBoolean("PangngalanDone", false)
                && sharedPreferences.getBoolean("PandiwaDone", false)
                && sharedPreferences.getBoolean("PangUriDone", false)
                && sharedPreferences.getBoolean("PangHalipDone", false)
                && sharedPreferences.getBoolean("PangAbayDone", false)
                && sharedPreferences.getBoolean("PangatnigDone", false)
                && sharedPreferences.getBoolean("PangUkolDone", false)
                && sharedPreferences.getBoolean("PangAkopDone", false)
                && sharedPreferences.getBoolean("PadamdamDone", false)
                && sharedPreferences.getBoolean("PangawingDone", false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module1Updates = new HashMap<>();

        module1Updates.put("modulename", "Bahagi ng Pananalita");
        module1Updates.put("status", allDone ? "completed" : "in_progress");

        update.put("module_1", module1Updates);

        db.collection("module_progress").document(uid)
                .set(update, SetOptions.merge());
    }

}
