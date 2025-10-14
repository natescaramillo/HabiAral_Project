package com.habiaral.app.Activity;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.habiaral.app.R;
import com.habiaral.app.Utils.SoundClickUtils;

public class AboutUsActivity extends AppCompatActivity {

    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_about_us);

        btnBack = findViewById(R.id.back_button);
        btnBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

    }
}
