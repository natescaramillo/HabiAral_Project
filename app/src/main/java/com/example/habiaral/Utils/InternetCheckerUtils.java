package com.example.habiaral.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.habiaral.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class InternetCheckerUtils {

    private static boolean isDialogShowing = false;

    public static void checkInternet(Context context, Runnable onConnected) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean connected = isInternetAvailable(context);
            Handler handler = new Handler(Looper.getMainLooper());

            if (connected) {
                handler.post(onConnected);
            } else {
                handler.postDelayed(() -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        boolean stillDisconnected = !isInternetAvailable(context);
                        handler.post(() -> {
                            if (stillDisconnected) {
                                showNoInternetDialog(context, onConnected);
                            } else {
                                onConnected.run();
                            }
                        });
                    });
                }, 3000);
            }
        });
    }

    public static void resetDialogFlag() {
        isDialogShowing = false;
    }

    private static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection)
                            new URL("https://www.google.com/generate_204").openConnection();
                    urlConnection.setRequestProperty("User-Agent", "Android");
                    urlConnection.setRequestProperty("Connection", "close");
                    urlConnection.setConnectTimeout(2000);
                    urlConnection.setReadTimeout(2000);
                    urlConnection.connect();
                    return urlConnection.getResponseCode() == 204;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    private static void showNoInternetDialog(Context context, Runnable onConnected) {
        if (isDialogShowing) return;

        isDialogShowing = true;
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_box_no_internet, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        ImageView image = dialogView.findViewById(R.id.imageView10);
        TextView title = dialogView.findViewById(R.id.textView9);
        TextView message = dialogView.findViewById(R.id.textView15);
        ProgressBar loadingSpinner = dialogView.findViewById(R.id.loading_spinner);
        TextView waitingText = dialogView.findViewById(R.id.waiting_text);
        Button retryButton = dialogView.findViewById(R.id.internet_retry_button);

        retryButton.setOnClickListener(v -> {
            image.setVisibility(View.GONE);
            title.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);

            loadingSpinner.setVisibility(View.VISIBLE);
            waitingText.setVisibility(View.VISIBLE);

            handler.postDelayed(() -> waitForInternetConnection(context, dialog, onConnected), 1500);
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private static void waitForInternetConnection(Context context, AlertDialog dialog, Runnable onConnected) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean connected = isInternetAvailable(context);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                dialog.dismiss();
                isDialogShowing = false;
                if (connected) {
                    onConnected.run();
                } else {
                    showNoInternetDialog(context, onConnected);
                }
            });
        });
    }
}
