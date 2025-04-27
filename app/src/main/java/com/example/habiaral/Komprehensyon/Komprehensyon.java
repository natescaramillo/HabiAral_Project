package com.example.habiaral.Komprehensyon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Komprehensyon.Stories.Kwento1;
import com.example.habiaral.Komprehensyon.Stories.Kwento2;
import com.example.habiaral.Komprehensyon.Stories.Kwento3;

import com.example.habiaral.R;

public class Komprehensyon extends AppCompatActivity {

    LinearLayout btnKwento1, btnKwento2, btnKwento3;
    FrameLayout kwento1Lock, kwento2Lock, kwento3Lock;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_komprehensyon);

        btnKwento1 = findViewById(R.id.kwento1);
        btnKwento2 = findViewById(R.id.kwento2);
        btnKwento3 = findViewById(R.id.kwento3);

        kwento1Lock = findViewById(R.id.kwento1Lock);
        kwento2Lock = findViewById(R.id.kwento2Lock);
        kwento3Lock = findViewById(R.id.kwento3Lock);

        sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);

        boolean kwento1Done = sharedPreferences.getBoolean("Kwento1Done", false);
        boolean kwento2Done = sharedPreferences.getBoolean("Kwento2Done", false);
        boolean kwento3Done = sharedPreferences.getBoolean("Kwento3Done", false);

        unlockButton(btnKwento1, true, kwento1Lock);
        unlockButton(btnKwento2, kwento1Done, kwento2Lock);
        unlockButton(btnKwento3, kwento2Done, kwento3Lock);

        btnKwento1.setOnClickListener(v -> startActivity(new Intent(this, Kwento1.class)));
        btnKwento2.setOnClickListener(v -> startActivity(new Intent(this, Kwento2.class)));
        btnKwento3.setOnClickListener(v -> startActivity(new Intent(this, Kwento3.class)));
    }

    private void unlockButton(LinearLayout layout, boolean isUnlocked, FrameLayout lock) {
        layout.setEnabled(isUnlocked);
        layout.setClickable(isUnlocked);
        layout.setAlpha(isUnlocked ? 1.0f : 0.5f);
        lock.setVisibility(isUnlocked ? FrameLayout.GONE : FrameLayout.VISIBLE);
    }
}
