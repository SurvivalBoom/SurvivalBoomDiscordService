package net.survivalboom.sbds.modules.chatbot.commands.console;

import com.openai.models.ChatModel;
import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.GuildArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.Command;
import net.survivalboom.sbds.api.commands.base.CommandArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.console.ConsoleCommand;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.modules.chatbot.storage.GuildModels;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Command(name = "guild-model", description = "Sets the model for chatbot in specified guild")
public class SetModelCommand extends CommandBase implements ConsoleCommand {

    private final GuildModels guildModels;


    public SetModelCommand(@NotNull GuildModels guildModels) {
        this.guildModels = guildModels;
    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        String modelRaw = info.arguments().getCastOrNull("model", String.class);
        Guild guild = info.arguments().getCastOrNull("guild", Guild.class);

        Objects.requireNonNull(modelRaw, "model == null");
        Objects.requireNonNull(guild, "guild == null");

        ChatModel model = GuildModels.getModelFromFuckingKotlin(modelRaw);
        if (model == null) {
            info.logger().error("Invalid model `{}`.", modelRaw);
            return;
        }

        guildModels.setModel(guild, model);

        info.logger().info("Successfully set model `{}` for guild `{}`.", model, guild.getName());

    }


    @CommandArgument(name = "guild")
    public Argument<?> guild() {
        return new GuildArgument();
    }

    @CommandArgument(name = "model", index = 1)
    public Argument<?> model() {
        return new StringArgument();
    }

}
