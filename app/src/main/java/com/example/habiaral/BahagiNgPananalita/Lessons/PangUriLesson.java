package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangUriQuiz;
import com.example.habiaral.R;

public class PangUriLesson extends AppCompatActivity {

    Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panguri_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPanguri);

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PangUriLesson.this, PangUriQuiz.class);
                startActivity(intent);
            }
        });
    }
}
