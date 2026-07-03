package net.survivalboom.sbds.modules.logging.events;

import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class DeleteListener implements EventListener {
    private final LoggingModule module;

    private final Map<Long, List<DeletedMessageInfo>> deleteBuffers = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DeleteListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    // !УВАГА! В цьому класі відбувається повний капець!! Будьте обережні!

    @SuppressWarnings("unused") // Метход онДелете іс нот усаге 🤓
    @EventHandler
    public void onDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        long messageId = event.getMessageIdLong();

        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);

        var enabledFuture = config.get("enabled", Boolean.class, true);
        var groupFuture = config.get("events.message", Boolean.class, true);
        var specificFuture = config.get("events.message.delete", Boolean.class, true);

        CompletableFuture.allOf(enabledFuture, groupFuture, specificFuture).thenAcceptAsync(v -> {
            try {
                boolean isEnabled = enabledFuture.join().orElse(false);
                boolean isGroupEnabled = groupFuture.join().orElse(true);
                boolean isSpecificEnabled = specificFuture.join().orElse(true);

                if (!isEnabled || !isGroupEnabled || !isSpecificEnabled) return;

                module.getCachedMessage(messageId).thenAcceptAsync(loggedMessage -> {
                    if (loggedMessage == null) return;

                    DeletedMessageInfo info = new DeletedMessageInfo(
                            messageId,
                            loggedMessage.getAuthorId(),
                            loggedMessage.getContent(),
                            loggedMessage.getTimestamp()
                    );

                    bufferMessage(channelId, guildId, info, event);
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private synchronized void bufferMessage(long channelId, long guildId, DeletedMessageInfo info, MessageDeleteEvent event) {
        deleteBuffers.computeIfAbsent(channelId, k -> new ArrayList<>()).add(info);

        if (scheduledTasks.containsKey(channelId)) {
            scheduledTasks.get(channelId).cancel(false);
        }

        ScheduledFuture<?> task = scheduler.schedule(() -> processBuffer(channelId, guildId, event), 2000, TimeUnit.MILLISECONDS);
        scheduledTasks.put(channelId, task);
    }

    private synchronized void processBuffer(long channelId, long guildId, MessageDeleteEvent event) {
        List<DeletedMessageInfo> messages = deleteBuffers.remove(channelId);
        scheduledTasks.remove(channelId);

        if (messages == null || messages.isEmpty()) return;
        messages.sort(Comparator.comparingLong(m -> m.timestamp));

        var coreTemplate = module.getSbds().getGuildConfigManager().getTemplate((IModule) null);
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);
        var coreConfig = module.getSbds().getGuildConfigManager().getGuildConfig(coreTemplate, guildId);

        var channelFuture = config.get("channel", Channel.class, true);
        var languageFuture = coreConfig.get("language", String.class, true);
        var timezoneFuture = coreConfig.get("timezone", TimeZone.class, true);

        CompletableFuture.allOf(channelFuture, languageFuture, timezoneFuture).thenAcceptAsync(v -> {
            try {
                Channel logChannelRaw = channelFuture.join().orElse(null);
                if (!(logChannelRaw instanceof TextChannel logChannel)) return;

                String langKey = languageFuture.join().orElse("sbds:english");
                TimeZone tz = timezoneFuture.join().orElse(TimeZone.getDefault());

                ITranslation translation = module.getSbds().getTranslationManager().getTranslation(langKey);
                if (translation == null) translation = module.getSbds().getTranslationManager().getDefaultTranslation();

                String channelMention = "<#" + channelId + ">";

                if (messages.size() == 1) {
                    DeletedMessageInfo msg = messages.getFirst();
                    long unixSeconds = msg.timestamp / 1000;
                    String authorMention = "<@" + msg.authorId + ">";

                    String safeContent = msg.content.replace("```", "");

                    boolean isLarge = safeContent.length() > 1000;
                    String fileName = "deleted_" + msg.messageId + ".txt";

                    String displayContent = isLarge ? fileName : safeContent;

                    IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.delete", translation, true);
                    if (template != null) {
                        TextParser parser = TextParser.builder().addPlaceholders(
                                "author", authorMention,
                                "author_id", msg.authorId,
                                "channel", channelMention,
                                "content", displayContent,
                                "time", String.valueOf(unixSeconds),
                                "message_id", String.valueOf(msg.messageId)
                        ).build();

                        StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());
                        logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();
                    }

                    if (isLarge) {
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                                .withZone(tz.toZoneId());
                        String time = formatter.format(java.time.Instant.ofEpochMilli(msg.timestamp));

                        net.dv8tion.jda.api.entities.User author = event.getJDA().getUserById(msg.authorId);
                        String authorName = (author != null) ? author.getName() : "Unknown (" + msg.authorId + ")";

                        String fileContent = String.format("[%s %s] User: %s | Message ID: %d\n\n%s",
                                tz.getID(), time, authorName, msg.messageId, msg.content);

                        logChannel.sendFiles(
                                FileUpload.fromData(fileContent.getBytes(StandardCharsets.UTF_8), fileName)
                        ).queue();
                    }
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Channel: #").append(event.getChannel().getName()).append(" (ID: ").append(channelId).append(")\n");
                sb.append("Number: ").append(messages.size()).append("\n\n");

                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                        .withZone(tz.toZoneId());

                for (DeletedMessageInfo msg : messages) {
                    String time = formatter.format(java.time.Instant.ofEpochMilli(msg.timestamp));
                    net.dv8tion.jda.api.entities.User author = event.getJDA().getUserById(msg.authorId);
                    String authorName = (author != null) ? author.getName() : "Unknown (" + msg.authorId + ")";
                    sb.append(String.format("[%s %s] User: %s | Message ID: %d\n", tz.getID(), time, authorName, msg.messageId));
                    sb.append(msg.content).append("\n\n");
                }

                IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.delete_bulk", translation, true);
                if (template != null) {
                    TextParser parser = TextParser.builder().addPlaceholders(
                            "channel", channelMention,
                            "count", String.valueOf(messages.size())
                    ).build();

                    StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());
                    logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();
                }

                logChannel.sendFiles(
                        FileUpload.fromData(sb.toString().getBytes(StandardCharsets.UTF_8), "delete_" + channelId + ".log")
                ).queue();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private record DeletedMessageInfo(
            long messageId,
            long authorId,
            String content,
            long timestamp
    ) {}
}