package com.example.habiaral.Panitikan;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.Panitikan.Alamat.Alamat;
import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.MaiklingKuwento.MaiklingKuwento;
import com.example.habiaral.Panitikan.Pabula.Pabula;
import com.example.habiaral.Panitikan.Parabula.Parabula;
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.AchievementM3Utils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Panitikan extends AppCompatActivity {

    private LinearLayout epikoBtn, parabulaBtn, pabulaBtn, maiklingKuwentoBtn, alamatBtn;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        markModuleInProgress();

        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        saveStudentIdAndLoadProgress();

        initViews();

        ImageView panitikanBack = findViewById(R.id.panitikan_back);

        panitikanBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

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

        TextView titleText = dialogView.findViewById(R.id.description_dialog_title);
        TextView contentText = dialogView.findViewById(R.id.textView24);
        ImageView closeBtn = dialogView.findViewById(R.id.description_dialog_close);

        // 👉 Title ng popup
        titleText.setText("Panitikan");

        // 👉 General explanation (ano ito at para saan)
        contentText.setText(
                "Ang Panitikan ay ang kalipunan ng mga akdang nagpapahayag ng damdamin, " +
                        "karanasan, kaalaman, at kaisipan ng tao. Ito ay maaaring pasalita o pasulat " +
                        "na anyo na naglalarawan ng kultura, tradisyon, at pamumuhay ng isang lipunan. \n\n" +
                        "Layunin ng panitikan na magbigay-aliw, magturo ng aral, magpahayag ng saloobin, " +
                        "at mapanatili ang mga karanasang makabuluhan sa kasaysayan. Sa pamamagitan nito, " +
                        "naipapasa ang yaman ng wika at karunungan mula sa isang henerasyon tungo sa susunod."
        );

        closeBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            descriptionDialog.dismiss();
        });

        descriptionDialog.show();
    }


    private boolean isFirstTime() {
        return getSharedPreferences("PanitikanPrefs", MODE_PRIVATE)
                .getBoolean("isFirstTime", true);
    }

    private void setFirstTimeShown() {
        getSharedPreferences("PanitikanPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstTime", false)
                .apply();
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
            SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);

            markCategoryInProgress(categoryName);

            AchievementM3Utils.checkAndUnlockAchievement(this, db, uid);

            startActivity(new Intent(Panitikan.this, cls));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private void markModuleInProgress() {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                        if (module3 != null) {
                            String status = (String) module3.get("status");
                            if ("completed".equals(status)) {
                                return;
                            }
                        }
                    }

                    Map<String, Object> moduleUpdate = new HashMap<>();
                    moduleUpdate.put("modulename", "Panitikan");
                    moduleUpdate.put("status", "in_progress");

                    db.collection("module_progress").document(uid)
                            .set(Map.of("module_3", moduleUpdate), SetOptions.merge());
                });
    }

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




    private void updateUIFromProgress(Map<String, Object> progressData) {}

    private void loadLessonProgressFromFirestore() {}
}
