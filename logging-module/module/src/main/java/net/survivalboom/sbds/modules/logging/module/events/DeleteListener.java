package net.survivalboom.sbds.modules.logging.module.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfig;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.api.utils.queue.PushQueue;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class DeleteListener implements EventListener {

    private final LoggingModule module;

    private final Map<Long, PushQueue<DeletedMessageInfo>> queueMap = new ConcurrentHashMap<>();

    public DeleteListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    // !УВАГА! В цьому класі відбувається повний катастрофічно хрюковий капець!! Будьте обережні!

    @SuppressWarnings("unused") // Метход онДелете іс нот усаге 🤓
    @EventHandler
    public void onDelete(MessageDeleteEvent event) {

        if (!event.isFromGuild()) {
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        long messageId = event.getMessageIdLong();

        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);

        module.schedule(() -> {

            boolean isEnabled = config.get("enabled", Boolean.class, true).join().orElse(false);
            boolean isGroupEnabled = config.get("events.message", Boolean.class, true).join().orElse(true);
            boolean isSpecificEnabled = config.get("events.message.delete", Boolean.class, true).join().orElse(true);

            if (!isEnabled || !isGroupEnabled || !isSpecificEnabled) {
                return;
            }

            ILoggedMessage pusak = module.getCachedMessage(messageId).join();

            if (pusak == null) {
                return;
            }

            DeletedMessageInfo info = new DeletedMessageInfo(pusak, event);

            PushQueue<DeletedMessageInfo> queue = queueMap.computeIfAbsent(channelId, k -> new PushQueue<>(module, this::bufferTask, "DeleteBuffer", 3000));
            queue.initIfNeeded();
            queue.append(info);

        });
    }

    private void bufferTask(@NotNull PushQueue<DeletedMessageInfo> queue) {

        List<DeletedMessageInfo> messages = queue.getQueue();
        messages.sort(Comparator.comparing(e -> e.message.getTimestamp()));

        DeletedMessageInfo firstMessage = messages.getFirst();

        Guild guild = firstMessage.event.getGuild();
        MessageChannel channel = firstMessage.event.getChannel();

        IGuildConfigManager manager = module.getGuildConfigManager();

        IGuildConfig coreConfig = manager.getSbdsConfig().obtainConfig(guild);
        IGuildConfig config = module.getGuildConfig().obtainConfig(guild);

        TextChannel logChannel = config.get("channel", TextChannel.class).join().orElse(null);
        TimeZone serverTimezone = coreConfig.get("timezone", TimeZone.class).join().orElseThrow();

        if (logChannel == null) {
            module.schedule(queue::shutdown);
            return;
        }

        String channelMention = channel.getAsMention();

        if (messages.size() == 1) {

            ILoggedMessage msg = firstMessage.message;

            long unixSeconds = msg.getTimestamp() / 1000;
            String authorMention = "<@" + msg.getAuthorId() + ">";

            String safeContent = msg.getContent().replace("```", "");

            boolean isLarge = safeContent.length() > 1000;
            String fileName = "deleted_" + msg.getMessageId() + ".txt";

            String displayContent = isLarge ? fileName : safeContent;

            IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.delete", guild, true);

            if (template != null) {

                TextParser parser = TextParser.builder().addPlaceholders(
                        "author", authorMention,
                        "author_id", msg.getAuthorId(),
                        "channel", channelMention,
                        "content", displayContent,
                        "time", unixSeconds,
                        "message_id", msg.getMessageId()
                ).build();

                StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());
                logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();

            }

            if (isLarge) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(serverTimezone.toZoneId());
                String time = formatter.format(java.time.Instant.ofEpochMilli(msg.getTimestamp()));

                net.dv8tion.jda.api.entities.User author = firstMessage.event.getJDA().getUserById(msg.getAuthorId());
                String authorName = (author != null) ? author.getName() : "Unknown (" + msg.getAuthorId() + ")";

                String fileContent = String.format("[%s %s] User: %s | Message ID: %d\n\n%s",
                        serverTimezone.getID(),
                        time,
                        authorName,
                        msg.getMessageId(),
                        msg.getContent()
                );

                logChannel.sendFiles(
                        FileUpload.fromData(fileContent.getBytes(StandardCharsets.UTF_8), fileName)
                ).queue();
            }

        }

        else {

            StringBuilder sb = new StringBuilder();
            sb.append("Channel: #")
                    .append(channel.getName())
                    .append(" (ID: ")
                    .append(channel.getIdLong())
                    .append(")\n");

            sb.append("Number: ")
                    .append(messages.size())
                    .append("\n\n");

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                    .withZone(serverTimezone.toZoneId());

            for (DeletedMessageInfo msg : messages) {

                String time = formatter.format(Instant.ofEpochMilli(msg.message.getTimestamp()));
                User author = msg.event.getJDA().getUserById(msg.message.getAuthorId());

                String authorName = (author != null) ? author.getName() : "Unknown (" + msg.message.getAuthorId() + ")";

                sb.append(String.format("[%s %s] User: %s | Message ID: %d\n", serverTimezone.getID(), time, authorName, msg.message.getMessageId()));
                sb.append(msg.message.getContent()).append("\n\n");

            }

            IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.delete_bulk", guild, true);
            if (template != null) {

                TextParser parser = TextParser.builder().addPlaceholders(
                        "channel", channelMention,
                        "count", String.valueOf(messages.size())
                ).build();

                StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());
                logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();

            }

            logChannel.sendFiles(
                    FileUpload.fromData(sb.toString().getBytes(StandardCharsets.UTF_8), "delete_" + channel.getIdLong() + ".log")
            ).queue();

        }

        module.schedule(queue::shutdown);

    }

    private record DeletedMessageInfo(
            @NotNull ILoggedMessage message,
            @NotNull MessageDeleteEvent event
    ) {}

}