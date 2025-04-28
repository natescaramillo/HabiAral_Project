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

public class ProgressBarFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView progressPercentageTextView;
    private SharedPreferences sharedPreferences;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        progressPercentageTextView = view.findViewById(R.id.progressPercentage);
        sharedPreferences = getActivity().getSharedPreferences("LessonProgress", getContext().MODE_PRIVATE);

        updateProgress();
    }

    private void updateProgress() {
        boolean pangngalanDone = sharedPreferences.getBoolean("PangngalanDone", false);
        boolean pandiwaDone = sharedPreferences.getBoolean("PandiwaDone", false);
        boolean pangUriDone = sharedPreferences.getBoolean("PangUriDone", false);
        boolean pangHalipDone = sharedPreferences.getBoolean("PangHalipDone", false);
        boolean pangAbayDone = sharedPreferences.getBoolean("PangAbayDone", false);
        boolean pangatnigDone = sharedPreferences.getBoolean("PangatnigDone", false);
        boolean pangUkolDone = sharedPreferences.getBoolean("PangUkolDone", false);
        boolean pangAkopDone = sharedPreferences.getBoolean("PangAkopDone", false);
        boolean padamdamDone = sharedPreferences.getBoolean("PadamdamDone", false);
        boolean pangawingDone = sharedPreferences.getBoolean("PangawingDone", false);

        int completedLessons = 0;
        if (pangngalanDone) completedLessons++;
        if (pandiwaDone) completedLessons++;
        if (pangUriDone) completedLessons++;
        if (pangHalipDone) completedLessons++;
        if (pangAbayDone) completedLessons++;
        if (pangatnigDone) completedLessons++;
        if (pangUkolDone) completedLessons++;
        if (pangAkopDone) completedLessons++;
        if (padamdamDone) completedLessons++;
        if (pangawingDone) completedLessons++;

        int totalLessons = 10;
        int progressPercentage = (completedLessons * 100) / totalLessons;

        progressBar.setProgress(progressPercentage);

        progressPercentageTextView.setText(progressPercentage + "%");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress_bar, container, false);
    }

    public void onNextLessonButtonPressed(String lessonKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(lessonKey, true);
        editor.apply();

        updateProgress();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProgress();
    }
}
