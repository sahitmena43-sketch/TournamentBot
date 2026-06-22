package com.tournamentbot;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static Connection connection;
    
    public static void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:tournaments.db");
            createTables();
            System.out.println("✅ Database connected successfully!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }
    
    private static void createTables() throws SQLException {
        String sqlTournaments = "CREATE TABLE IF NOT EXISTS tournaments (" +
                                "id TEXT PRIMARY KEY, " +
                                "name TEXT, " +
                                "game TEXT, " +
                                "adminId TEXT, " +
                                "maxPlayers INTEGER, " +
                                "status TEXT, " +
                                "groupStage TEXT, " +
                                "knockoutStage TEXT, " +
                                "winnerId TEXT, " +
                                "guildId TEXT)";
        connection.createStatement().execute(sqlTournaments);
        
        String sqlPlayers = "CREATE TABLE IF NOT EXISTS players (" +
                            "tournamentId TEXT, " +
                            "userId TEXT, " +
                            "username TEXT, " +
                            "isAdmin INTEGER, " +
                            "wins INTEGER, " +
                            "draws INTEGER, " +
                            "losses INTEGER, " +
                            "points INTEGER, " +
                            "goalsFor INTEGER, " +
                            "goalsAgainst INTEGER, " +
                            "PRIMARY KEY (tournamentId, userId), " +
                            "FOREIGN KEY (tournamentId) REFERENCES tournaments(id))";
        connection.createStatement().execute(sqlPlayers);
        
        String sqlPoints = "CREATE TABLE IF NOT EXISTS points (" +
                           "tournamentId TEXT, " +
                           "userId TEXT, " +
                           "points INTEGER, " +
                           "PRIMARY KEY (tournamentId, userId), " +
                           "FOREIGN KEY (tournamentId) REFERENCES tournaments(id))";
        connection.createStatement().execute(sqlPoints);
        
        String sqlGroups = "CREATE TABLE IF NOT EXISTS groups (" +
                           "tournamentId TEXT, " +
                           "groupName TEXT, " +
                           "userId TEXT, " +
                           "PRIMARY KEY (tournamentId, groupName, userId), " +
                           "FOREIGN KEY (tournamentId) REFERENCES tournaments(id))";
        connection.createStatement().execute(sqlGroups);
        
        String sqlMatches = "CREATE TABLE IF NOT EXISTS matches (" +
                            "tournamentId TEXT, " +
                            "matchId INTEGER, " +
                            "player1Id TEXT, " +
                            "player2Id TEXT, " +
                            "score1 INTEGER, " +
                            "score2 INTEGER, " +
                            "finished INTEGER, " +
                            "bye INTEGER, " +
                            "matchGroup TEXT, " +
                            "isKnockout INTEGER, " +
                            "PRIMARY KEY (tournamentId, matchId), " +
                            "FOREIGN KEY (tournamentId) REFERENCES tournaments(id))";
        connection.createStatement().execute(sqlMatches);
    }
    
    public static void saveTournament(Tournament t) {
        try {
            String sql = "INSERT OR REPLACE INTO tournaments (id, name, game, adminId, maxPlayers, status, groupStage, knockoutStage, winnerId, guildId) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, t.getId());
            stmt.setString(2, t.getName());
            stmt.setString(3, t.getGame());
            stmt.setString(4, t.getAdminId());
            stmt.setInt(5, t.getMaxPlayers());
            stmt.setString(6, t.getStatus());
            stmt.setString(7, t.getGroupStage());
            stmt.setString(8, t.getKnockoutStage());
            stmt.setString(9, t.getWinnerId());
            stmt.setString(10, t.getGuildId());
            stmt.executeUpdate();
            
            for (Player p : t.getPlayers().values()) {
                String sqlPlayer = "INSERT OR REPLACE INTO players (tournamentId, userId, username, isAdmin, wins, draws, losses, points, goalsFor, goalsAgainst) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(sqlPlayer);
                pstmt.setString(1, t.getId());
                pstmt.setString(2, p.getUserId());
                pstmt.setString(3, p.getUsername());
                pstmt.setInt(4, p.isAdmin() ? 1 : 0);
                pstmt.setInt(5, p.getWins());
                pstmt.setInt(6, p.getDraws());
                pstmt.setInt(7, p.getLosses());
                pstmt.setInt(8, p.getPoints());
                pstmt.setInt(9, p.getGoalsFor());
                pstmt.setInt(10, p.getGoalsAgainst());
                pstmt.executeUpdate();
            }
            
            for (Map.Entry<String, Integer> entry : t.getPoints().entrySet()) {
                String sqlPoints = "INSERT OR REPLACE INTO points (tournamentId, userId, points) VALUES (?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(sqlPoints);
                pstmt.setString(1, t.getId());
                pstmt.setString(2, entry.getKey());
                pstmt.setInt(3, entry.getValue());
                pstmt.executeUpdate();
            }
            
            for (Map.Entry<String, List<Player>> entry : t.getGroups().entrySet()) {
                String groupName = entry.getKey();
                for (Player p : entry.getValue()) {
                    String sqlGroup = "INSERT OR REPLACE INTO groups (tournamentId, groupName, userId) VALUES (?, ?, ?)";
                    PreparedStatement pstmt = connection.prepareStatement(sqlGroup);
                    pstmt.setString(1, t.getId());
                    pstmt.setString(2, groupName);
                    pstmt.setString(3, p.getUserId());
                    pstmt.executeUpdate();
                }
            }
            
            int matchId = 1;
            for (Match m : t.getBrackets()) {
                saveMatch(t.getId(), m, matchId++, false);
            }
            for (Match m : t.getKnockoutMatches()) {
                saveMatch(t.getId(), m, matchId++, true);
            }
            
            System.out.println("✅ Tournament saved: " + t.getName());
            
        } catch (SQLException e) {
            System.err.println("❌ Error saving tournament: " + e.getMessage());
        }
    }
    
    private static void saveMatch(String tournamentId, Match m, int matchId, boolean isKnockout) throws SQLException {
        String sql = "INSERT OR REPLACE INTO matches (tournamentId, matchId, player1Id, player2Id, score1, score2, finished, bye, matchGroup, isKnockout) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, tournamentId);
        stmt.setInt(2, matchId);
        stmt.setString(3, m.getPlayer1() != null ? m.getPlayer1().getUserId() : null);
        stmt.setString(4, m.getPlayer2() != null ? m.getPlayer2().getUserId() : null);
        stmt.setInt(5, m.getScore1());
        stmt.setInt(6, m.getScore2());
        stmt.setInt(7, m.isFinished() ? 1 : 0);
        stmt.setInt(8, m.isBye() ? 1 : 0);
        stmt.setString(9, m.getGroup());
        stmt.setInt(10, isKnockout ? 1 : 0);
        stmt.executeUpdate();
    }
    
    public static Map<String, Tournament> loadTournaments() {
        Map<String, Tournament> loadedTournaments = new HashMap<>();
        
        try {
            String sql = "SELECT * FROM tournaments";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String game = rs.getString("game");
                String adminId = rs.getString("adminId");
                int maxPlayers = rs.getInt("maxPlayers");
                String status = rs.getString("status");
                String groupStage = rs.getString("groupStage");
                String knockoutStage = rs.getString("knockoutStage");
                String winnerId = rs.getString("winnerId");
                String guildId = rs.getString("guildId");
                
                Tournament t = new Tournament(id, name, game, adminId, maxPlayers, guildId);
                t.setStatus(status);
                t.setGroupStage(groupStage);
                t.setKnockoutStage(knockoutStage);
                t.setWinnerId(winnerId);
                
                loadPlayers(t);
                loadPoints(t);
                loadGroups(t);
                loadMatches(t);
                
                loadedTournaments.put(id, t);
                System.out.println("✅ Tournament loaded: " + name);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error loading tournaments: " + e.getMessage());
        }
        
        return loadedTournaments;
    }
    
    private static void loadPlayers(Tournament t) throws SQLException {
        String sql = "SELECT * FROM players WHERE tournamentId = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, t.getId());
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            String userId = rs.getString("userId");
            String username = rs.getString("username");
            boolean isAdmin = rs.getInt("isAdmin") == 1;
            
            Player p = new Player(userId, username, isAdmin);
            p.setWins(rs.getInt("wins"));
            p.setDraws(rs.getInt("draws"));
            p.setLosses(rs.getInt("losses"));
            p.setPoints(rs.getInt("points"));
            p.setGoalsFor(rs.getInt("goalsFor"));
            p.setGoalsAgainst(rs.getInt("goalsAgainst"));
            
            t.getPlayers().put(userId, p);
        }
    }
    
    private static void loadPoints(Tournament t) throws SQLException {
        String sql = "SELECT * FROM points WHERE tournamentId = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, t.getId());
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            t.getPoints().put(rs.getString("userId"), rs.getInt("points"));
        }
    }
    
    private static void loadGroups(Tournament t) throws SQLException {
        String sql = "SELECT * FROM groups WHERE tournamentId = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, t.getId());
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            String groupName = rs.getString("groupName");
            String userId = rs.getString("userId");
            
            t.getGroups().computeIfAbsent(groupName, k -> new ArrayList<>());
            Player p = t.getPlayers().get(userId);
            if (p != null) {
                t.getGroups().get(groupName).add(p);
            }
        }
    }
    
    private static void loadMatches(Tournament t) throws SQLException {
        String sql = "SELECT * FROM matches WHERE tournamentId = ? ORDER BY isKnockout, matchId";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, t.getId());
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            String player1Id = rs.getString("player1Id");
            String player2Id = rs.getString("player2Id");
            int score1 = rs.getInt("score1");
            int score2 = rs.getInt("score2");
            boolean finished = rs.getInt("finished") == 1;
            boolean bye = rs.getInt("bye") == 1;
            String group = rs.getString("matchGroup");
            boolean isKnockout = rs.getInt("isKnockout") == 1;
            
            Player p1 = player1Id != null ? t.getPlayers().get(player1Id) : null;
            Player p2 = player2Id != null ? t.getPlayers().get(player2Id) : null;
            
            Match m = new Match(rs.getInt("matchId"), p1, p2, group);
            m.setScore(score1, score2);
            m.setFinished(finished);
            m.setBye(bye);
            
            if (isKnockout) {
                t.getKnockoutMatches().add(m);
            } else {
                t.getBrackets().add(m);
            }
        }
    }
    
    public static void deleteTournament(String tournamentId) {
        try {
            String sql = "DELETE FROM tournaments WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, tournamentId);
            stmt.executeUpdate();
            System.out.println("✅ Tournament deleted from database: " + tournamentId);
        } catch (SQLException e) {
            System.err.println("❌ Error deleting tournament: " + e.getMessage());
        }
    }
    
    public static void close() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("✅ Database closed.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing database: " + e.getMessage());
        }
    }
}