package com.example.habiaral.Palaro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;

public class Palaro extends AppCompatActivity {

    Button button1, button2, button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro);

        //0button1 = findViewById(R.id.button);
        //button2 = findViewById(R.id.button2);
        //button3 = findViewById(R.id.button3);

        button1.setOnClickListener(v -> {
            Intent intent = new Intent(Palaro.this, PalaroBaguhan.class);
            startActivity(intent);
        });

        button2.setOnClickListener(v -> {
            Intent intent = new Intent(Palaro.this, PalaroHusay.class);
            startActivity(intent);
        });

        button3.setOnClickListener(v -> {
            Intent intent = new Intent(Palaro.this, PalaroDalubhasa.class);
            startActivity(intent);
        });
    }
}
