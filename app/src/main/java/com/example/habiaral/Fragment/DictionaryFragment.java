package com.example.habiaral.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.habiaral.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class DictionaryFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout wordContainer;
    private LayoutInflater inflater;

    private TextToSpeech textToSpeech;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dictionary, container, false);
        wordContainer = view.findViewById(R.id.word_container);
        this.inflater = inflater;

        db = FirebaseFirestore.getInstance();
        loadWordsFromFirestore();

        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("tl", "PH"));
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
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String word = doc.getString("word");
                        String meaning = doc.getString("meaning");
                        addWordToLayout(word, meaning);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error here (log or show a Toast)
                });
    }
    private void addWordToLayout(String word, String meaning) {
        View wordItemView = inflater.inflate(R.layout.dictionary_word_item, wordContainer, false);

        TextView wordText = wordItemView.findViewById(R.id.wordTextView);
        TextView meaningText = wordItemView.findViewById(R.id.meaningTextView);
        ImageView speakerIcon = wordItemView.findViewById(R.id.speakerIcon);

        wordText.setText(word);
        meaningText.setText(meaning);

        speakerIcon.setOnClickListener(v -> {
            if (textToSpeech != null) {
                String toSpeak = word + ". Kahulugan: " + meaning;
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
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
}
