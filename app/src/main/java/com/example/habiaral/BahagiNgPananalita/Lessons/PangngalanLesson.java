package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.habiaral.Adapters.SlideAdapter;
import com.example.habiaral.BahagiNgPananalita.Quiz.PangngalanQuiz;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PangngalanLesson extends AppCompatActivity {

    private Button unlockButton;
    private ViewPager2 viewPager;
    private TextView instructionText;
    private VideoView videoView;
    private TextToSpeech textToSpeech;
    private boolean isLessonDone = false;
    private List<String> allLines;

    private final String[] slideUrls = { "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/1.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzEucG5nIiwiaWF0IjoxNzU1MzM4OTY2LCJleHAiOjE3NTU5NDM3NjZ9.wSiMAh0i2NGdNrQDVs2JjCsobaElomVIojgBU_IlHQY", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/2.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzIucG5nIiwiaWF0IjoxNzU1MzM4OTkzLCJleHAiOjE3NTU5NDM3OTN9.hoF2z8bFL_h5Ofor_PIJBKbGcez_GVQdMXw1G3Zh2-4", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/3.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzMucG5nIiwiaWF0IjoxNzU1MzM5MDIzLCJleHAiOjE3NTU5NDM4MjN9.qcHm_N8tXbXxeo11HQMIqOWTu72UR9QtMG7draUMVT8", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/4.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzQucG5nIiwiaWF0IjoxNzU1MzM5MDU3LCJleHAiOjE3NTU5NDM4NTd9.sR8EaOUYTWe2oXLuRiiG7lRpDDXj5LTvpjr31g3fhik", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/5.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzUucG5nIiwiaWF0IjoxNzU1MzM5MDcyLCJleHAiOjE3NTU5NDM4NzJ9.glzg5VnN868MdqDoZtVEAFbuEezrMDcZv62USyo6220", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/6.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzYucG5nIiwiaWF0IjoxNzU1MzM5MTMzLCJleHAiOjE3NTU5NDM5MzN9.CRKi5O7YSjRCgEyAbWPnUzHwDJUAXWLnnAKCU8z6B14", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/7.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzcucG5nIiwiaWF0IjoxNzU1MzM5MTQzLCJleHAiOjE3NTU5NDM5NDN9.clrnQWIah5zv39_PEX0vHCKAwGgOC0VWRKBJuhIUQDA", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/8.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzgucG5nIiwiaWF0IjoxNzU1MzM5MTU5LCJleHAiOjE3NTU5NDM5NTl9.QJw5yyTpbnIR18M0TDhzgWDOvE7pcYyzm1qq3vkslFc", "https://ubxiwtxuswedwfdcqfja.supabase.co/storage/v1/object/sign/pangngalan/9.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV8yYjRiYWMwNC1mNjQwLTQ5OTEtODgzNC0zZDhlYzFlNzFmNjMiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJwYW5nbmdhbGFuLzkucG5nIiwiaWF0IjoxNzU1MzM5MTcwLCJleHAiOjE3NTU5NDM5NzB9.qXE_v8cFO6JKqq7qGe5SuFLCeoJKyHn2064ZRnQTrsU" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bahagi_ng_pananalita_pangngalan_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangngalan);
        viewPager = findViewById(R.id.viewPagerSlides);
        instructionText = findViewById(R.id.instructionText);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        checkLessonStatus();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.1f);
                loadCharacterLines(); // start lines after TTS ready
            }
        });

        unlockButton.setOnClickListener(view -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            saveProgressToFirestore();
            startActivity(new Intent(PangngalanLesson.this, PangngalanQuiz.class));
        });
    }

    // ======================
    // LOAD CHARACTER LINES
    // ======================
    private void loadCharacterLines() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("lesson_character_lines").document("LCL1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> lines = (List<String>) documentSnapshot.get("line");
                        if (lines != null && !lines.isEmpty()) {
                            allLines = lines;
                            List<String> introLines = lines.size() >= 3 ? lines.subList(0, 3) : lines;
                            displayIntroLines(introLines);
                        }
                    }
                });
    }

    private void displayIntroLines(List<String> introLines) {
        Handler handler = new Handler();
        final int[] index = {0};
        instructionText.setText("");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < introLines.size()) {
                    String line = introLines.get(index[0]);
                    instructionText.setText(line);
                    speak(line);
                    index[0]++;
                    handler.postDelayed(this, 4500);
                } else {
                    instructionText.setText("");
                    setupSlides();
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    // ======================
    // VIEWPAGER / SLIDES
    // ======================
    private void setupSlides() {
        SlideAdapter adapter = new SlideAdapter(this, slideUrls);
        viewPager.setAdapter(adapter);

        // TAP LEFT/RIGHT
        viewPager.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                int width = v.getWidth();
                int currentItem = viewPager.getCurrentItem();

                if (x < width / 2f && currentItem > 0) {
                    viewPager.setCurrentItem(currentItem - 1, true);
                } else if (x >= width / 2f && currentItem < slideUrls.length - 1) {
                    viewPager.setCurrentItem(currentItem + 1, true);
                }
                return true; // consume tap
            }
            return false; // allow swipe
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                unlockButton.setEnabled(position == slideUrls.length - 1);
                unlockButton.setAlpha(position == slideUrls.length - 1 ? 1f : 0.5f);
            }
        });

        // Speak remaining lines after slides
        if (allLines != null && allLines.size() > 3) {
            List<String> afterSlideLines = allLines.subList(3, allLines.size());
            displayLinesAfterSlides(afterSlideLines);
        }
    }

    private void displayLinesAfterSlides(List<String> lines) {
        Handler handler = new Handler();
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < lines.size()) {
                    String line = lines.get(index[0]);
                    instructionText.setText(line);
                    speak(line);
                    index[0]++;
                    handler.postDelayed(this, 4500);
                } else {
                    instructionText.setText("");
                    if (videoView != null) videoView.start();
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private void speak(String text) {
        if (textToSpeech != null && text != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onPause() {
        if (textToSpeech != null) textToSpeech.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // ======================
    // FIRESTORE LESSON STATUS
    // ======================
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
                            if (lessons != null && lessons.containsKey("pangngalan")) {
                                Map<String, Object> pangngalan = (Map<String, Object>) lessons.get("pangngalan");
                                if (pangngalan != null && "completed".equals(pangngalan.get("status"))) {
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

        Map<String, Object> lessonStatus = new HashMap<>();
        lessonStatus.put("status", "completed");

        Map<String, Object> lessonsMap = new HashMap<>();
        lessonsMap.put("pangngalan", lessonStatus);

        Map<String, Object> moduleMap = new HashMap<>();
        moduleMap.put("modulename", "Bahagi ng Pananalita");
        moduleMap.put("status", "in_progress");
        moduleMap.put("current_lesson", "pangngalan");
        moduleMap.put("lessons", lessonsMap);

        db.collection("module_progress").document(uid)
                .set(Map.of("module_1", moduleMap), SetOptions.merge());
    }
}
