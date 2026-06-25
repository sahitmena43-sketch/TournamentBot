package com.tournamentbot;

import java.util.*;

public class Tournament {
    private String id;
    private String name;
    private String game;
    private String adminId;
    private int maxPlayers;
    private String status;
    private String guildId;
    private Map<String, Player> players;
    private List<Match> brackets;
    private Map<String, List<Player>> groups;
    private Map<String, Integer> points;
    private List<Match> knockoutMatches;
    private String groupStage;
    private String knockoutStage;
    private String winnerId;
    private String tournamentType;
    
    private static final Set<String> FOOTBALL_GAMES = new HashSet<>(Arrays.asList(
        "Dream League Soccer 2026",
        "DLS",
        "FC Mobile",
        "FIFA",
        "eFootball",
        "PES",
        "Pro Evolution Soccer",
        "Football",
        "Soccer"
    ));
    
    private static final Set<String> BATTLE_ROYALE_GAMES = new HashSet<>(Arrays.asList(
        "Free Fire",
        "PUBG",
        "PUBG Mobile",
        "Fortnite",
        "Apex Legends",
        "Call of Duty Mobile",
        "CODM",
        "Warzone",
        "Battle Royale"
    ));
    
    public Tournament(String id, String name, String game, String adminId, int maxPlayers, String guildId) {
        this.id = id;
        this.name = name;
        this.game = game;
        this.adminId = adminId;
        this.maxPlayers = maxPlayers;
        this.guildId = guildId;
        this.status = "WAITING";
        this.players = new HashMap<>();
        this.brackets = new ArrayList<>();
        this.groups = new HashMap<>();
        this.points = new HashMap<>();
        this.knockoutMatches = new ArrayList<>();
        this.groupStage = "GROUPS";
        this.knockoutStage = "ROUND_16";
        this.winnerId = null;
        this.tournamentType = determineTournamentType(game);
        players.put(adminId, new Player(adminId, "Admin", true));
    }
    
