package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Quiz.PayakQuiz;
import com.example.habiaral.R;

public class PayakLesson extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_payak_lesson);

        Button quizButton = findViewById(R.id.UnlockButtonPayak);

        quizButton.setOnClickListener(v -> {
            Intent intent = new Intent(PayakLesson.this, PayakQuiz.class);
            startActivity(intent);
        });
    }
}
