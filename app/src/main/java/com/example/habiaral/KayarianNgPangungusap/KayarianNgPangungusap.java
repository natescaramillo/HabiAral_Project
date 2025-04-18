package com.example.habiaral.KayarianNgPangungusap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Lessons.Payak;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Tambalan;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Hugnayan;
import com.example.habiaral.KayarianNgPangungusap.Lessons.Langkapan;

import com.example.habiaral.R;

public class KayarianNgPangungusap extends AppCompatActivity {

    LinearLayout btnPayak, btnTambalan, btnHugnayan, btnLangkapan;
    FrameLayout payakLock, tambalanLock, hugnayanLock, langkapanLock;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kayarian_ng_pangungusap);

        btnPayak = findViewById(R.id.payak);
        btnTambalan = findViewById(R.id.tambalan);
        btnHugnayan = findViewById(R.id.hugnayan);
        btnLangkapan = findViewById(R.id.langkapan);

        payakLock = findViewById(R.id.payakLock);
        tambalanLock = findViewById(R.id.tambalanLock);
        hugnayanLock = findViewById(R.id.hugnayanLock);
        langkapanLock = findViewById(R.id.langkapanLock);

        sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);

        boolean payakDone = sharedPreferences.getBoolean("PayakDone", false);
        boolean tambalanDone = sharedPreferences.getBoolean("TambalanDone", false);
        boolean hugnayanDone = sharedPreferences.getBoolean("HugnayanDone", false);
        boolean langkapanDone = sharedPreferences.getBoolean("LangkapanDone", false);

        unlockButton(btnPayak, true, payakLock);
        unlockButton(btnTambalan, payakDone, tambalanLock);
        unlockButton(btnHugnayan, tambalanDone, hugnayanLock);
        unlockButton(btnLangkapan, hugnayanDone, langkapanLock);

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
