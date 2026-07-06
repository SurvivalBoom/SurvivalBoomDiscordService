package net.survivalboom.sbds.modules.logging.module.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfig;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;
import net.survivalboom.sbds.modules.logging.module.database.MessageRecord;
import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.utils.FileUpload;
import java.nio.charset.StandardCharsets;

public class EditListener implements EventListener {

    private final LoggingModule module;

    public EditListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    // !УВАГА! В цьому класі відбувається повний капець!! Будьте обережні!

    @SuppressWarnings("unused") // Метход онМессагеЕдіт іс нот усаге 🤓
    @EventHandler
    public void onMessageEdit(MessageUpdateEvent event) {

        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        if (!event.isFromGuild()) return;

        long messageId = event.getMessageIdLong();
        long guildId = event.getGuild().getIdLong();

        String newContent1 = event.getMessage().getContentRaw();
        if (newContent1.isBlank()) return;

        module.schedule(() -> {

            Guild guild = event.getGuild();

            IGuildConfigManager manager = module.getGuildConfigManager();

            IGuildConfig coreConfig = manager.getSbdsConfig().obtainConfig(guild);
            IGuildConfig config = module.getGuildConfig().obtainConfig(guild);

            boolean enabled = config.get("enabled", Boolean.class).join().orElseThrow();
            boolean group = config.get("events.message", Boolean.class).join().orElseThrow();
            boolean specific = config.get("events.message.edit", Boolean.class).join().orElseThrow();

            TextChannel logChannel = config.get("channel", TextChannel.class).join().orElse(null);

            if (logChannel == null || !enabled || !group || !specific) {
                return;
            }

            ILoggedMessage pusak = module.getCachedMessage(messageId).join();
            if (pusak == null) {
                return;
            }

            String oldContent1 = pusak.getContent();
            if (newContent1.equals(oldContent1)) {
                return;
            }

            String authorMention = "<@" + event.getAuthor().getId() + ">";
            String channelMention = "<#" + event.getChannel().getId() + ">";
            String messageLink = event.getMessage().getJumpUrl();

            String oldContent = oldContent1.replace("```", "");
            String newContent = newContent1.replace("```", "");

            String fullDiff = generateDiff(oldContent, newContent);
            boolean isLarge = fullDiff.length() > 480;
            String displayDiff = isLarge ? "[changes.diff]" : fullDiff;

            IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.edit", guild, true);

            if (template != null) {
                TextParser parser = TextParser.builder().addPlaceholders(
                        "author", authorMention,
                        "author_id", event.getAuthor().getId(),
                        "channel", channelMention,
                        "diff", displayDiff,
                        "old_content", oldContent,
                        "new_content", newContent,
                        "link", messageLink,
                        "message_id", String.valueOf(messageId)
                ).build();

                StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());
                logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();
            }

            if (isLarge) {
                logChannel.sendFiles(
                        FileUpload.fromData(fullDiff.getBytes(StandardCharsets.UTF_8), "changes.diff")
                ).queue();
            }

            if (pusak instanceof MessageRecord record) {
                record.setContent(newContent);
                module.getMessageManager().saveMessage(record);
            }

        });

    }

    private String generateDiff(String oldText, String newText) {

        String[] oldLines = oldText.split("\\r?\\n");
        String[] newLines = newText.split("\\r?\\n");

        int minLen = Math.min(oldLines.length, newLines.length);

        int prefix = 0;
        while (prefix < minLen && oldLines[prefix].equals(newLines[prefix])) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < minLen - prefix && oldLines[oldLines.length - 1 - suffix].equals(newLines[newLines.length - 1 - suffix])) {
            suffix++;
        }

        StringBuilder diff = new StringBuilder();

        for (int i = 0; i < prefix; i++) {
            diff.append(" ").append(oldLines[i]).append("\n");
        }

        for (int i = prefix; i < oldLines.length - suffix; i++) {
            diff.append("-").append(oldLines[i]).append("\n");
        }

        for (int i = prefix; i < newLines.length - suffix; i++) {
            diff.append("+").append(newLines[i]).append("\n");
        }

        for (int i = oldLines.length - suffix; i < oldLines.length; i++) {
            diff.append(" ").append(oldLines[i]).append("\n");
        }

        return diff.toString().trim();

    }
}