package net.survivalboom.sbds.modules.logging.commands;

import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "disable", description = "Disable logging module", translationKey = "logging.command.disable", permission = "logging.command.manage")
public class LoggingDisableCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor {

    private final LoggingModule module;

    public LoggingDisableCommand(LoggingModule module) {
        this.module = module;
    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) { executes0(info); }

    @Override
    public void executes(@NotNull StringExecutionInfo info) { executes0(info); }

    private void executes0(@NotNull InteractionHolder info) {
        long guildId = info.guild().getIdLong();
        var template = module.getGuildConfig();
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(template, guildId);

        config.set("enabled", false).thenRun(() -> {
            info.reply("logging.command.disable.success").queue();
        });
    }
}