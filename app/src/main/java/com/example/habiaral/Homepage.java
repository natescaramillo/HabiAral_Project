package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class Homepage extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_homepage);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize buttons
        Button bahagiButton = findViewById(R.id.bahagi);
        Button komprehensyonButton = findViewById(R.id.komprehensyon);
        Button kayarianButton = findViewById(R.id.kayarian);
        Button palaroButton = findViewById(R.id.palaro);

        // Set the same OnClickListener for all buttons
        bahagiButton.setOnClickListener(this);
        komprehensyonButton.setOnClickListener(this);
        kayarianButton.setOnClickListener(this);
        palaroButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;

        // Use if-else statements to determine which button was clicked
        if (view.getId() == R.id.bahagi) {
            intent = new Intent(this, Bahagi_ng_kayarian.class); // Ensure this class exists
        } else if (view.getId() == R.id.komprehensyon) {
            intent = new Intent(this, Komprehensyon.class); // Ensure this class exists
        } else if (view.getId() == R.id.kayarian) {
            intent = new Intent(this, Kayarian_ng_pangungusap.class); // Ensure this class exists
        } else if (view.getId() == R.id.palaro) {
            intent = new Intent(this, Palaro.class); // Ensure this class exists
        }

        // Start the activity if the intent is not null
        if (intent != null) {
            startActivity(intent);
        }
    }
}