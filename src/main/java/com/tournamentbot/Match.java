package com.tournamentbot;

public class Match {
    private int id;
    private Player player1;
    private Player player2;
    private int score1;
    private int score2;
    private boolean finished;
    private boolean bye;
    private String group;
    
    public Match(int id, Player player1, Player player2) {
        this(id, player1, player2, null);
    }
    
    public Match(int id, Player player1, Player player2, String group) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.score1 = 0;
        this.score2 = 0;
        this.finished = false;
        this.bye = false;
        this.group = group;
    }
    
    public void setScore(int score1, int score2) {
        this.score1 = score1;
        this.score2 = score2;
        this.finished = true;
        
        if (player1 != null) {
            player1.setGoalsFor(player1.getGoalsFor() + score1);
            player1.setGoalsAgainst(player1.getGoalsAgainst() + score2);
        }
        if (player2 != null) {
            player2.setGoalsFor(player2.getGoalsFor() + score2);
            player2.setGoalsAgainst(player2.getGoalsAgainst() + score1);
        }
        
        if (score1 > score2 && player1 != null && player2 != null) {
            player1.setWins(player1.getWins() + 1);
            player2.setLosses(player2.getLosses() + 1);
        } else if (score2 > score1 && player2 != null && player1 != null) {
            player2.setWins(player2.getWins() + 1);
            player1.setLosses(player1.getLosses() + 1);
        } else if (score1 == score2 && player1 != null && player2 != null) {
            player1.setDraws(player1.getDraws() + 1);
            player2.setDraws(player2.getDraws() + 1);
        }
    }
    
    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    
    @Override
    public String toString() {
        String prefix = group != null && !group.equals("Knockout") ? "[Group " + group + "] " : "";
        if (group != null && group.equals("Knockout")) prefix = "";
        
        if (bye) {
            return prefix + "Match " + id + ": " + 
                   (player1 != null ? "<@" + player1.getUserId() + ">" : "Unknown") + " advances!";
        }
        if (player2 == null) {
            return prefix + "Match " + id + ": Waiting for opponent";
        }
        if (finished) {
            return prefix + "Match " + id + ": " +
                   "<@" + player1.getUserId() + "> " + 
                   score1 + " - " + score2 + " " +
                   "<@" + player2.getUserId() + ">";
        }
        return prefix + "Match " + id + ": " +
               "<@" + player1.getUserId() + "> vs " +
               "<@" + player2.getUserId() + ">";
    }
    
    public int getId() { return id; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public boolean isFinished() { return finished; }
    public boolean isBye() { return bye; }
    public void setBye(boolean bye) { this.bye = bye; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
}