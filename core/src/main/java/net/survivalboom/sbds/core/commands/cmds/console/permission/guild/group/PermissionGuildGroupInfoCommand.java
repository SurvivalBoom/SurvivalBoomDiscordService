package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CommandClass(name = "info", description = "Show all permissions of the guild permission group")
public class PermissionGuildGroupInfoCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        String name = info.arguments().getCast("name", String.class).orElseThrow();

        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild, name).join();
        IGlobalPermissionGroup globalGroup = manager.getGlobalGroup(name);
        if (group == null && globalGroup == null) {
            info.logger().error("No group with name `{}` was found in guild `{}`.", name, guild.getName());
            return;
        }

        List<Permission> groupPermissions = group != null ? group.getPermissionList() : List.of();
        List<Permission> globalPermissions = globalGroup != null ? globalGroup.getPermissionList() : List.of();

        info.logger().info("--- --- < Group Info > --- ---");
        info.logger().info("> Guild: {}", guild.getName());
        info.logger().info("> Group: {}", name);
        info.logger().info(" ");

        if (groupPermissions.isEmpty() && globalPermissions.isEmpty()) {
            info.logger().info("! There is no permissions defined in this group.");
        }

        if (!groupPermissions.isEmpty()) {

            info.logger().info("> Permissions:");

            for (var permission : groupPermissions) {
                info.logger().info("* {}:{}", permission.permission(), permission.value());
            }
            info.logger().info(" ");

        }

        if (!globalPermissions.isEmpty()) {

            info.logger().info("> Global Permissions:");

            for (var permission : globalPermissions) {
                info.logger().info("* {}:{}", permission.permission(), permission.value());
            }

            info.logger().info(" ");

        }

        info.logger().info("--- --- ---- ---- ---- --- ---");

    }

    @ArgumentMethod(index = 4)
    public StringArgument name() {
        return new StringArgument();
    }

}
