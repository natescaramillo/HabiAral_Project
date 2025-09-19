package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.KayarianNgPangungusap.Quiz.TambalanQuiz;
import com.example.habiaral.R;

public class TambalanLesson extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kayarian_ng_pangungusap_tambalan_lesson);

        Button quizButton = findViewById(R.id.UnlockButtonTambalan);

        quizButton.setOnClickListener(v -> {
            Intent intent = new Intent(TambalanLesson.this, TambalanQuiz.class);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                startActivity(new Intent(TambalanLesson.this, KayarianNgPangungusap.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
    }
}
