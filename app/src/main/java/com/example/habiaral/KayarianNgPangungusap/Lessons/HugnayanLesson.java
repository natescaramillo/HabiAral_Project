package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Quiz.HugnayanQuiz;
import com.example.habiaral.R;

public class HugnayanLesson extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_hugnayan_lesson);

        Button quizButton = findViewById(R.id.UnlockButtonHugnayan);

        quizButton.setOnClickListener(v -> {
            Intent intent = new Intent(HugnayanLesson.this, HugnayanQuiz.class);
            startActivity(intent);
        });
    }
}
