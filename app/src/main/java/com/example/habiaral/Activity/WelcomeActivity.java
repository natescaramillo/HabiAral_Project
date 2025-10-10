package com.example.habiaral.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.habiaral.Cache.LessonProgressCache;
import com.example.habiaral.R;
import com.example.habiaral.Utils.InternetCheckerUtils;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_PRIVACY_ACCEPTED = "privacy_accepted";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;
    private ConstraintLayout btnGoogleLogin;
    private Handler handler = new Handler();
    private Runnable internetCheckRunnable;
    private boolean activityInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        boolean hasAccepted = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_PRIVACY_ACCEPTED, false);

        if (hasAccepted) {
            startInternetChecking();
        } else {
            showPrivacyPolicyDialog();
        }
    }

    private void showPrivacyPolicyDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_policy, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button btnAgree = dialogView.findViewById(R.id.sang_ayon);
        Button btnDecline = dialogView.findViewById(R.id.hindi_muna);

        btnAgree.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_PRIVACY_ACCEPTED, true)
                    .apply();

            dialog.dismiss();
            startInternetChecking();
        });

        btnDecline.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Kailangan mong sumang-ayon sa Patakaran upang magpatuloy.", Toast.LENGTH_LONG).show();
            finish();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        TextView policyText = dialogView.findViewById(R.id.patakarantext);
        policyText.setText(Html.fromHtml(getString(R.string.policy), Html.FROM_HTML_MODE_LEGACY));

    }

    private void startInternetChecking() {
        internetCheckRunnable = new Runnable() {
            @Override
            public void run() {
                InternetCheckerUtils.checkInternet(WelcomeActivity.this, () -> {
                    if (!activityInitialized) {
                        RunActivity();
                        activityInitialized = true;
                    }
                });
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(internetCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(internetCheckRunnable);
        InternetCheckerUtils.resetDialogFlag();
    }

    private void RunActivity() {
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

        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (lastAccount != null && mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            checkAndGreetUser(uid);
        } else {
            mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    GoogleSignInAccount account = task.getResult();
                    if (account != null && account.getIdToken() != null) {
                        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                        mAuth.signInWithCredential(credential)
                                .addOnCompleteListener(authTask -> {
                                    if (authTask.isSuccessful()) {
                                        saveUserToFirestore(account);
                                    } else {
                                        btnGoogleLogin.setEnabled(true);
                                    }
                                });
                    } else {
                        btnGoogleLogin.setEnabled(true);
                    }
                } else {
                    btnGoogleLogin.setEnabled(true);
                }
            });
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
                    Toast.makeText(this, "Nabigo ang pag-sign in sa Google: Walang laman ang ID token.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, authTask -> {
                            btnGoogleLogin.setEnabled(true);
                            if (authTask.isSuccessful()) {
                                saveUserToFirestore(account);
                            } else {
                                Toast.makeText(this, "Nabigo ang pag-authenticate sa Firebase", Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (ApiException e) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(this, "Nabigo ang pag-sign in sa Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                db.collection("students").orderBy("studentId")
                        .get().addOnSuccessListener(allDocs -> {
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

                            String generatedId = String.format("STUDENT%03d", maxNumber + 1);
                            Map<String, Object> student = new HashMap<>();
                            student.put("email", email);
                            student.put("studentId", generatedId);

                            docRef.set(student)
                                    .addOnSuccessListener(unused -> goToIntroduction())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Nabigo ang pag-save ng gumagamit", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Nabigo ang pagbuo ng student ID", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "May error sa pagsuri ng gumagamit", Toast.LENGTH_SHORT).show());
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
                        mAuth.signOut();
                        btnGoogleLogin.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> btnGoogleLogin.setEnabled(true));
    }

    private void preloadLessonProgressAndGoHome(String uid) {
        db.collection("module_progress").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData());
            }
            goToHomePage();
        }).addOnFailureListener(e -> goToHomePage());
    }

    private void goToIntroduction() {
        Intent intent = new Intent(this, IntroductionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToHomePage() {
        startActivity(new Intent(this, HomepageActivity.class));
        finish();
    }
}