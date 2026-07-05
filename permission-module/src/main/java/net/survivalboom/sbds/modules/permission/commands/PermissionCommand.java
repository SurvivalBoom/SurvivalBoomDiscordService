package net.survivalboom.sbds.modules.permission.commands;

import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.modules.permission.commands.group.PermissionGroupCommand;
import net.survivalboom.sbds.modules.permission.commands.member.PermissionMemberCommand;

@CommandClass(name = "permission", aliases = "perm", description = "Manage SBDS permissions")
public class PermissionCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor {

    @ArgumentMethod
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionMemberCommand(),
                new PermissionGroupCommand()
        );
    }

}