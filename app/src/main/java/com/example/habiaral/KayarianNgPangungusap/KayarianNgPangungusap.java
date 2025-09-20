package com.example.habiaral.KayarianNgPangungusap;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Lessons.PayakLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.TambalanLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.HugnayanLesson;
import com.example.habiaral.KayarianNgPangungusap.Lessons.LangkapanLesson;
import com.example.habiaral.R;
import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.Utils.SoundClickUtils;
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

        ImageView kayarianBack = findViewById(R.id.kayarian_back);
        kayarianBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // Load cached progress first
        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        // Ensure module_2 exists in Firestore
        ensureModule2Exists();

        // Save studentId to module_progress if available
        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentSnap -> {
                    if (studentSnap.exists() && studentSnap.contains("studentId")) {
                        String studentID = studentSnap.getString("studentId");
                        Map<String, Object> update = new HashMap<>();
                        update.put("studentId", studentID);

                        db.collection("module_progress").document(uid)
                                .set(update, SetOptions.merge())
                                .addOnSuccessListener(unused -> loadModuleProgress())
                                .addOnFailureListener(e -> loadModuleProgress());
                    } else {
                        loadModuleProgress();
                    }
                })
                .addOnFailureListener(e -> loadModuleProgress());

        if (isFirstTime()) {
            showDescriptionDialog();
            setFirstTimeShown();
        }

        ImageView helpBtn = findViewById(R.id.imageView14);
        helpBtn.setOnClickListener(v -> showDescriptionDialog());
    }

    private void showDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_box_description, null);
        builder.setView(dialogView);

        AlertDialog descriptionDialog = builder.create();
        descriptionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ImageView closeBtn = dialogView.findViewById(R.id.description_dialog_close);

        closeBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            descriptionDialog.dismiss();
        });

        descriptionDialog.show();
    }

    private boolean isFirstTime() {
        return getSharedPreferences("KayarianPrefs", MODE_PRIVATE)
                .getBoolean("isFirstTime", true);
    }

    private void setFirstTimeShown() {
        getSharedPreferences("KayarianPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstTime", false)
                .apply();
    }

    private void ensureModule2Exists() {
        db.collection("module_progress").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || !snapshot.contains("module_2")) {
                        Map<String, Object> module2Map = new HashMap<>();
                        module2Map.put("modulename", "Kayarian ng Pangungusap");
                        module2Map.put("status", "in_progress");

                        Map<String, Object> update = new HashMap<>();
                        update.put("module_2", module2Map);

                        db.collection("module_progress").document(uid)
                                .set(update, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    LessonProgressCache.setData(update);
                                    updateUIFromProgress(update);
                                });
                    }
                });
    }

    private void loadModuleProgress() {
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

        Object module2Obj = data.get("module_2");
        if (!(module2Obj instanceof Map)) return;

        Map<String, Object> module2 = (Map<String, Object>) module2Obj;

        // Unlock buttons based on status
        String status = (String) module2.get("status");
        boolean isInProgress = "in_progress".equals(status);
        unlockButton(btnPayak, true, payakLock);
        unlockButton(btnTambalan, isInProgress, tambalanLock);
        unlockButton(btnHugnayan, isInProgress, hugnayanLock);
        unlockButton(btnLangkapan, isInProgress, langkapanLock);
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
