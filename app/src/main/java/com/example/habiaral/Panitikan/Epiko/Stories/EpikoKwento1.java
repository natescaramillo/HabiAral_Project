package com.example.habiaral.Panitikan.Epiko.Stories;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.Epiko.Quiz.EpikoKwento1Quiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundManagerUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EpikoKwento1 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.sulayman_01, R.drawable.sulayman_02, R.drawable.sulayman_03,
            R.drawable.sulayman_04, R.drawable.sulayman_05, R.drawable.sulayman_06,
            R.drawable.sulayman_07, R.drawable.sulayman_08, R.drawable.sulayman_09,
            R.drawable.sulayman_10, R.drawable.sulayman_11, R.drawable.sulayman_12,
            R.drawable.sulayman_13, R.drawable.sulayman_14, R.drawable.sulayman_15,
            R.drawable.sulayman_16, R.drawable.sulayman_17, R.drawable.sulayman_18,
            R.drawable.sulayman_19, R.drawable.sulayman_20, R.drawable.sulayman_21,
            R.drawable.sulayman_22, R.drawable.sulayman_23
    };

    private static final String STORY_ID = "EpikoKwento1";
    private static final String STORY_TITLE = "Indarapatra at Sulayman";

    private boolean isLessonDone = false;
    private boolean introFinished = false; // ✅ kailangan matapos intro bago makapag-next sa cover
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    private FirebaseFirestore db;
    private String uid;

    // ✅ TTS
    private TextToSpeech textToSpeech;
    private List<String> introLines;
    private int currentIntroIndex = 0;

    private List<String> pageLines;
    private int currentLineIndex = 0;
    private boolean introPlayed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_alamat_kwento1);

        storyImage = findViewById(R.id.imageViewComic);
        unlockButton = findViewById(R.id.UnlockButtonKwento1);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        storyImage.setImageResource(comicPages[currentPage]);

        // ✅ setup TTS
        initTTS();

        // ✅ click page
        storyImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float width = storyImage.getWidth();
                if (x < width / 2) previousPage();
                else nextPage();
            }
            return true;
        });

        unlockButton.setOnClickListener(v -> {
            if (isLessonDone) {
                startActivity(new Intent(EpikoKwento1.this, EpikoKwento1Quiz.class));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(EpikoKwento1.this, Epiko.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        loadCurrentProgress();
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this,
                                "Hindi ma-open ang installer ng TTS.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // piliin FIL voice kung available
                    Voice selected = null;
                    for (Voice v : textToSpeech.getVoices()) {
                        Locale vLocale = v.getLocale();
                        if (vLocale != null && vLocale.getLanguage().equals("fil")) {
                            selected = v;
                            break;
                        } else if (v.getName().toLowerCase().contains("fil")) {
                            selected = v;
                            break;
                        }
                    }
                    if (selected != null) {
                        textToSpeech.setVoice(selected);
                    }

                    textToSpeech.setSpeechRate(1.0f);
                    SoundManagerUtils.setTts(textToSpeech);

                    // ✅ load intro mula Firestore (LCL17)
                    loadIntroLines();
                }
            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadIntroLines() {
        if (introFinished) return;

        db.collection("lesson_character_lines").document("LCL17").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        introLines = (List<String>) snapshot.get("intro");
                        if (introLines != null && !introLines.isEmpty()) {
                            currentIntroIndex = 0;
                            speakIntro();
                        }
                    }
                });
    }

    private void speakIntro() {
        if (introLines != null && currentIntroIndex < introLines.size()) {
            String line = introLines.get(currentIntroIndex);
            textToSpeech.speak(line, TextToSpeech.QUEUE_FLUSH, null, "INTRO_" + currentIntroIndex);

            textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {}
                @Override public void onError(String utteranceId) {}
                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        currentIntroIndex++;
                        if (currentIntroIndex < introLines.size()) {
                            speakIntro();
                        } else {
                            introFinished = true; // ✅ tapos na intro
                        }
                    });
                }
            });
        } else {
            introFinished = true;
        }
    }

    private void nextPage() {
        if (currentPage == 0 && !introFinished) {
            Toast.makeText(this, "Hintayin munang matapos ang intro.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPage < comicPages.length - 1) {
            currentPage++;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);
            if (currentPage == 1) {
                // ✅ Page 1 = intro (play only once)
                if (!introPlayed) {
                    loadPageLines(currentPage);
                    introPlayed = true;
                }
            } else {
                // ✅ Pages 2+ = laging load narration
                loadPageLines(currentPage);
            }
            if (currentPage == comicPages.length - 1) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
            }
        }
    }

    private void loadPageLines(int page) {
        if (pageLines != null) {
            textToSpeech.stop(); // ✅ ihinto ang kasalukuyang speech
        }

        db.collection("lesson_character_lines").document("LCL17").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<Map<String, Object>> pages = (List<Map<String, Object>>) snapshot.get("pages");
                        if (pages != null) {
                            for (Map<String, Object> p : pages) {
                                Object pageNumObj = p.get("page");
                                if (pageNumObj instanceof Number) {
                                    int firestorePage = ((Number) pageNumObj).intValue();
                                    // ✅ Firestore page ay 1-based, comicPages array ay 0-based
                                    if (firestorePage - 1 == page) {
                                        List<String> lines = (List<String>) p.get("line");
                                        if (lines != null && !lines.isEmpty()) {
                                            pageLines = lines;
                                            currentLineIndex = 0;
                                            speakPageLine();
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
    }


    private void speakPageLine() {
        if (pageLines == null || currentLineIndex >= pageLines.size()) return;

        String line = pageLines.get(currentLineIndex);

        textToSpeech.speak(line, TextToSpeech.QUEUE_FLUSH, null, "PAGE_" + currentPage + "_" + currentLineIndex);

        textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onError(String utteranceId) {}
            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    currentLineIndex++;
                    if (currentLineIndex < pageLines.size()) {
                        speakPageLine();
                    }
                });
            }
        });
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);
            if (currentPage == 1) {
                // ✅ wag ulitin intro
                if (!introPlayed) {
                    loadPageLines(currentPage);
                    introPlayed = true;
                }
            } else if (currentPage >= 2) {
                loadPageLines(currentPage);
            }        }
    }

    private void loadCurrentProgress() {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                    if (module3 == null) return;

                    Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                    if (categories == null) return;

                    Map<String, Object> epiko = (Map<String, Object>) categories.get("Epiko");
                    if (epiko == null) return;

                    Map<String, Object> stories = (Map<String, Object>) epiko.get("stories");
                    if (stories == null) return;

                    Map<String, Object> story = (Map<String, Object>) stories.get(STORY_ID);
                    if (story == null) return;

                    Object checkpointObj = story.get("checkpoint");
                    Object statusObj = story.get("status");

                    if (checkpointObj instanceof Number) {
                        currentPage = ((Number) checkpointObj).intValue();
                    }
                    storyImage.setImageResource(comicPages[currentPage]);

                    if (currentPage > 0 || "completed".equals(statusObj)) {
                        introFinished = true;
                    }

                    if ("completed".equals(statusObj)) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);
                    }
                });
    }

    private void updateCheckpoint(int checkpoint) {
        if (uid == null) return;

        db.collection("module_progress").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    String currentStatus = "in_progress";
                    if (snapshot.exists()) {
                        Map<String, Object> module3 = (Map<String, Object>) snapshot.get("module_3");
                        if (module3 != null) {
                            Map<String, Object> categories = (Map<String, Object>) module3.get("categories");
                            if (categories != null) {
                                Map<String, Object> epiko = (Map<String, Object>) categories.get("Epiko");
                                if (epiko != null) {
                                    Map<String, Object> stories = (Map<String, Object>) epiko.get("stories");
                                    if (stories != null) {
                                        Map<String, Object> story = (Map<String, Object>) stories.get(STORY_ID);
                                        if (story != null && story.get("status") != null) {
                                            currentStatus = story.get("status").toString();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Map<String, Object> storyData = new HashMap<>();
                    storyData.put("checkpoint", checkpoint);
                    storyData.put("title", STORY_TITLE);
                    storyData.put("status", currentStatus);

                    Map<String, Object> epikoData = new HashMap<>();
                    epikoData.put("stories", Map.of(STORY_ID, storyData));

                    Map<String, Object> categories = new HashMap<>();
                    categories.put("Epiko", epikoData);

                    Map<String, Object> module3 = new HashMap<>();
                    module3.put("categories", categories);

                    db.collection("module_progress")
                            .document(uid)
                            .set(Map.of("module_3", module3), SetOptions.merge());

                    if (checkpoint == comicPages.length - 1) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);


                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
