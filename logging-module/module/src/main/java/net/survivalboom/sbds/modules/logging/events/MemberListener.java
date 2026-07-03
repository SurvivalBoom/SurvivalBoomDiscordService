package net.survivalboom.sbds.modules.logging.events;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.entities.Role;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.modules.logging.logging.LogManager;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class MemberListener implements EventListener {

    private final LoggingModule module;

    public MemberListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(GuildMemberJoinEvent event) {
        String memberName = event.getMember().getEffectiveName();
        String userMention = event.getUser().getAsMention();

        LogManager.dispatch(
                module,
                event.getGuild().getIdLong(),
                "events.member",
                "events.member.join",
                "logging.message.member.join",
                event.getUser().getJDA().getSelfUser(),
                "name", memberName,
                "user_id", event.getUser().getId()
        );
    }

    @EventHandler
    public void onLeave(GuildMemberRemoveEvent event) {
        String memberName = event.getUser().getName();
        String userMention = event.getUser().getAsMention();

        LogManager.dispatch(
                module,
                event.getGuild().getIdLong(),
                "events.member",
                "events.member.leave",
                "logging.message.member.leave",
                event.getUser().getJDA().getSelfUser(),
                "name", memberName,
                "user_id", event.getUser().getId()
        );
    }

    @EventHandler
    public void onNicknameChange(GuildMemberUpdateNicknameEvent event) {
        // if (event.getUser().isBot()) return; <-- ну ладна, пусть будут ботікі

        String globalName = event.getUser().getName();
        String oldName = event.getOldNickname() != null ? event.getOldNickname() : globalName;
        String newName = event.getNewNickname() != null ? event.getNewNickname() : globalName;
        String userMention = event.getUser().getAsMention();

        LogManager.dispatch(
                module,
                event.getGuild().getIdLong(),
                "events.member",
                "events.member.nickname",
                "logging.message.member.nickname",
                event.getUser().getJDA().getSelfUser(),
                "user", userMention,
                "user_id", event.getUser().getId(),
                "old_name", oldName,
                "new_name", newName
        );
    }

    @EventHandler
    public void onRoleAdd(GuildMemberRoleAddEvent event) {
        String roles = event.getRoles().stream()
                .map(Role::getAsMention)
                .collect(Collectors.joining(", "));

        LogManager.dispatch(
                module,
                event.getGuild().getIdLong(),
                "events.member",
                "events.member.role_add",
                "logging.message.member.role_add",
                "user", event.getMember().getEffectiveName(),
                "user_id", event.getUser().getId(),
                "roles", roles
        );
    }

    @EventHandler
    public void onRoleRemove(GuildMemberRoleRemoveEvent event) {
        String roles = event.getRoles().stream()
                .map(Role::getAsMention)
                .collect(Collectors.joining(", "));

        LogManager.dispatch(
                module,
                event.getGuild().getIdLong(),
                "events.member",
                "events.member.role_remove",
                "logging.message.member.role_add",
                "user", event.getMember().getEffectiveName(),
                "user_id", event.getUser().getId(),
                "roles", roles
        );
    }
}