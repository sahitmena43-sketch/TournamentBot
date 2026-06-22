package com.tournamentbot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();
        String guildId = event.getGuild().getId();
        String channelId = event.getChannel().getId();
        
        TournamentBot.handleUserInput(guildId, channelId, userId, message);
    }
}