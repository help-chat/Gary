package me.piggypiglet.gary.core.handlers.misc.services;

import me.piggypiglet.gary.core.handlers.GEvent;
import me.piggypiglet.gary.core.objects.enums.EventsEnum;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.ArrayList;
import java.util.List;

// ------------------------------
// Copyright (c) PiggyPiglet 2019
// https://www.piggypiglet.me
// ------------------------------
public final class RMSReactionHandler extends GEvent {
    public RMSReactionHandler() {
        super(EventsEnum.MESSAGE_REACTION_ADD);
    }

    @Override
    protected void execute(GenericEvent event) {
        GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) event;

        if (e.getChannel().getName().equalsIgnoreCase("rate-my-server") && !e.getUser().isBot()) {
            Message message = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
            List<MessageReaction> reactions = new ArrayList<>(message.getReactions());
            // this exception will never happen, so can be ignored
            //noinspection OptionalGetWithoutIsPresent
            reactions.remove(reactions.stream().filter(r -> r.toString().equals(e.getReaction().toString())).findFirst().get());

            if (reactions.stream().anyMatch(d -> d.retrieveUsers().complete().contains(e.getUser()))) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }
        }
    }
}
