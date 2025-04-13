package com.example.habiaral;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;


public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout btnAboutUs = view.findViewById(R.id.about_us);
        FrameLayout btnExit = view.findViewById(R.id.exit_app);

        btnAboutUs.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), About_Us.class)));
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitConfirmationDialog();
            }
        });

        return view;
    }
    private void showExitConfirmationDialog() {
        // Inflate the custom dialog view
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.custom_dialog_box, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);


        AlertDialog dialog = builder.create();


        View btnYes = dialogView.findViewById(R.id.button5);
        View btnNo = dialogView.findViewById(R.id.button6);


        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // Close the dialog
            }
        });

        // Show the dialog
        dialog.show();
    }
}
