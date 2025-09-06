package com.example.habiaral.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.habiaral.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;


import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class DictionaryFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout wordContainer;
    private LayoutInflater inflater;

    private TextToSpeech textToSpeech;
    private EditText searchBar;
    private List<DocumentSnapshot> allWords = new ArrayList<>();
    private boolean hasShownNoResults = false;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dictionary, container, false); // ✅ initialize first

        wordContainer = view.findViewById(R.id.word_container);
        searchBar = view.findViewById(R.id.searchBarId); // ✅ now this works
        this.inflater = inflater;

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

        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
                textToSpeech.setSpeechRate(1.1f); // adjust mo value (1.2 - 1.5 okay na mabilis)

            } else {
                Toast.makeText(getContext(), "Text-to-Speech failed to initialize", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
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
                    Toast.makeText(getContext(), "Failed to load words", Toast.LENGTH_SHORT).show();
                });
    }
    private void displayWords(List<DocumentSnapshot> words) {
        wordContainer.removeAllViews(); // clear old list

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
                Toast.makeText(getContext(), "Walang natagpuang salita", Toast.LENGTH_SHORT).show();
                hasShownNoResults = true;
            }
        } else {
            hasShownNoResults = false;  // Reset flag if matches are found
        }
    }

    private void addWordToLayout(String word, String meaning) {
        View wordItemView = inflater.inflate(R.layout.dictionary_word_item, wordContainer, false);

        TextView wordText = wordItemView.findViewById(R.id.wordTextView);
        TextView meaningText = wordItemView.findViewById(R.id.meaningTextView);
        ImageView speakerIcon = wordItemView.findViewById(R.id.speakerIcon);

        wordText.setText(word);
        meaningText.setText(meaning);

// Set default tint to white
        speakerIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

        // Change icon color on press
        speakerIcon.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                speakerIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                speakerIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

                // 🔊 Play button click sound
                playButtonClickSound();

                // Speak the word when released
                if (textToSpeech != null) {
                    String toSpeak = word + ". Ang kahulugan nito ay: " + meaning;
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
            return true;
        });

        wordContainer.addView(wordItemView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop(); // patigilin agad kahit naka-hide lang
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // Fragment is hidden, stop TTS
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
        }
    }

    public void onResume() {
        super.onResume();
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(requireContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(new Locale("tl", "PH"));
                    textToSpeech.setSpeechRate(1.1f);
                }
            });
        }
    }

    private void playButtonClickSound() {
        if (getContext() != null) {
            android.media.MediaPlayer mp = android.media.MediaPlayer.create(getContext(), R.raw.button_click);
            mp.setOnCompletionListener(android.media.MediaPlayer::release);
            mp.start();
        }
    }

}
