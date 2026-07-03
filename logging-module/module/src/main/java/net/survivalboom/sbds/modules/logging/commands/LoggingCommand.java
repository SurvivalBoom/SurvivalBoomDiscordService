package net.survivalboom.sbds.modules.logging.commands;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.modules.logging.LoggingModule;

@CommandClass(name = "logging", description = "Manage logging module", translationKey = "logging.command")
public class LoggingCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor {

    private final LoggingModule module;

    public LoggingCommand(LoggingModule module) {
        this.module = module;
    }

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new LoggingEnableCommand(module),
                new LoggingDisableCommand(module),
                new LoggingChannelCommand(module)
        );
    }

}