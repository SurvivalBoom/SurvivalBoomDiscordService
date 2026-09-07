package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.modules.logging.api.events.messages.UserMessageDeleteEvent;
import net.survivalboom.sbds.modules.logging.api.events.messages.UserMessageEditEvent;
import net.survivalboom.sbds.modules.logging.api.events.messages.UserMessageReceiveEvent;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.module.storage.LoggedMessageRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MessageListener extends AbstractLogListener {

    public MessageListener(@NotNull LoggingModule module) {
        super(module);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onJdaMessageReceive(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.isWebhookMessage()) return;

        String content = event.getMessage().getContentRaw();
        var attachments = event.getMessage().getAttachments();

        String cmdPrefix = module.getSbds().getGuildConfigManager().getSbdsConfig()
                .obtainConfig(event.getGuild())
                .get("prefix", String.class)
                .join()
                .orElse("!");

        if (content.startsWith(cmdPrefix)) return;

        content = processContentWithAttachments(content, attachments);
        if (content.isBlank()) return;

        LoggedMessageRecord record = new LoggedMessageRecord(
                event.getMessageIdLong(),
                event.getGuild().getIdLong(),
                event.getChannel().getIdLong(),
                event.getAuthor().getIdLong(),
                content,
                System.currentTimeMillis()
        );

        UserMessageReceiveEvent customEvent = new UserMessageReceiveEvent(module, event, record);
        module.callEvent(customEvent);

        if (customEvent.isCancelled()) {
            event.getMessage().delete().queue();
            return;
        }

        module.getLogDataManager().saveMessage(record);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onJdaMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;

        String newContent = processContentWithAttachments(
                event.getMessage().getContentRaw(),
                event.getMessage().getAttachments()
        );

        if (newContent.isBlank()) return;

        module.getLogDataManager().getMessage(event.getMessageIdLong()).thenAccept(oldRecord -> {

            UserMessageEditEvent customEvent = new UserMessageEditEvent(module, event, oldRecord, newContent);
            module.callEvent(customEvent);

            if (oldRecord != null) {
                if (!oldRecord.getContent().equals(newContent)) {
                    oldRecord.setContent(newContent);
                    module.getLogDataManager().saveMessage(oldRecord);
                }
            } else {
                LoggedMessageRecord newRecord = new LoggedMessageRecord(
                        event.getMessageIdLong(),
                        event.getGuild().getIdLong(),
                        event.getChannel().getIdLong(),
                        event.getAuthor().getIdLong(),
                        newContent,
                        System.currentTimeMillis()
                );
                module.getLogDataManager().saveMessage(newRecord);
            }
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onJdaMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;

        module.getLogDataManager().getMessage(event.getMessageIdLong()).thenAccept(record -> {
            if (record == null) return;

            UserMessageDeleteEvent customEvent = new UserMessageDeleteEvent(module, event, record);
            module.callEvent(customEvent);
        });
    }

    private String processContentWithAttachments(String content, java.util.List<Message.Attachment> attachments) {
        if (!attachments.isEmpty()) {
            StringBuilder sb = new StringBuilder(content);

            if (!content.isBlank()) {
                sb.append("\n\n");
            }

            for (var attachment : attachments) {
                sb.append(attachment.getUrl()).append("\n");
            }
            return sb.toString().trim();
        }
        return content;
    }
}