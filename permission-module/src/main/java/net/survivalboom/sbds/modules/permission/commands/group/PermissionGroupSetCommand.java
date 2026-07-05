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
        name = "set",
        description = "Set a permission to the guild permission group",
        translationKey = "permission.command.group.set",
        permission = "permission.command.group.set"
)
public class PermissionGroupSetCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        String groupRaw = info.arguments().getCast("group", String.class).orElseThrow();
        String permissionRaw = info.arguments().getCast("permission", String.class).orElseThrow();
        String valueRaw = info.arguments().getCast("value", String.class).orElseThrow();

        if (!valueRaw.equals("true") && !valueRaw.equals("false") && !valueRaw.equals("null")) {
            info.reply("permission.command.group.set.invalid-value")
                    .withPlaceholders("value", valueRaw, "permission", permissionRaw)
                    .queue();
            return;
        }

        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild.getIdLong(), groupRaw).join();
        IGlobalPermissionGroup globalGroup = manager.getGlobalGroup(groupRaw);

        if (group == null && globalGroup == null) {
            info.reply("permission.command.group.set.not-found")
                    .withPlaceholders("group", groupRaw)
                    .queue();
            return;
        }

        if (group == null) {
            group = manager.createGuildGroup(guild.getIdLong(), groupRaw).join();
        }

        Boolean value = valueRaw.equals("null") ? null : Boolean.valueOf(valueRaw);

        if (value == null) {
            group.removePermission(permissionRaw);
            info.reply("permission.command.group.set.removed")
                    .withPlaceholders(
                            "permission", permissionRaw,
                            "group.name", group.getName()
                    )
                    .queue();
            return;
        }

        Permission permission = group.setPermission(permissionRaw, value);
        info.reply("permission.command.group.set.success")
                .withPlaceholders(
                        "permission", permissionRaw,
                        "value", value,
                        "group.name", group.getName()
                )
                .queue();

    }

    //
    // ARGUMENTS
    //

    @ArgumentMethod(index = 1)
    public StringArgument group() {
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

    @ArgumentMethod(index = 2)
    public StringArgument permission() {
        return new StringArgument();
    }

    @ArgumentMethod(index = 3)
    public StringArgument value() {
        return new StringArgument(context -> List.of(
                new Command.Choice("True", "true"),
                new Command.Choice("False", "false"),
                new Command.Choice("Remove", "null")
        ));
    }

}