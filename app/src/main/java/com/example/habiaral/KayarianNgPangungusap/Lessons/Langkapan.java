package com.example.habiaral.KayarianNgPangungusap.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.R;

public class Langkapan extends AppCompatActivity {

    Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_langkapan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonLangkapan);

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlockLesson();
            }
        });
    }

    private void unlockLesson() {
        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("LangkapanDone", true);
        editor.apply();

        Toast.makeText(this, "Congratulations! You've completed all lessons!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Langkapan.this, KayarianNgPangungusap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}
