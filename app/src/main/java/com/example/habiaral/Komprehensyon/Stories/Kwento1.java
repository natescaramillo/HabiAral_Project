package com.example.habiaral.Komprehensyon.Stories;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Komprehensyon.Komprehensyon;
import com.example.habiaral.R;

public class Kwento1 extends AppCompatActivity {
    Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kwento1);

        unlockButton = findViewById(R.id.UnlockButtonKwento1);

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

        editor.putBoolean("Kwento1Done", true);
        editor.apply();

        Toast.makeText(this, "Next Story Unlocked: Kwento2!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Kwento1.this, Komprehensyon.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}
