package net.survivalboom.sbds.modules.chatbot.commands;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.TextChannelArgument;
import net.survivalboom.sbds.api.commands.base.Command;
import net.survivalboom.sbds.api.commands.base.CommandArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.console.ConsoleCommand;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.slash.SlashCommand;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.modules.chatbot.storage.AllowedChannels;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Command(name = "chatbot-set", description = "Allow/Deny chatbot talk in specified channel.", translationKey = "chatbot.command.set", permission = "chatbot.command.set")
public class SetChannelCommand extends CommandBase implements SlashCommand, ConsoleCommand {

    private final AllowedChannels allowedChannels;


    public SetChannelCommand(@NotNull AllowedChannels allowedChannels) {
        this.allowedChannels = allowedChannels;
    }


    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        TextChannel textChannel = info.arguments().getCastOrNull("channel", TextChannel.class);
        if (textChannel == null) {
            info.reply("chatbot.command.set.invalid-channel").queue();
            return;
        }

        boolean value = !allowedChannels.isAllowedChannel(textChannel);

        allowedChannels.setChannelAllowed(textChannel, value);

        String str = value ? "chatbot.command.set.allowed" : "chatbot.command.set.deny";
        info.reply(str).withPlaceholders("{CHANNEL}", textChannel.getAsMention()).queue();

    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        TextChannel textChannel = info.arguments().getCastOrNull("channel", TextChannel.class);
        Objects.requireNonNull(textChannel, "channel -- null");

        boolean value = info.arguments().getCastOrDefault("value", Boolean.class, true);

        allowedChannels.setChannelAllowed(textChannel, value);


        if (value) info.logger().info("Successfully allowed chatbot in channel `{}`", textChannel.getName());
        else info.logger().info("Successfully removed chatbot from channel `{}`.", textChannel.getName());

    }

    @CommandArgument(name = "channel", description = "A channel")
    public Argument<?> channel() {
        return new TextChannelArgument();
    }

}
