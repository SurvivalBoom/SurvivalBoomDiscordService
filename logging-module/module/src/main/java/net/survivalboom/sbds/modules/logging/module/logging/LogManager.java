package net.survivalboom.sbds.modules.logging.module.logging;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.messages.parsers.TextParser;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;

// Я, котрий пишу цей клас о третій ночі можу описати його так: ВЩАЩВЩЩАВАВЩАШВЩІПЩАПЩОИВАОПИА
// Короче клас для перевірки конфігу та надсилання логів, гиии

public class LogManager {
    public static void dispatch(
            LoggingModule module,
            long guildId,
            String groupConfigKey,
            String specificConfigKey,
            String translationKey,
            Object... placeholders
    ) {

        var config = module.getGuildConfig().obtainConfig(guildId);

        module.schedule(() -> {

            boolean enabled = config.get("enabled", Boolean.class).join().orElseThrow();
            boolean group = config.get(groupConfigKey, Boolean.class).join().orElseThrow();
            TextChannel logChannel = config.get("channel", TextChannel.class).join().orElse(null);

            boolean specific = config.get(specificConfigKey, Boolean.class).join().orElse(true);

            // Зв || із || дєц !(по вн ий)
            if (!enabled || !group || !specific || logChannel == null) {
                return;
            }

            Guild guild = module.getSbds().getBot().getGuildById(guildId);
            IMessageTemplate template = module.getSbds().getMessages().getMessage(translationKey, guild, true);

            if (template != null) {

                TextParser parser = TextParser.builder().addPlaceholders(placeholders).build();
                StringParser stringParser = parser.createStringParser(module.getSbds().getMessages());

                logChannel.sendMessage(template.createMessageData(stringParser, null).build()).queue();

            }

        });

    }
}