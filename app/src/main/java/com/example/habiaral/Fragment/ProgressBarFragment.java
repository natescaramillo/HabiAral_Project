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

        updateProgress();
    }

    private void updateProgress() {
        int bahagiCompleted = 0;
        if (sharedPreferences.getBoolean("PangngalanDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PandiwaDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangUriDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangHalipDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangAbayDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangatnigDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangUkolDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangAkopDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PadamdamDone", false)) bahagiCompleted++;
        if (sharedPreferences.getBoolean("PangawingDone", false)) bahagiCompleted++;

        int bahagiTotal = 10;
        int bahagiProgress = (bahagiCompleted * 100) / bahagiTotal;
        progressBarBahagi.setProgress(bahagiProgress);
        progressPercentageBahagi.setText(bahagiProgress + "%");

        int komprehensyonCompleted = 0;
        if (sharedPreferences.getBoolean("Kwento1Done", false)) komprehensyonCompleted++;
        if (sharedPreferences.getBoolean("Kwento2Done", false)) komprehensyonCompleted++;
        if (sharedPreferences.getBoolean("Kwento3Done", false)) komprehensyonCompleted++;

        int komprehensyonTotal = 3;
        int komprehensyonProgress = (komprehensyonCompleted * 100) / komprehensyonTotal;
        progressBarKomprehensyon.setProgress(komprehensyonProgress);
        progressPercentageKomprehensyon.setText(komprehensyonProgress + "%");

        int kayarianCompleted = 0;
        if (sharedPreferences.getBoolean("PayakDone", false)) kayarianCompleted++;
        if (sharedPreferences.getBoolean("TambalanDone", false)) kayarianCompleted++;
        if (sharedPreferences.getBoolean("HugnayanDone", false)) kayarianCompleted++;
        if (sharedPreferences.getBoolean("LangkapanDone", false)) kayarianCompleted++;

        int kayarianTotal = 4;
        int kayarianProgress = (kayarianCompleted * 100) / kayarianTotal;
        progressBarKayarian.setProgress(kayarianProgress);
        progressPercentageKayarian.setText(kayarianProgress + "%");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress_bar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProgress();
    }
}
