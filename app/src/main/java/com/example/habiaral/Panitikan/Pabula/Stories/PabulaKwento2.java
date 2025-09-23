package com.example.habiaral.Panitikan.Pabula.Stories;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
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

import com.example.habiaral.Panitikan.Pabula.Pabula;
import com.example.habiaral.Panitikan.Pabula.Quiz.PabulaKwento2Quiz;
import com.example.habiaral.R;
import com.example.habiaral.Utils.ResumeDialogUtils;
import com.example.habiaral.Utils.SoundManagerUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PabulaKwento2 extends AppCompatActivity {

    private final int[] comicPages = {
            R.drawable.cover_page_rosas, R.drawable.kwento1_page01, R.drawable.kwento1_page02,
            R.drawable.kwento1_page03, R.drawable.kwento1_page04, R.drawable.kwento1_page05,
            R.drawable.kwento1_page06, R.drawable.kwento1_page07, R.drawable.kwento1_page08
            , R.drawable.kwento1_page09, R.drawable.kwento1_page10, R.drawable.kwento1_page11
            , R.drawable.kwento1_page12, R.drawable.kwento1_page13, R.drawable.kwento1_page14
            , R.drawable.kwento1_page15, R.drawable.kwento1_page16, R.drawable.kwento1_page17
            , R.drawable.kwento1_page18, R.drawable.kwento1_page19
    };

    private static final String STORY_ID = "PabulaKwento2";
    private static final String STORY_TITLE = "Ang Mag-Inang Palakang Puno";

    private boolean isLessonDone = false;
    private boolean introFinished = false;
    private ImageView storyImage;
    private Button unlockButton;
    private int currentPage = 0;

    private FirebaseFirestore db;
    private String uid;

    private TextToSpeech textToSpeech;
    private List<String> introLines;
    private int currentIntroIndex = 0;

    private List<String> pageLines;
    private int currentLineIndex = 0;
    private boolean introPlayed = false;

    private AudioManager audioManager;
    private int originalVolume;
    private boolean isNavigatingToQuiz = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panitikan_pabula_kwento2);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        storyImage = findViewById(R.id.imageViewComic4);
        unlockButton = findViewById(R.id.UnlockButton);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) uid = user.getUid();

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        storyImage.setImageResource(comicPages[currentPage]);

        initTTS();

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
                if (audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                }

                if (textToSpeech != null) {
                    textToSpeech.stop();
                }

                isNavigatingToQuiz = true;
                startActivity(new Intent(PabulaKwento2.this, PabulaKwento2Quiz.class));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(PabulaKwento2.this, Pabula.class)
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
                }
            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadIntroLines() {
        if (introLines != null && !introLines.isEmpty()) {
            currentIntroIndex = 0;
            speakIntro();
            return;
        }

        db.collection("lesson_character_lines").document("LCL22").get()
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
                            introFinished = true;

                            isLessonDone = true;
                            unlockButton.setEnabled(true);
                            unlockButton.setAlpha(1f);
                        }
                    });
                }
            });
        } else {
            introFinished = true;

            isLessonDone = true;
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        }
    }


    private void nextPage() {

        if (currentPage < comicPages.length - 1) {
            currentPage++;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);

            if (textToSpeech != null) {
                textToSpeech.stop();
            }
            currentLineIndex = 0;

            if (currentPage == 1) {
                if (!introPlayed) {
                    loadPageLines(currentPage);
                    introPlayed = true;
                }
            } else {
                loadPageLines(currentPage);
            }

            if (currentPage == comicPages.length - 1) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
            }
        }
    }


    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            storyImage.setImageResource(comicPages[currentPage]);
            storyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

            updateCheckpoint(currentPage);

            if (textToSpeech != null) {
                textToSpeech.stop();
            }
            currentLineIndex = 0;

            if (currentPage == 0) {
                introFinished = false;
                introPlayed = false;
                loadIntroLines();

            } else if (currentPage >= 2) {
                loadPageLines(currentPage);
            }
        }
    }


    private void loadPageLines(int page) {
        if (pageLines != null) {
            textToSpeech.stop();
        }

        db.collection("lesson_character_lines").document("LCL22").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<Map<String, Object>> pages = (List<Map<String, Object>>) snapshot.get("pages");
                        if (pages != null) {
                            for (Map<String, Object> p : pages) {
                                Object pageNumObj = p.get("page");
                                if (pageNumObj instanceof Number) {
                                    int firestorePage = ((Number) pageNumObj).intValue();
                                    if (firestorePage == currentPage) {
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

    private void checkResumeDialog(int checkpoint) {
        if (checkpoint > 0) {
            ResumeDialogUtils.showResumeDialog(this, new ResumeDialogUtils.ResumeDialogListener() {
                @Override
                public void onResumeLesson() {
                    currentPage = checkpoint;
                    storyImage.setImageResource(comicPages[currentPage]);
                    introFinished = currentPage != 0;
                    introPlayed = introFinished;

                    if (currentPage == 0) {
                        loadIntroLines();
                    } else {
                        loadPageLines(currentPage);
                    }

                    if (currentPage == comicPages.length - 1) {
                        isLessonDone = true;
                        unlockButton.setEnabled(true);
                        unlockButton.setAlpha(1f);
                    }
                }

                @Override
                public void onRestartLesson() {
                    currentPage = 0;
                    storyImage.setImageResource(comicPages[currentPage]);
                    introFinished = false;
                    introPlayed = false;
                    loadIntroLines();
                }
            });
        } else {
            if (currentPage == 0) {
                introFinished = false;
                introPlayed = false;
                loadIntroLines();
            } else {
                introFinished = true;
                introPlayed = true;
                loadPageLines(currentPage);
            }
        }
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

                    Map<String, Object> pabula = (Map<String, Object>) categories.get("");
                    if (pabula == null) return;

                    Map<String, Object> stories = (Map<String, Object>) pabula.get("stories");
                    if (stories == null) return;

                    Map<String, Object> story = (Map<String, Object>) stories.get(STORY_ID);
                    if (story == null) return;

                    Object checkpointObj = story.get("checkpoint");
                    Object statusObj = story.get("status");

                    int checkpoint = 0;
                    if (checkpointObj instanceof Number) {
                        checkpoint = ((Number) checkpointObj).intValue();
                    }

                    checkResumeDialog(checkpoint);

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
                                Map<String, Object> pabula = (Map<String, Object>) categories.get("Pabula");
                                if (pabula != null) {
                                    Map<String, Object> stories = (Map<String, Object>) pabula.get("stories");
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

                    Map<String, Object> pabulaData = new HashMap<>();
                    pabulaData.put("stories", Map.of(STORY_ID, storyData));

                    Map<String, Object> categories = new HashMap<>();
                    categories.put("Pabula", pabulaData);

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
    protected void onPause() {
        super.onPause();
        if (!isNavigatingToQuiz && audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isNavigatingToQuiz = false;
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        }
    }

    @Override
    protected void onDestroy() {
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
