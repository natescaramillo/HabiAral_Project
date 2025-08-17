package com.example.habiaral.Activity;

import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGoogleLogin.setOnClickListener(v -> {
            btnGoogleLogin.setEnabled(false);

            // Force sign out so account picker appears
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(revokeTask -> {
                    signInWithGoogle();
                });
            });
        });

        if (mAuth.getCurrentUser() != null) {
            // Already logged in → greet user
            String uid = mAuth.getCurrentUser().getUid();
            checkAndGreetUser(uid);
        } else {
            btnGoogleLogin.setEnabled(true);
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

                if (account == null || account.getIdToken() == null) {
                    btnGoogleLogin.setEnabled(true);
                    Toast.makeText(this, "Google sign-in failed: ID token is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, authTask -> {
                            btnGoogleLogin.setEnabled(true);
                            if (authTask.isSuccessful()) {
                                saveUserToFirestore(account);
                            } else {
                                Toast.makeText(this, "Firebase authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (ApiException e) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserToFirestore(GoogleSignInAccount account) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = account.getEmail();

        DocumentReference docRef = db.collection("students").document(uid);
        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String nickname = snapshot.getString("nickname");
                if (nickname != null && !nickname.isEmpty()) {
                    Toast.makeText(this, "Maligayang Pagbalik, " + nickname + "!", Toast.LENGTH_SHORT).show();
                    preloadLessonProgressAndGoHome(uid);
                } else {
                    goToIntroduction();
                }
            } else {
                // Fetch all students to find the last number
                db.collection("students").get().addOnSuccessListener(allDocs -> {
                    int maxNumber = 0;
                    for (DocumentSnapshot document : allDocs) {
                        String sid = document.getString("studentId");
                        if (sid != null && sid.startsWith("STUDENT")) {
                            try {
                                int num = Integer.parseInt(sid.replace("STUDENT", ""));
                                if (num > maxNumber) maxNumber = num;
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    String generatedId = String.format("STUDENT%03d", maxNumber + 1); // STUDENT001, 002, 003…

                    Map<String, Object> student = new HashMap<>();
                    student.put("email", email);
                    student.put("studentId", generatedId);

                    docRef.set(student)
                            .addOnSuccessListener(unused -> goToIntroduction())
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show();
                            });
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to generate student ID", Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error checking user", Toast.LENGTH_SHORT).show();
        });
    }



    private void checkAndGreetUser(String uid) {
        db.collection("students").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String nickname = snapshot.getString("nickname");
                        if (nickname != null && !nickname.isEmpty()) {
                            Toast.makeText(this, "Maligayang Pagbalik, " + nickname + "!", Toast.LENGTH_SHORT).show();
                            preloadLessonProgressAndGoHome(uid);
                        } else {
                            goToIntroduction();
                        }
                    } else {
                        // Account exists in FirebaseAuth but not in Firestore → sign out
                        mAuth.signOut();
                        btnGoogleLogin.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    btnGoogleLogin.setEnabled(true);
                });
    }

    private void preloadLessonProgressAndGoHome(String uid) {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData());
            }
            goToHomePage();
        }).addOnFailureListener(e -> {
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
