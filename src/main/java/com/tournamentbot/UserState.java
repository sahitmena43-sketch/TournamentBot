package com.tournamentbot;

import java.util.Map;

public class UserState {
    private String step;
    private String userId;
    private String guildId;
    private String channelId;
    private String tournamentName;
    private String tournamentGame;
    private Map<Integer, String> tournamentMap;
    
    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getGuildId() { return guildId; }
    public void setGuildId(String guildId) { this.guildId = guildId; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public String getTournamentGame() { return tournamentGame; }
    public void setTournamentGame(String tournamentGame) { this.tournamentGame = tournamentGame; }
    public Map<Integer, String> getTournamentMap() { return tournamentMap; }
    public void setTournamentMap(Map<Integer, String> tournamentMap) { this.tournamentMap = tournamentMap; }
}