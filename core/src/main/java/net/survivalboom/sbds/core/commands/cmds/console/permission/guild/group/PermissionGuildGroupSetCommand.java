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

@CommandClass(name = "set", description = "Set a permission to the guild permission group")
public class PermissionGuildGroupSetCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        String groupRaw = info.arguments().getCast("group", String.class).orElseThrow();
        String permissionRaw = info.arguments().getCast("permission", String.class).orElseThrow();
        String valueRaw = info.arguments().getCast("value", String.class).orElseThrow();

        if (!valueRaw.equals("true") && !valueRaw.equals("false") && !valueRaw.equals("null")) {
            info.logger().error("Invalid value `{}` for permission `{}`. Can be true/false or null to remove permission.", valueRaw, permissionRaw);
            return;
        }

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild, groupRaw).join();
        IGlobalPermissionGroup globalGroup = manager.getGlobalGroup(groupRaw);

        if (group == null && globalGroup == null) {
            info.logger().error("No group with name `{}` was found in guild `{}`.", groupRaw, guild.getName());
            return;
        }

        if (group == null) {
            group = manager.createGuildGroup(guild, groupRaw).join();
        }

        Boolean value = valueRaw.equals("null") ? null : Boolean.valueOf(valueRaw);
        if (value == null) {
            group.removePermission(permissionRaw);
            info.logger().info("Successfully removed permission `{}` from `{}` in the guild `{}`.", permissionRaw, group, guild.getName());
            return;
        }

        Permission permission = group.setPermission(permissionRaw, value);
        info.logger().info("Successfully set permission `{}` in `{}` for the guild `{}`.", permission, groupRaw, guild.getName());

    }

    @ArgumentMethod(index = 4)
    public StringArgument group() {
        return new StringArgument();
    }

    @ArgumentMethod(index = 5)
    public StringArgument permission() {
        return new StringArgument();
    }

    @ArgumentMethod(index = 6)
    public StringArgument value() {
        return new StringArgument();
    }


}
