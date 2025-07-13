package com.example.habiaral.Fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProgressBarFragment extends Fragment {

    private ProgressBar progressBarBahagi, progressBarKomprehensyon, progressBarKayarian;
    private TextView progressPercentageBahagi, progressPercentageKomprehensyon, progressPercentageKayarian;
    private SharedPreferences sharedPreferences;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBarBahagi = view.findViewById(R.id.progressBarBahagi);
        progressPercentageBahagi = view.findViewById(R.id.progressPercentageBahagi);

        progressBarKomprehensyon = view.findViewById(R.id.progressBarKomprehensyon);
        progressPercentageKomprehensyon = view.findViewById(R.id.progressPercentageKomprehensyon);

        progressBarKayarian = view.findViewById(R.id.progressBarKayarian);
        progressPercentageKayarian = view.findViewById(R.id.progressPercentageKayarian);

        sharedPreferences = getActivity().getSharedPreferences("LessonProgress", getContext().MODE_PRIVATE);

        updateProgressFromFirestore();
    }

    private void updateProgressFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                    if (module1 == null) return;

                    Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                    if (lessons == null) return;

                    int bahagiCompleted = 0;
                    String[] bahagiKeys = {
                            "pangngalan", "pandiwa", "panguri", "panghalip", "pangabay",
                            "pangatnig", "pangukol", "pangakop", "padamdam", "pangawing"
                    };

                    for (String key : bahagiKeys) {
                        Map<String, Object> lessonData = (Map<String, Object>) lessons.get(key);
                        if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                            bahagiCompleted++;
                        }
                    }

                    int bahagiProgress = (bahagiCompleted * 100) / bahagiKeys.length;
                    progressBarBahagi.setProgress(bahagiProgress);
                    progressPercentageBahagi.setText(bahagiProgress + "%");

                    // Update other progress bars from SharedPreferences
                    updateKomprehensyonFromSharedPref();
                    updateKayarianFromSharedPref();
                });
    }

    private void updateKomprehensyonFromSharedPref() {
        int completed = 0;
        if (sharedPreferences.getBoolean("Kwento1Done", false)) completed++;
        if (sharedPreferences.getBoolean("Kwento2Done", false)) completed++;
        if (sharedPreferences.getBoolean("Kwento3Done", false)) completed++;

        int total = 3;
        int progress = (completed * 100) / total;
        progressBarKomprehensyon.setProgress(progress);
        progressPercentageKomprehensyon.setText(progress + "%");
    }

    private void updateKayarianFromSharedPref() {
        int completed = 0;
        if (sharedPreferences.getBoolean("PayakDone", false)) completed++;
        if (sharedPreferences.getBoolean("TambalanDone", false)) completed++;
        if (sharedPreferences.getBoolean("HugnayanDone", false)) completed++;
        if (sharedPreferences.getBoolean("LangkapanDone", false)) completed++;

        int total = 4;
        int progress = (completed * 100) / total;
        progressBarKayarian.setProgress(progress);
        progressPercentageKayarian.setText(progress + "%");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress_bar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProgressFromFirestore();
    }
}
