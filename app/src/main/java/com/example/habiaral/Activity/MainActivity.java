package com.example.habiaral.Activity;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.LoadingDialog;
import com.example.habiaral.NetworkUtil;
import com.example.habiaral.R;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private static final int INTERNET_CHECK_INTERVAL = 750;
    private ConnectivityManager.NetworkCallback networkCallback;
    private LoadingDialog loadingDialog;
    private Handler handler = new Handler();
    private Runnable internetCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        loadingDialog = new LoadingDialog(this);

        networkCallback = NetworkUtil.registerNetworkListener(this, new NetworkUtil.NetworkListener() {
            @Override
            public void onNetworkConnected() {
                startInternetChecking();
            }

            @Override
            public void onNetworkDisconnected() {
                stopInternetChecking();
                loadingDialog.show();
            }
        });

        if (!NetworkUtil.isNetworkAvailable(this)) {
            loadingDialog.show();
        } else {
            startInternetChecking();
        }
    }

    private void startInternetChecking() {
        if (internetCheckRunnable != null) return;

        internetCheckRunnable = new Runnable() {
            @Override
            public void run() {
                NetworkUtil.hasInternetAccess(hasInternet -> {
                    if (hasInternet) {
                        loadingDialog.dismiss();
                        goToWelcome();
                    } else {
                        loadingDialog.show();
                    }
                });
                handler.postDelayed(this, INTERNET_CHECK_INTERVAL);
            }
        };
        handler.post(internetCheckRunnable);
    }

    private void stopInternetChecking() {
        if (internetCheckRunnable != null) {
            handler.removeCallbacks(internetCheckRunnable);
            internetCheckRunnable = null;
        }
    }

    private void goToWelcome() {
        stopInternetChecking();
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkUtil.unregisterNetworkListener(this, networkCallback);
        stopInternetChecking();
        loadingDialog.dismiss();
    }
}
