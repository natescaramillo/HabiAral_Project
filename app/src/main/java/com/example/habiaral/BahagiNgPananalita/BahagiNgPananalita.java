package com.example.habiaral.BahagiNgPananalita;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Lessons.*;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class BahagiNgPananalita extends AppCompatActivity {

    private LinearLayout btnPangngalan, btnPandiwa, btnPangUri, btnPangHalip, btnPangAbay,
            btnPangatnig, btnPangUkol, btnPangAkop, btnPadamdam, btnPangawing;
    private FrameLayout pangngalanLock, pandiwaLock, pangUriLock, pangHalipLock, pangAbayLock,
            pangatnigLock, pangUkolLock, pangAkopLock, padamdamLock, pangawingLock;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita);

        initViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // Load cached progress if available
        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        // Fetch studentId from students collection and save to module_progress collection
        db.collection("students").document(uid).get().addOnSuccessListener(studentSnap -> {
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
        }).addOnFailureListener(e -> {
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

        Object module1Obj = data.get("module_1");
        if (!(module1Obj instanceof Map)) return;
        Map<String, Object> module1 = (Map<String, Object>) module1Obj;

        Object lessonsObj = module1.get("lessons");
        if (!(lessonsObj instanceof Map)) return;
        Map<String, Object> lessons = (Map<String, Object>) lessonsObj;

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

        // Unlock buttons based on completion
        unlockButton(btnPangngalan, true, pangngalanLock);
        unlockButton(btnPandiwa, pangngalanDone, pandiwaLock);
        unlockButton(btnPangUri, pandiwaDone, pangUriLock);
        unlockButton(btnPangHalip, pangUriDone, pangHalipLock);
        unlockButton(btnPangAbay, pangHalipDone, pangAbayLock);
        unlockButton(btnPangatnig, pangAbayDone, pangatnigLock);
        unlockButton(btnPangUkol, pangatnigDone, pangUkolLock);
        unlockButton(btnPangAkop, pangUkolDone, pangAkopLock);
        unlockButton(btnPadamdam, pangAkopDone, padamdamLock);
        unlockButton(btnPangawing, padamdamDone, pangawingLock);

        checkAndCompleteModule(pangngalanDone, pandiwaDone, pangUriDone, pangHalipDone,
                pangAbayDone, pangatnigDone, pangUkolDone, pangAkopDone, padamdamDone, pangawingDone);
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
        btnPangngalan = findViewById(R.id.pangngalan);
        btnPandiwa = findViewById(R.id.pandiwa);
        btnPangUri = findViewById(R.id.panguri);
        btnPangHalip = findViewById(R.id.panghalip);
        btnPangAbay = findViewById(R.id.pangabay);
        btnPangatnig = findViewById(R.id.pangatnig);
        btnPangUkol = findViewById(R.id.pangukol);
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
        btnPangUkol.setOnClickListener(v -> startActivity(new Intent(this, PangUkolLesson.class)));
        btnPangAkop.setOnClickListener(v -> startActivity(new Intent(this, PangAkopLesson.class)));
        btnPadamdam.setOnClickListener(v -> startActivity(new Intent(this, PadamdamLesson.class)));
        btnPangawing.setOnClickListener(v -> startActivity(new Intent(this, PangawingLesson.class)));
    }

    private void checkAndCompleteModule(boolean pangngalanDone, boolean pandiwaDone, boolean pangUriDone, boolean pangHalipDone,
                                        boolean pangAbayDone, boolean pangatnigDone, boolean pangUkolDone, boolean pangAkopDone,
                                        boolean padamdamDone, boolean pangawingDone) {
        boolean allDone = pangngalanDone && pandiwaDone && pangUriDone && pangHalipDone &&
                pangAbayDone && pangatnigDone && pangUkolDone && pangAkopDone && padamdamDone && pangawingDone;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module1Updates = new HashMap<>();
        module1Updates.put("modulename", "Bahagi ng Pananalita");
        module1Updates.put("status", allDone ? "completed" : "in_progress");
        update.put("module_1", module1Updates);

        db.collection("module_progress").document(uid).set(update, SetOptions.merge());
    }
}
