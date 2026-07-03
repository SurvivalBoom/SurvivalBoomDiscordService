package net.survivalboom.sbds.modules.logging.logging;

import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.modules.logging.LoggingModule;

import java.util.concurrent.CompletableFuture;

// Я, котрий пишу цей клас о третій ночі можу описати його так: ВЩАЩВЩЩАВАВЩАШВЩІПЩАПЩОИВАОПИА
// Короче клас для перевірки конфігу та надсилання логів, гиии

public class LogManager {
    public static void dispatch(
            LoggingModule module,
            long guildId,
            String groupConfigKey,
            String specificConfigKey,
            String translationKey,
            Object... placeholders) {

        var coreTemplate = module.getSbds().getGuildConfigManager().getTemplate((IModule) null);
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);
        var coreConfig = module.getSbds().getGuildConfigManager().getGuildConfig(coreTemplate, guildId);

        var enabledFuture = config.get("enabled", Boolean.class, true);
        var groupFuture = config.get(groupConfigKey, Boolean.class, true);
        var channelFuture = config.get("channel", Channel.class, true);

        var languageFuture = coreConfig.get("language", String.class, true);

        CompletableFuture<Boolean> specificFuture;
        if (specificConfigKey != null && !specificConfigKey.isBlank()) {
            specificFuture = config.get(specificConfigKey, Boolean.class, true)
                    .thenApply(opt -> opt.orElse(true));
        } else {
            specificFuture = CompletableFuture.completedFuture(true);
        }

        CompletableFuture.allOf(enabledFuture, groupFuture, specificFuture, channelFuture, languageFuture).thenAcceptAsync(v -> {
            try {
                boolean isEnabled = enabledFuture.join().orElse(false);
                boolean isGroupEnabled = groupFuture.join().orElse(true);
                boolean isSpecificEnabled = specificFuture.join();
                Channel logChannelRaw = channelFuture.join().orElse(null);
                String langKey = languageFuture.join().orElse("sbds:english");

                // Зв || із || дєц !(по вн ий)
                if (!isEnabled || !isGroupEnabled || !isSpecificEnabled || !(logChannelRaw instanceof TextChannel logChannel)) {
                    return;
                }

                ITranslation translation = module.getSbds().getTranslationManager().getTranslation(langKey);
                if (translation == null) translation = module.getSbds().getTranslationManager().getDefaultTranslation();

                IMessageTemplate template = module.getSbds().getMessages().getMessage(translationKey, translation, true);

                if (template != null) {
                    TextParser parser = TextParser.builder().addPlaceholders(placeholders).build();
                    StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());

                    logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue(
                            null,
                            err -> {
                                System.err.println("[LogManager] Discord API Error:");
                                err.printStackTrace();
                            }
                    );
                }
            } catch (Exception e) {
                System.err.println("[LogManager] Internal Error:");
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            System.err.println("[LogManager] Async Fatal Error:");
            ex.printStackTrace();
            return null;
        });
    }
}