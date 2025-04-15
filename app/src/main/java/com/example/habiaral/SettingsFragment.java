package com.example.habiaral;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout btnAboutUs = view.findViewById(R.id.about_us);
        FrameLayout btnExit = view.findViewById(R.id.exit_app);

        btnAboutUs.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), About_Us.class)));

        btnExit.setOnClickListener(v -> showExitConfirmationDialog());

        return view;
    }

    private void showExitConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.custom_dialog_box, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.button5).setOnClickListener(v -> {
            requireActivity().finish();
        });

        dialogView.findViewById(R.id.button6).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
}
