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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBarBahagi = view.findViewById(R.id.progressBarBahagi);
        progressPercentageBahagi = view.findViewById(R.id.progressPercentageBahagi);

        progressBarPagUnawa = view.findViewById(R.id.progressBarPagUnawa);
        progressPercentagePagUnawa = view.findViewById(R.id.progressPercentagePagUnawa);

        progressBarKayarian = view.findViewById(R.id.progressBarKayarian);
        progressPercentageKayarian = view.findViewById(R.id.progressPercentageKayarian);

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

                    // ===== Bahagi (Module 1) =====
                    Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                    if (module1 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                        if (lessons != null) {
                            String[] bahagiKeys = {
                                    "pangngalan", "pandiwa", "panguri", "panghalip", "pangabay",
                                    "pangatnig", "pangukol", "pangakop", "padamdam", "pangawing"
                            };
                            int bahagiCompleted = 0;
                            for (String key : bahagiKeys) {
                                Map<String, Object> lessonData = (Map<String, Object>) lessons.get(key);
                                if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                                    bahagiCompleted++;
                                }
                            }
                            int bahagiProgress = (bahagiCompleted * 100) / bahagiKeys.length;
                            progressBarBahagi.setProgress(bahagiProgress);
                            progressPercentageBahagi.setText(bahagiProgress + "%");
                        }
                    }

                    // ===== Pag Unawa (Module 3) =====
                    Map<String, Object> module2 = (Map<String, Object>) snapshot.get("module_3");
                    if (module2 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module2.get("lessons");
                        if (lessons != null) {
                            String[] pagunawaKeys = {
                                    "kwento1", "kwento2", "kwento3"
                            };
                            int pagunawaCompleted = 0;
                            for (String key : pagunawaKeys) {
                                Map<String, Object> lessonData = (Map<String, Object>) lessons.get(key);
                                if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                                    pagunawaCompleted++;
                                }
                            }
                            int pagunawaProgress = (pagunawaCompleted * 100) / pagunawaKeys.length;
                            progressBarPagUnawa.setProgress(pagunawaProgress);
                            progressPercentagePagUnawa.setText(pagunawaProgress + "%");
                        }
                    }

                    // ===== Kayarian (Module 2) =====
                    Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_2");
                    if (module3 != null) {
                        Map<String, Object> lessons = (Map<String, Object>) module3.get("lessons");
                        if (lessons != null) {
                            String[] kayarianKeys = {
                                    "payak", "tambalan", "hugnayan", "langkapan"
                            };
                            int kayarianCompleted = 0;
                            for (String key : kayarianKeys) {
                                Map<String, Object> lessonData = (Map<String, Object>) lessons.get(key);
                                if (lessonData != null && "completed".equals(lessonData.get("status"))) {
                                    kayarianCompleted++;
                                }
                            }
                            int kayarianProgress = (kayarianCompleted * 100) / kayarianKeys.length;
                            progressBarKayarian.setProgress(kayarianProgress);
                            progressPercentageKayarian.setText(kayarianProgress + "%");
                        }
                    }
                });
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
