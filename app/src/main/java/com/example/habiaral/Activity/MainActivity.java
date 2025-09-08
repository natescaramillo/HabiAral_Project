package com.example.habiaral.Activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Utils.InternetChecker;
import com.example.habiaral.R;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        InternetChecker.checkInternet(this, () -> goToWelcome());
    }

    private void goToWelcome() {
        getWindow().getDecorView().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}