package net.survivalboom.sbds.modules.permission.commands.group;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IGuildPermissionsGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import org.jetbrains.annotations.NotNull;

@CommandClass(
        name = "create",
        description = "Create a group in the guild",
        translationKey = "permission.command.group.create",
        permission = "permission.command.group.create"
)
public class PermissionGroupCreateCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        String name = info.arguments().getCast("name", String.class).orElseThrow();
        IPermissionManager manager = info.sbds().getPermissionManager();

        IGuildPermissionsGroup group = manager.getGuildGroup(guild.getIdLong(), name).join();

        if (group != null) {
            info.reply("permission.command.group.create.exists")
                    .withPlaceholders("name", name)
                    .queue();
            return;
        }

        manager.createGuildGroup(guild.getIdLong(), name).join();

        info.reply("permission.command.group.create.success")
                .withPlaceholders("name", name)
                .queue();
    }

    @ArgumentMethod
    public StringArgument name() {
        return new StringArgument();
    }

}