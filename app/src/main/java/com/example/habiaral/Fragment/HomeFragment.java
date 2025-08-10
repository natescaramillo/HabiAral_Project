package com.example.habiaral.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private final Map<Integer, Class<?>> lessonMap = new HashMap<>();
    private TextView nicknameTextView;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home, container, false);

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
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Record that user played today
            recordLogDate(studentId);

            // Check streak & achievement
            checkSevenDayStreak(studentId);
        }
    }
    public void checkSevenDayStreak(String studentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference datesRef = db.collection("daily_play_logs").document(studentId).collection("dates");

        datesRef.get().addOnSuccessListener(snapshot -> {
            int streak = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();

            for (int i = 0; i < 7; i++) {
                String date = sdf.format(calendar.getTime());
                if (snapshot.getDocuments().stream().anyMatch(doc -> doc.getId().equals(date))) {
                    streak++;
                } else {
                    break; // break streak if a day is missing
                }
                calendar.add(Calendar.DATE, -1);
            }

            if (streak == 7) {
                unlockA6Achievement(studentId);
            }
        });
    }

    private void unlockA6Achievement(String studentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String achievementCode = "SA6";
        String achievementId = "A6";

        db.collection("student_achievements").document(uid).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> existing = (Map<String, Object>) snapshot.get("achievements");
            if (existing != null && existing.containsKey(achievementCode)) return; // already unlocked

            db.collection("achievements").document(achievementId).get().addOnSuccessListener(achDoc -> {
                if (!achDoc.exists()) return;

                String title = achDoc.getString("title");

                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementID", achievementId);
                achievementData.put("title", title);
                achievementData.put("unlockedAt", com.google.firebase.Timestamp.now());

                Map<String, Object> achievementMap = new HashMap<>();
                achievementMap.put(achievementCode, achievementData);

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("studentId", studentId);
                wrapper.put("achievements", achievementMap);

                db.collection("student_achievements").document(uid)
                        .set(wrapper, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(unused -> requireActivity().runOnUiThread(() -> {
                            showAchievementUnlockedDialog(title, R.drawable.a6);
                        }));
            });
        });
    }
    private void recordLogDate(String studentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        Map<String, Object> update = new HashMap<>();
        update.put(today, true); // date as field

        db.collection("daily_play_logs").document(studentId)
                .set(update, SetOptions.merge()); // merge to keep existing dates
    }

    private void showAchievementUnlockedDialog(String title, int imageRes){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View toastView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = toastView.findViewById(R.id.imageView19);
        TextView tv = toastView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);

        ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, line1.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new android.text.style.RelativeSizeSpan(1.3f), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);

        Toast toast = new Toast(getContext());
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
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
