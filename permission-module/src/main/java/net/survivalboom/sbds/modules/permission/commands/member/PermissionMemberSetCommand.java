package net.survivalboom.sbds.modules.permission.commands.member;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.permissions.IMemberPermissions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandClass(
        name = "set",
        description = "Set a permission for the member in a guild",
        translationKey = "permission.command.member.set",
        permission = "permission.command.member.set"
)
public class PermissionMemberSetCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        Guild guild = info.guild();
        if (guild == null) return;

        User user = info.arguments().getCast("user", User.class).orElseThrow();
        Member member = guild.retrieveMember(user).complete();

        if (member == null) {
            info.reply("permission.command.member.set.not-found").queue();
            return;
        }

        if (member.getUser().isBot()) {
            info.reply("permission.command.member.set.bot").queue();
            return;
        }

        String permissionRaw = info.arguments().getCast("permission", String.class).orElseThrow();
        String valueRaw = info.arguments().getCast("value", String.class).orElseThrow();

        if (!valueRaw.equals("true") && !valueRaw.equals("false") && !valueRaw.equals("null")) {
            info.reply("permission.command.member.set.invalid-value")
                    .withPlaceholders("value", valueRaw, "permission", permissionRaw)
                    .queue();
            return;
        }

        Boolean value = valueRaw.equals("null") ? null : Boolean.valueOf(valueRaw);

        IMemberPermissions permissions = info.sbds().getPermissionManager().getMemberPermissions(guild.getIdLong(), member.getIdLong()).join();

        if (value == null) {
            permissions.removePermission(permissionRaw);
            info.reply("permission.command.member.set.removed")
                    .withPlaceholders(
                            "permission", permissionRaw,
                            "user.name", member.getEffectiveName()
                    )
                    .queue();
            return;
        }

        permissions.setPermission(permissionRaw, value);
        info.reply("permission.command.member.set.success")
                .withPlaceholders(
                        "permission", permissionRaw,
                        "value", value,
                        "user.name", member.getEffectiveName()
                )
                .queue();
    }

    //
    // ARGUMENTS
    //

    @ArgumentMethod(index = 1)
    public UserArgument user() {
        return new UserArgument();
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