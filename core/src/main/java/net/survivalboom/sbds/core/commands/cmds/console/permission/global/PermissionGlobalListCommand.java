package net.survivalboom.sbds.core.commands.cmds.console.permission.global;

import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandClass(name = "list", description = "Show a list ")
public class PermissionGlobalListCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        List<IGlobalPermissionGroup> globalGroups = info.sbds().getPermissionManager().getGlobalGroups();
        if (globalGroups.isEmpty()) {
            info.logger().info("There is no global permission groups :(");
            info.logger().info("You can define them in settings.yml");
            return;
        }

        info.logger().info("--- --- --- < Permission Groups > --- --- ---");

        for (var group : globalGroups) {
            info.logger().info("> {} ({})", group.getName(), group.getRegistration().key());
        }

        info.logger().info("--- --- --- ---- --- - - ---- --- --- --- ---");

    }

}
