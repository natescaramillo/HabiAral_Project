package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.R;

public class Pangatnig extends AppCompatActivity {

    Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangatnig_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangatnig);

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

        editor.putBoolean("PangatnigDone", true);
        editor.apply();

        Toast.makeText(this, "Next Lesson Unlocked: Pang-Ukol!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Pangatnig.this, BahagiNgPananalita.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}
