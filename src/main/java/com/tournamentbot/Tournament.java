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
    
    // ✅ Grupet dhe pikët
    private Map<String, List<Player>> groups;
    private Map<String, Integer> points;
    private String groupStage;
    
    public Tournament(String id, String name, String game, String adminId, int maxPlayers) {
        this.id = id;
        this.name = name;
        this.game = game;
        this.adminId = adminId;
        this.maxPlayers = maxPlayers;
        this.status = "WAITING";
        this.players = new HashMap<>();
        this.brackets = new ArrayList<>();
        this.groups = new HashMap<>();
        this.points = new HashMap<>();
        this.groupStage = "GROUPS";
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
    
    /**
     * ✅ Ndan lojtarët në grupe
     */
    public void generateGroups() {
        groups.clear();
        points.clear();
        
        List<Player> playerList = new ArrayList<>(players.values());
        Collections.shuffle(playerList);
        
        // Sa grupe do të jenë (A, B, C, D)
        int numGroups = Math.min(4, Math.max(2, playerList.size() / 2));
        int playersPerGroup = Math.max(2, playerList.size() / numGroups);
        
        String[] groupNames = {"A", "B", "C", "D"};
        
        for (int g = 0; g < Math.min(numGroups, groupNames.length); g++) {
            String groupName = groupNames[g];
            List<Player> groupPlayers = new ArrayList<>();
            
            int start = g * playersPerGroup;
            int end = Math.min((g + 1) * playersPerGroup, playerList.size());
            
            for (int i = start; i < end; i++) {
                groupPlayers.add(playerList.get(i));
                points.put(playerList.get(i).getUserId(), 0);
            }
            
            if (!groupPlayers.isEmpty()) {
                groups.put(groupName, groupPlayers);
            }
        }
    }
    
    /**
     * ✅ Shton pikë për një lojtar
     */
    public void addPoints(String userId, int pointsToAdd) {
        points.put(userId, points.getOrDefault(userId, 0) + pointsToAdd);
        Player p = players.get(userId);
        if (p != null) {
            p.setPoints(p.getPoints() + pointsToAdd);
        }
    }
    
    /**
     * ✅ Gjeneron bracket-in e grupeve
     */
    public void generateBrackets() {
        brackets.clear();
        generateGroups();
        
        // Krijo ndeshjet brenda grupeve
        for (Map.Entry<String, List<Player>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<Player> groupPlayers = entry.getValue();
            
            for (int i = 0; i < groupPlayers.size(); i++) {
                for (int j = i + 1; j < groupPlayers.size(); j++) {
                    Match m = new Match(
                        brackets.size() + 1,
                        groupPlayers.get(i),
                        groupPlayers.get(j),
                        groupName
                    );
                    brackets.add(m);
                }
            }
        }
    }
    
    /**
     * ✅ Vendos rezultatin e një ndeshje dhe përditëson pikët
     */
    public boolean setMatchScore(int matchId, int score1, int score2) {
        for (Match m : brackets) {
            if (m.getId() == matchId) {
                m.setScore(score1, score2);
                
                // ✅ Përditëso pikët
                if (score1 > score2) {
                    addPoints(m.getPlayer1().getUserId(), 3);
                    addPoints(m.getPlayer2().getUserId(), 0);
                } else if (score2 > score1) {
                    addPoints(m.getPlayer1().getUserId(), 0);
                    addPoints(m.getPlayer2().getUserId(), 3);
                } else {
                    addPoints(m.getPlayer1().getUserId(), 1);
                    addPoints(m.getPlayer2().getUserId(), 1);
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * ✅ Klasifikimi i grupeve
     */
    public String getGroupStandings() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Group Standings:**\n\n");
        
        for (Map.Entry<String, List<Player>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<Player> groupPlayers = entry.getValue();
            
            sb.append("**Group ").append(groupName).append("**\n");
            sb.append("```\n");
            sb.append(String.format("%-20s %-8s %-8s %-8s %-8s\n", "Player", "P", "W", "D", "L"));
            sb.append("----------------------------------------\n");
            
            // Rendit sipas pikëve
            groupPlayers.sort((p1, p2) -> {
                int pts1 = points.getOrDefault(p1.getUserId(), 0);
                int pts2 = points.getOrDefault(p2.getUserId(), 0);
                return Integer.compare(pts2, pts1);
            });
            
            for (Player p : groupPlayers) {
                int pts = points.getOrDefault(p.getUserId(), 0);
                int wins = p.getWins();
                int draws = p.getDraws();
                int losses = p.getLosses();
                sb.append(String.format("%-20s %-8d %-8d %-8d %-8d\n", 
                    p.getUsername(), pts, wins, draws, losses));
            }
            sb.append("```\n\n");
        }
        return sb.toString();
    }
    
    /**
     * ✅ Bracket-i i plotë (grupe + knockout)
     */
    public String getBracketsString() {
        if (brackets.isEmpty()) return "Bracket not generated.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Bracket:**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n\n");
        
        // Grupet
        sb.append(getGroupStandings());
        
        // Ndeshjet
        sb.append("**📋 Matches:**\n\n");
        for (Match m : brackets) {
            sb.append(m.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * ✅ Rezultatet me pikë
     */
    public String getResultsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Results:**\n\n");
        sb.append("Name: ").append(name).append("\n\n");
        
        sb.append(getGroupStandings());
        
        // Ndeshjet e përfunduara
        sb.append("\n**📋 Completed Matches:**\n\n");
        for (Match m : brackets) {
            if (m.isFinished()) {
                sb.append(m.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Information**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n");
        sb.append("Players: ").append(players.size()).append("/").append(maxPlayers).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Admin: <@").append(adminId).append(">\n\n");
        sb.append(getGroupStandings());
        
        sb.append("\n**👤 Players:**\n");
        int i = 1;
        for (Player p : players.values()) {
            sb.append(i++).append(". <@").append(p.getUserId()).append(">");
            if (p.isAdmin()) sb.append(" (Admin)");
            int pts = points.getOrDefault(p.getUserId(), 0);
            sb.append(" - ").append(pts).append(" pts\n");
        }
        return sb.toString();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getGame() { return game; }
    public String getAdminId() { return adminId; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Player> getPlayers() { return players; }
    public List<Match> getBrackets() { return brackets; }
    public Map<String, Integer> getPoints() { return points; }
    public Map<String, List<Player>> getGroups() { return groups; }
}