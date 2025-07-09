package net.survivalboom.sbds.modules.chatbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.base.Command;
import net.survivalboom.sbds.api.commands.base.CommandArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.slash.SlashCommand;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.modules.chatbot.storage.BannedUsers;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Command(name = "chatbot-ban", description = "Ban user from interacting with bot", translationKey = "chatbot.command.ban", permission = "chatbot.command.ban")
public class BanUserCommand extends CommandBase implements SlashCommand {

    private final BannedUsers bannedUsers;


    public BanUserCommand(@NotNull BannedUsers bannedUsers) {
        this.bannedUsers = bannedUsers;
    }


    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        User user = info.arguments().get("user", User.class);
        assert user != null;

        Guild guild = Objects.requireNonNull(info.guild());
        boolean value = !bannedUsers.isUserBanned(guild, user);

        bannedUsers.setUserAllowed(guild, user, value);

        String str = value ? "chatbot.command.ban.banned" : "chatbot.command.ban.unbanned";
        info.reply(str).withPlaceholders("{USER}", user.getAsMention()).queue();

    }

    @CommandArgument(name = "user", description = "User to ban")
    public Argument<?> user() {
        return new UserArgument();
    }

}
