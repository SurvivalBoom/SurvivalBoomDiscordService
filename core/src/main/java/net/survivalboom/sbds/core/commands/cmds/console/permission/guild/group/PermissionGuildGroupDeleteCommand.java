package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "delete", description = "Delete guild permission group")
public class PermissionGuildGroupDeleteCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        String name = info.arguments().getCast("name", String.class).orElseThrow();

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        IGuildPermissionsGroup group = info.sbds().getPermissionManager().getGuildGroup(guild, name).join();
        if (group == null) {
            info.logger().error("No group with name `{}` was found in guild `{}`.", name, guild.getName());
            return;
        }

        info.sbds().getPermissionManager().deleteGuildGroup(group);

        info.logger().info("Deleted group `{}` in guild `{}` successfully.", name, guild.getName());

    }

    @ArgumentMethod(index = 4)
    public StringArgument name() {
        return new StringArgument();
    }

}
