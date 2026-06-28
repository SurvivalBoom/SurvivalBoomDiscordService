package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.member;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.IMemberPermissions;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "set", description = "Set a permission for the member in a guild")
public class PermissionGuildMemberSet extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        User user = info.arguments().getCast("user", User.class).orElseThrow();
        Member member = guild.retrieveMember(user).complete();

        String permissionRaw = info.arguments().getCast("permission", String.class).orElseThrow();
        String valueRaw = info.arguments().getCast("value", String.class).orElseThrow();

        if (!valueRaw.equals("true") && !valueRaw.equals("false") && !valueRaw.equals("null")) {
            info.logger().error("Invalid value `{}` for permission `{}`. Can be true/false or null to remove permission.", valueRaw, permissionRaw);
            return;
        }

        Boolean value = valueRaw.equals("null") ? null : Boolean.valueOf(valueRaw);

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        IMemberPermissions permissions = info.sbds().getPermissionManager().getMemberPermissions(member).join();

        if (value == null) {
            permissions.removePermission(permissionRaw);
            info.logger().info("Successfully removed permission `{}` from `{}` in th guild `{}`.", permissionRaw, member.getEffectiveName(), guild.getName());
            return;
        }

        permissions.setPermission(permissionRaw, value);
        info.logger().info("Successfully set permission `{}` in `{}` for the guild `{}`.", permissionRaw, member.getEffectiveName(), guild.getName());

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
