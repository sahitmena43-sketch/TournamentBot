package com.tournamentbot;

public class Match {
    private int id;
    private Player player1;
    private Player player2;
    private int score1;
    private int score2;
    private boolean finished;
    private boolean bye;
    
    public Match(int id, Player player1, Player player2) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.score1 = 0;
        this.score2 = 0;
        this.finished = false;
        this.bye = false;
    }
    
    public void setScore(int score1, int score2) {
        this.score1 = score1;
        this.score2 = score2;
        this.finished = true;
        
        if (score1 > score2 && player1 != null) {
            player1.setWins(player1.getWins() + 1);
            if (player2 != null) player2.setLosses(player2.getLosses() + 1);
        } else if (score2 > score1 && player2 != null) {
            player2.setWins(player2.getWins() + 1);
            if (player1 != null) player1.setLosses(player1.getLosses() + 1);
        }
    }
    
    public int getId() { return id; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public boolean isFinished() { return finished; }
    public boolean isBye() { return bye; }
    public void setBye(boolean bye) { this.bye = bye; }
}