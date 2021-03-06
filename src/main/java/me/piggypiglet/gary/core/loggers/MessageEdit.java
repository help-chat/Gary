package me.piggypiglet.gary.core.loggers;

import me.piggypiglet.gary.core.framework.logging.Logger;
import me.piggypiglet.gary.core.objects.Constants;
import me.piggypiglet.gary.core.objects.enums.EventsEnum;
import me.piggypiglet.gary.core.utils.mysql.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.ZonedDateTime;

// ------------------------------
// Copyright (c) PiggyPiglet 2018
// https://www.piggypiglet.me
// ------------------------------
public final class MessageEdit extends Logger {
    public MessageEdit() {
        super(EventsEnum.MESSAGE_EDIT);
    }

    @Override
    protected MessageEmbed send() {
        User user = users.get(0);
        TextChannel channel = textChannels.get(0);
        Message message = messages.get(0);

        return new EmbedBuilder()
                .setAuthor(user.getName() + "#" + user.getDiscriminator(), null, user.getEffectiveAvatarUrl())
                .setColor(Constants.BLUE)
                .setDescription("**[Message](" + message.getJumpUrl() + ") edited in " + channel.getAsMention() + "**")
                .addField("Before", MessageUtils.getPreviousMessage(message.getIdLong()), false)
                .addField("After", message.getContentRaw().length() >= 229 ? message.getContentRaw().substring(0, 229) + "..." : message.getContentRaw(), false)
                .setFooter("User ID: " + user.getId() , null)
                .setTimestamp(ZonedDateTime.now())
                .build();
    }
}
