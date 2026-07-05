package net.survivalboom.sbds.modules.permission.commands.group;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandClass(
        name = "delete",
        description = "Delete a group from the guild",
        translationKey = "permission.command.group.delete",
        permission = "permission.command.group.delete"
)
public class PermissionGroupDeleteCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        String name = info.arguments().getCast("name", String.class).orElseThrow();
        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild.getIdLong(), name).join();

        if (group == null) {
            info.reply("permission.command.group.delete.not-found")
                    .withPlaceholders("name", name)
                    .queue();
            return;
        }

        manager.deleteGuildGroup(group);

        info.reply("permission.command.group.delete.success")
                .withPlaceholders("name", name)
                .queue();
    }

    @ArgumentMethod
    public StringArgument name() {
        return new StringArgument(context -> {
            CommandAutoCompleteInteractionEvent event = context.event();
            Guild guild = event.getGuild();

            if (guild == null) return List.of();

            return context.sbds().getPermissionManager().getGuildGroups(guild.getIdLong()).join().stream()
                    .map(g -> new Command.Choice(g.getName(), g.getName()))
                    .toList();
        });
    }

}