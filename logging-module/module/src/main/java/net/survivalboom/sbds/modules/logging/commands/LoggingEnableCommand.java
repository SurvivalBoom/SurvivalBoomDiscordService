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
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "enable", description = "Enable logging module", translationKey = "logging.command.enable", permission = "logging.command.manage")
public class LoggingEnableCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor {

    private final LoggingModule module;

    public LoggingEnableCommand(LoggingModule module) {
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

        Channel channelArg = arguments.getCast("channel", Channel.class).orElse(null);

        long guildId = info.guild().getIdLong();
        var template = module.getGuildConfig();
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(template, guildId);

        if (channelArg != null) {
            config.set("channel", channelArg).join();
        } else {
            Channel savedChannel = config.get("channel", Channel.class, true).join().orElse(null);
            if (savedChannel == null) {
                info.reply("logging.command.enable.no-channel").queue();
                return;
            }
        }

        config.set("enabled", true).thenRun(() -> {
            info.reply("logging.command.enable.success").queue();
        });
    }

    @ArgumentMethod(index = 0, description = "channel", required = false)
    public Argument<?> channel() {
        return new TextChannelArgument();
    }
}