    private String determineTournamentType(String game) {
        for (String footballGame : FOOTBALL_GAMES) {
            if (game.equalsIgnoreCase(footballGame) || game.toLowerCase().contains(footballGame.toLowerCase())) {
                return "FOOTBALL";
            }
        }
        for (String battleGame : BATTLE_ROYALE_GAMES) {
            if (game.equalsIgnoreCase(battleGame) || game.toLowerCase().contains(battleGame.toLowerCase())) {
                return "BATTLE_ROYALE";
            }
        }
        return "GENERAL";
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
    
    public void generateGroups() {
        groups.clear();
        points.clear();
        if (!tournamentType.equals("FOOTBALL")) return;
        
        List<Player> playerList = new ArrayList<>(players.values());
        Collections.shuffle(playerList);
        
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
    
    public void addPoints(String userId, int pointsToAdd) {
        points.put(userId, points.getOrDefault(userId, 0) + pointsToAdd);
        Player p = players.get(userId);
        if (p != null) p.setPoints(p.getPoints() + pointsToAdd);
    }
    
    public void generateBrackets() {
        brackets.clear();
        knockoutMatches.clear();
        
        if (tournamentType.equals("FOOTBALL")) {
            generateGroups();
            for (Map.Entry<String, List<Player>> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                List<Player> groupPlayers = entry.getValue();
                for (int i = 0; i < groupPlayers.size(); i++) {
                    for (int j = i + 1; j < groupPlayers.size(); j++) {
                        Match m = new Match(brackets.size() + 1, groupPlayers.get(i), groupPlayers.get(j), groupName);
                        brackets.add(m);
                    }
                }
            }
        } else {
            List<Player> playerList = new ArrayList<>(players.values());
            Collections.shuffle(playerList);
            int matchId = 1;
            for (int i = 0; i < playerList.size() - 1; i += 2) {
                Player p1 = playerList.get(i);
                Player p2 = (i + 1 < playerList.size()) ? playerList.get(i + 1) : null;
                Match m = new Match(matchId++, p1, p2, "Knockout");
                if (p2 == null) m.setBye(true);
                brackets.add(m);
            }
        }
    }
    
    public boolean setMatchScore(int matchId, int score1, int score2) {
        for (Match m : brackets) {
            if (m.getId() == matchId) {
                m.setScore(score1, score2);
                if (tournamentType.equals("FOOTBALL")) {
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
                }
                if (tournamentType.equals("FOOTBALL")) {
                    checkGroupStageComplete();
                } else {
                    checkAllMatchesComplete();
                }
                return true;
            }
        }
        for (Match m : knockoutMatches) {
            if (m.getId() == matchId) {
                m.setScore(score1, score2);
                checkKnockoutStageComplete();
                return true;
            }
        }
        return false;
    }
    
    private void checkAllMatchesComplete() {
        for (Match m : brackets) {
            if (!m.isFinished()) return;
        }
        generateKnockoutStage();
    }
    
    private void checkGroupStageComplete() {
        for (Match m : brackets) {
            if (!m.isFinished()) return;
        }
        generateKnockoutStage();
    }
    
    private void generateKnockoutStage() {
        knockoutMatches.clear();
        knockoutStage = "ROUND_16";
        List<Player> sortedPlayers;
        
        if (tournamentType.equals("FOOTBALL")) {
            sortedPlayers = new ArrayList<>(players.values());
            sortedPlayers.sort((p1, p2) -> {
                int pts1 = points.getOrDefault(p1.getUserId(), 0);
                int pts2 = points.getOrDefault(p2.getUserId(), 0);
                return Integer.compare(pts2, pts1);
            });
        } else {
            sortedPlayers = new ArrayList<>();
            for (Match m : brackets) {
                if (m.isFinished()) {
                    if (m.getScore1() > m.getScore2() && m.getPlayer1() != null) {
                        sortedPlayers.add(m.getPlayer1());
                    } else if (m.getScore2() > m.getScore1() && m.getPlayer2() != null) {
                        sortedPlayers.add(m.getPlayer2());
                    } else if (m.isBye() && m.getPlayer1() != null) {
                        sortedPlayers.add(m.getPlayer1());
                    }
                }
            }
        }
        
        int numPlayers = sortedPlayers.size();
        int nextPowerOfTwo = 1;
        while (nextPowerOfTwo < numPlayers) nextPowerOfTwo *= 2;
        while (sortedPlayers.size() < nextPowerOfTwo) sortedPlayers.add(null);
        
        int matchId = brackets.size() + 1;
        for (int i = 0; i < sortedPlayers.size() / 2; i++) {
            Player p1 = sortedPlayers.get(i);
            Player p2 = sortedPlayers.get(sortedPlayers.size() - 1 - i);
            if (p1 == null && p2 == null) continue;
            Match m = new Match(matchId++, p1, p2, "Knockout");
            if (p1 == null || p2 == null) {
                m.setBye(true);
                if (p1 != null) m.setScore(1, 0);
                else if (p2 != null) m.setScore(0, 1);
            }
            knockoutMatches.add(m);
        }
        
        if (knockoutMatches.size() <= 4) knockoutStage = "QUARTER";
        else if (knockoutMatches.size() <= 2) knockoutStage = "SEMI";
        else if (knockoutMatches.size() <= 1) knockoutStage = "FINAL";
    }
    
    private void checkKnockoutStageComplete() {
        for (Match m : knockoutMatches) {
            if (!m.isFinished()) return;
        }
        
        List<Player> winners = new ArrayList<>();
        for (Match m : knockoutMatches) {
            if (m.getPlayer1() != null && m.getScore1() > m.getScore2()) {
                winners.add(m.getPlayer1());
            } else if (m.getPlayer2() != null && m.getScore2() > m.getScore1()) {
                winners.add(m.getPlayer2());
            }
        }
        
        if (winners.size() <= 1) {
            finishTournament(winners.isEmpty() ? null : winners.get(0));
            return;
        }
        
        int matchId = brackets.size() + knockoutMatches.size() + 1;
        List<Match> nextRound = new ArrayList<>();
        for (int i = 0; i < winners.size() - 1; i += 2) {
            Player p1 = winners.get(i);
            Player p2 = (i + 1 < winners.size()) ? winners.get(i + 1) : null;
            Match m = new Match(matchId++, p1, p2, "Knockout");
            if (p2 == null) { m.setBye(true); m.setScore(1, 0); }
            nextRound.add(m);
        }
        knockoutMatches = nextRound;
        
        if (knockoutMatches.size() <= 1) knockoutStage = "FINAL";
        else if (knockoutMatches.size() <= 2) knockoutStage = "SEMI";
        else if (knockoutMatches.size() <= 4) knockoutStage = "QUARTER";
        else knockoutStage = "ROUND_16";
    }
    
    private void finishTournament(Player winner) {
        this.status = "FINISHED";
        this.knockoutStage = "FINISHED";
        if (winner != null) {
            this.winnerId = winner.getUserId();
            winner.setWins(winner.getWins() + 1);
        }
    }
    
    public String getGroupStandings() {
        if (!tournamentType.equals("FOOTBALL") || groups.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Group Standings:**\n\n");
        for (Map.Entry<String, List<Player>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<Player> groupPlayers = entry.getValue();
            sb.append("**Group ").append(groupName).append("**\n");
            sb.append("```\n");
            sb.append(String.format("%-20s %-8s %-8s %-8s %-8s\n", "Player", "P", "W", "D", "L"));
            sb.append("----------------------------------------\n");
            groupPlayers.sort((p1, p2) -> {
                int pts1 = points.getOrDefault(p1.getUserId(), 0);
                int pts2 = points.getOrDefault(p2.getUserId(), 0);
                return Integer.compare(pts2, pts1);
            });
            for (Player p : groupPlayers) {
                int pts = points.getOrDefault(p.getUserId(), 0);
                sb.append(String.format("%-20s %-8d %-8d %-8d %-8d\n", p.getUsername(), pts, p.getWins(), p.getDraws(), p.getLosses()));
            }
            sb.append("```\n\n");
        }
        return sb.toString();
    }
    
    public String getBracketsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Bracket:**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n");
        sb.append("Type: ").append(tournamentType).append("\n");
        sb.append("Status: ").append(status).append("\n\n");
        
        if (status.equals("WAITING")) {
            sb.append("⏳ Tournament is waiting to start. Use /starttournament to begin!\n");
            return sb.toString();
        }
        
        if (tournamentType.equals("FOOTBALL")) sb.append(getGroupStandings());
        
        if (!brackets.isEmpty()) {
            String sectionName = tournamentType.equals("FOOTBALL") ? "**📋 Group Matches:**\n\n" : "**📋 Matches:**\n\n";
            sb.append(sectionName);
            for (Match m : brackets) sb.append(m.toString()).append("\n");
        }
        
        if (!knockoutMatches.isEmpty()) {
            String stageName = "";
            switch (knockoutStage) {
                case "ROUND_16": stageName = "🏅 Round of 16"; break;
                case "QUARTER": stageName = "🏅 Quarterfinals"; break;
                case "SEMI": stageName = "🏅 Semifinals"; break;
                case "FINAL": stageName = "🏆 FINAL"; break;
                case "FINISHED": stageName = "🏆 Tournament Finished"; break;
                default: stageName = "🏅 Knockout Stage";
            }
            sb.append("\n**").append(stageName).append("**\n\n");
            for (Match m : knockoutMatches) sb.append(m.toString()).append("\n");
        }
        
        if (winnerId != null && status.equals("FINISHED")) {
            sb.append("\n**🏆 CHAMPION: <@").append(winnerId).append(">** 🎉\n");
        }
        return sb.toString();
    }
    
    public String getResultsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Results:**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n");
        sb.append("Type: ").append(tournamentType).append("\n\n");
        
        if (tournamentType.equals("FOOTBALL")) {
            sb.append(getGroupStandings());
            sb.append("\n");
        }
        
        sb.append("**📋 Matches:**\n\n");
        for (Match m : brackets) {
            if (m.isFinished()) sb.append(m.toString()).append("\n");
        }
        
        if (!knockoutMatches.isEmpty()) {
            sb.append("\n**📋 Knockout Matches:**\n\n");
            for (Match m : knockoutMatches) {
                if (m.isFinished()) sb.append(m.toString()).append("\n");
            }
        }
        
        if (winnerId != null && status.equals("FINISHED")) {
            sb.append("\n**🏆 CHAMPION: <@").append(winnerId).append(">** 🎉\n");
        }
        return sb.toString();
    }
    
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("**🏆 Tournament Information**\n\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Game: ").append(game).append("\n");
        sb.append("Type: ").append(tournamentType).append("\n");
        sb.append("Players: ").append(players.size()).append("/").append(maxPlayers).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Admin: <@").append(adminId).append(">\n\n");
        
        if (tournamentType.equals("FOOTBALL") && !groups.isEmpty()) sb.append(getGroupStandings());
        
        sb.append("\n**👤 Players:**\n");
        int i = 1;
        for (Player p : players.values()) {
            sb.append(i++).append(". <@").append(p.getUserId()).append(">");
            if (p.isAdmin()) sb.append(" (Admin)");
            if (tournamentType.equals("FOOTBALL")) {
                int pts = points.getOrDefault(p.getUserId(), 0);
                sb.append(" - ").append(pts).append(" pts");
            }
            sb.append("\n");
        }
        if (winnerId != null && status.equals("FINISHED")) {
            sb.append("\n**🏆 CHAMPION: <@").append(winnerId).append(">** 🎉\n");
        }
        return sb.toString();
    }
    
    // Getters & Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getGame() { return game; }
    public String getAdminId() { return adminId; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGuildId() { return guildId; }
    public Map<String, Player> getPlayers() { return players; }
    public List<Match> getBrackets() { return brackets; }
    public Map<String, Integer> getPoints() { return points; }
    public Map<String, List<Player>> getGroups() { return groups; }
    public List<Match> getKnockoutMatches() { return knockoutMatches; }
    public String getGroupStage() { return groupStage; }
    public void setGroupStage(String groupStage) { this.groupStage = groupStage; }
    public String getKnockoutStage() { return knockoutStage; }
    public void setKnockoutStage(String knockoutStage) { this.knockoutStage = knockoutStage; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getTournamentType() { return tournamentType; }
}