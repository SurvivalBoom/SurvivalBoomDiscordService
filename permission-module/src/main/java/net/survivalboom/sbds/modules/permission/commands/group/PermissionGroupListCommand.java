package net.survivalboom.sbds.modules.permission.commands.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandClass(
        name = "list",
        description = "Shows a list of permission groups on the guild",
        translationKey = "permission.command.group.list",
        permission = "permission.command.group.list"
)
public class PermissionGroupListCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        IPermissionManager manager = info.sbds().getPermissionManager();

        List<IGuildPermissionsGroup> groups = manager.getGuildGroups(guild.getIdLong()).join();
        List<IGlobalPermissionGroup> globalGroups = manager.getGlobalGroups();

        StringBuilder groupsBuilder = new StringBuilder();
        for (var group : groups) {
            groupsBuilder.append("> **").append(group.getName()).append("** (")
                    .append(group.getPermissionsCount()).append(")\n");
        }
        String groupsFormatted = groupsBuilder.isEmpty() ? "$[permission.values.empty]" : groupsBuilder.toString().trim();

        StringBuilder globalBuilder = new StringBuilder();
        for (var group : globalGroups) {
            globalBuilder.append("> **").append(group.getName()).append("** (")
                    .append(group.getPermissionsCount()).append(")\n");
        }
        String globalFormatted = globalBuilder.isEmpty() ? "$[permission.values.empty]" : globalBuilder.toString().trim();

        info.reply("permission.command.group.list.success")
                .withPlaceholders(
                        "guild.name", guild.getName(),
                        "groups.local", groupsFormatted,
                        "groups.global", globalFormatted
                )
                .queue();
    }

}