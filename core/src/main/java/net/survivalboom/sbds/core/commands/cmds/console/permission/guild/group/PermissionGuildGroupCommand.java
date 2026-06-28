package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;

@CommandClass(name = "group", description = "Manage guild permission groups")
public class PermissionGuildGroupCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod(index = 3)
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGuildGroupInfoCommand(),
                new PermissionGuildGroupListCommand(),
                new PermissionGuildGroupCreateCommand(),
                new PermissionGuildGroupDeleteCommand(),
                new PermissionGuildGroupSetCommand()
        );
    }

}
