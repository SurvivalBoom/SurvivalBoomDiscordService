package net.survivalboom.sbds.core.commands.cmds.console.permission.guild;

import net.survivalboom.sbds.api.commands.argument.discord.GuildArgument;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group.PermissionGuildGroupCommand;
import net.survivalboom.sbds.core.commands.cmds.console.permission.guild.member.PermissionGuildMemberCommand;

@CommandClass(name = "guild", description = "Manage guild permissions")
public class PermissionGuildCommand extends CommandBase implements ConsoleCommandExecutor {

    @ArgumentMethod(index = 1)
    public GuildArgument guild() {
        return new GuildArgument();
    }

    @ArgumentMethod(index = 2)
    public SubCommandArgument subcommand() {
        return new SubCommandArgument(
                new PermissionGuildGroupCommand(),
                new PermissionGuildMemberCommand()
        );
    }

}
