package com.tournamentbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentBot extends ListenerAdapter {
    
    // Token-i merret nga variablat e mjedisit (Railway Variables)
    private static final String TOKEN = System.getenv("BOT_TOKEN") != null 
        ? System.getenv("BOT_TOKEN") 
        : "YOUR_BOT_TOKEN_HERE";
    
    private static JDA jdaInstance;
    
    private static final Map<String, Tournament> tournaments = new ConcurrentHashMap<>();
    private static final Map<String, UserState> userStates = new ConcurrentHashMap<>();
    private static long tournamentCounter = 0;
    
    // ✅ Flag për të siguruar që komandat regjistrohen vetëm një herë
    private static boolean commandsRegistered = false;
    
    public static void main(String[] args) {
        try {
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
            System.out.println("========================================");
            
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
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
        // ✅ Regjistro komandat VETËM NJË HERË
        if (!commandsRegistered) {
            registerGlobalCommands(event.getJDA());
            commandsRegistered = true;
            
            // Gjithashtu regjistro për serverat ekzistues (për përshpejtim)
            for (Guild guild : event.getJDA().getGuilds()) {
                registerServerCommands(guild);
            }
            
            System.out.println("✅ Global commands registered successfully!");
            System.out.println("✅ Server commands registered on " + event.getJDA().getGuilds().size() + " servers!");
            System.out.println("========================================");
        }
    }
    
    /**
     * Regjistron komandat GLOBALISHT - shfaqen në të gjithë serverat
     * Vonesë: deri në 1 orë
     */
    private void registerGlobalCommands(JDA jda) {
        List<SlashCommandData> commands = new ArrayList<>();
        
        // Komandat publike
        commands.add(Commands.slash("help", "Show all available commands"));
        commands.add(Commands.slash("join", "Join an existing tournament"));
        commands.add(Commands.slash("list", "Show all active tournaments"));
        commands.add(Commands.slash("bracket", "Show tournament bracket"));
        commands.add(Commands.slash("results", "Show tournament results"));
        commands.add(Commands.slash("info", "Show tournament information"));
        commands.add(Commands.slash("leave", "Leave the tournament"));
        
        // Komandat admin
        commands.add(Commands.slash("newtournament", "Create a new tournament (Server Admins only)"));
        commands.add(Commands.slash("starttournament", "Start the tournament (Tournament Admin only)"));
        commands.add(Commands.slash("addplayer", "Add a player (Tournament Admin only)")
                .addOption(OptionType.USER, "user", "Player to add", true));
        commands.add(Commands.slash("setscore", "Set match score (Tournament Admin only)")
                .addOption(OptionType.INTEGER, "match_id", "Match ID", true)
                .addOption(OptionType.INTEGER, "score1", "Player 1 score", true)
                .addOption(OptionType.INTEGER, "score2", "Player 2 score", true));
        commands.add(Commands.slash("deletetournament", "Delete tournament (Server Admins only)"));
        
        // Regjistro komandat globalisht
        jda.updateCommands().addCommands(commands).queue(
            success -> System.out.println("✅ Global commands registered successfully!"),
            error -> System.err.println("❌ Error registering global commands: " + error.getMessage())
        );
    }
    
    /**
     * Regjistron komandat VETËM PËR NJË SERVER - shfaqen menjëherë
     * Përdoret për serverat ku bot-i është aktualisht
     */
    private void registerServerCommands(Guild guild) {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("help", "Show all available commands"));
        commands.add(Commands.slash("join", "Join an existing tournament"));
        commands.add(Commands.slash("list", "Show all active tournaments"));
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
        
        guild.updateCommands().addCommands(commands).queue(
            success -> System.out.println("✅ Server commands registered on: " + guild.getName()),
            error -> System.err.println("❌ Error registering server commands: " + error.getMessage())
        );
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        
        switch (command) {
            case "help": sendHelp(event); break;
            case "newtournament": newTournament(event); break;
            case "join": joinTournament(event); break;
            case "list": listTournaments(event); break;
            case "starttournament": startTournament(event); break;
            case "bracket": showBracket(event); break;
            case "results": showResults(event); break;
            case "info": showInfo(event); break;
            case "leave": leaveTournament(event); break;
            case "addplayer": addPlayer(event); break;
            case "setscore": setScore(event); break;
            case "deletetournament": deleteTournament(event); break;
            default: event.reply("Unknown command").queue(); break;
        }
    }
    
    private void sendHelp(SlashCommandInteractionEvent event) {
        String msg = "**Available Commands:**\n\n" +
                     "**Server Admin Commands:**\n" +
                     "/newtournament - Create new tournament\n" +
                     "/deletetournament - Delete tournament\n\n" +
                     "**Tournament Admin Commands:**\n" +
                     "/starttournament - Start tournament\n" +
                     "/addplayer @user - Add player\n" +
                     "/setscore match_id score1 score2 - Set score\n\n" +
                     "**Public Commands:**\n" +
                     "/join - Join tournament\n" +
                     "/list - List tournaments\n" +
                     "/bracket - Show bracket\n" +
                     "/results - Show results\n" +
                     "/info - Tournament info\n" +
                     "/leave - Leave tournament";
        event.reply(msg).queue();
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
        if (tournaments.isEmpty()) {
            event.reply("No active tournaments available.").queue();
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
            event.reply("No tournaments waiting.").queue();
            return;
        }
        
        msg.append("\nType the tournament number in chat:");
        event.reply(msg.toString()).queue();
        
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();
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
    
    private void listTournaments(SlashCommandInteractionEvent event) {
        if (tournaments.isEmpty()) {
            event.reply("No active tournaments.").queue();
            return;
        }
        
        StringBuilder msg = new StringBuilder("Active Tournaments:\n\n");
        for (Tournament t : tournaments.values()) {
            msg.append("Name: ").append(t.getName()).append("\n")
               .append("Game: ").append(t.getGame()).append("\n")
               .append("Players: ").append(t.getPlayers().size())
               .append("/").append(t.getMaxPlayers()).append("\n")
               .append("Status: ").append(t.getStatus()).append("\n")
               .append("Admin: <@").append(t.getAdminId()).append(">\n\n");
        }
        event.reply(msg.toString()).queue();
    }
    
    private void startTournament(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Tournament adminTournament = null;
        
        for (Tournament t : tournaments.values()) {
            if (t.getAdminId().equals(userId) && t.getStatus().equals("WAITING")) {
                adminTournament = t;
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
        
        event.reply("Tournament started!\n\nName: " + adminTournament.getName() + 
                   "\nPlayers: " + adminTournament.getPlayers().size() + 
                   "\n\nUse /bracket to see matches.\nUse /setscore to set results.").queue();
    }
    
    private void showBracket(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                if (t.getStatus().equals("IN_PROGRESS") || t.getStatus().equals("FINISHED")) {
                    event.reply(t.getBracketsString()).queue();
                    return;
                } else {
                    event.reply("Tournament has not started yet.").queue();
                    return;
                }
            }
        }
        event.reply("You are not part of any tournament.").queue();
    }
    
    private void showResults(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                event.reply(t.getResultsString()).queue();
                return;
            }
        }
        event.reply("You are not part of any tournament.").queue();
    }
    
    private void showInfo(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId)) {
                event.reply(t.getDetailedInfo()).queue();
                return;
            }
        }
        event.reply("You are not part of any tournament.").queue();
    }
    
    private void leaveTournament(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        for (Tournament t : tournaments.values()) {
            if (t.getPlayers().containsKey(userId) && !t.getAdminId().equals(userId)) {
                t.removePlayer(userId);
                event.reply("You left the tournament: " + t.getName()).queue();
                return;
            }
        }
        event.reply("You are not part of any tournament.").queue();
    }
    
    private void addPlayer(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Tournament adminTournament = null;
        
        for (Tournament t : tournaments.values()) {
            if (t.getAdminId().equals(userId) && t.getStatus().equals("WAITING")) {
                adminTournament = t;
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
        event.reply(targetName + " was added to " + adminTournament.getName() + 
                   "\nAdded by: <@" + userId + ">\nPlayers: " + 
                   adminTournament.getPlayers().size() + "/" + adminTournament.getMaxPlayers()).queue();
    }
    
    private void setScore(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Tournament adminTournament = null;
        
        for (Tournament t : tournaments.values()) {
            if (t.getAdminId().equals(userId) && t.getStatus().equals("IN_PROGRESS")) {
                adminTournament = t;
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
            event.reply("Score updated!\n\nTournament: " + adminTournament.getName() + 
                       "\nMatch " + matchId + ": " + score1 + " - " + score2 + 
                       "\nSet by: <@" + userId + ">").queue();
        } else {
            event.reply("Match with ID " + matchId + " not found.").queue();
        }
    }
    
    private void deleteTournament(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!\nThis command is only for Server Administrators.").queue();
            return;
        }
        
        String userId = event.getUser().getId();
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
            event.reply("You are not an admin of any tournament.").queue();
            return;
        }
        
        tournaments.remove(tournamentId);
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
            
            sendMessage(channelId, "✅ Game: " + game + "\n\nHow many players? (2-16):");
        }
        else if (step.equals("tournament_players")) {
            try {
                int max = Integer.parseInt(message);
                if (max < 2 || max > 16) {
                    sendMessage(channelId, "❌ Number must be 2-16. Try again:");
                    return;
                }
                
                tournamentCounter++;
                String tournamentId = String.valueOf(tournamentCounter);
                Tournament t = new Tournament(
                    tournamentId,
                    state.getTournamentName(),
                    state.getTournamentGame(),
                    state.getUserId(),
                    max
                );
                tournaments.put(tournamentId, t);
                userStates.remove(key);
                
                sendMessage(channelId, "✅ Tournament created!\n\nName: " + t.getName() + 
                           "\nGame: " + t.getGame() + "\nPlayers: 1/" + max + 
                           "\nAdmin: <@" + state.getUserId() + ">\n\nUse /join to invite players\nUse /starttournament to start");
            } catch (NumberFormatException e) {
                sendMessage(channelId, "❌ Enter a number (2-16):");
            }
        }
        else if (step.equals("join_tournament")) {
            try {
                int selected = Integer.parseInt(message);
                Map<Integer, String> tournamentMap = state.getTournamentMap();
                
                if (tournamentMap.containsKey(selected)) {
                    String tournamentId = tournamentMap.get(selected);
                    Tournament t = tournaments.get(tournamentId);
                    
                    if (t.getPlayers().size() >= t.getMaxPlayers()) {
                        sendMessage(channelId, "❌ Tournament is full!");
                        userStates.remove(key);
                        return;
                    }
                    
                    t.addPlayer(userId, "Player_" + userId);
                    userStates.remove(key);
                    
                    sendMessage(channelId, "✅ You joined!\n\nTournament: " + t.getName() + 
                               "\nPlayers: " + t.getPlayers().size() + "/" + t.getMaxPlayers());
                } else {
                    sendMessage(channelId, "❌ Invalid number. Try again:");
                }
            } catch (NumberFormatException e) {
                sendMessage(channelId, "❌ Enter a number:");
            }
        }
    }
    
    private static void sendMessage(String channelId, String message) {
        if (jdaInstance != null) {
            try {
                jdaInstance.getTextChannelById(channelId)
                    .sendMessage(message)
                    .queue();
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
        System.out.println("Message to channel " + channelId + ": " + message);
    }
}