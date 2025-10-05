package com.example.habiaral.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.habiaral.BahagiNgPananalita.BahagiNgPananalita;
import com.example.habiaral.KayarianNgPangungusap.KayarianNgPangungusap;
import com.example.habiaral.Panitikan.Panitikan;
import com.example.habiaral.Palaro.Palaro;
import com.example.habiaral.Diksyonaryo.Diksyonaryo;
import com.example.habiaral.R;
import com.example.habiaral.Utils.AchievementDialogUtils;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private final Map<Integer, Class<?>> lessonMap = new HashMap<>();
    private TextView nicknameTextView;
    private ImageView imageView;
    private boolean isFragmentActive = false;
    private Handler idleGifHandler = new Handler();
    private Runnable idleGifRunnable;
    private int animationStep = 0;

    @Override
    public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                          android.view.ViewGroup container,
                                          Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.home, container, false);

        imageView = view.findViewById(R.id.imageView7);
        Glide.with(this).asGif().load(R.drawable.idle).into(imageView);

        nicknameTextView = view.findViewById(R.id.nickname_id);

        // Map lesson buttons to their activities
        lessonMap.put(R.id.bahagi_cardView, BahagiNgPananalita.class);
        lessonMap.put(R.id.panitikan_cardView, Panitikan.class);
        lessonMap.put(R.id.kayarian_cardView, KayarianNgPangungusap.class);
        lessonMap.put(R.id.talasalitaan_cardView, Diksyonaryo.class);
        lessonMap.put(R.id.palaro_cardView, Palaro.class);

        // Attach listeners to each card
        for (Map.Entry<Integer, Class<?>> entry : lessonMap.entrySet()) {
            View button = view.findViewById(entry.getKey()); // FIX: use View instead of LinearLayout
            Class<?> activityClass = entry.getValue();
            button.setOnClickListener(v -> {
                SoundClickUtils.playClickSound(getContext(), R.raw.button_click);

                // Play click animation
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.card_click));

                // Delay the activity start slightly so animation plays first
                new Handler().postDelayed(() -> {
                    if (isAdded()) {
                        Intent intent = new Intent(getActivity(), activityClass);
                        startActivity(intent);
                    }
                }, 150);
            });

        }

        // Listen for nickname changes
        getParentFragmentManager().setFragmentResultListener("nicknameKey", this, (requestKey, bundle) -> {
            String nickname = bundle.getString("nickname");
            nicknameTextView.setText(nickname);
            requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .edit().putString("nickname", nickname).apply();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNicknameFromPrefs();
        loadNicknameFromFirestore();

        // Start GIF animation safely
        startIdleGifRandomizer();

        // Firebase actions
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            recordLogDate(studentId);
            checkSevenDayStreak(studentId);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopIdleGifRandomizer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopIdleGifRandomizer();
    }

    /** ----------------- Idle GIF Randomizer ----------------- **/
    private void startIdleGifRandomizer() {
        stopIdleGifRandomizer();
        isFragmentActive = true;

        idleGifRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFragmentActive || imageView == null) return;

                int delay = 3000 + (int) (Math.random() * 5000);

                if (animationStep == 0) {
                    Glide.with(HomeFragment.this).asGif().load(R.drawable.hello).into(imageView);
                    idleGifHandler.postDelayed(() -> {
                        if (isFragmentActive && imageView != null)
                            Glide.with(HomeFragment.this).asGif().load(R.drawable.idle).into(imageView);
                        animationStep = 1;
                        idleGifHandler.postDelayed(idleGifRunnable, delay);
                    }, 2000);

                } else if (animationStep == 1) {
                    Glide.with(HomeFragment.this).asGif().load(R.drawable.right_2).into(imageView);
                    idleGifHandler.postDelayed(() -> {
                        if (isFragmentActive && imageView != null)
                            Glide.with(HomeFragment.this).asGif().load(R.drawable.idle).into(imageView);
                        animationStep = 0;
                        idleGifHandler.postDelayed(idleGifRunnable, delay);
                    }, 2000);
                }
            }
        };

        idleGifHandler.postDelayed(idleGifRunnable, 2000);
    }

    private void stopIdleGifRandomizer() {
        isFragmentActive = false;
        if (idleGifHandler != null)
            idleGifHandler.removeCallbacksAndMessages(null);
        if (imageView != null)
            Glide.with(this).asGif().load(R.drawable.idle).into(imageView);
    }

    /** ----------------- Nickname Handling ----------------- **/
    private void loadNicknameFromPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "Walang Palayaw");
        if (nicknameTextView != null)
            nicknameTextView.setText(nickname);
    }

    private void loadNicknameFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("students").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            nicknameTextView.setText(nickname);
                            requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                    .edit().putString("nickname", nickname).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Di makuha ang palayaw", Toast.LENGTH_SHORT).show());
    }

    /** ----------------- Firebase Achievement / Log ----------------- **/
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

                            db.collection("daily_play_logs").document(studentDocId)
                                    .set(update, SetOptions.merge());
                        }
                    }
                });
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
                        "Kamalian sa pagtingin ng streak: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void unlockA6Achievement(String uid, String studentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String achievementCode = "SA6";
        String achievementId = "A6";

        db.collection("student_achievements").document(uid).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> existing = (Map<String, Object>) snapshot.get("achievements");
            if (existing != null && existing.containsKey(achievementCode))
                return;

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
                            AchievementDialogUtils.showAchievementUnlockedDialog(requireContext(), title, R.drawable.achievement06);
                        }));
            });
        });
    }
}
