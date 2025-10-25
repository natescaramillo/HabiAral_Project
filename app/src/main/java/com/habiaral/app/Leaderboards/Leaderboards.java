package com.habiaral.app.Leaderboards;

public class Leaderboards {
    private String nickname;
    private int totalScore;
    private int rank;

    public Leaderboards(String nickname, int totalScore, int rank) {
        this.nickname = nickname;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    public String getNickname() {
        return nickname;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getRank() {
        return rank;
    }
}
