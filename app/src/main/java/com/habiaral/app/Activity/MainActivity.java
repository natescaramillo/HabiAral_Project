package com.habiaral.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.habiaral.app.Utils.InternetCheckerUtils;
import com.habiaral.app.R;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        InternetCheckerUtils.checkInternet(this, () -> goToWelcome());
    }

    private void goToWelcome() {
        getWindow().getDecorView().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}