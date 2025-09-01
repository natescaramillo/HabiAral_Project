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
    private ConnectivityManager.NetworkCallback networkCallback;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        loadingDialog = new LoadingDialog(this);

        networkCallback = NetworkUtil.registerNetworkListener(this, new NetworkUtil.NetworkListener() {
            @Override
            public void onNetworkConnected() {
                NetworkUtil.hasInternetAccess(hasInternet -> {
                    if (hasInternet) {
                        loadingDialog.dismiss();
                        goToWelcome();
                    } else {
                        loadingDialog.show();
                    }
                });
            }

            @Override
            public void onNetworkDisconnected() {
                loadingDialog.show();
            }
        });

        if (!NetworkUtil.isNetworkAvailable(this)) {
            loadingDialog.show();
        } else {
            NetworkUtil.hasInternetAccess(hasInternet -> {
                if (hasInternet) {
                    goToWelcome();
                } else {
                    loadingDialog.show();
                }
            });
        }
    }

    private void goToWelcome() {
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
        loadingDialog.dismiss();
    }
}
