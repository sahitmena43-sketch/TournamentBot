package com.tournamentbot;

public class Player {
    private String userId;
    private String username;
    private boolean isAdmin;
    private int wins;
    private int losses;
    
    public Player(String userId, String username, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.isAdmin = isAdmin;
        this.wins = 0;
        this.losses = 0;
    }
    
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
}