package net.survivalboom.sbds.modules.chatbot.commands.console;

import com.openai.models.ChatModel;
import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.GuildArgument;
import net.survivalboom.sbds.api.commands.base.Command;
import net.survivalboom.sbds.api.commands.base.CommandArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.console.ConsoleCommand;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.modules.chatbot.storage.GuildModels;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Command(name = "model-view", description = "Shows current model of chatbot for the specified guild")
public class ViewModelCommand extends CommandBase implements ConsoleCommand {

    private final GuildModels guildModels;


    public ViewModelCommand(@NotNull GuildModels guildModels) {
        this.guildModels = guildModels;
    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        Guild guild = info.arguments().getCastOrNull("guild", Guild.class);
        Objects.requireNonNull(guild, "guild == null");

        ChatModel model = guildModels.getModel(guild);

        info.logger().info("Current model for guild `{}` is `{}`.", guild.getName(), model);

    }

    @CommandArgument(name = "guild")
    public Argument<?> guild() {
        return new GuildArgument();
    }

}
