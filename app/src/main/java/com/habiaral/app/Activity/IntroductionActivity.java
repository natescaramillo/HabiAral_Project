package com.habiaral.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.habiaral.app.Cache.LessonProgressCache;
import com.habiaral.app.Utils.InternetCheckerUtils;
import com.habiaral.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class IntroductionActivity extends AppCompatActivity {

    private EditText nicknameInput;
    private FirebaseFirestore db;
    private String userId;
    private boolean isSaving = false;
    private Handler handler = new Handler();
    private Runnable internetCheckRunnable;
    private boolean activityInitialized = false;

    private Button patuloy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction);

        ImageView imageView = findViewById(R.id.imageView6);
        Glide.with(this).asGif().load(R.drawable.hello).into(imageView);

        patuloy = findViewById(R.id.patuloy);

        patuloy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNickname();
            }
        });
        startInternetChecking();
    }

    private void startInternetChecking() {
        internetCheckRunnable = new Runnable() {
            @Override
            public void run() {
                InternetCheckerUtils.checkInternet(IntroductionActivity.this, () -> {
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
        nicknameInput = findViewById(R.id.nickname);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Hindi naka-login ang gumagamit.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();
        db = FirebaseFirestore.getInstance();

    }

    private void saveNickname() {
        if (isSaving) return;

        String nickname = nicknameInput.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "Paki-type ang iyong palayaw.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nickname.length() < 2) {
            Toast.makeText(this, "Ang palayaw ay dapat may 2 letra o higit pa.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSaving = true;
        nicknameInput.setEnabled(false);

        DocumentReference studentRef = db.collection("students").document(userId);

        Map<String, Object> update = new HashMap<>();
        update.put("nickname", nickname);
        update.put("updatedAt", System.currentTimeMillis());

        studentRef.set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Matagumpay na nai-save ang palayaw!", Toast.LENGTH_SHORT).show();
                    preloadLessonProgressAndGoHome();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hindi nai-save ang palayaw", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    isSaving = false;
                    nicknameInput.setEnabled(true);
                });
    }

    private void preloadLessonProgressAndGoHome() {
        db.collection("module_progress").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                LessonProgressCache.setData(snapshot.getData());
            }
            goToHome();
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            goToHome();
        });
    }

    private void goToHome() {
        Intent intent = new Intent(IntroductionActivity.this, HomepageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
