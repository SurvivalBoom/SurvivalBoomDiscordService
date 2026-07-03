package net.survivalboom.sbds.modules.logging.commands;

import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.TextChannelArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import net.survivalboom.sbds.api.utils.typemap.TypeMap;
import net.survivalboom.sbds.modules.logging.utils.LoggingTextChannelArgument;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "channel", description = "Set a logging channel", translationKey = "logging.command.channel", permission = "logging.command.manage")
public class LoggingChannelCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor {

    private final LoggingModule module;

    public LoggingChannelCommand(LoggingModule module) {
        this.module = module;
    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {
        executes0(info, info.arguments());
    }

    @Override
    public void executes(@NotNull StringExecutionInfo info) {
        executes0(info, info.arguments());
    }

    private void executes0(@NotNull InteractionHolder info, @NotNull TypeMap arguments) {

        Channel channel = arguments.getCast("channel", Channel.class).orElseThrow();

        long guildId = info.guild().getIdLong();
        var template = module.getGuildConfig();
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(template, guildId);

        config.set("channel", channel).thenRun(() -> {
            info.reply("logging.command.channel.success")
                    .withPlaceholders("channel", channel.getName())
                    .queue();
        });
    }

    @ArgumentMethod(description = "channel", required = false)
    public Argument<?> channel() {
        return new LoggingTextChannelArgument();
    }
}