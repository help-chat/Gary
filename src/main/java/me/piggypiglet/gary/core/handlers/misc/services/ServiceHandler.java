package me.piggypiglet.gary.core.handlers.misc.services;

import com.google.inject.Inject;
import me.piggypiglet.gary.core.handlers.GEvent;
import me.piggypiglet.gary.core.objects.Constants;
import me.piggypiglet.gary.core.objects.enums.EventsEnum;
import me.piggypiglet.gary.core.objects.services.FormatScanner;
import me.piggypiglet.gary.core.objects.services.MinecraftServer;
import me.piggypiglet.gary.core.storage.file.Lang;
import me.piggypiglet.gary.core.utils.discord.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import sh.okx.timeapi.TimeAPI;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

// ------------------------------
// Copyright (c) PiggyPiglet 2018
// https://www.piggypiglet.me
// ------------------------------
public final class ServiceHandler extends GEvent {
    @Inject private ServiceBumpHandler serviceBumpHandler;

    public ServiceHandler() {
        super(EventsEnum.MESSAGE_CREATE, EventsEnum.MESSAGE_EDIT);
    }

    @Override
    protected void execute(GenericEvent event) {
        GenericGuildMessageEvent e = (GenericGuildMessageEvent) event;
        TextChannel channel = e.getChannel();
        Message message = channel.retrieveMessageById(e.getMessageId()).complete();
        User author = message.getAuthor();

        if (!author.isBot()) {
            FormatScanner scanner = new FormatScanner(message);
            String[] keys = null;

            switch (e.getChannel().getName()) {
                case "request-free":
                    keys = new String[]{"service", "request"};
                    break;

                case "request-paid":
                    keys = new String[]{"service", "request", "budget"};
                    break;

                case "rate-my-server":
                    if (!scanner.containsKeys("review") && !scanner.containsKeys("name", "description", "ip")) {
                        message.delete().queue();
                        sendError(author, channel, message.getContentRaw());
                    }

                    if (scanner.containsKeys("name", "description", "ip")) {
                        EmbedBuilder builder = scanner.toEmbed("name", "Description", "IP", "Website");
                        Map<String, String> values = scanner.getValues();
                        MinecraftServer server = new MinecraftServer(values.get("ip"));

                        if (server.isSuccess()) {
                            builder.addField("Extras:", "Premium: " + server.isPremium() + "\nVersion: " + server.getVersion(), false);
                            builder.setFooter("Posted by " + author.getName() + "#" + author.getDiscriminator(), null);

                            if (!server.getFavicon().equalsIgnoreCase("null")) {
                                builder.setThumbnail("attachment://server.png");
                                InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(server.getFavicon().replace("\n", "").split(",")[1]));
                                channel.sendFile(stream, "server.png").embed(builder.build()).queue(s -> Arrays.stream(Constants.RATINGS).forEach(em -> s.addReaction(e.getJDA().getEmoteById(em)).queue()));

                                try {
                                    stream.close();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            } else {
                                channel.sendMessage(builder.build()).queue(s -> Arrays.stream(Constants.RATINGS).forEach(em -> s.addReaction(e.getJDA().getEmoteById(em)).queue()));
                            }
                        } else {
                            String msg = Lang.getString("formats.rate-my-server.server-error", author.getAsMention());
                            MessageUtils.sendMessage(msg, author, channel, msg);
                        }

                        message.delete().queue();
                    }
                    return;
            }

            if (keys != null && !scanner.containsKeys(keys)) {
                message.delete().queue();
                sendError(author, channel, message.getContentRaw());
            } else {
                serviceBumpHandler.execute(e, keys);
            }
        }
    }

    private void sendError(User author, TextChannel channel, String message) {
        String name = channel.getName().toLowerCase();

        MessageUtils.sendMessageHaste(
                String.join("\n", Lang.getAlternateList("formats.error.message", channel.getAsMention(),
                        String.join("\n", Lang.getAlternateList("formats." + name + ".requirements")),
                        String.join("\n", Lang.getAlternateList("formats." + name + ".template")),
                        message)),
                author, channel,
                String.join("\n", Lang.getAlternateList("formats.error.backup-message", author.getAsMention(), channel.getAsMention())),
                new TimeAPI("30secs")
        );
    }
}
