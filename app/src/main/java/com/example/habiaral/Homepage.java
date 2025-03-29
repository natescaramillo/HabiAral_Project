package com.example.habiaral;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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



        bahagi.setOnClickListener(v -> startActivity(new Intent(Homepage.this, Bahagi_ng_kayarian.class)));
        kayarian.setOnClickListener(v -> startActivity(new Intent(Homepage.this, Kayarian_ng_pangungusap.class)));
        komprehensyon.setOnClickListener(v -> startActivity(new Intent(Homepage.this, Komprehensyon.class)));
        palaro.setOnClickListener(v -> startActivity(new Intent(Homepage.this, Palaro.class)));
    }
}
