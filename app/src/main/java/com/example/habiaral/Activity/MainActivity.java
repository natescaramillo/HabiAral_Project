package com.example.habiaral.Activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private static final int CHECK_INTERVAL = 3000;
    private static final int PROGRESS_BAR_DELAY = 2000;
    private ProgressBar loading;
    private TextView internetReturned;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        loading = findViewById(R.id.loading_spinner);
        internetReturned = findViewById(R.id.waiting_text);

        // Check internet
        if (!isInternetAvailable()) {
            // No internet: start the delay before showing loading spinner
                handler.postDelayed(() -> {
                loading.setVisibility(View.VISIBLE);
                internetReturned.setVisibility(View.VISIBLE);
                waitForInternetConnection();
            }, PROGRESS_BAR_DELAY);
        } else {
            // Internet OK: continue after splash delay
            goToWelcome();
        }
    }

    // Method to check if device has internet
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // Keep checking if internet comes back
    private void waitForInternetConnection() {
        handler.postDelayed(() -> {
            if (isInternetAvailable()) {
                internetReturned.setText("Internet returned!");
                goToWelcome();
            } else {
                waitForInternetConnection(); // Check again after delay
            }
        }, CHECK_INTERVAL);
    }

    // Move to WelcomeActivity
    private void goToWelcome() {
        handler.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
