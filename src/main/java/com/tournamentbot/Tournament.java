package com.tournamentbot;

import java.util.*;

public class Tournament {
    private String id;
    private String name;
    private String game;
    private String adminId;
    private int maxPlayers;
    private String status;
    private Map<String, Player> players;
    private List<Match> brackets;
    
    public Tournament(String id, String name, String game, String adminId, int maxPlayers) {
        this.id = id;
        this.name = name;
        this.game = game;
        this.adminId = adminId;
        this.maxPlayers = maxPlayers;
        this.status = "WAITING";
        this.players = new HashMap<>();
        this.brackets = new ArrayList<>();
        players.put(adminId, new Player(adminId, "Admin", true));
    }
    
    public void addPlayer(String userId, String username) {
        if (players.size() < maxPlayers && !players.containsKey(userId)) {
            players.put(userId, new Player(userId, username, false));
        }
    }
    
    public void removePlayer(String userId) {
        if (!userId.equals(adminId)) {
            players.remove(userId);
        }
    }
    
    public void generateBrackets() {
        brackets.clear();
        List<Player> list = new ArrayList<>(players.values());
        Collections.shuffle(list);
        
        for (int i = 0; i < list.size() - 1; i += 2) {
            Match m = new Match(brackets.size() + 1, list.get(i), list.get(i + 1));
            brackets.add(m);
        }
        if (list.size() % 2 != 0) {
            Match m = new Match(brackets.size() + 1, list.get(list.size() - 1), null);
            m.setBye(true);
            brackets.add(m);
        }
    }
    
    public boolean setMatchScore(int matchId, int score1, int score2) {
        for (Match m : brackets) {
            if (m.getId() == matchId) {
                m.setScore(score1, score2);
                return true;
            }
        }
        return false;
    }
    
    public String getBracketsString() {
        if (brackets.isEmpty()) return "Bracket not generated.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("**Tournament Bracket:**\n\n");
        sb.append("Name: ").append(name).append("\n\n");
        
        for (Match m : brackets) {
            sb.append("Match ").append(m.getId()).append(": ");
            
            if (m.isBye()) {
                sb.append("<@").append(m.getPlayer1().getUserId()).append("> advances!");
            } else if (m.getPlayer2() == null) {
                sb.append("Waiting for opponent");
            } else if (m.isFinished()) {
                sb.append("<@").append(m.getPlayer1().getUserId()).append("> ")
                  .append(m.getScore1()).append(" - ").append(m.getScore2())
                  .append(" <@").append(m.getPlayer2().getUserId()).append(">");
                if (m.getScore1() > m.getScore2()) {
                    sb.append(" -> <@").append(m.getPlayer1().getUserId()).append("> wins!");
                } else if (m.getScore2() > m.getScore1()) {
                    sb.append(" -> <@").append(m.getPlayer2().getUserId()).append("> wins!");
                }
            } else {
                sb.append("<@").append(m.getPlayer1().getUserId()).append("> vs ")
                  .append("<@").append(m.getPlayer2().getUserId()).append(">");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public String getResultsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**Tournament Results:**\n\n");
        sb.append("Name: ").append(name).append("\n\n");
        
        boolean hasResults = false;
        for (Match m : brackets) {
            if (m.isFinished()) {
                hasResults = true;
                sb.append("<@").append(m.getPlayer1().getUserId()).append("> ")
                  .append(m.getScore1()).append(" - ").append(m.getScore2())
                  .append(" <@").append(m.getPlayer2().getUserId()).append(">\n");
            }
        }
        
        if (!hasResults) {
            sb.append("No results recorded yet.\n");
        }
        
        sb.append("\n**Rankings:**\n");
        List<Player> sorted = new ArrayList<>(players.values());
        sorted.sort((p1, p2) -> Integer.compare(p2.getWins(), p1.getWins()));
        
        int rank = 1;
        for (Player p : sorted) {
            sb.append(rank++).append(". <@").append(p.getUserId()).append(">")
              .append(" - ").append(p.getWins()).append(" wins");
            if (p.isAdmin()) sb.append(" (Admin)");
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("**Tournament Information**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n");
        sb.append("Players: ").append(players.size()).append("/").append(maxPlayers).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Admin: <@").append(adminId).append(">\n\n");
        
        sb.append("**Players:**\n");
        int i = 1;
        for (Player p : players.values()) {
            sb.append(i++).append(". <@").append(p.getUserId()).append(">");
            if (p.isAdmin()) sb.append(" (Admin)");
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getGame() { return game; }
    public String getAdminId() { return adminId; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Player> getPlayers() { return players; }
    public List<Match> getBrackets() { return brackets; }
}