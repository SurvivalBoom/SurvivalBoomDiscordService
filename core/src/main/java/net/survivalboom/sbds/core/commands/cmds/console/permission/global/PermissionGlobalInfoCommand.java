package net.survivalboom.sbds.core.commands.cmds.console.permission.global;

import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "info", description = "Show information about global group")
public class PermissionGlobalInfoCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        String groupRaw = info.arguments().getCast("group", String.class).orElseThrow();

        IGlobalPermissionGroup group = info.sbds().getPermissionManager().getGlobalGroup(groupRaw);
        if (group == null) {
            info.logger().error("No group with name `{}` was found.", groupRaw);
            return;
        }

        info.logger().info("--- --- < Group Info > --- ---");
        info.logger().info("> Group: {}", groupRaw);
        info.logger().info("> Key: {}", group.getRegistration().key());
        info.logger().info(" ");
        info.logger().info("> Permissions:");

        for (var entry : group.getPermissions().entrySet()) {
            info.logger().info("* {}:{}", entry.getKey(), entry.getValue().value());
        }

        info.logger().info("--- --- ---- ---- ---- --- ---");

    }

    @ArgumentMethod
    public StringArgument group() {
        return new StringArgument();
    }

}
