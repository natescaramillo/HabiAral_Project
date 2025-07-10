package com.example.habiaral.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.Komprehensyon.Komprehensyon;
import com.example.habiaral.Palaro.Palaro;
import com.example.habiaral.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private final Map<Integer, Class<?>> lessonMap = new HashMap<>();
    private TextView nicknameTextView;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        nicknameTextView = view.findViewById(R.id.nickname_id); // Make sure this ID exists in XML

        lessonMap.put(R.id.bahagi, BahagiNgPananalita.class);
        lessonMap.put(R.id.komprehensyon, Komprehensyon.class);
        lessonMap.put(R.id.kayarian, KayarianNgPangungusap.class);
        lessonMap.put(R.id.palaro, Palaro.class);

        for (Map.Entry<Integer, Class<?>> entry : lessonMap.entrySet()) {
            LinearLayout button = view.findViewById(entry.getKey());
            Class<?> activityClass = entry.getValue();
            if (button != null) {
                button.setOnClickListener(v -> startActivity(new Intent(getActivity(), activityClass)));
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNicknameFromPrefs();
        loadNicknameFromFirestore(); // Optional: load fresh copy
    }

    private void loadNicknameFromPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "Walang Palayaw");

        if (nicknameTextView != null) {
            nicknameTextView.setText(nickname);
        }
    }

    private void loadNicknameFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("students").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nickname = document.getString("nickname");
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            nicknameTextView.setText(nickname);

                            // Update SharedPreferences for next time
                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                            prefs.edit().putString("nickname", nickname).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Di makuha ang palayaw", Toast.LENGTH_SHORT).show());
    }
}
