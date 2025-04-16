package com.example.habiaral;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class KayarianNgPangungusap extends AppCompatActivity {

    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kayarian_ng_pangungusap);

        button1 = findViewById(R.id.button);

        resetButtonStyle();

        button1.setOnClickListener(v -> {
            button1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EE4211")));
            button1.setEnabled(false);

            Intent intent = new Intent(KayarianNgPangungusap.this, KayarianNgPangungusapLesson.class);
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
