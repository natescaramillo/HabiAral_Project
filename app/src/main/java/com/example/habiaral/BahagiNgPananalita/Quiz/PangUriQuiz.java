package com.example.habiaral.BahagiNgPananalita.Quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.R;

public class PangUriQuiz extends AppCompatActivity {

    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panguri_quiz);

        nextButton = findViewById(R.id.panguriNextButton);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlockNextLesson();
            }
        });
    }

    private void unlockNextLesson() {
        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("PangUriDone", true);
        editor.apply();

        Toast.makeText(this, "Next Lesson Unlocked: Panghalip!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(PangUriQuiz.this, BahagiNgPananalita.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}
