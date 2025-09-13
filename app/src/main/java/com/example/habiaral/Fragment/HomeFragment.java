package com.example.habiaral.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.PagUnawa.PagUnawa;
import com.example.habiaral.Palaro.Palaro;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.media.MediaPlayer;


public class HomeFragment extends Fragment {

    private final Map<Integer, Class<?>> lessonMap = new HashMap<>();
    private TextView nicknameTextView;

    public HomeFragment() {
    }

    private MediaPlayer mediaPlayer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home, container, false);

        nicknameTextView = view.findViewById(R.id.nickname_id);

        lessonMap.put(R.id.bahagi, BahagiNgPananalita.class);
        lessonMap.put(R.id.komprehensyon, PagUnawa.class);
        lessonMap.put(R.id.kayarian, KayarianNgPangungusap.class);
        lessonMap.put(R.id.palaro, Palaro.class);

        for (Map.Entry<Integer, Class<?>> entry : lessonMap.entrySet()) {
            LinearLayout button = view.findViewById(entry.getKey());
            Class<?> activityClass = entry.getValue();
            if (button != null) {
                button.setOnClickListener(v -> {
                    SoundClickUtils.playClickSound(getContext(), R.raw.button_click);
                    startActivity(new Intent(getActivity(), activityClass));
                });
            }
        }

        getParentFragmentManager().setFragmentResultListener("nicknameKey", this, (requestKey, bundle) -> {
            String nickname = bundle.getString("nickname");
            nicknameTextView.setText(nickname);

            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("nickname", nickname).apply();
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        loadNicknameFromPrefs();
        loadNicknameFromFirestore();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            recordLogDate(studentId);

            checkSevenDayStreak(studentId);
        }
    }

    private void checkSevenDayStreak(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        db.collection("daily_play_logs").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    Map<String, Object> data = snapshot.getData();
                    if (data == null) return;

                    List<String> playedDates = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                            if (entry.getKey().matches("\\d{4}-\\d{2}-\\d{2}")) {
                                playedDates.add(entry.getKey());
                            }
                        }
                    }

                    if (playedDates.isEmpty()) return;

                    Collections.sort(playedDates);

                    int maxStreak = 1;
                    int currentStreak = 1;

                    for (int i = 1; i < playedDates.size(); i++) {
                        try {
                            Date prevDate = sdf.parse(playedDates.get(i - 1));
                            Date currentDate = sdf.parse(playedDates.get(i));

                            Calendar calPrev = Calendar.getInstance();
                            calPrev.setTime(prevDate);

                            Calendar calCurrent = Calendar.getInstance();
                            calCurrent.setTime(currentDate);

                            calPrev.add(Calendar.DAY_OF_YEAR, 1);

                            if (sdf.format(calPrev.getTime()).equals(playedDates.get(i))) {
                                currentStreak++;
                                maxStreak = Math.max(maxStreak, currentStreak);
                            } else {
                                currentStreak = 1;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (maxStreak >= 7) {
                        String studentId = snapshot.getString("studentId");
                        if (studentId != null) {
                            unlockA6Achievement(uid, studentId);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Error checking streak: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void unlockA6Achievement(String uid, String studentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String achievementCode = "SA6";
        String achievementId = "A6";

        db.collection("student_achievements").document(uid).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> existing = (Map<String, Object>) snapshot.get("achievements");
            if (existing != null && existing.containsKey(achievementCode))
                return; // already unlocked

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
                        .set(wrapper, SetOptions.merge())
                        .addOnSuccessListener(unused -> requireActivity().runOnUiThread(() -> {
                            showAchievementUnlockedDialog(title, R.drawable.achievement06);
                        }));
            });
        });
    }

    private void recordLogDate(String studentDocId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(studentDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String studentId = documentSnapshot.getString("studentId");

                        if (studentId != null) {
                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Calendar.getInstance().getTime());

                            Map<String, Object> update = new HashMap<>();
                            update.put(today, true);
                            update.put("studentId", studentId);

                            db.collection("daily_play_logs")
                                    .document(studentDocId)
                                    .set(update, SetOptions.merge());
                        }
                    }
                });
    }


    private void showAchievementUnlockedDialog(String title, int imageRes) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.achievement_unlocked, null);

        ImageView iv = dialogView.findViewById(R.id.imageView19);
        TextView tv = dialogView.findViewById(R.id.textView14);

        iv.setImageResource(imageRes);
        String line1 = "Nakamit mo na ang parangal:\n";
        String line2 = title;

        SpannableStringBuilder ssb = new SpannableStringBuilder(line1 + line2);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = line1.length();
        int end = line1.length() + line2.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.1f), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ssb);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50; // offset mula sa taas (px)
            dialog.getWindow().setAttributes(params);
        }

        dialog.setOnShowListener(d -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(requireContext(), R.raw.achievement_pop);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        });

        dialog.show();
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

                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                            prefs.edit().putString("nickname", nickname).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Di makuha ang palayaw", Toast.LENGTH_SHORT).show());
    }
}
