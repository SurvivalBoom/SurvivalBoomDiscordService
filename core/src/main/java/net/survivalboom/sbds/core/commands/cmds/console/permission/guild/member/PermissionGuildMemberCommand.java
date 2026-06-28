package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.member;

import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;

@CommandClass(name = "member", description = "Manager member permissions")
public class PermissionGuildMemberCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod(index = 3)
    public UserArgument user() {
        return new UserArgument();
    }

    @ArgumentMethod(index = 4)
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGuildMemberSet(),
                new PermissionGuildMemberInfo()
        );
    }



}
