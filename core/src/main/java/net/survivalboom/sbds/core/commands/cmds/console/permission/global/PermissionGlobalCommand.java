package net.survivalboom.sbds.core.commands.cmds.console.permission.global;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;

@CommandClass(name = "global", description = "Manager global permissions")
public class PermissionGlobalCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod(index = 1)
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGlobalListCommand(),
                new PermissionGlobalInfoCommand()
        );
    }

}
