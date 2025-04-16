package com.example.habiaral.Komprehensyon;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;

public class Komprehensyon extends AppCompatActivity {

    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_komprehensyon);

        button1 = findViewById(R.id.button);

        resetButtonStyle();

        button1.setOnClickListener(v -> {
            button1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EE4211")));
            button1.setEnabled(false);

            Intent intent = new Intent(Komprehensyon.this, KomprehensyonLesson.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetButtonStyle();
    }

    private void resetButtonStyle() {
        button1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0B782")));
        button1.setEnabled(true);
    }
}
