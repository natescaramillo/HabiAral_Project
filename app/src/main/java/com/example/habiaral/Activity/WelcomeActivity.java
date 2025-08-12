package com.example.habiaral.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.LessonProgressCache; // ✅ import cache
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if it's first launch
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean firstLaunch = prefs.getBoolean("firstLaunch", true);

        if (firstLaunch) {
            FirebaseAuth.getInstance().signOut();
            prefs.edit().putBoolean("firstLaunch", false).apply();
        } else if (mAuth.getCurrentUser() != null) {
            GoogleSignInAccount lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (lastSignedInAccount != null) {
                saveUserToFirestore(lastSignedInAccount);
            } else {
                mGoogleSignInClient.signOut();
            }
            return;
        }

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Bind UI
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGoogleLogin.setOnClickListener(view -> {
            btnGoogleLogin.setEnabled(false);
            signInWithGoogle();
        });
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
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("GoogleSignIn", "Sign-in failed", e);
                btnGoogleLogin.setEnabled(true);
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
                        Log.e("FirebaseAuth", "Authentication failed", task.getException());
                    }
                });
    }

    private void saveUserToFirestore(GoogleSignInAccount account) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = account.getEmail();
        DocumentReference docRef = db.collection("students").document(uid);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String nickname = documentSnapshot.getString("nickname");

                if (nickname != null && !nickname.trim().isEmpty()) {
                    Toast.makeText(this, "Maligayang Pagbalik, " + nickname + "!", Toast.LENGTH_SHORT).show();
                    preloadLessonProgressAndGoHome(uid); // ✅ preload before going home
                } else {
                    goToIntroduction();
                }

            } else {
                db.collection("students").get().addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    String generatedId = String.format("STUDENT%03d", count + 1);

                    Map<String, Object> student = new HashMap<>();
                    student.put("email", email);
                    student.put("studentId", generatedId);

                    docRef.set(student)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Matagumpay ang pag-log in", Toast.LENGTH_SHORT).show();
                                goToIntroduction();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                Log.e("Firestore", "Save error", e);
                            });
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to count students", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Counting error", e);
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error checking user data", Toast.LENGTH_SHORT).show();
            Log.e("Firestore", "Check error", e);
        });
    }

    // ✅ This fetches all lesson progress and caches it
    private void preloadLessonProgressAndGoHome(String uid) {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData()); // save in memory
            }
            goToHomePage();
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to load lesson progress early", e);
            goToHomePage(); // still go home even if failed
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
