package com.example.habiaral.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppNetworkMonitor extends BroadcastReceiver {
    private static AlertDialog noInternetDialog;
    private static Activity currentActivity;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static boolean isCheckingInternet = false;
    private static Runnable periodicCheck;
    private static boolean isDialogShowing = false;
    private static boolean isAppPaused = false;
    private static View loadingSpinner;
    private static TextView waitingText;
    private static TextView textView9;
    private static TextView textView15;
    private static Button dialogRetryButton;

    public static void register(Application app) {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        app.registerReceiver(new AppNetworkMonitor(), filter);

        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle bundle) {
                dismissNoInternetDialog();
                currentActivity = activity;
            }
            @Override public void onActivityStarted(Activity activity) {
                dismissNoInternetDialog();
                currentActivity = activity;
            }
            @Override public void onActivityResumed(Activity activity) {
                dismissNoInternetDialog();
                currentActivity = activity;
            }
            @Override public void onActivityPaused(Activity activity) {

                if (currentActivity == activity) {
                    dismissNoInternetDialog();
                }
            }
            @Override public void onActivityStopped(Activity activity) {
                if (currentActivity == activity) {
                    dismissNoInternetDialog();
                }
            }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }
            @Override public void onActivityDestroyed(Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                    dismissNoInternetDialog();
                }
            }
        });

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mainHandler.post(() -> {
            boolean isConnected = isNetworkConnected(context);
            if (!isConnected) {
                showNoInternetDialog();
            } else {
                if (isDialogShowing) {
                    dismissNoInternetDialog();
                }
            }
        });
    }

    private static void startPeriodicInternetCheck() {
        return;
    }

    private static void stopPeriodicInternetCheck() {
        if (periodicCheck != null) {
            mainHandler.removeCallbacks(periodicCheck);
            periodicCheck = null;
        }
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private static void checkInternetConnection(Context context) {
        if (isCheckingInternet) return;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                mainHandler.post(() -> showNoInternetDialog());
                return;
            }
        }

        isCheckingInternet = true;
        executor.execute(() -> {
            boolean hasInternet = pingGoogle() || pingCloudflare() || pingOpenDNS();

            mainHandler.post(() -> {
                isCheckingInternet = false;
                if (hasInternet) {
                    if (isDialogShowing) {
                        dismissNoInternetDialog();
                    }
                } else {
                    showNoInternetDialog();
                }
            });
        });
    }

    private static boolean pingGoogle() {
        return pingHost("https://www.google.com", 5000);
    }

    private static boolean pingCloudflare() {
        return pingHost("https://1.1.1.1", 5000);
    }

    private static boolean pingOpenDNS() {
        return pingHost("https://208.67.222.222", 5000);
    }

    private static boolean pingHost(String urlString, int timeout) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "HabiAral-NetworkCheck");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            return false;
        }
    }

    private static void showNoInternetDialog() {
        Activity activity = currentActivity;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;


        dismissNoInternetDialog();

        isDialogShowing = true;


        pauseApp(activity);

        try {
            View dialogView = LayoutInflater.from(activity).inflate(
                    activity.getResources().getIdentifier("dialog_box_no_internet", "layout", activity.getPackageName()),
                    null
            );

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setCancelable(false);

            noInternetDialog = builder.create();
            if (noInternetDialog.getWindow() != null) {
                noInternetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Get references to all dialog elements
            loadingSpinner = dialogView.findViewById(
                    activity.getResources().getIdentifier("loading_spinner", "id", activity.getPackageName())
            );
            waitingText = dialogView.findViewById(
                    activity.getResources().getIdentifier("waiting_text", "id", activity.getPackageName())
            );
            textView9 = dialogView.findViewById(
                    activity.getResources().getIdentifier("textView9", "id", activity.getPackageName())
            );
            textView15 = dialogView.findViewById(
                    activity.getResources().getIdentifier("textView15", "id", activity.getPackageName())
            );
            dialogRetryButton = dialogView.findViewById(
                    activity.getResources().getIdentifier("internet_retry_button", "id", activity.getPackageName())
            );

            if (dialogRetryButton != null) {
                dialogRetryButton.setOnClickListener(v -> {
                    showCheckingStatus();
                    checkInternetConnectionManual(activity);
                });
            }

            if (!activity.isFinishing() && !activity.isDestroyed()) {
                noInternetDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            isDialogShowing = false;
            resumeApp(activity);
        }
    }

    private static void showCheckingStatus() {
        // Show loading elements
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        if (waitingText != null) {
            waitingText.setVisibility(View.VISIBLE);
        }

        // Hide static text elements when loading
        if (textView9 != null) {
            textView9.setVisibility(View.GONE);
        }
        if (textView15 != null) {
            textView15.setVisibility(View.GONE);
        }

        // Disable retry button
        if (dialogRetryButton != null) {
            dialogRetryButton.setEnabled(false);
            dialogRetryButton.setAlpha(0.5f);
        }
    }

    private static void hideCheckingStatus() {
        // Hide loading elements
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.GONE);
        }
        if (waitingText != null) {
            waitingText.setVisibility(View.GONE);
        }

        // Show static text elements again
        if (textView9 != null) {
            textView9.setVisibility(View.VISIBLE);
        }
        if (textView15 != null) {
            textView15.setVisibility(View.VISIBLE);
        }

        // Re-enable retry button
        if (dialogRetryButton != null) {
            dialogRetryButton.setEnabled(true);
            dialogRetryButton.setAlpha(1.0f);
        }
    }

    private static void checkInternetConnectionManual(Context context) {
        if (isCheckingInternet) return;

        isCheckingInternet = true;

        // Auto-hide checking status after 10 seconds
        mainHandler.postDelayed(() -> {
            if (isCheckingInternet) {
                isCheckingInternet = false;
                mainHandler.post(() -> {
                    hideCheckingStatus();
                });
            }
        }, 10000);

        executor.execute(() -> {
            boolean hasInternet = pingGoogle() || pingCloudflare() || pingOpenDNS();

            mainHandler.post(() -> {
                if (isCheckingInternet) {
                    isCheckingInternet = false;

                    if (hasInternet) {
                        dismissNoInternetDialog();
                    } else {
                        hideCheckingStatus();
                    }
                }
            });
        });
    }

    private static void dismissNoInternetDialog() {
        if (noInternetDialog != null) {
            try {
                if (noInternetDialog.isShowing()) {
                    noInternetDialog.dismiss();
                }
            } catch (Exception ignored) {}
            noInternetDialog = null;
        }

        // Clear all dialog references
        loadingSpinner = null;
        waitingText = null;
        textView9 = null;
        textView15 = null;
        dialogRetryButton = null;
        isDialogShowing = false;


        if (currentActivity != null) {
            resumeApp(currentActivity);
        }
    }

    private static void pauseApp(Activity activity) {
        if (isAppPaused) return;
        isAppPaused = true;

        try {

            Method onPauseMethod = Activity.class.getDeclaredMethod("onPause");
            onPauseMethod.setAccessible(true);
            onPauseMethod.invoke(activity);
        } catch (Exception e) {

            pauseAppComponents(activity);
        }
    }

    private static void resumeApp(Activity activity) {
        if (!isAppPaused) return;
        isAppPaused = false;

        try {

            Method onResumeMethod = Activity.class.getDeclaredMethod("onResume");
            onResumeMethod.setAccessible(true);
            onResumeMethod.invoke(activity);
        } catch (Exception e) {

            resumeAppComponents(activity);
        }
    }

    private static void pauseAppComponents(Activity activity) {

        try {

            Class<?> timerSoundClass = Class.forName("com.example.habiaral.Utils.TimerSoundUtils");
            Method pauseMethod = timerSoundClass.getDeclaredMethod("pause");
            pauseMethod.invoke(null);
        } catch (Exception ignored) {}

        try {

            Class<?> soundClickClass = Class.forName("com.example.habiaral.Utils.SoundClickUtils");
            Method pauseMethod = soundClickClass.getDeclaredMethod("pause");
            pauseMethod.invoke(null);
        } catch (Exception ignored) {}


        pauseActivityComponents(activity);
    }

    private static void resumeAppComponents(Activity activity) {

        try {

            Class<?> timerSoundClass = Class.forName("com.example.habiaral.Utils.TimerSoundUtils");
            Method resumeMethod = timerSoundClass.getDeclaredMethod("resume");
            resumeMethod.invoke(null);
        } catch (Exception ignored) {}

        try {

            Class<?> soundClickClass = Class.forName("com.example.habiaral.Utils.SoundClickUtils");
            Method resumeMethod = soundClickClass.getDeclaredMethod("resume");
            resumeMethod.invoke(null);
        } catch (Exception ignored) {}


        resumeActivityComponents(activity);
    }

    private static void pauseActivityComponents(Activity activity) {

        try {

            java.lang.reflect.Field[] fields = activity.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(activity);

                if (fieldValue instanceof android.os.CountDownTimer) {
                    ((android.os.CountDownTimer) fieldValue).cancel();
                } else if (fieldValue instanceof android.media.MediaPlayer) {
                    android.media.MediaPlayer mp = (android.media.MediaPlayer) fieldValue;
                    if (mp.isPlaying()) {
                        mp.pause();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void resumeActivityComponents(Activity activity) {



    }

    public static void cleanup() {
        stopPeriodicInternetCheck();
        dismissNoInternetDialog();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
