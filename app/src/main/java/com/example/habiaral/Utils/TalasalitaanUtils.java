package com.example.habiaral.Utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TalasalitaanUtils {

    private static List<DocumentSnapshot> cachedWords = new ArrayList<>();
    private static boolean isLoaded = false;

    public static List<DocumentSnapshot> getCachedWords() {
        return cachedWords;
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    // --- New listener interface ---
    public interface OnWordsLoadedListener {
        void onWordsLoaded(List<DocumentSnapshot> words);
        void onWordsLoadFailed(Exception e);
    }

    public static void preloadWords(Context context, OnWordsLoadedListener listener) {
        if (isLoaded) {
            if (listener != null) {
                listener.onWordsLoaded(cachedWords);
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("diksiyonaryo")
                .orderBy("wordID")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cachedWords.clear();
                    cachedWords.addAll(queryDocumentSnapshots.getDocuments());
                    isLoaded = true;

                    if (listener != null) {
                        listener.onWordsLoaded(cachedWords);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to preload words", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onWordsLoadFailed(e);
                    }
                });
    }
}
