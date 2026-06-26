package com.tournamentbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentBot extends ListenerAdapter {
    
    private static final String TOKEN = System.getenv("BOT_TOKEN") != null 
        ? System.getenv("BOT_TOKEN") 
        : "YOUR_BOT_TOKEN_HERE";
    
    private static JDA jdaInstance;
    
    private static final Map<String, Map<String, Tournament>> serverTournaments = new ConcurrentHashMap<>();
    private static final Map<String, UserState> userStates = new ConcurrentHashMap<>();
    private static long tournamentCounter = 0;
    private static boolean commandsRegistered = false;
    private static boolean dataLoaded = false;
    
    public static void main(String[] args) {
        try {
            DatabaseManager.connect();
            startHealthServer();
            
            jdaInstance = JDABuilder.createDefault(TOKEN)
                    .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS
                    )
                    .addEventListeners(new TournamentBot())
                    .addEventListeners(new MessageListener())
                    .build();
                    
            System.out.println("========================================");
            System.out.println("Tournament Bot is starting...");
            System.out.println("Bot is active!");
            System.out.println("Each server has its own tournaments!");
            System.out.println("========================================");
            
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseManager.close();
        }
    }
    
    private static void startHealthServer() {
        new Thread(() -> {
            try {
                com.sun.net.httpserver.HttpServer server = 
                    com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(8080), 0
                    );
                server.createContext("/", exchange -> {
                    String response = "Tournament Bot is alive!";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                });
                server.setExecutor(null);
                server.start();
                System.out.println("Health check server started on port 8080");
            } catch (Exception e) {
                System.err.println("Health server error: " + e.getMessage());
            }
        }).start();
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        if (!commandsRegistered) {
            registerGlobalCommands(event.getJDA());
            commandsRegistered = true;
            System.out.println("✅ Global commands registered successfully!");
        }
        
        if (!dataLoaded) {
            Map<String, Map<String, Tournament>> loaded = DatabaseManager.loadAllTournaments();
            serverTournaments.putAll(loaded);
            dataLoaded = true;
            
            int total = 0;
            for (Map<String, Tournament> map : serverTournaments.values()) {
                total += map.size();
            }
            System.out.println("✅ Loaded " + total + " tournaments from " + serverTournaments.size() + " servers!");
        }
        
        System.out.println("========================================");
    }
    
    private void registerGlobalCommands(JDA jda) {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("help", "Show all available commands"));
        commands.add(Commands.slash("join", "Join an existing tournament"));
        commands.add(Commands.slash("list", "Show all active tournaments in this server"));
        commands.add(Commands.slash("bracket", "Show tournament bracket"));
        commands.add(Commands.slash("results", "Show tournament results"));
        commands.add(Commands.slash("info", "Show tournament information"));
        commands.add(Commands.slash("leave", "Leave the tournament"));
        
        commands.add(Commands.slash("newtournament", "Create a new tournament (Server Admins only)"));
        commands.add(Commands.slash("starttournament", "Start the tournament (Tournament Admin only)"));
        commands.add(Commands.slash("addplayer", "Add a player (Tournament Admin only)")
                .addOption(OptionType.USER, "user", "Player to add", true));
        commands.add(Commands.slash("setscore", "Set match score (Tournament Admin only)")
                .addOption(OptionType.INTEGER, "match_id", "Match ID", true)
                .addOption(OptionType.INTEGER, "score1", "Player 1 score", true)
                .addOption(OptionType.INTEGER, "score2", "Player 2 score", true));
        commands.add(Commands.slash("deletetournament", "Delete tournament (Server Admins only)"));
        
        jda.updateCommands().addCommands(commands).queue(
            success -> System.out.println("✅ " + commands.size() + " global commands registered!"),
            error -> System.err.println("❌ Error: " + error.getMessage())
        );
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        String guildId = event.getGuild().getId();
        
        switch (command) {
            case "help": sendHelp(event); break;
            case "newtournament": newTournament(event); break;
            case "join": joinTournament(event); break;
            case "list": listTournaments(event, guildId); break;
            case "starttournament": startTournament(event, guildId); break;
            case "bracket": showBracket(event, guildId); break;
            case "results": showResults(event, guildId); break;
            case "info": showInfo(event, guildId); break;
            case "leave": leaveTournament(event, guildId); break;
            case "addplayer": addPlayer(event, guildId); break;
            case "setscore": setScore(event, guildId); break;
            case "deletetournament": deleteTournament(event, guildId); break;
            default: event.reply("Unknown command").queue(); break;
        }
    }
    
    private void sendHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Tournament Bot Commands");
        embed.setColor(Color.BLUE);
        embed.setFooter("Tournament Bot", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        
        embed.addField("🔒 Server Admin Commands",
                       "/newtournament - Create new tournament\n" +
                       "/deletetournament - Delete tournament", false);
        
        embed.addField("👑 Tournament Admin Commands",
                       "/starttournament - Start tournament\n" +
                       "/addplayer @user - Add player\n" +
                       "/setscore match_id score1 score2 - Set score", false);
        
        embed.addField("👥 Public Commands",
                       "/join - Join tournament\n" +
                       "/list - List tournaments in this server\n" +
                       "/bracket - Show bracket\n" +
                       "/results - Show results\n" +
                       "/info - Tournament info\n" +
                       "/leave - Leave tournament", false);
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void newTournament(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!\nThis command is only for Server Administrators.").queue();
            return;
        }
        
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();
        String channelId = event.getChannel().getId();
        String key = guildId + ":" + channelId;
        
        UserState state = new UserState();
        state.setStep("tournament_name");
        state.setUserId(userId);
        state.setGuildId(guildId);
        state.setChannelId(channelId);
        userStates.put(key, state);
        
        event.reply("Create New Tournament\n\nType the tournament name in chat:\nExample: Free Fire Cup 2026").queue();
    }
    
    private void joinTournament(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null || tournaments.isEmpty()) {
            event.reply("No active tournaments in this server.").queue();
            return;
        }
        
        StringBuilder msg = new StringBuilder("Select a tournament:\n\n");
        int i = 1;
        Map<Integer, String> tournamentMap = new HashMap<>();
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getStatus().equals("WAITING")) {
                msg.append(i).append(". ").append(t.getName()).append("\n");
                tournamentMap.put(i, entry.getKey());
                i++;
            }
        }
        
        if (i == 1) {
            event.reply("No tournaments waiting in this server.").queue();
            return;
        }
        
        msg.append("\nType the tournament number in chat:");
        event.reply(msg.toString()).queue();
        
        String userId = event.getUser().getId();
        String channelId = event.getChannel().getId();
        String key = guildId + ":" + channelId;
        
        UserState state = new UserState();
        state.setStep("join_tournament");
        state.setUserId(userId);
        state.setGuildId(guildId);
        state.setChannelId(channelId);
        state.setTournamentMap(tournamentMap);
        userStates.put(key, state);
    }
    
    private void listTournaments(SlashCommandInteractionEvent event, String guildId) {
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null || tournaments.isEmpty()) {
            event.reply("No active tournaments in this server.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Active Tournaments in this Server");
        embed.setColor(Color.BLUE);
        embed.setFooter("Tournament Bot", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        
        StringBuilder description = new StringBuilder();
        int count = 0;
        
        for (Tournament t : tournaments.values()) {
            count++;
            String statusEmoji = t.getStatus().equals("WAITING") ? "⏳" : 
                                t.getStatus().equals("IN_PROGRESS") ? "⚔️" : "✅";
            
            description.append("**").append(count).append(". ").append(t.getName()).append("**\n")
                       .append("🎮 **Game:** ").append(t.getGame()).append("\n")
                       .append("👥 **Players:** ").append(t.getPlayers().size())
                       .append("/").append(t.getMaxPlayers()).append("\n")
                       .append("📊 **Status:** ").append(statusEmoji).append(" ").append(t.getStatus()).append("\n")
                       .append("👑 **Admin:** <@").append(t.getAdminId()).append(">\n\n");
        }
        
        embed.setDescription(description.toString());
        embed.addField("📌 Total Tournaments", String.valueOf(count), false);
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void startTournament(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament adminTournament = null;
        String tournamentId = null;
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getAdminId().equals(userId) && t.getStatus().equals("WAITING")) {
                adminTournament = t;
                tournamentId = entry.getKey();
                break;
            }
        }
        
        if (adminTournament == null) {
            event.reply("You don't have permission to use this command!\nThis command is only for Tournament Administrators.").queue();
            return;
        }
        
        if (adminTournament.getPlayers().size() < 2) {
            event.reply("Need at least 2 players to start.\nCurrent players: " + adminTournament.getPlayers().size()).queue();
            return;
        }
        
        adminTournament.setStatus("IN_PROGRESS");
        adminTournament.generateBrackets();
        DatabaseManager.saveTournament(guildId, tournamentId, adminTournament);
        
        event.reply("Tournament started!\n\nName: " + adminTournament.getName() + 
                   "\nPlayers: " + adminTournament.getPlayers().size() + 
                   "\n\nUse /bracket to see matches.\nUse /setscore to set results.").queue();
    }
    
    private void showBracket(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament foundTournament = null;
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                foundTournament = t;
                break;
            }
        }
        
        if (foundTournament == null) {
            event.reply("You are not part of any tournament in this server.").queue();
            return;
        }
        
        if (foundTournament.getStatus().equals("WAITING")) {
            event.reply("Tournament has not started yet.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔀 Tournament Bracket - " + foundTournament.getName());
        embed.setColor(Color.ORANGE);
        embed.setFooter("Tournament Bot", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        
        embed.addField("🎮 Game", foundTournament.getGame(), true);
        embed.addField("📊 Status", foundTournament.getStatus(), true);
        embed.addField("👥 Players", String.valueOf(foundTournament.getPlayers().size()), true);
        
        if (foundTournament.getTournamentType().equals("FOOTBALL") && !foundTournament.getGroups().isEmpty()) {
            StringBuilder groupsInfo = new StringBuilder();
            for (Map.Entry<String, List<Player>> entry : foundTournament.getGroups().entrySet()) {
                groupsInfo.append("**Group ").append(entry.getKey()).append(":** ");
                List<String> names = new ArrayList<>();
                for (Player p : entry.getValue()) {
                    names.add("<@" + p.getUserId() + ">");
                }
                groupsInfo.append(String.join(", ", names)).append("\n");
            }
            embed.addField("📋 Groups", groupsInfo.toString(), false);
        }
        
        if (!foundTournament.getBrackets().isEmpty()) {
            StringBuilder matches = new StringBuilder();
            int matchCount = 0;
            for (Match m : foundTournament.getBrackets()) {
                matchCount++;
                matches.append(m.toString()).append("\n");
                if (matchCount >= 15) {
                    matches.append("... and more matches");
                    break;
                }
            }
            embed.addField("📋 Matches", matches.toString(), false);
        }
        
        if (!foundTournament.getKnockoutMatches().isEmpty()) {
            StringBuilder knockout = new StringBuilder();
            int matchCount = 0;
            for (Match m : foundTournament.getKnockoutMatches()) {
                matchCount++;
                knockout.append(m.toString()).append("\n");
                if (matchCount >= 10) {
                    knockout.append("... and more");
                    break;
                }
            }
            embed.addField("🏅 Knockout Matches", knockout.toString(), false);
        }
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void showResults(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament foundTournament = null;
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                foundTournament = t;
                break;
            }
        }
        
        if (foundTournament == null) {
            event.reply("You are not part of any tournament in this server.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Tournament Results - " + foundTournament.getName());
        embed.setColor(Color.YELLOW);
        embed.setFooter("Tournament Bot", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        
        embed.addField("🎮 Game", foundTournament.getGame(), true);
        embed.addField("📊 Status", foundTournament.getStatus(), true);
        embed.addField("👥 Total Players", String.valueOf(foundTournament.getPlayers().size()), true);
        
        // ✅ KORRIGJUAR - përdor variabël final
        List<Player> sortedPlayers = new ArrayList<>(foundTournament.getPlayers().values());
        final Tournament finalTournament = foundTournament;
        
        if (foundTournament.getTournamentType().equals("FOOTBALL")) {
            sortedPlayers.sort((p1, p2) -> {
                return Integer.compare(
                    finalTournament.getPoints().getOrDefault(p2.getUserId(), 0),
                    finalTournament.getPoints().getOrDefault(p1.getUserId(), 0)
                );
            });
        } else {
            sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getWins(), p1.getWins()));
        }
        
        StringBuilder ranking = new StringBuilder();
        int rank = 1;
        for (Player p : sortedPlayers) {
            String pts = foundTournament.getTournamentType().equals("FOOTBALL") ? 
                        " - " + foundTournament.getPoints().getOrDefault(p.getUserId(), 0) + " pts" : 
                        " - " + p.getWins() + " wins";
            ranking.append(rank++).append(". <@").append(p.getUserId()).append(">").append(pts).append("\n");
            if (rank > 20) {
                ranking.append("... and ").append(sortedPlayers.size() - 20).append(" more");
                break;
            }
        }
        embed.addField("📊 Ranking", ranking.toString(), false);
        
        if (!foundTournament.getBrackets().isEmpty()) {
            StringBuilder finishedMatches = new StringBuilder();
            int matchCount = 0;
            for (Match m : foundTournament.getBrackets()) {
                if (m.isFinished()) {
                    matchCount++;
                    finishedMatches.append(m.toString()).append("\n");
                    if (matchCount >= 10) break;
                }
            }
            if (matchCount > 0) {
                embed.addField("✅ Finished Matches", finishedMatches.toString(), false);
            }
        }
        
        if (!foundTournament.getKnockoutMatches().isEmpty()) {
            StringBuilder knockoutMatches = new StringBuilder();
            int matchCount = 0;
            for (Match m : foundTournament.getKnockoutMatches()) {
                if (m.isFinished()) {
                    matchCount++;
                    knockoutMatches.append(m.toString()).append("\n");
                    if (matchCount >= 10) break;
                }
            }
            if (matchCount > 0) {
                embed.addField("🏅 Knockout Matches", knockoutMatches.toString(), false);
            }
        }
        
        if (foundTournament.getWinnerId() != null && foundTournament.getStatus().equals("FINISHED")) {
            embed.addField("🏆 CHAMPION", "<@" + foundTournament.getWinnerId() + "> 🎉", false);
        }
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void showInfo(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament foundTournament = null;
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                foundTournament = t;
                break;
            }
        }
        
        if (foundTournament == null) {
            event.reply("You are not part of any tournament in this server.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Tournament Information");
        embed.setColor(Color.GREEN);
        embed.setFooter("Tournament Bot", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        
        String statusEmoji = foundTournament.getStatus().equals("WAITING") ? "⏳" : 
                            foundTournament.getStatus().equals("IN_PROGRESS") ? "⚔️" : "✅";
        
        embed.addField("📌 Name", foundTournament.getName(), true);
        embed.addField("🎮 Game", foundTournament.getGame(), true);
        embed.addField("📊 Status", statusEmoji + " " + foundTournament.getStatus(), true);
        embed.addField("👥 Players", foundTournament.getPlayers().size() + "/" + foundTournament.getMaxPlayers(), true);
        embed.addField("👑 Admin", "<@" + foundTournament.getAdminId() + ">", true);
        embed.addField("🏷️ Type", foundTournament.getTournamentType(), true);
        
        StringBuilder playersList = new StringBuilder();
        int i = 1;
        for (Player p : foundTournament.getPlayers().values()) {
            String role = p.isAdmin() ? " 👑" : "";
            playersList.append(i++).append(". <@").append(p.getUserId()).append(">").append(role).append("\n");
            if (i > 20) {
                playersList.append("... and ").append(foundTournament.getPlayers().size() - 20).append(" more");
                break;
            }
        }
        embed.addField("👤 Players List", playersList.toString(), false);
        
        if (foundTournament.getTournamentType().equals("FOOTBALL") && !foundTournament.getGroups().isEmpty()) {
            StringBuilder groupsInfo = new StringBuilder();
            for (Map.Entry<String, List<Player>> entry : foundTournament.getGroups().entrySet()) {
                groupsInfo.append("**Group ").append(entry.getKey()).append(":** ");
                List<String> names = new ArrayList<>();
                for (Player p : entry.getValue()) {
                    names.add("<@" + p.getUserId() + ">");
                }
                groupsInfo.append(String.join(", ", names)).append("\n");
            }
            embed.addField("📋 Groups", groupsInfo.toString(), false);
        }
        
        if (foundTournament.getTournamentType().equals("FOOTBALL") && !foundTournament.getPoints().isEmpty()) {
            StringBuilder pointsInfo = new StringBuilder();
            List<Map.Entry<String, Integer>> sortedPoints = new ArrayList<>(foundTournament.getPoints().entrySet());
            sortedPoints.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            for (Map.Entry<String, Integer> entry : sortedPoints) {
                pointsInfo.append("<@").append(entry.getKey()).append("> - ").append(entry.getValue()).append(" pts\n");
                if (pointsInfo.length() > 1000) break;
            }
            embed.addField("📊 Points", pointsInfo.toString(), false);
        }
        
        if (foundTournament.getWinnerId() != null && foundTournament.getStatus().equals("FINISHED")) {
            embed.addField("🏆 CHAMPION", "<@" + foundTournament.getWinnerId() + "> 🎉", false);
        }
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void leaveTournament(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        String tournamentId = null;
        Tournament foundTournament = null;
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getPlayers().containsKey(userId) && !t.getAdminId().equals(userId)) {
                foundTournament = t;
                tournamentId = entry.getKey();
                break;
            }
        }
        
        if (foundTournament == null) {
            event.reply("You are not part of any tournament in this server.").queue();
            return;
        }
        
        foundTournament.removePlayer(userId);
        removeRoleFromPlayer(guildId, userId, foundTournament.getGame());
        DatabaseManager.saveTournament(guildId, tournamentId, foundTournament);
        
        event.reply("✅ You left the tournament: " + foundTournament.getName()).queue();
    }
    
    private void addPlayer(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament adminTournament = null;
        String tournamentId = null;
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getAdminId().equals(userId) && t.getStatus().equals("WAITING")) {
                adminTournament = t;
                tournamentId = entry.getKey();
                break;
            }
        }
        
        if (adminTournament == null) {
            event.reply("You don't have permission to use this command!\nThis command is only for Tournament Administrators.").queue();
            return;
        }
        
        String targetUserId = event.getOption("user").getAsUser().getId();
        String targetName = event.getOption("user").getAsUser().getName();
        
        if (adminTournament.getPlayers().containsKey(targetUserId)) {
            event.reply(targetName + " is already in the tournament!").queue();
            return;
        }
        
        if (adminTournament.getPlayers().size() >= adminTournament.getMaxPlayers()) {
            event.reply("Tournament is full! (" + adminTournament.getMaxPlayers() + "/" + adminTournament.getMaxPlayers() + ")").queue();
            return;
        }
        
        adminTournament.addPlayer(targetUserId, targetName);
        DatabaseManager.saveTournament(guildId, tournamentId, adminTournament);
        
        event.reply(targetName + " was added to " + adminTournament.getName() + 
                   "\nAdded by: <@" + userId + ">\nPlayers: " + 
                   adminTournament.getPlayers().size() + "/" + adminTournament.getMaxPlayers()).queue();
    }
    
    private void setScore(SlashCommandInteractionEvent event, String guildId) {
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        Tournament adminTournament = null;
        String tournamentId = null;
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getAdminId().equals(userId) && t.getStatus().equals("IN_PROGRESS")) {
                adminTournament = t;
                tournamentId = entry.getKey();
                break;
            }
        }
        
        if (adminTournament == null) {
            event.reply("You don't have permission to use this command!\nThis command is only for Tournament Administrators.").queue();
            return;
        }
        
        int matchId = event.getOption("match_id").getAsInt();
        int score1 = event.getOption("score1").getAsInt();
        int score2 = event.getOption("score2").getAsInt();
        
        if (score1 < 0 || score2 < 0) {
            event.reply("Scores cannot be negative!").queue();
            return;
        }
        
        boolean found = adminTournament.setMatchScore(matchId, score1, score2);
        if (found) {
            DatabaseManager.saveTournament(guildId, tournamentId, adminTournament);
            event.reply("Score updated!\n\nTournament: " + adminTournament.getName() + 
                       "\nMatch " + matchId + ": " + score1 + " - " + score2 + 
                       "\nSet by: <@" + userId + ">").queue();
            
            if (adminTournament.getStatus().equals("FINISHED") && adminTournament.getWinnerId() != null) {
                giveChampionRole(guildId, adminTournament.getWinnerId(), adminTournament.getGame());
            }
        } else {
            event.reply("Match with ID " + matchId + " not found.").queue();
        }
    }
    
    private void deleteTournament(SlashCommandInteractionEvent event, String guildId) {
        Member member = event.getMember();
        
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!\nThis command is only for Server Administrators.").queue();
            return;
        }
        
        String userId = event.getUser().getId();
        Map<String, Tournament> tournaments = serverTournaments.get(guildId);
        
        if (tournaments == null) {
            event.reply("No tournaments in this server.").queue();
            return;
        }
        
        String tournamentId = null;
        String tournamentName = null;
        
        for (Map.Entry<String, Tournament> entry : tournaments.entrySet()) {
            Tournament t = entry.getValue();
            if (t.getAdminId().equals(userId)) {
                tournamentId = entry.getKey();
                tournamentName = t.getName();
                break;
            }
        }
        
        if (tournamentId == null) {
            event.reply("You are not an admin of any tournament in this server.").queue();
            return;
        }
        
        tournaments.remove(tournamentId);
        DatabaseManager.deleteTournament(guildId, tournamentId);
        
        event.reply("Tournament deleted!\n\nName: " + tournamentName + "\nDeleted by: <@" + userId + ">").queue();
    }
    
    public static void handleUserInput(String guildId, String channelId, String userId, String message) {
        String key = guildId + ":" + channelId;
        
        if (!userStates.containsKey(key)) return;
        
        UserState state = userStates.get(key);
        if (!state.getUserId().equals(userId)) return;
        
        String step = state.getStep();
        
        if (step.equals("tournament_name")) {
            state.setTournamentName(message);
            state.setStep("tournament_game");
            
            String msg = "✅ Name: " + message + "\n\nChoose game (type number):\n1 - Free Fire\n2 - Dream League Soccer 2026\n3 - FC Mobile\n4 - Other (type name)";
            sendMessage(channelId, msg);
        }
        else if (step.equals("tournament_game")) {
            String game = message;
            if (message.equals("1")) game = "Free Fire";
            else if (message.equals("2")) game = "Dream League Soccer 2026";
            else if (message.equals("3")) game = "FC Mobile";
            
            state.setTournamentGame(game);
            state.setStep("tournament_players");
            
            sendMessage(channelId, "✅ Game: " + game + "\n\nHow many players? (Minimum 2):");
        }
        else if (step.equals("tournament_players")) {
            try {
                int max = Integer.parseInt(message);
                if (max < 2) {
                    sendMessage(channelId, "❌ Number must be at least 2. Try again:");
                    return;
                }
                
                tournamentCounter++;
                String tournamentId = String.valueOf(tournamentCounter);
                Tournament t = new Tournament(
                    tournamentId,
                    state.getTournamentName(),
                    state.getTournamentGame(),
                    state.getUserId(),
                    max,
                    guildId
                );
                
                serverTournaments.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>())
                                 .put(tournamentId, t);
                
                userStates.remove(key);
                DatabaseManager.saveTournament(guildId, tournamentId, t);
                
                sendMessage(channelId, "✅ Tournament created!\n\nName: " + t.getName() + 
                           "\nGame: " + t.getGame() + "\nPlayers: 1/" + max + 
                           "\nAdmin: <@" + state.getUserId() + ">\n\nUse /join to invite players\nUse /starttournament to start");
            } catch (NumberFormatException e) {
                sendMessage(channelId, "❌ Enter a number:");
            }
        }
        else if (step.equals("join_tournament")) {
            try {
                int selected = Integer.parseInt(message);
                Map<Integer, String> tournamentMap = state.getTournamentMap();
                
                if (tournamentMap.containsKey(selected)) {
                    String tournamentId = tournamentMap.get(selected);
                    Map<String, Tournament> tournaments = serverTournaments.get(guildId);
                    
                    if (tournaments == null) {
                        sendMessage(channelId, "❌ Tournament not found!");
                        userStates.remove(key);
                        return;
                    }
                    
                    Tournament t = tournaments.get(tournamentId);
                    
                    if (t == null) {
                        sendMessage(channelId, "❌ Tournament not found!");
                        userStates.remove(key);
                        return;
                    }
                    
                    if (t.getPlayers().size() >= t.getMaxPlayers()) {
                        sendMessage(channelId, "❌ Tournament is full!");
                        userStates.remove(key);
                        return;
                    }
                    
                    t.addPlayer(userId, "Player_" + userId);
                    userStates.remove(key);
                    
                    addRoleToPlayer(guildId, userId, t.getGame());
                    DatabaseManager.saveTournament(guildId, tournamentId, t);
                    
                    String msg = "✅ You joined!\n\n" +
                                 "Tournament: " + t.getName() + "\n" +
                                 "Players: " + t.getPlayers().size() + "/" + t.getMaxPlayers();
                    sendMessage(channelId, msg);
                } else {
                    sendMessage(channelId, "❌ Invalid number. Try again:");
                }
            } catch (NumberFormatException e) {
                sendMessage(channelId, "❌ Enter a number:");
            }
        }
    }
    
    // ==================== METODAT PËR ROLE ====================
    
    private static boolean isFootballGame(String game) {
        String gameLower = game.toLowerCase();
        return gameLower.contains("dream league") || 
               gameLower.contains("dls") || 
               gameLower.contains("fc mobile") || 
               gameLower.contains("fifa") || 
               gameLower.contains("efootball") ||
               gameLower.contains("pes") ||
               gameLower.equals("football") ||
               gameLower.equals("soccer");
    }
    
    private static String getRoleName(String game) {
        String gameLower = game.toLowerCase();
        if (gameLower.contains("dream league") || gameLower.contains("dls")) {
            return "DLS PLAYER";
        } else if (gameLower.contains("fc mobile")) {
            return "FC MOBILE PLAYER";
        } else if (gameLower.contains("fifa")) {
            return "FIFA PLAYER";
        } else {
            return game.toUpperCase() + " PLAYER";
        }
    }
    
    private static Role getOrCreateRole(Guild guild, String roleName) {
        List<Role> roles = guild.getRolesByName(roleName, true);
        if (!roles.isEmpty()) {
            return roles.get(0);
        }
        return guild.createRole()
                .setName(roleName)
                .setMentionable(true)
                .complete();
    }
    
    private static void addRoleToPlayer(String guildId, String userId, String game) {
        try {
            if (!isFootballGame(game)) {
                System.out.println("⚠️ No role assigned for " + game + " (only DLS and FC Mobile get roles)");
                return;
            }
            
            String roleName = getRoleName(game);
            Guild guild = jdaInstance.getGuildById(guildId);
            if (guild == null) return;
            
            Member member = guild.getMemberById(userId);
            if (member == null) return;
            
            Role role = getOrCreateRole(guild, roleName);
            guild.addRoleToMember(member, role).queue(
                success -> System.out.println("✅ Added role " + roleName + " to " + member.getUser().getName()),
                error -> System.err.println("❌ Failed to add role: " + error.getMessage())
            );
        } catch (Exception e) {
            System.err.println("❌ Error adding role: " + e.getMessage());
        }
    }
    
    private static void removeRoleFromPlayer(String guildId, String userId, String game) {
        try {
            if (!isFootballGame(game)) return;
            
            String roleName = getRoleName(game);
            Guild guild = jdaInstance.getGuildById(guildId);
            if (guild == null) return;
            
            Member member = guild.getMemberById(userId);
            if (member == null) return;
            
            List<Role> roles = guild.getRolesByName(roleName, true);
            if (roles.isEmpty()) return;
            
            guild.removeRoleFromMember(member, roles.get(0)).queue(
                success -> System.out.println("✅ Removed role " + roleName + " from " + member.getUser().getName()),
                error -> System.err.println("❌ Failed to remove role: " + error.getMessage())
            );
        } catch (Exception e) {
            System.err.println("❌ Error removing role: " + e.getMessage());
        }
    }
    
    private static void giveChampionRole(String guildId, String userId, String game) {
        try {
            if (!isFootballGame(game)) {
                System.out.println("⚠️ No champion role for " + game);
                return;
            }
            
            String roleName = game + " CHAMPION";
            Guild guild = jdaInstance.getGuildById(guildId);
            if (guild == null) return;
            
            Member member = guild.getMemberById(userId);
            if (member == null) return;
            
            Role role = getOrCreateRole(guild, roleName);
            if (role != null && !role.getColor().equals(Color.YELLOW)) {
                role.getManager().setColor(Color.YELLOW).queue();
            }
            
            guild.addRoleToMember(member, role).queue(
                success -> {
                    System.out.println("✅ Added Champion role " + roleName + " to " + member.getUser().getName());
                    String channelId = getActiveChannelId(guildId);
                    if (channelId != null) {
                        sendMessage(channelId, "🏆 **" + member.getUser().getName() + "** është **" + game + " CHAMPION**! 🎉👑");
                    }
                },
                error -> System.err.println("❌ Failed to add Champion role: " + error.getMessage())
            );
        } catch (Exception e) {
            System.err.println("❌ Error giving Champion role: " + e.getMessage());
        }
    }
    
    private static String getActiveChannelId(String guildId) {
        try {
            Guild guild = jdaInstance.getGuildById(guildId);
            if (guild != null && !guild.getTextChannels().isEmpty()) {
                return guild.getTextChannels().get(0).getId();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting active channel: " + e.getMessage());
        }
        return null;
    }
    
    private static void sendMessage(String channelId, String message) {
        if (jdaInstance != null) {
            try {
                jdaInstance.getTextChannelById(channelId).sendMessage(message).queue();
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }
}