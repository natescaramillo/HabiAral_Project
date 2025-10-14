package com.habiaral.app.Diksyonaryo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.habiaral.app.R;
import com.habiaral.app.Utils.SoundClickUtils;
import com.habiaral.app.Utils.TalasalitaanUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Diksyonaryo extends AppCompatActivity {

    private LinearLayout wordContainer;
    private LayoutInflater inflater;
    private TextToSpeech textToSpeech;
    private EditText searchBar;
    private List<DocumentSnapshot> allWords = new ArrayList<>();
    private boolean hasShownNoResults = false, isSpeakerActive = false;
    private ImageView activeSpeakerIcon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.talasalitaan);

        wordContainer = findViewById(R.id.word_container);
        searchBar = findViewById(R.id.searchBarId);
        inflater = LayoutInflater.from(this);

        ImageView talasalitaanBack = findViewById(R.id.talasalitaan_back);
        talasalitaanBack.setOnClickListener(v -> {
            SoundClickUtils.playClickSound(this, R.raw.button_click);
            finish();
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWords(s.toString().trim());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (TalasalitaanUtils.isLoaded()) {
            allWords.clear();
            allWords.addAll(TalasalitaanUtils.getCachedWords());

            Collections.sort(allWords, new Comparator<DocumentSnapshot>() {
                @Override
                public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                    String w1 = d1.getString("word");
                    String w2 = d2.getString("word");
                    if (w1 == null) w1 = "";
                    if (w2 == null) w2 = "";
                    return w1.compareToIgnoreCase(w2);
                }
            });

            displayWords(allWords);

        } else {
            Toast.makeText(this, "Inaayos ang mga salita...", Toast.LENGTH_SHORT).show();
            TalasalitaanUtils.preloadWords(this, new TalasalitaanUtils.OnWordsLoadedListener() {
                @Override
                public void onWordsLoaded(List<DocumentSnapshot> words) {
                    runOnUiThread(() -> {
                        allWords.clear();
                        allWords.addAll(words);

                        Collections.sort(allWords, (d1, d2) -> {
                            String w1 = d1.getString("word");
                            String w2 = d2.getString("word");
                            if (w1 == null) w1 = "";
                            if (w2 == null) w2 = "";
                            return w1.compareToIgnoreCase(w2);
                        });

                        displayWords(allWords);
                    });
                }

                @Override
                public void onWordsLoadFailed(Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(Diksyonaryo.this, "Hindi ma-load ang mga salita", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }


        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Hindi ma-open ang installer ng TTS.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    for (Voice v : textToSpeech.getVoices()) {
                        if (v.getLocale() != null && v.getLocale().getLanguage().equals("fil")) {
                            textToSpeech.setVoice(v);
                            break;
                        }
                    }
                    textToSpeech.setSpeechRate(1.0f);
                }
            } else {
                Toast.makeText(this, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void displayWords(List<DocumentSnapshot> words) {
        wordContainer.removeAllViews();

        for (DocumentSnapshot doc : words) {
            String word = doc.getString("word");
            String meaning = doc.getString("meaning");
            if (word != null && meaning != null) {
                addWordToLayout(word, meaning);
            }
        }
    }

    private void filterWords(String query) {
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : allWords) {
            String word = doc.getString("word");
            if (word != null && word.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(doc);
            }
        }

        displayWords(filteredList);

        if (filteredList.isEmpty()) {
            if (!hasShownNoResults) {
                Toast.makeText(this, "Walang natagpuang salita", Toast.LENGTH_SHORT).show();
                hasShownNoResults = true;
            }
        } else {
            hasShownNoResults = false;
        }
    }

    private void addWordToLayout(String word, String meaning) {
        View wordItemView = inflater.inflate(R.layout.dictionary_word_item, wordContainer, false);

        TextView wordText = wordItemView.findViewById(R.id.wordTextView);
        TextView meaningText = wordItemView.findViewById(R.id.meaningTextView);
        ImageView speakerIcon = wordItemView.findViewById(R.id.speakerIcon);

        wordText.setText(word);
        meaningText.setText(meaning);

        speakerIcon.setImageResource(R.drawable.speaker_off);

        speakerIcon.setOnClickListener(v -> {
            playButtonClickSound();

            if (activeSpeakerIcon != null && activeSpeakerIcon != speakerIcon) {
                activeSpeakerIcon.setImageResource(R.drawable.speaker_off);
                isSpeakerActive = false;
                if (textToSpeech != null) {
                    textToSpeech.stop();
                }
            }

            if (!isSpeakerActive || activeSpeakerIcon != speakerIcon) {
                activeSpeakerIcon = speakerIcon;
                isSpeakerActive = true;
                speakerIcon.setImageResource(R.drawable.speaker_on);

                if (textToSpeech != null) {
                    String toSpeak = word + ". Ang kahulugan nito ay: " + meaning;
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                }

            } else {
                speakerIcon.setImageResource(R.drawable.speaker_off);
                isSpeakerActive = false;
                activeSpeakerIcon = null;

                if (textToSpeech != null) {
                    textToSpeech.stop();
                }
            }
        });

        wordContainer.addView(wordItemView);
    }

    private void playButtonClickSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.button_click);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(new Locale("tl", "PH"));
                    textToSpeech.setSpeechRate(1.1f);
                }
            });
        }
    }
}
