package com.example.habiaral.PagUnawa;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.PagUnawa.Stories.Kwento1;
import com.example.habiaral.PagUnawa.Stories.Kwento2;
import com.example.habiaral.PagUnawa.Stories.Kwento3;
import com.example.habiaral.PagUnawa.Stories.Kwento4;
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

public class PagUnawa extends AppCompatActivity {

    LinearLayout btnEpiko, btnParabula, btnPabula, btnMaiklingKuwento, btnAlamat;
    FirebaseFirestore db;
    String uid;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa);

        initViews();
        lockAllButtons();

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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

        boolean epikoDone = isCompleted(lessons, "epiko");
        boolean parabulaDone = isCompleted(lessons, "parabula");
        boolean pabulaDone = isCompleted(lessons, "pabula");
        boolean maiklingKuwentoDone = isCompleted(lessons, "maikling_kuwento");
        boolean alamatDone = isCompleted(lessons, "alamat");

        unlockButton(btnEpiko, true);
        unlockButton(btnParabula, epikoDone);
        unlockButton(btnPabula, parabulaDone);
        unlockButton(btnMaiklingKuwento, pabulaDone);
        unlockButton(btnAlamat, maiklingKuwentoDone);

        checkAndCompleteModule(epikoDone, parabulaDone, pabulaDone, maiklingKuwentoDone, alamatDone);
    }

    private boolean isCompleted(Map<String, Object> lessons, String key) {
        Object lessonObj = lessons.get(key);
        if (!(lessonObj instanceof Map)) return false;

        Map<String, Object> lessonData = (Map<String, Object>) lessonObj;
        return "completed".equals(lessonData.get("status"));
    }

    private void unlockButton(LinearLayout layout, boolean isUnlocked) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
    }

    private void initViews() {
        btnEpiko = findViewById(R.id.epiko);
        btnParabula = findViewById(R.id.parabula);
        btnPabula = findViewById(R.id.pabula);
        btnMaiklingKuwento = findViewById(R.id.maikling_kuwento);
        btnAlamat = findViewById(R.id.alamat);

        btnEpiko.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, Kwento1.class));
        });

        btnParabula.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, Kwento2.class));
        });

        btnPabula.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, Kwento3.class));
        });

        btnMaiklingKuwento.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            startActivity(new Intent(this, Kwento4.class));
        });

        btnAlamat.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            // TODO: Add corresponding activity for Alamat
        });
    }

    private void checkAndCompleteModule(boolean epikoDone, boolean parabulaDone,
                                        boolean pabulaDone, boolean maiklingKuwentoDone, boolean alamatDone) {
        boolean allDone = epikoDone && parabulaDone && pabulaDone && maiklingKuwentoDone && alamatDone;

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> module3Updates = new HashMap<>();

        module3Updates.put("modulename", "Pag-unawa");
        module3Updates.put("status", allDone ? "completed" : "in_progress");

        update.put("module_3", module3Updates);

        db.collection("module_progress").document(uid).set(update, SetOptions.merge());
    }

    private void lockAllButtons() {
        lockButton(btnParabula);
        lockButton(btnPabula);
        lockButton(btnMaiklingKuwento);
        lockButton(btnAlamat);
    }

    private void lockButton(LinearLayout button) {
        button.setClickable(false);
        button.setAlpha(0.5f);
    }
}
