package com.example.habiaral.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.habiaral.Activity.WelcomeActivity;

import com.example.habiaral.Activity.AboutUsActivity;
import com.example.habiaral.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ImageView btnSound;
    private boolean isMuted = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FrameLayout btnAboutUs = view.findViewById(R.id.about_us);
        FrameLayout btnChangeUsername = view.findViewById(R.id.change_username);
        FrameLayout btnLogout = view.findViewById(R.id.logout);
        FrameLayout btnSounds = view.findViewById(R.id.sounds);
        btnSound = view.findViewById(R.id.imageView11);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean("isMuted", false);

        if (isMuted) {
            btnSound.setImageResource(R.drawable.speaker_off);
        } else {
            btnSound.setImageResource(R.drawable.speaker_on);
        }

        btnAboutUs.setOnClickListener(v -> startActivity(new Intent(requireActivity(), AboutUsActivity.class)));
        btnChangeUsername.setOnClickListener(v -> showChangeNicknameDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        btnSounds.setOnClickListener(v -> muteSound());


        return view;
    }

    private void muteSound() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (isMuted) {
            btnSound.setImageResource(R.drawable.speaker_on);
            isMuted = false;
        } else {
            btnSound.setImageResource(R.drawable.speaker_off);
            isMuted = true;
        }

        editor.putBoolean("isMuted", isMuted);
        editor.apply();
    }

    private void showChangeNicknameDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_box_change_username, null);

        EditText editTextUsername = dialogView.findViewById(R.id.editTextUsername);
        Button buttonConfirm = dialogView.findViewById(R.id.buttonConfirm);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        buttonConfirm.setOnClickListener(v -> {
            String newNickname = editTextUsername.getText().toString().trim();

            if (!newNickname.isEmpty()) {
                if (auth.getCurrentUser() != null) {
                    String userId = auth.getCurrentUser().getUid();

                    Map<String, Object> update = new HashMap<>();
                    update.put("nickname", newNickname);

                    db.collection("students").document(userId)
                            .update(update)
                            .addOnSuccessListener(unused -> {
                                // ✅ Update SharedPreferences
                                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                prefs.edit().putString("nickname", newNickname).apply();

                                Toast.makeText(requireContext(), "Na-update ang palayaw!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "❌ Hindi na-update sa Firestore", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(requireContext(), "Walang user na naka-login", Toast.LENGTH_SHORT).show();
                }
            } else {
                editTextUsername.setError("Pakilagay ang palayaw");
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().getAttributes().y = -200;
            dialog.getWindow().setAttributes(dialog.getWindow().getAttributes());
        }
    }

    private void showLogoutConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_box_logout, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnYes = dialogView.findViewById(R.id.btnLogoutYes);
        Button btnNo = dialogView.findViewById(R.id.btnLogoutNo);

        btnYes.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // ✅ Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                // ✅ Sign out from Google
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id)) // Make sure this matches your Firebase project
                        .requestEmail()
                        .build();

                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    // ✅ Optional: revoke access to force account picker to show next login
                    mGoogleSignInClient.revokeAccess();

                    // ✅ Redirect to login screen
                    Intent intent = new Intent(requireActivity(), WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    dialog.dismiss();
                    requireActivity().finish();
                });
            }, 2000); // 2-second delay
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().getAttributes().y = -200;
            dialog.getWindow().setAttributes(dialog.getWindow().getAttributes());
        }
    }

}