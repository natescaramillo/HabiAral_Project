package com.example.habiaral.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.habiaral.Activity.AboutUsActivity;
import com.example.habiaral.R;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout btnAboutUs = view.findViewById(R.id.about_us);
        FrameLayout btnExit = view.findViewById(R.id.exit_app);
        FrameLayout btnChangeUsername = view.findViewById(R.id.change_username);


        btnAboutUs.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), AboutUsActivity.class)));
        btnExit.setOnClickListener(v -> showExitConfirmationDialog());
        btnChangeUsername.setOnClickListener(v -> showChangeNicknameDialog());

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
    private void showChangeNicknameDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext()); // Use requireContext() for Fragment
        View dialogView = inflater.inflate(R.layout.dialog_box_change_username, null); // Make sure XML filename is correct

        EditText editTextUsername = dialogView.findViewById(R.id.editTextUsername);
        Button buttonConfirm = dialogView.findViewById(R.id.buttonConfirm);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()) // or getActivity()
                .setView(dialogView)
                .setCancelable(true)
                .create();

        buttonConfirm.setOnClickListener(v -> {
            String newNickname = editTextUsername.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                Toast.makeText(requireContext(), "Bagong Palayaw: " + newNickname, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                editTextUsername.setError("Pakilagay ang palayaw");
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}