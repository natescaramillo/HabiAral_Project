package com.habiaral.app.BahagiNgPananalita;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.habiaral.app.BahagiNgPananalita.Lessons.*;
import com.habiaral.app.Cache.LessonProgressCache;
import com.habiaral.app.R;
import com.habiaral.app.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import android.media.MediaPlayer;
import android.widget.TextView;


public class BahagiNgPananalita extends AppCompatActivity {

    private LinearLayout btnPangngalan, btnPandiwa, btnPangUri, btnPangHalip, btnPangAbay,
            btnPangatnig, btnPangUkol, btnPangAkop, btnPadamdam, btnPangawing;
    private FrameLayout pangngalanLock, pandiwaLock, pangUriLock, pangHalipLock, pangAbayLock,
            pangatnigLock, pangUkolLock, pangAkopLock, padamdamLock, pangawingLock;
    private FirebaseFirestore db;
    private String uid;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita);

        initViews();
        lockAllButtons();
        ImageView bahagiBack = findViewById(R.id.bahagi_back);

        bahagiBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        Map<String, Object> cachedData = LessonProgressCache.getData();
        if (cachedData != null) {
            updateUIFromProgress(cachedData);
        }

        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentSnap -> {
                    if (studentSnap.exists()) {
                        if (studentSnap.contains("studentId")) {
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
                    } else {
                        loadLessonProgressFromFirestore();
                    }
                })
                .addOnFailureListener(e -> loadLessonProgressFromFirestore());

        if (isFirstTime()) {
            showDescriptionDialog();
            setFirstTimeShown();
        }

        ImageView helpBtn = findViewById(R.id.imageView14);
        helpBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            showDescriptionDialog();
        });
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

        titleText.setText("Bahagi ng Pananalita");

        contentText.setText(
                "Ang Bahagi ng Pananalita ay mga salita na may kanya-kanyang tungkulin sa pangungusap. " +
                        "Ito ang nagsisilbing balangkas ng wika upang maging malinaw, maayos, at mabisa ang komunikasyon. " +
                        "Sa pamamagitan ng mga bahagi ng pananalita, nagiging posible ang pagpapahayag ng ideya, " +
                        "damdamin, at kaisipan ng tao."
        );

        closeBtn.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            descriptionDialog.dismiss();
        });

        descriptionDialog.show();
    }


    private boolean isFirstTime() {
        return getSharedPreferences("BahagiPrefs", MODE_PRIVATE)
                .getBoolean("isFirstTime", true);
    }

    private void setFirstTimeShown() {
        getSharedPreferences("BahagiPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstTime", false)
                .apply();
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
        boolean pangAkopDone = isCompleted(lessons, "pangangkop");
        boolean padamdamDone = isCompleted(lessons, "pandamdam");
        boolean pangawingDone = isCompleted(lessons, "pangawing");

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

        checkAndCompleteModule(
                pangngalanDone, pandiwaDone, pangUriDone, pangHalipDone, pangAbayDone,
                pangatnigDone, pangUkolDone, pangAkopDone, padamdamDone, pangawingDone
        );
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

        btnPangngalan.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangngalanLesson.class));
        });

        btnPandiwa.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PandiwaLesson.class));
        });

        btnPangUri.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangUriLesson.class));
        });

        btnPangHalip.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangHalipLesson.class));
        });

        btnPangAbay.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangAbayLesson.class));
        });

        btnPangatnig.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangatnigLesson.class));
        });

        btnPangUkol.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangUkolLesson.class));
        });

        btnPangAkop.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangAngkopLesson.class));
        });

        btnPadamdam.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PandamdamLesson.class));
        });

        btnPangawing.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, PangawingLesson.class));
        });
        }


    private void checkAndCompleteModule(
            boolean pangngalanDone, boolean pandiwaDone, boolean pangUriDone,
            boolean pangHalipDone, boolean pangAbayDone, boolean pangatnigDone,
            boolean pangUkolDone, boolean pangAkopDone, boolean padamdamDone,
            boolean pangawingDone) {

        boolean allDone = pangngalanDone && pandiwaDone && pangUriDone && pangHalipDone &&
                pangAbayDone && pangatnigDone && pangUkolDone && pangAkopDone &&
                padamdamDone && pangawingDone;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module1Updates = new HashMap<>();

        module1Updates.put("modulename", "Bahagi ng Pananalita");
        module1Updates.put("status", allDone ? "completed" : "in_progress");
        update.put("module_1", module1Updates);

        db.collection("module_progress").document(uid).set(update, SetOptions.merge());
    }

    private void lockAllButtons() {
        lockButton(btnPandiwa);
        lockButton(btnPangUri);
        lockButton(btnPangHalip);
        lockButton(btnPangAbay);
        lockButton(btnPangatnig);
        lockButton(btnPangUkol);
        lockButton(btnPangAkop);
        lockButton(btnPadamdam);
        lockButton(btnPangawing);
    }

    private void lockButton(LinearLayout button) {
        button.setClickable(false);
        button.setAlpha(0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
