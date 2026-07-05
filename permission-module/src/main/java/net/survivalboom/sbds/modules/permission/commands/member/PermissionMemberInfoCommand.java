package net.survivalboom.sbds.modules.permission.commands.member;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IMemberPermissions;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.IPermissionsHolder;
import net.survivalboom.sbds.api.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@CommandClass(
        name = "info",
        description = "Show information about member permissions on a guild",
        translationKey = "permission.command.member",
        permission = "permission.command.member"
)
public class PermissionMemberInfoCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {
        Guild guild = info.guild();
        if (guild == null) return;

        User user = info.arguments().getCast("user", User.class).orElse(info.user());

        Member member = guild.retrieveMember(user).complete();
        if (member == null) {
            info.reply("permission.command.member.info.not-found").queue();
            return;
        }

        IPermissionManager manager = info.sbds().getPermissionManager();
        IMemberPermissions memberPermissions = manager.getMemberPermissions(guild.getIdLong(), member.getIdLong()).join();

        List<String> groupNames = memberPermissions.getMemberGroups().join().stream()
                .map(IPermissionsHolder::getName)
                .toList();

        String groupsFormatted = groupNames.isEmpty() ? "$[permission.values.empty]" : String.join(", ", groupNames);

        Map<String, Permission> permissionMap = memberPermissions.getPermissionMap();
        StringBuilder permissionsBuilder = new StringBuilder();

        for (var entry : permissionMap.entrySet()) {
            permissionsBuilder.append("`").append(entry.getKey()).append("` : **")
                    .append(entry.getValue().value()).append("**\n");
        }

        if (permissionsBuilder.isEmpty()) {
            permissionsBuilder.append("$[permission.values.empty]");
        }

        info.reply("permission.command.member.info.success")
                .withPlaceholders(
                        "user.ping", user.getAsMention(),
                        "user.name", user.getEffectiveName(),
                        "member.groups", groupsFormatted,
                        "member.permissions", permissionsBuilder.toString().trim()
                )
                .queue();

    }

    @ArgumentMethod
    public UserArgument user() {
        return new UserArgument();
    }

}