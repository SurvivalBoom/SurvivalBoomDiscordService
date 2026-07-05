package net.survivalboom.sbds.core.commands.cmds.console.libraries;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;

@CommandClass(name = "libraries", aliases = "libs", description = "Manage SBDS libraries")
public class LibrariesCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new LibrariesListCommand(),
                new LibrariesInfoCommand()
        );
    }

}
