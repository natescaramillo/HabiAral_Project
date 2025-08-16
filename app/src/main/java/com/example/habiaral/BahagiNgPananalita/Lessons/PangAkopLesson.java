package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangAkopQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class PangAkopLesson extends AppCompatActivity {

    private WebView webView;
    private Button unlockButton;
    private boolean isLessonDone = false;

    private static final String SIGNED_PPT_URL = "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/presentations/Business%20Proposal%20PPT.pptx?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwcmVzZW50YXRpb25zL0J1c2luZXNzIFByb3Bvc2FsIFBQVC5wcHR4IiwiaWF0IjoxNzU1MzI3MjAyLCJleHAiOjE3NTU5MzIwMDJ9.Pyi_W5wiXfKBvXPDtkYallVz6NzQICcwxd-wh8R-E70";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangakop_lesson);

        webView = findViewById(R.id.webViewPangakop);
        unlockButton = findViewById(R.id.UnlockButtonPangakop);

        // Initially disabled visually, but enable so user can tap
        unlockButton.setEnabled(true);
        unlockButton.setAlpha(0.5f);

        setupWebView();
        loadPPT();
        checkLessonStatus();

        unlockButton.setOnClickListener(v -> {
            if (!isLessonDone) {
                isLessonDone = true;
                saveProgressToFirestore(); // mark as in_progress
            }
            startActivity(new Intent(PangAkopLesson.this, PangAkopQuiz.class));
        });
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Disable scrolling
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);

        webView.setWebViewClient(new WebViewClient());
    }

    private void loadPPT() {
        try {
            String encoded = URLEncoder.encode(SIGNED_PPT_URL, "UTF-8");
            String officeUrl = "https://view.officeapps.live.com/op/embed.aspx?src=" + encoded;
            webView.loadUrl(officeUrl);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // Page loaded → visually indicate user can tap button
                    unlockButton.setAlpha(1f);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkLessonStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("module_progress").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> module1 = (Map<String, Object>) snapshot.get("module_1");
                        if (module1 != null && module1.containsKey("lessons")) {
                            Map<String, Object> lessons = (Map<String, Object>) module1.get("lessons");
                            if (lessons != null && lessons.containsKey("pangakop")) {
                                Map<String, Object> pangakop = (Map<String, Object>) lessons.get("pangakop");
                                if (pangakop != null && "completed".equals(pangakop.get("status"))) {
                                    isLessonDone = true;
                                    unlockButton.setEnabled(true);
                                    unlockButton.setAlpha(1f);
                                }
                            }
                        }
                    }
                });
    }

    private void saveProgressToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> pangAkopStatus = new HashMap<>();
        pangAkopStatus.put("status", "in_progress");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangakop", pangAkopStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pangakop");
        moduleMap.put("lessons", lessonsMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge())
                .addOnSuccessListener(aVoid -> System.out.println("Progress saved as in_progress ✅"))
                .addOnFailureListener(e -> e.printStackTrace());
    }
}
