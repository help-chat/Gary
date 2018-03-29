package me.piggypiglet.gary.core.utils.channel;

import com.google.inject.Inject;
import me.piggypiglet.gary.core.utils.mc.ServerInfoUtils;
import me.piggypiglet.gary.core.utils.web.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// ------------------------------
// Copyright (c) PiggyPiglet 2018
// https://www.piggypiglet.me
// ------------------------------
public class RMSUtils {

    @Inject private MessageUtils mutil;
    @Inject private WebUtils wutil;
    @Inject private ServerInfoUtils siutils;

    private boolean checkMessage(GenericMessageEvent e) {
        User author = null;
        Message message = null;

        if (e instanceof MessageReceivedEvent) {
            MessageReceivedEvent ev = (MessageReceivedEvent) e;
            author = ev.getAuthor();
            message = ev.getMessage();
        }

        if (e instanceof MessageUpdateEvent) {
            MessageUpdateEvent ev = (MessageUpdateEvent) e;
            author = ev.getAuthor();
            message = ev.getMessage();
        }

        if (author != null && message != null) {
            MessageChannel channel = e.getChannel();
            String msg = message.getContentRaw();

            List<String> items = new ArrayList<>();
            Stream.of("[name]", "[ip]", "[description]").forEach(items::add);

            if (mutil.contains(msg, items) || mutil.startsWith(msg, "[REVIEW]")) {
                System.out.println(author.getName() + "#" + author.getDiscriminator() + " has successfully created a request.");
                return true;
            } else {
                message.delete().queue();
                String requirements = "**Reviews** - The message must start with [REVIEW]\n" +
                        "**RMS Request** - You must have '[Name]' in your request.\n" +
                        "**RMS Request** - You must have '[IP]' in your request\n" +
                        "**RMS Request** - Not required, but we suggest having '[Website]' in your request.\n" +
                        "**RMS Request** - You must have '[Description]' in your request.";
                String exampleReview = "[REVIEW]\n" +
                        "message";
                String exampleRMS = "[Name] TestPlugins\n" +
                        "[IP] testplugins.com\n" +
                        "[Website] https://testplugins.com\n" +
                        "[Description] Cool description";

                User finalAuthor = author;
                author.openPrivateChannel().queue(privateChannel -> {
                    String string = "Your latest request is not following the requirements for <#424460627663126538>.\n" +
                            "The requirements are as below:\n```" + requirements + "```\nFor example, your review needs to look like this: \n```" + exampleReview + "```\n" +
                            "RMS requests need to look like this:\n```" + exampleRMS + "```\n" +
                            "Please edit your message below to fit the requirements:\n```" + msg + "```";

                    privateChannel.sendMessage(string).queue(message1 -> {
                    }, throwable -> {
                        String hastebin = wutil.hastebin(string.replace("`", ""));

                        String toSend = !hastebin.equals("fail")
                                ? "**THIS MESSAGE WILL BE REMOVED IN 30 SECONDS!**\n" + finalAuthor.getAsMention()
                                + " Your message does not follow the requirements for <#424460627663126538>, "
                                + "please read this:\n" + hastebin
                                : "**THIS MESSAGE WILL BE REMOVED IN 30 SECONDS!**\n" + finalAuthor.getAsMention()
                                + " Your message does not follow the requirements for <#424460627663126538>, "
                                + "please fix any mistakes.\nhastebin.com is down and you have pm's disabled, "
                                + "I cannot show you your message, you will have to try remember it.";

                        channel.sendMessage(toSend).queue(message1 -> message1.delete().completeAfter(30, TimeUnit.SECONDS));
                    });
                });
            }
        }
        return false;
    }

    public void createMessage(GenericMessageEvent e) {
        if (checkMessage(e)) {
            Message message = e.getChannel().getMessageById(e.getMessageIdLong()).complete();
            User author = message.getAuthor();

            if (!mutil.startsWith(message.getContentRaw(), "[review]")) {
                String name = "";
                String ip = "";
                String description = "";
                String website = "";

                Pattern p = Pattern.compile("([\\[][\\w]+[]])(.+)");
                Matcher m = p.matcher(message.getContentRaw());
                while (m.find()) {
                    String bracket = m.group(1);
                    String user = m.group(2);

                    switch (bracket.toLowerCase()) {
                        case "[name]":
                            name = user.trim();
                            break;
                        case "[ip]":
                            ip = user.trim();
                            break;
                        case "[description]":
                            description = user.trim();
                            break;
                        case "[website]":
                            website = user.trim();
                            break;
                    }
                }

                String[] ipSegments = ip.split(":");
                
                switch (ipSegments.length) {
                    case 0:
                        message.delete().queue();
                        e.getChannel().sendMessage(author.getAsMention() + " You didn't include an address for your server.").complete().delete().queueAfter(30, TimeUnit.SECONDS);
                        break;
                    case 1:
                        ipSegments = (ip + ":25565").split(":");
                        break;
                    default:
                        message.delete().queue();
                        e.getChannel().sendMessage(author.getAsMention() + " The address you entered is invalid, please resend your rms.").complete().delete().queueAfter(30, TimeUnit.SECONDS);

                }

                if (!siutils.serverStatus(ipSegments[0], ipSegments[1])) {
                    message.delete().queue();
                    e.getChannel().sendMessage(author.getAsMention() + " The server you specified is either offline or doesn't exist, please check the address/turn the server on.").complete().delete().queueAfter(30, TimeUnit.SECONDS);
                    return;
                }

                String extra = "Version - " + siutils.getInfo("version", ipSegments[0], ipSegments[1]) + "\n" +
                        "MOTD - " + siutils.getInfo("motd", ipSegments[0], ipSegments[1]);

                MessageEmbed.Field nameField = new MessageEmbed.Field("Name:", name, false);
                MessageEmbed.Field ipField = new MessageEmbed.Field("IP:", ip, false);
                MessageEmbed.Field descriptionField = new MessageEmbed.Field("Description:", description, false);
                MessageEmbed.Field extraField = new MessageEmbed.Field("Extra:", extra,true);

                EmbedBuilder newMessage = new EmbedBuilder()
                        .setThumbnail(siutils.getIconURL(ipSegments[0], ipSegments[1]))
                        .addField(nameField)
                        .addField(ipField)
                        .addField(descriptionField)
                        .addField(extraField)
                        .setFooter(author.getName() + "#" + author.getDiscriminator(), message.getAuthor().getAvatarUrl());

                if (!website.equals("")) {
                    newMessage.setTitle("Rate My Server", website);
                } else {
                    newMessage.setTitle("Rate My Server");
                }

                e.getChannel().sendMessage(newMessage.build()).queue();
                message.delete().queue();
            }
        }
    }

}
