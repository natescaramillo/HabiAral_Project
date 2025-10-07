package com.example.habiaral.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProgressBarFragment extends Fragment {

    private ProgressBar progressBarBahagi, progressBarPagUnawa, progressBarKayarian;
    private TextView progressPercentageBahagi, progressPercentagePagUnawa, progressPercentageKayarian;
    private TextView bahagiDescription, pagUnawaDescription, kayarianDescription;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBarBahagi = view.findViewById(R.id.progressBarBahagi);
        progressPercentageBahagi = view.findViewById(R.id.progressPercentageBahagi);
        bahagiDescription = view.findViewById(R.id.bahagi_progress_description);

        progressBarPagUnawa = view.findViewById(R.id.progressBarPagUnawa);
        progressPercentagePagUnawa = view.findViewById(R.id.progressPercentagePagUnawa);
        pagUnawaDescription = view.findViewById(R.id.pag_unawa_progress_description);

        progressBarKayarian = view.findViewById(R.id.progressBarKayarian);
        progressPercentageKayarian = view.findViewById(R.id.progressPercentageKayarian);
        kayarianDescription = view.findViewById(R.id.kayarian_progress_description);

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
                    if (module1 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                        if (lessons != null) {
                            int totalLessons = 10;
                            int completedCount = 0;

                            for (Map.Entry<String, Object> entry : lessons.entrySet()) {
                                Map<String, Object> lessonData = (Map<String, Object>) entry.getValue();
                                if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                                    completedCount++;
                                }
                            }

                            int progress = (completedCount * 100) / totalLessons;
                            progressBarBahagi.setProgress(progress);
                            progressPercentageBahagi.setText(progress + "%");
                            bahagiDescription.setText(getDescription(progress));
                        } else {
                            progressBarBahagi.setProgress(0);
                            progressPercentageBahagi.setText("0%");
                            bahagiDescription.setText(getDescription(0));
                        }
                    }

                    Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_2");
                    if (module2 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                        if (lessons != null) {
                            int totalLessons = 4;
                            int completedCount = 0;

                            for (Map.Entry<String, Object> entry : lessons.entrySet()) {
                                Map<String, Object> lessonData = (Map<String, Object>) entry.getValue();
                                if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                                    completedCount++;
                                }
                            }

                            int progress = (completedCount * 100) / totalLessons;
                            progressBarKayarian.setProgress(progress);
                            progressPercentageKayarian.setText(progress + "%");
                            kayarianDescription.setText(getDescription(progress));
                        } else {
                            progressBarKayarian.setProgress(0);
                            progressPercentageKayarian.setText("0%");
                            kayarianDescription.setText(getDescription(0));
                        }
                    }

                    Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                    if (module3 != null) {
                        Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                        if (categories != null) {
                            int totalCategories = 5;
                            int completedCategories = 0;

                            for (Map.Entry<String, Object> categoryEntry : categories.entrySet()) {
                                Map<String, Object> categoryData = (Map<String, Object>) categoryEntry.getValue();
                                if (categoryData != null && "completed".equals(categoryData.get("status"))) {
                                    completedCategories++;
                                }
                            }

                            int progress = (completedCategories * 100) / totalCategories;
                            progressBarPagUnawa.setProgress(progress);
                            progressPercentagePagUnawa.setText(progress + "%");
                            pagUnawaDescription.setText(getDescription(progress));
                        } else {
                            progressBarPagUnawa.setProgress(0);
                            progressPercentagePagUnawa.setText("0%");
                            pagUnawaDescription.setText(getDescription(0));
                        }
                    }
                });
    }

    private String getDescription(int progress) {
        if (progress == 100) {
            return "Tapos na!";
        } else if (progress >= 70) {
            return "Malapit nang matapos...";
        } else if (progress >= 40) {
            return "Patuloy lang, kalahati na!";
        } else if (progress > 0) {
            return "Kakaumpisa pa lang.";
        } else {
            return "Wala pang progreso.";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.progress_bar_lesson, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProgressFromFirestore();
    }
}
