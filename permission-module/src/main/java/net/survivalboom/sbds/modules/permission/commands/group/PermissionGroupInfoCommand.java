package net.survivalboom.sbds.modules.permission.commands.group;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CommandClass(
        name = "info",
        description = "Show all permissions of the guild permission group",
        translationKey = "permission.command.group.info",
        permission = "permission.command.group.info"
)
public class PermissionGroupInfoCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        String name = info.arguments().getCast("name", String.class).orElseThrow();
        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild.getIdLong(), name).join();
        IGlobalPermissionGroup globalGroup = manager.getGlobalGroup(name);

        if (group == null && globalGroup == null) {
            info.reply("permission.command.group.info.not-found")
                    .withPlaceholders("name", name)
                    .queue();
            return;
        }

        List<Permission> groupPermissions = group != null ? group.getPermissionList() : List.of();
        List<Permission> globalPermissions = globalGroup != null ? globalGroup.getPermissionList() : List.of();

        StringBuilder localBuilder = new StringBuilder();
        for (Permission permission : groupPermissions) {
            localBuilder.append("`").append(permission.permission()).append("` : **")
                    .append(permission.value()).append("**\n");
        }
        String localFormatted = localBuilder.isEmpty() ? "$[permission.values.empty]" : localBuilder.toString().trim();

        StringBuilder globalBuilder = new StringBuilder();
        for (Permission permission : globalPermissions) {
            globalBuilder.append("`").append(permission.permission()).append("` : **")
                    .append(permission.value()).append("**\n");
        }
        String globalFormatted = globalBuilder.isEmpty() ? "$[permission.values.empty]" : globalBuilder.toString().trim();

        info.reply("permission.command.group.info.success")
                .withPlaceholders(
                        "guild.name", guild.getName(),
                        "group.name", name,
                        "permissions.local", localFormatted,
                        "permissions.global", globalFormatted
                )
                .queue();

    }

    @ArgumentMethod
    public StringArgument name() {
        return new StringArgument(context -> {
            CommandAutoCompleteInteractionEvent event = context.event();
            Guild guild = event.getGuild();
            if (guild == null) return List.of();

            IPermissionManager manager = context.sbds().getPermissionManager();

            List<String> groupNames = new ArrayList<>(manager.getGuildGroups(guild.getIdLong()).join().stream()
                    .map(IGuildPermissionsGroup::getName)
                    .toList());

            manager.getGlobalGroups().stream()
                    .map(IGlobalPermissionGroup::getName)
                    .filter(n -> !groupNames.contains(n))
                    .forEach(groupNames::add);

            return groupNames.stream()
                    .map(n -> new Command.Choice(n, n))
                    .toList();
        });
    }

}