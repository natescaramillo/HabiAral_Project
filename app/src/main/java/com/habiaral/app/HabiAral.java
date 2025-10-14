package com.habiaral.app;

import android.app.Application;
import com.habiaral.app.Utils.AppNetworkMonitor;

public class HabiAral extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppNetworkMonitor.register(this);
    }
}
