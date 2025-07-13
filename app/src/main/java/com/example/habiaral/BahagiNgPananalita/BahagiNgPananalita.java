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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class BahagiNgPananalita extends AppCompatActivity {

    LinearLayout btnPangngalan, btnPandiwa, btnPangUri, btnPangHalip, btnPangAbay, btnPangatnig, btnPangukol, btnPangAkop, btnPadamdam, btnPangawing;
    FrameLayout pangngalanLock, pandiwaLock, pangUriLock, pangHalipLock, pangAbayLock, pangatnigLock, pangUkolLock, pangAkopLock, padamdamLock, pangawingLock;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bahagi_ng_pananalita);

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

        sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = user.getUid();

            db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                    if (module1 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                        if (lessons != null) {
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
                        }
                    }
                }
            });
        }

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

        if (allDone) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = user.getUid();

            Map<String, Object> moduleStatus = new HashMap<>();
            moduleStatus.put("status", "completed");

            db.collection("module_progress")
                    .document(uid)
                    .set(Map.of("module_1", moduleStatus), SetOptions.merge());
        }
    }
}
