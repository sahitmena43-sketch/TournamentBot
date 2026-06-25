package com.tournamentbot;

public class Player {
    private String userId;
    private String username;
    private boolean isAdmin;
    private int wins;
    private int draws;
    private int losses;
    private int points;
    private int goalsFor;
    private int goalsAgainst;
    
    public Player(String userId, String username, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.isAdmin = isAdmin;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.points = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
    }
    
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(int goalsAgainst) { this.goalsAgainst = goalsAgainst; }
}