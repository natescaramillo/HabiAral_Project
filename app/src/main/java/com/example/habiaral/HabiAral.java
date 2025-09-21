package com.example.habiaral;

import android.app.Application;
import com.example.habiaral.Utils.AppNetworkMonitor;

public class HabiAral extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppNetworkMonitor.register(this);
    }
}
