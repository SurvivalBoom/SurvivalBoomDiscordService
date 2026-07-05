package net.survivalboom.sbds.modules.permission.commands.member;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;

@CommandClass(name = "member", description = "Manage member permissions")
public class PermissionMemberCommand extends CommandBase {

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionMemberInfoCommand(),
                new PermissionMemberSetCommand()
        );
    }

}