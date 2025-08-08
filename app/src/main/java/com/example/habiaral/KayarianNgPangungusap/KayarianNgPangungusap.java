package com.example.habiaral.KayarianNgPangungusap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Lessons.Payak;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Tambalan;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Hugnayan;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Langkapan;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class KayarianNgPangungusap extends AppCompatActivity {

    LinearLayout btnPayak, btnTambalan, btnHugnayan, btnLangkapan;
    FrameLayout payakLock, tambalanLock, hugnayanLock, langkapanLock;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap);

        btnPayak = findViewById(R.id.payak);
        btnTambalan = findViewById(R.id.tambalan);
        btnHugnayan = findViewById(R.id.hugnayan);
        btnLangkapan = findViewById(R.id.langkapan);

        payakLock = findViewById(R.id.payakLock);
        tambalanLock = findViewById(R.id.tambalanLock);
        hugnayanLock = findViewById(R.id.hugnayanLock);
        langkapanLock = findViewById(R.id.langkapanLock);

        if (user == null) return;

        db.collection("lesson_progress").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean payakDone = documentSnapshot.getBoolean("PayakDone") != null && documentSnapshot.getBoolean("PayakDone");
                    boolean tambalanDone = documentSnapshot.getBoolean("TambalanDone") != null && documentSnapshot.getBoolean("TambalanDone");
                    boolean hugnayanDone = documentSnapshot.getBoolean("HugnayanDone") != null && documentSnapshot.getBoolean("HugnayanDone");
                    boolean langkapanDone = documentSnapshot.getBoolean("LangkapanDone") != null && documentSnapshot.getBoolean("LangkapanDone");

                    unlockButton(btnPayak, true, payakLock);
                    unlockButton(btnTambalan, payakDone, tambalanLock);
                    unlockButton(btnHugnayan, tambalanDone, hugnayanLock);
                    unlockButton(btnLangkapan, hugnayanDone, langkapanLock);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Failed to load lesson progress", e);
                    // fallback: only first lesson is unlocked
                    unlockButton(btnPayak, true, payakLock);
                    unlockButton(btnTambalan, true, tambalanLock);
                    unlockButton(btnHugnayan, false, hugnayanLock);
                    unlockButton(btnLangkapan, false, langkapanLock);
                });

        btnPayak.setOnClickListener(v -> startActivity(new Intent(this, Payak.class)));
        btnTambalan.setOnClickListener(v -> startActivity(new Intent(this, Tambalan.class)));
        btnHugnayan.setOnClickListener(v -> startActivity(new Intent(this, Hugnayan.class)));
        btnLangkapan.setOnClickListener(v -> startActivity(new Intent(this, Langkapan.class)));
    }

    private void unlockButton(LinearLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }
}
