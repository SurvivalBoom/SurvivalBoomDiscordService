package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandClass(name = "list", description = "Shows list of permission groups on the guild")
public class PermissionGuildGroupListCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        IPermissionManager manager = info.sbds().getPermissionManager();
        List<IGuildPermissionsGroup> groups = manager.getGuildGroups(guild).join();
        List<IGlobalPermissionGroup> globalGroups = manager.getGlobalGroups();

        info.logger().info("--- --- --- < Permission Groups > --- --- ---");
        info.logger().info("> Guild: {}", guild.getName());

        if (groups.isEmpty() && globalGroups.isEmpty()) {
            info.logger().info("! There is no groups exist in this guild.");
        }

        if (!groups.isEmpty()) {

            info.logger().info(" ");
            info.logger().info("> Groups:");

            for (var group : groups) {
                info.logger().info("* {} ({} permissions)", group.getName(), group.getPermissionsCount());
            }

        }

        if (!globalGroups.isEmpty()) {

            info.logger().info(" ");
            info.logger().info("> Global:");

            for (var group : globalGroups) {
                info.logger().info("* {} ({} permissions)", group.getName(), group.getPermissionsCount());
            }

        }

        info.logger().info("--- --- --- ---- --- - - ---- --- --- --- ---");

    }
}
