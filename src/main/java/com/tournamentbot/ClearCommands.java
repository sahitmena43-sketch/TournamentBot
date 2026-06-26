package com.tournamentbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class ClearCommands {
    public static void main(String[] args) {
        String token = System.getenv("BOT_TOKEN");
        if (token == null) {
            token = "YOUR_BOT_TOKEN_HERE";
        }
        
        try {
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build();
            
            jda.updateCommands().queue(
                success -> System.out.println("✅ Global commands cleared!"),
                error -> System.out.println("❌ Error: " + error.getMessage())
            );
            
            jda.getGuilds().forEach(guild -> {
                guild.updateCommands().queue(
                    s -> System.out.println("✅ Cleared commands for: " + guild.getName()),
                    e -> System.out.println("❌ Error for " + guild.getName() + ": " + e.getMessage())
                );
            });
            
            Thread.sleep(5000);
            jda.shutdown();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}