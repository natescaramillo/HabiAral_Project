package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class Homepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_homepage);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        Button bahagi = findViewById(R.id.pananalita);
        Button kayarian = findViewById(R.id.kayarian);
        Button komprehensyon = findViewById(R.id.komprehensyon);
        Button palaro = findViewById(R.id.palaro);

        bahagi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Homepage.this, Bahagi_ng_kayarian.class);
                startActivity(intent);
            }
        });
        kayarian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Homepage.this, Kayarian_ng_pangungusap.class);
                startActivity(intent);
            }
        });
        komprehensyon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Homepage.this, Komprehensyon.class);
                startActivity(intent);
            }
        });
        palaro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, Palaro.class);
                startActivity(intent);
            }
        });
    }
}