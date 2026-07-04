package net.survivalboom.sbds.modules.logging.module.utils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.commands.argument.ArgumentParseException;
import net.survivalboom.sbds.api.commands.argument.ArgumentParsingContext;
import net.survivalboom.sbds.api.commands.argument.discord.channel.TextChannelArgument;
import org.jetbrains.annotations.NotNull;

public class LoggingTextChannelArgument extends TextChannelArgument {

    @Override
    public @NotNull TextChannel parse(@NotNull Object input, @NotNull ArgumentParsingContext context) throws ArgumentParseException {

        if (input instanceof String string) {

            String cleanId = string.replaceAll("[<#>]", "");

            try {
                Long.parseLong(cleanId);

                return super.parse(cleanId, context);

            } catch (NumberFormatException e) {
                // Ууу! Інгліш >.<
                throw new ArgumentParseException("Invalid channel format. Please mention a channel or use its ID");
            }
        }

        return super.parse(input, context);
    }
}