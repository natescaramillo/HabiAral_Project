package com.habiaral.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
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

import com.habiaral.app.Activity.WelcomeActivity;
import com.habiaral.app.Activity.AboutUsActivity;
import com.habiaral.app.R;
import com.habiaral.app.Utils.MuteButtonUtils;
import com.habiaral.app.Utils.SoundClickUtils;
import com.habiaral.app.Utils.SoundManagerUtils;
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
    private MediaPlayer mediaPlayer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FrameLayout btnAboutUs = view.findViewById(R.id.about_us);
        FrameLayout btnLogout = view.findViewById(R.id.logout);
        FrameLayout btnSounds = view.findViewById(R.id.sounds);
        btnSound = view.findViewById(R.id.imageView11);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isMuted = SoundManagerUtils.isMuted(requireContext());
        updateSoundIcon();

        btnLogout.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(getContext(), R.raw.button_click);
            showLogoutConfirmationDialog();
        });
        btnAboutUs.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(getContext(), R.raw.button_click);
            startActivity(new Intent(requireActivity(), AboutUsActivity.class));
        });

        btnSounds.setOnClickListener(v -> {
            MuteButtonUtils.toggleSound(requireContext());

            isMuted = !MuteButtonUtils.isSoundEnabled(requireContext());

            updateSoundIcon();

            if (MuteButtonUtils.isSoundEnabled(requireContext())) {
                Toast.makeText(requireContext(), "Naka-on ang tunog.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Naka-off ang tunog.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateSoundIcon() {
        if (MuteButtonUtils.isSoundEnabled(requireContext())) {
            btnSound.setImageResource(R.drawable.speaker_on);
        } else {
            btnSound.setImageResource(R.drawable.speaker_off);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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
            SoundClickUtils.playClickSound(getContext(), R.raw.button_click);
            Toast.makeText(requireContext(), "Nagla-log out...", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                FirebaseAuth.getInstance().signOut();

                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    mGoogleSignInClient.revokeAccess();

                    Intent intent = new Intent(requireActivity(), WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    dialog.dismiss();
                    requireActivity().finish();
                });
            }, 2000);
        });

        btnNo.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(getContext(), R.raw.button_click);
            dialog.dismiss();
        });

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
