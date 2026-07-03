package net.survivalboom.sbds.modules.logging.events;

import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.modules.logging.database.MessageRecord;
import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.utils.FileUpload;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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

        String newContent = event.getMessage().getContentRaw();
        if (newContent.isBlank()) return;

        var coreTemplate = module.getSbds().getGuildConfigManager().getTemplate((IModule) null);
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);
        var coreConfig = module.getSbds().getGuildConfigManager().getGuildConfig(coreTemplate, guildId);

        var enabledFuture = config.get("enabled", Boolean.class, true);
        var groupFuture = config.get("events.message", Boolean.class, true);
        var specificFuture = config.get("events.message.edit", Boolean.class, true);
        var channelFuture = config.get("channel", Channel.class, true);
        var languageFuture = coreConfig.get("language", String.class, true);

        CompletableFuture.allOf(enabledFuture, groupFuture, specificFuture, channelFuture, languageFuture).thenAcceptAsync(v -> {
            try {
                boolean isEnabled = enabledFuture.join().orElse(false);
                boolean isGroupEnabled = groupFuture.join().orElse(true);
                boolean isSpecificEnabled = specificFuture.join().orElse(true);
                Channel logChannelRaw = channelFuture.join().orElse(null);
                String langKey = languageFuture.join().orElse("sbds:english");

                if (!isEnabled || !isGroupEnabled || !isSpecificEnabled) return;
                if (!(logChannelRaw instanceof TextChannel logChannel)) return;

                module.getCachedMessage(messageId).thenAcceptAsync(oldMessage -> {
                    try {
                        if (oldMessage == null) return;
                        String oldContent = oldMessage.getContent();
                        if (newContent.equals(oldContent)) return;

                        String authorMention = "<@" + event.getAuthor().getId() + ">";
                        String channelMention = "<#" + event.getChannel().getId() + ">";
                        String messageLink = event.getMessage().getJumpUrl();

                        String fullDiff = generateDiff(oldContent, newContent);
                        String safeDiff = fullDiff.replace("```", "");
                        boolean isLarge = fullDiff.length() > 480;
                        String displayDiff = isLarge ? "[changes.diff]" : fullDiff;

                        ITranslation translation = module.getSbds().getTranslationManager().getTranslation(langKey);
                        if (translation == null) translation = module.getSbds().getTranslationManager().getDefaultTranslation();

                        IMessageTemplate template = module.getSbds().getMessages().getMessage("logging.message.edit", translation, true);

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

                        if (oldMessage instanceof MessageRecord record) {
                            record.setContent(newContent);
                            module.getMessageManager().saveMessage(record);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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