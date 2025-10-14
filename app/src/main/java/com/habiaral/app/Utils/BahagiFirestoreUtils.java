package com.habiaral.app.Utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class BahagiFirestoreUtils {

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static void saveLessonProgress(String uid, String lessonName, int checkpoint, boolean completed) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String status = completed ? "completed" : "in-progress";

        Map<String, Object> lessonStatus = Map.of(
                "status", status,
                "checkpoint", checkpoint
        );

        Map<String, Object> lessons = Map.of(lessonName, lessonStatus);

        Map<String, Object> moduleMap = Map.of(
                "modulename", "Bahagi ng Pananalita",
                "status", "in_progress",
                "current_lesson", lessonName,
                "lessons", lessons
        );

        db.collection("module_progress")
                .document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }
}