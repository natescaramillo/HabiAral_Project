package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.Quiz.LangkapanQuiz;
import com.example.habiaral.R;

public class LangkapanLesson extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_langkapan_lesson);

        Button quizButton = findViewById(R.id.UnlockButtonLangkapan);

        quizButton.setOnClickListener(v -> {
            Intent intent = new Intent(LangkapanLesson.this, LangkapanQuiz.class);
            startActivity(intent);
        });
    }
}
