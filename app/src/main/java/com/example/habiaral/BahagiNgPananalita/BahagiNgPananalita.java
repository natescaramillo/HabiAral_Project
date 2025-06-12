package com.example.habiaral.BahagiNgPananalita;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Lessons.PadamdamLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PandiwaLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangAbayLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangAkopLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangHalipLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangUkolLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangUriLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangatnigLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangawingLesson;
import com.example.habiaral.BahagiNgPananalita.Lessons.PangngalanLesson;

import com.example.habiaral.R;

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

        boolean pangngalanDone = sharedPreferences.getBoolean("PangngalanDone", false);
        boolean pandiwaDone = sharedPreferences.getBoolean("PandiwaDone", false);
        boolean pangUriDone = sharedPreferences.getBoolean("PangUriDone", false);
        boolean pangHalipDone = sharedPreferences.getBoolean("PangHalipDone", false);
        boolean pangAbayDone = sharedPreferences.getBoolean("PangAbayDone", false);
        boolean pangatnigDone = sharedPreferences.getBoolean("PangatnigDone", false);
        boolean pangUkolDone = sharedPreferences.getBoolean("PangUkolDone", false);
        boolean pangAkopDone = sharedPreferences.getBoolean("PangAkopDone", false);
        boolean padamdamDone = sharedPreferences.getBoolean("PadamdamDone", false);
        boolean pangawingDone = sharedPreferences.getBoolean("PangawingDone", false);

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

    private void unlockButton(LinearLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }
}
