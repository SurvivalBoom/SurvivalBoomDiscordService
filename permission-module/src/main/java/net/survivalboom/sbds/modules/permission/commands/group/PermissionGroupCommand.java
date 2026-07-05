package net.survivalboom.sbds.modules.permission.commands.group;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;

@CommandClass(name = "group", description = "Manage guild permission groups")
public class PermissionGroupCommand extends CommandBase {

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGroupListCommand(),
                new PermissionGroupInfoCommand(),
                new PermissionGroupSetCommand(),
                new PermissionGroupCreateCommand(),
                new PermissionGroupDeleteCommand()
        );
    }

}