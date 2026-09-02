package net.survivalboom.sbds.core.commands.cmds.console;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.survivalboom.sbds.api.commands.argument.discord.channel.MessageChannelArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.GreedyStringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "say", description = "Sends a message to a channel")
public class SayCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        MessageChannel channel = info.arguments().getCast("channel", MessageChannel.class).orElseThrow();
        String message = info.arguments().getCast("message", String.class).orElseThrow();

        channel.sendMessage(message).complete();
        info.logger().info("Sent &b{} &rto &b{}&r...", message, channel.getName());

    }

    @ArgumentMethod
    public MessageChannelArgument channel() {
        return new MessageChannelArgument();
    }

    @ArgumentMethod(index = 1)
    public GreedyStringArgument message() {
        return new GreedyStringArgument();
    }

}
