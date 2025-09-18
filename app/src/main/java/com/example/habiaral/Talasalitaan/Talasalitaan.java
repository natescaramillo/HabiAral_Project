package com.example.habiaral.Talasalitaan;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Talasalitaan extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout wordContainer;
    private LayoutInflater inflater;

    private TextToSpeech textToSpeech;
    private EditText searchBar;
    private List<DocumentSnapshot> allWords = new ArrayList<>();
    private boolean hasShownNoResults = false;

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

        db = FirebaseFirestore.getInstance();
        loadWordsFromFirestore();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.1f);
            } else {
                Toast.makeText(this, "Text-to-Speech failed to initialize", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWordsFromFirestore() {
        db.collection("diksiyonaryo")
                .orderBy("wordID")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allWords.clear();
                    allWords.addAll(queryDocumentSnapshots.getDocuments());
                    displayWords(allWords);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load words", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayWords(List<DocumentSnapshot> words) {
        wordContainer.removeAllViews();

        for (DocumentSnapshot doc : words) {
            String word = doc.getString("word");
            String meaning = doc.getString("meaning");
            addWordToLayout(word, meaning);
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

        // Default white tint
        speakerIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));

        speakerIcon.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                speakerIcon.setColorFilter(ContextCompat.getColor(this, R.color.black));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                speakerIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));

                playButtonClickSound();

                if (textToSpeech != null) {
                    String toSpeak = word + ". Ang kahulugan nito ay: " + meaning;
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
            return true;
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
