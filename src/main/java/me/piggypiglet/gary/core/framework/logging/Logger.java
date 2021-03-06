package me.piggypiglet.gary.core.framework.logging;

import lombok.Getter;
import me.piggypiglet.gary.core.objects.Constants;
import me.piggypiglet.gary.core.objects.enums.EventsEnum;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ------------------------------
// Copyright (c) PiggyPiglet 2018
// https://www.piggypiglet.me
// ------------------------------
public abstract class Logger {
    @Getter private final EventsEnum type;
    protected Guild guild;
    protected List<User> users = new ArrayList<>();
    protected List<TextChannel> textChannels = new ArrayList<>();
    protected List<VoiceChannel> voiceChannels = new ArrayList<>();
    protected List<Message> messages = new ArrayList<>();
    protected List<String> list = new ArrayList<>();
    protected List<Long> longs = new ArrayList<>();
    protected List<String> strings = new ArrayList<>();
    protected Object[] other;

    protected Logger(EventsEnum type) {
        this.type = type;
    }

    protected abstract MessageEmbed send() throws Exception;

    @SuppressWarnings("unchecked")
    public void log(JDA jda, Guild guild, Object... other) {
        this.guild = guild;
        this.other = other;

        Arrays.stream(other).forEach(obj -> {
            switch (obj.getClass().getSimpleName()) {
                case "UserImpl":
                    users.add((User) obj);
                    break;

                case "TextChannelImpl":
                    textChannels.add((TextChannel) obj);
                    break;

                case "VoiceChannelImpl":
                    voiceChannels.add((VoiceChannel) obj);
                    break;

                case "ReceivedMessage":
                    messages.add((Message) obj);
                    break;

                case "List":
                    for (Object object : (List<?>) obj) {
                        if (object instanceof String) {
                            list = (List<String>) obj;
                        }
                    }
                    break;

                case "Long":
                    longs.add((Long) obj);
                    break;

                case "String":
                    strings.add((String) obj);
                    break;
            }
        });

        try {
            MessageEmbed messageEmbed = send();
            clearAll();

            if (messageEmbed != null) {
                jda.getTextChannelById(Constants.LOG).sendMessage(messageEmbed).queue();
            }
        } catch (Exception e) {
            if (!(e instanceof InsufficientPermissionException)) {
                e.printStackTrace();
            }
        }
    }

    private void clearAll() {
        users.clear();
        textChannels.clear();
        voiceChannels.clear();
        messages.clear();
        list.clear();
        longs.clear();
        strings.clear();
        other = null;
    }
}
