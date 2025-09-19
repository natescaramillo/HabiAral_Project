package com.example.habiaral.Panitikan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.Panitikan.Alamat.Alamat;
import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.MaiklingKuwento.MaiklingKuwento;
import com.example.habiaral.Panitikan.Pabula.Pabula;
import com.example.habiaral.Panitikan.Parabula.Parabula;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Panitikan extends AppCompatActivity {

    LinearLayout epikoBtn, parabulaBtn, pabulaBtn, maiklingKuwentoBtn, alamatBtn;
    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        // ✅ Mark module_3 as in_progress
        markModuleInProgress();

        // ✅ Load from cache if available
        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        // ✅ Save studentId and load progress
        saveStudentIdAndLoadProgress();

        // ✅ Init buttons
        initViews();
    }

    private void initViews() {
        epikoBtn = findViewById(R.id.epiko);
        parabulaBtn = findViewById(R.id.parabula);
        pabulaBtn = findViewById(R.id.pabula);
        maiklingKuwentoBtn = findViewById(R.id.maikling_kuwento);
        alamatBtn = findViewById(R.id.alamat);

        alamatBtn.setOnClickListener(v -> openCategory(Alamat.class, "Alamat"));
        epikoBtn.setOnClickListener(v -> openCategory(Epiko.class, "Epiko"));
        maiklingKuwentoBtn.setOnClickListener(v -> openCategory(MaiklingKuwento.class, "Maikling Kuwento"));
        pabulaBtn.setOnClickListener(v -> openCategory(Pabula.class, "Pabula"));
        parabulaBtn.setOnClickListener(v -> openCategory(Parabula.class, "Parabula"));
    }

    private void openCategory(Class<?> cls, String categoryName) {
        try {
            // ✅ Play click sound
            SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);

            // ✅ Update Firestore category progress
            markCategoryInProgress(categoryName);

            // ✅ Open the Activity
            startActivity(new Intent(Panitikan.this, cls));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Save studentId then load lesson progress
    private void saveStudentIdAndLoadProgress() {
        if (uid == null) return;

        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentSnap -> {
                    if (studentSnap.exists() && studentSnap.contains("studentId")) {
                        String studentID = studentSnap.getString("studentId");
                        Map<String, Object> update = new HashMap<>();
                        update.put("studentId", studentID);

                        db.collection("module_progress").document(uid)
                                .set(update, SetOptions.merge())
                                .addOnSuccessListener(unused -> loadLessonProgressFromFirestore())
                                .addOnFailureListener(e -> loadLessonProgressFromFirestore());
                    } else {
                        loadLessonProgressFromFirestore();
                    }
                })
                .addOnFailureListener(e -> loadLessonProgressFromFirestore());
    }

    // 🔹 Mark module_3 progress
    private void markModuleInProgress() {
        if (uid == null) return;

        Map<String, Object> moduleUpdate = new HashMap<>();
        moduleUpdate.put("modulename", "Panitikan");
        moduleUpdate.put("status", "in_progress");

        db.collection("module_progress").document(uid)
                .set(Map.of("module_3", moduleUpdate), SetOptions.merge());
    }

    // 🔹 Mark category progress
    private void markCategoryInProgress(String categoryName) {
        if (uid == null) return;

        Map<String, Object> categoryUpdate = new HashMap<>();
        categoryUpdate.put("categoryname", categoryName);
        categoryUpdate.put("status", "in_progress");

        db.collection("module_progress").document(uid)
                .set(Map.of("module_3",
                        Map.of("categories",
                                Map.of(categoryName, categoryUpdate)
                        )), SetOptions.merge());
    }

    // 🔹 Placeholder functions para hindi mag-crash
    private void updateUIFromProgress(Map<String, Object> progressData) {
        // TODO: gamitin progressData para i-update yung UI
    }

    private void loadLessonProgressFromFirestore() {
        // TODO: i-fetch module_3 data from Firestore at update UI
    }
}
