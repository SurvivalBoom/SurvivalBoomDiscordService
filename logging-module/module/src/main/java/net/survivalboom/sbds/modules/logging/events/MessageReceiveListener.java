package net.survivalboom.sbds.modules.logging.events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.modules.logging.database.MessageRecord;

public class MessageReceiveListener implements EventListener {

    private final LoggingModule module;

    public MessageReceiveListener(LoggingModule module) {
        this.module = module;
    }

    @EventHandler
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.isWebhookMessage()) {
            return;
        }

        if (!event.isFromGuild()) {
            return;
        }

        String content = event.getMessage().getContentRaw();
        var attachments = event.getMessage().getAttachments();

        // Теоретично тут можна було б зробити читання префіксу сервера із бази даних, але на практиці я боюся, що від великої кількості серверів воно просто ляже
        if (content.startsWith("!")) {
            return;
        }

        if (!attachments.isEmpty()) {
            StringBuilder sb = new StringBuilder(content);

            if (!content.isBlank()) {
                sb.append("\n\n");
            }

            for (var attachment : attachments) {
                sb.append(attachment.getUrl()).append("\n");
            }
            content = sb.toString().trim();
        }

        if (content.isBlank()) {
            return;
        }

        MessageRecord record = new MessageRecord(
                event.getMessageIdLong(),
                event.getGuild().getIdLong(),
                event.getChannel().getIdLong(),
                event.getAuthor().getIdLong(),
                content,
                System.currentTimeMillis()
        );

        module.getMessageManager().saveMessage(record);
    }
}