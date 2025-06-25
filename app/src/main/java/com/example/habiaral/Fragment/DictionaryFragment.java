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

public class DictionaryFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout wordContainer;
    private LayoutInflater inflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dictionary, container, false);
        wordContainer = view.findViewById(R.id.word_container);
        this.inflater = inflater;

        db = FirebaseFirestore.getInstance();
        loadWordsFromFirestore();

        return view;
    }
    private void loadWordsFromFirestore() {
        db.collection("diksiyonaryo")
                .orderBy("wordID")  // make sure 'wordID' exists in Firestore
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

        // Optional: Add Text-to-Speech here later if needed

        wordContainer.addView(wordItemView);
    }
}
