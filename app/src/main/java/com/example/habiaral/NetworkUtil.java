package com.example.habiaral;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class NetworkUtil {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
                return caps != null &&
                        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }
        }
        return false;
    }

    public interface InternetCheckCallback {
        void onResult(boolean hasInternet);
    }

    public static void hasInternetAccess(InternetCheckCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean internetAvailable;
            try {
                URL url = new URL("https://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(2000);
                urlc.connect();
                int code = urlc.getResponseCode();
                internetAvailable = (code == 200);
            } catch (IOException e) {
                internetAvailable = false;
            }

            final boolean result = internetAvailable;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        });
    }

    public interface NetworkListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
    }

    public static ConnectivityManager.NetworkCallback registerNetworkListener(Context context, NetworkListener listener) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                new Handler(Looper.getMainLooper()).post(listener::onNetworkConnected);
            }

            @Override
            public void onLost(Network network) {
                new Handler(Looper.getMainLooper()).post(listener::onNetworkDisconnected);
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        return networkCallback;
    }

    public static void unregisterNetworkListener(Context context, ConnectivityManager.NetworkCallback callback) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null && callback != null) {
            connectivityManager.unregisterNetworkCallback(callback);
        }
    }
}
