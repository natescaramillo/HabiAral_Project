package com.example.habiaral.KayarianNgPangungusap;

import java.util.Map;

public class KayarianNgPangungusapProgress {
    private String modulename;
    private String status; // "in_progress", "completed", etc.
    private String current_lesson;
    private Map<String, Object> lessons;

    // Required empty constructor for Firestore
    public KayarianNgPangungusapProgress() {}

    public KayarianNgPangungusapProgress(String modulename, String status, String current_lesson, Map<String, Object> lessons) {
        this.modulename = modulename;
        this.status = status;
        this.current_lesson = current_lesson;
        this.lessons = lessons;
    }

    // Getters
    public String getModulename() {
        return modulename;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrent_lesson() {
        return current_lesson;
    }

    public Map<String, Object> getLessons() {
        return lessons;
    }

    // Setters
    public void setModulename(String modulename) {
        this.modulename = modulename;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCurrent_lesson(String current_lesson) {
        this.current_lesson = current_lesson;
    }

    public void setLessons(Map<String, Object> lessons) {
        this.lessons = lessons;
    }
}
