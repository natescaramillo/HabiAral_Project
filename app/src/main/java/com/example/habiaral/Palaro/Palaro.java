package com.example.habiaral.Palaro;

import android.content.Intent;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View;
import androidx.appcompat.app.AlertDialog;


import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;

public class Palaro extends AppCompatActivity {
    View button1, button2, button3;
    ImageView gameMechanicsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palaro);

        button1 = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        gameMechanicsIcon = findViewById(R.id.game_mechanic_icon);
        gameMechanicsIcon.setOnClickListener(v -> showGameMechanics());


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
    public void showGameMechanics() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.game_mechanics_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

    }
}
