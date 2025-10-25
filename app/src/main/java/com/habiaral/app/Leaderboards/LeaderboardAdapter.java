package com.habiaral.app.Leaderboards;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habiaral.app.R;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<Leaderboards> userList;
    private final Context context;

    public LeaderboardAdapter(List<Leaderboards> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leader_board_bar_users, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Leaderboards user = userList.get(position);
        int rank = user.getRank();

        holder.rankText.setText(String.valueOf(rank));
        holder.nameText.setText(user.getNickname());
        holder.pointsText.setText(user.getTotalScore() + " pts");

        // 🥇🥈🥉 Color or icon based on rank
        switch (rank) {
            case 1:
                holder.rankIcon.setImageResource(R.drawable.trophy_gold);
                holder.rankText.setTextColor(Color.parseColor("#FFD700")); // gold
                break;
            case 2:
                holder.rankIcon.setImageResource(R.drawable.trophy_silver);
                holder.rankText.setTextColor(Color.parseColor("#C0C0C0")); // silver
                break;
            case 3:
                holder.rankIcon.setImageResource(R.drawable.trophy_bronze);
                holder.rankText.setTextColor(Color.parseColor("#CD7F32")); // bronze
                break;
            default:
                holder.rankIcon.setImageResource(R.drawable.trophy_unranked);
                holder.rankText.setTextColor(Color.parseColor("#5D4037")); // dark brown (visible)
                break;
        }

        // ✨ Simple fade or slide animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, nameText, pointsText;
        ImageView rankIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            nameText = itemView.findViewById(R.id.name_text);
            pointsText = itemView.findViewById(R.id.points_text);
            rankIcon = itemView.findViewById(R.id.rank_icon);
        }
    }
}
