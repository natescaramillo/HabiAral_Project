package com.example.habiaral.Activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
        setContentView(R.layout.main);

        loading = findViewById(R.id.loading_spinner);
        internetReturned = findViewById(R.id.waiting_text);

        loading.setVisibility(View.GONE);
        internetReturned.setVisibility(View.GONE);

        if (!isInternetAvailable()) {
            handler.postDelayed(() -> {
                loading.setVisibility(View.VISIBLE);
                internetReturned.setVisibility(View.VISIBLE);
                internetReturned.setText("Waiting for internet...");
                waitForInternetConnection();
            }, PROGRESS_BAR_DELAY);
        } else {
            goToWelcome();
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void waitForInternetConnection() {
        handler.postDelayed(() -> {
            if (isInternetAvailable()) {
                internetReturned.setText("Internet returned!");
                goToWelcome();
            } else {
                waitForInternetConnection();
            }
        }, CHECK_INTERVAL);
    }

    private void goToWelcome() {
        handler.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
