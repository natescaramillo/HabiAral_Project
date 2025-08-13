package com.example.habiaral.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.LessonProgressCache;
import com.example.habiaral.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    private LinearLayout btnGoogleLogin;
    private boolean isFirstLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // SharedPreferences para ma-track kung first time i-open
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        isFirstLaunch = prefs.getBoolean("firstLaunch", true);

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGoogleLogin.setOnClickListener(v -> {
            btnGoogleLogin.setEnabled(false);
            signInWithGoogle();
        });

        if (isFirstLaunch) {
            // First install → logout muna para siguradong pipili ng account
            mAuth.signOut();
            prefs.edit().putBoolean("firstLaunch", false).apply();
            btnGoogleLogin.setEnabled(true);
        } else {
            // Not first launch → kung may current user, diretso Home
            if (mAuth.getCurrentUser() != null) {
                goToHomePage();
            } else {
                btnGoogleLogin.setEnabled(true);
            }
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken(), account);
            } catch (ApiException e) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("GoogleSignIn", "Error", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnGoogleLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        saveUserToFirestore(account);
                    } else {
                        Toast.makeText(this, "Firebase authentication failed", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseAuth", "Auth error", task.getException());
                    }
                });
    }

    private void saveUserToFirestore(GoogleSignInAccount account) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = account.getEmail();

        db.collection("students")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Existing user
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String nickname = doc.getString("nickname");

                        if (nickname != null && !nickname.trim().isEmpty()) {
                            Toast.makeText(this, "Maligayang Pagbalik, " + nickname + "!", Toast.LENGTH_SHORT).show();
                            preloadLessonProgressAndGoHome(uid);
                        } else {
                            goToIntroduction();
                        }
                    } else {
                        // New user → Generate unique studentId
                        db.collection("students").get().addOnSuccessListener(allDocs -> {
                            int maxNumber = 0;
                            for (DocumentSnapshot document : allDocs) {
                                String sid = document.getString("studentId");
                                if (sid != null && sid.startsWith("STUDENT")) {
                                    try {
                                        int num = Integer.parseInt(sid.replace("STUDENT", ""));
                                        if (num > maxNumber) {
                                            maxNumber = num;
                                        }
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            String generatedId = String.format("STUDENT%03d", maxNumber + 1);

                            Map<String, Object> student = new HashMap<>();
                            student.put("email", email);
                            student.put("studentId", generatedId);

                            db.collection("students").document(uid)
                                    .set(student)
                                    .addOnSuccessListener(unused -> goToIntroduction())
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show();
                                        Log.e("Firestore", "Save error", e);
                                    });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking user", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Check error", e);
                });
    }

    private void preloadLessonProgressAndGoHome(String uid) {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData());
            }
            goToHomePage();
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to load lesson progress early", e);
            goToHomePage();
        });
    }

    private void goToIntroduction() {
        Intent intent = new Intent(this, Introduction.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToHomePage() {
        startActivity(new Intent(this, HomepageActivity.class));
        finish();
    }
}