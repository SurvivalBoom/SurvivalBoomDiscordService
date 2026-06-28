package net.survivalboom.sbds.core.commands.cmds.console.permission;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.core.commands.cmds.console.permission.global.PermissionGlobalCommand;
import net.survivalboom.sbds.core.commands.cmds.console.permission.guild.PermissionGuildCommand;

@CommandClass(name = "permissions", aliases = "perm", description = "Manage SBDS permissions")
public class PermissionCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGuildCommand(),
                new PermissionGlobalCommand()
        );
    }

}
