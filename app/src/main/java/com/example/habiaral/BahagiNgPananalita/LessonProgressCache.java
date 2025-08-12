package com.example.habiaral.BahagiNgPananalita;

import java.util.Map;

public class LessonProgressCache {
    private static Map<String, Object> cachedModuleProgress;

    public static void setData(Map<String, Object> data) {
        cachedModuleProgress = data;
    }

    public static Map<String, Object> getData() {
        return cachedModuleProgress;
    }
}