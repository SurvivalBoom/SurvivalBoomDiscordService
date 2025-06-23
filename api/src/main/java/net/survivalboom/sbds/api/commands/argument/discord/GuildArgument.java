package net.survivalboom.sbds.api.commands.argument.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.ArgumentParseException;
import net.survivalboom.sbds.api.commands.argument.SimpleArgument;
import org.jetbrains.annotations.NotNull;

public class GuildArgument extends SimpleArgument<Guild> {

    @NotNull
    @Override
    protected Guild parse0(@NotNull Object input, @NotNull Argument.ArgumentResources resources) throws ArgumentParseException {

        JDA bot = resources.sbds().getBot();

        if (input instanceof String string) {
            Guild guild = bot.getGuildById(string);
            if (guild == null) throw new ArgumentParseException("Guild with id `" + string + "` not found");
            return guild;
        }

        if (input instanceof OptionMapping optionMapping) {
            String string = optionMapping.getAsString();
            Guild guild = bot.getGuildById(string);
            if (guild == null) throw new ArgumentParseException("Guild with id `" + string + "` not found");
            return guild;
        }

        throw new ArgumentParseException();

    }

    @NotNull
    @Override
    public OptionType getOptionType() {
        return OptionType.STRING;
    }

}
