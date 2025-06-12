package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangAkopQuiz;
import com.example.habiaral.R;

public class PangAkopLesson extends AppCompatActivity {

    Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangakop_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangakop);

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PangAkopLesson.this, PangAkopQuiz.class);
                startActivity(intent);
            }
        });
    }
}
