package com.example.habiaral.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

    private Button startBtn;
    private LinearLayout btnGoogleLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if it's first launch
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean firstLaunch = prefs.getBoolean("firstLaunch", true);

        if (firstLaunch) {
            FirebaseAuth.getInstance().signOut(); // Sign out any user
            prefs.edit().putBoolean("firstLaunch", false).apply(); // Mark as launched
        } else if (mAuth.getCurrentUser() != null) {
            // User already signed in, go to homepage directly
            goToHomePage();
            return;
        }

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Bind UI elements
        startBtn = findViewById(R.id.button);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // Start button (skip login)
        startBtn.setOnClickListener(view -> goToHomePage());

        // Google login
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
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                goToHomePage();
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
                                goToHomePage();
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

    private void goToHomePage() {
        startActivity(new Intent(this, HomepageActivity.class));
        finish();
    }
}
