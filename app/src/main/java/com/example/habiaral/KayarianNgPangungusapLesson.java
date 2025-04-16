package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class KayarianNgPangungusapLesson extends AppCompatActivity {

    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kayarian_ng_pangungusap_lesson);

        button1 = findViewById(R.id.buttonT);

        button1.setOnClickListener(v -> {
            Intent intent = new Intent(KayarianNgPangungusapLesson.this, KayarianNgPangungusapQuiz.class);
            startActivity(intent);
        });
    }
}