package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.member;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.permissions.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@CommandClass(name = "info", description = "Show information about member permissions on a guild")
public class PermissionGuildMemberInfo extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        User user = info.arguments().getCast("user", User.class).orElseThrow();

        info.logger().info("Retrieving guild `{}` info...", guild.getName());

        Member member = guild.retrieveMember(user).complete();

        IPermissionManager manager = info.sbds().getPermissionManager();
        IMemberPermissions memberPermissions = manager.getMemberPermissions(member).join();

        List<String> groupNames = memberPermissions.getMemberGroups().join().stream()
                .map(IPermissionsHolder::getName)
                .toList();

        Map<String, Permission> permissionMap = memberPermissions.getPermissionMap();

        info.logger().info("--- --- < Member Permissions > --- ---");
        info.logger().info("Guild: {}", guild.getName());
        info.logger().info("Member: {}", member.getEffectiveName());
        info.logger().info("Groups: {}", String.join(", ", groupNames));
        info.logger().info("");
        info.logger().info("> Permissions:");

        for (var permission : permissionMap.values()) {
            info.logger().info("* {}", permission);
        }

        info.logger().info("--- --- ---- --- ---- --- --- ---- ---");

    }

}
