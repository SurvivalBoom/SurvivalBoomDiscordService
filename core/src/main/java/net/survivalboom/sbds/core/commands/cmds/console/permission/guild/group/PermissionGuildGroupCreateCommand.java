package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "create", description = "Create a group in the guild")
public class PermissionGuildGroupCreateCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        String name = info.arguments().getCast("name", String.class).orElseThrow();

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild, name).join();;
        if (group != null) {
            info.logger().error("Group with name `{}` already exists in guild `{}`.", name, guild.getName());
            return;
        }

        manager.createGuildGroup(guild, name).join();

        info.logger().info("Created group `{}` on guild `{}` successfully.", name, guild.getName());

    }

    @ArgumentMethod(index = 4)
    public StringArgument name() {
        return new StringArgument();
    }

}
