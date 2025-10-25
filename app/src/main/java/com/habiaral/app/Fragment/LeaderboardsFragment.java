package com.habiaral.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.habiaral.app.Leaderboards.Leaderboards;
import com.habiaral.app.Leaderboards.LeaderboardAdapter;
import com.habiaral.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardsFragment extends Fragment {

    private RecyclerView recyclerLeaderboard;
    private TextView currentRankText, currentPointText;
    private LeaderboardAdapter adapter;
    private final List<Leaderboards> userList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leaderboard_fragment, container, false);

        recyclerLeaderboard = view.findViewById(R.id.recycler_leaderboard);
        recyclerLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));

        currentRankText = view.findViewById(R.id.current_rank);
        currentPointText = view.findViewById(R.id.current_point);

        adapter = new LeaderboardAdapter(userList, requireContext());
        recyclerLeaderboard.setAdapter(adapter);

        loadLeaderboardData();

        return view;
    }

    private void loadLeaderboardData() {
        db.collection("minigame_progress")
                .orderBy("total_score", Query.Direction.DESCENDING)
                .addSnapshotListener((progressSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Di ma-load ang leaderboard.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (progressSnapshots == null || progressSnapshots.isEmpty()) {
                        userList.clear();
                        adapter.notifyDataSetChanged();
                        currentRankText.setText("Walang data");
                        currentPointText.setText("0 puntos");
                        return;
                    }

                    userList.clear();
                    String currentUserUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
                    List<Leaderboards> tempList = new ArrayList<>();
                    AtomicInteger loadedCount = new AtomicInteger(0);
                    int totalDocs = progressSnapshots.size();

                    // For each record in minigame_progress
                    for (DocumentSnapshot progressDoc : progressSnapshots) {
                        String uid = progressDoc.getId();
                        Long totalScore = progressDoc.getLong("total_score");
                        if (totalScore == null) totalScore = 0L;

                        long finalScore = totalScore;

                        // Get student's nickname
                        db.collection("students").document(uid)
                                .get()
                                .addOnSuccessListener(studentDoc -> {
                                    String nickname = "Walang Pangalan";
                                    if (studentDoc.exists()) {
                                        String nick = studentDoc.getString("nickname");
                                        if (nick != null && !nick.isEmpty()) nickname = nick;
                                    }

                                    tempList.add(new Leaderboards(nickname, (int) finalScore, 0));

                                    if (loadedCount.incrementAndGet() == totalDocs) {
                                        showSortedLeaderboard(tempList, currentUserUid);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    if (loadedCount.incrementAndGet() == totalDocs) {
                                        showSortedLeaderboard(tempList, currentUserUid);
                                    }
                                });
                    }
                });
    }


    // Display sorted data after all documents are loaded
    private void showSortedLeaderboard(List<Leaderboards> tempList, String currentUserUid) {
        if (tempList.isEmpty()) {
            Toast.makeText(getContext(), "Walang leaderboard data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort by total score descending
        Collections.sort(tempList, Comparator.comparingInt(Leaderboards::getTotalScore).reversed());

        userList.clear();
        int rank = 1;
        for (Leaderboards user : tempList) {
            userList.add(new Leaderboards(user.getNickname(), user.getTotalScore(), rank));
            rank++;
        }
        adapter.notifyDataSetChanged();

        // Find current user rank & score
        db.collection("students").document(currentUserUid)
                .get()
                .addOnSuccessListener(currentDoc -> {
                    if (currentDoc.exists()) {
                        String myNick = currentDoc.getString("nickname");
                        if (myNick != null) {
                            for (Leaderboards user : userList) {
                                if (user.getNickname().equals(myNick)) {
                                    currentRankText.setText(String.valueOf(user.getRank()));
                                    currentPointText.setText(user.getTotalScore() + " puntos");
                                    return;
                                }
                            }
                        }
                    }
                    currentRankText.setText("N/A");
                    currentPointText.setText("0 puntos");
                });
    }
}
