package com.example.habiaral.Komprehensyon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;

public class KomprehensyonStory extends AppCompatActivity {

    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_komprehensyon_story);

        button1 = findViewById(R.id.buttonT);

        button1.setOnClickListener(v -> {
            Intent intent = new Intent(KomprehensyonStory.this, KomprehensyonQuiz.class);
            startActivity(intent);
        });
    }
}
