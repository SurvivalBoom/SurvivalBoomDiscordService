package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.modules.logging.api.events.members.*;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.module.kostily.InviteTracker;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class MemberListener extends AbstractLogListener {

    private final InviteTracker inviteTracker;

    public MemberListener(@NotNull LoggingModule module, @NotNull InviteTracker inviteTracker) {
        super(module);
        this.inviteTracker = inviteTracker;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onMemberJoin(GuildMemberJoinEvent event) {
        inviteTracker.findInviter(event).thenAccept(invite -> {

            UserJoinEvent customEvent = new UserJoinEvent(module, event, invite);
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().kick(event.getUser())
                        .reason(customEvent.getCancelReason())
                        .queue(null, ex -> {});
                return;
            }

            Long inviterId = null;
            if (invite != null && invite.getInviter() != null) {
                inviterId = invite.getInviter().getIdLong();
            }

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getUser().getIdLong(),
                    inviterId,
                    "MEMBER_JOIN",
                    null,
                    null
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onMemberLeave(GuildMemberRemoveEvent event) {
        UserLeaveEvent customEvent = new UserLeaveEvent(module, event);
        module.callEvent(customEvent);

        module.getLogDataManager().create(
                event.getGuild().getIdLong(),
                event.getUser().getIdLong(),
                null,
                "MEMBER_LEAVE",
                null,
                null
        );
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onNicknameChange(GuildMemberUpdateNicknameEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.MEMBER_UPDATE, event.getUser().getIdLong(), (moderator, reason) -> {

            UserNicknameUpdateEvent customEvent = new UserNicknameUpdateEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().modifyNickname(event.getMember(), event.getOldNickname())
                        .reason(customEvent.getCancelReason())
                        .queue(null, ex -> {});
                return;
            }

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Old: %s | New: %s", event.getOldNickname(), event.getNewNickname());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getUser().getIdLong(),
                    modId,
                    "NICKNAME_UPDATE",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onRoleAdd(GuildMemberRoleAddEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.MEMBER_ROLE_UPDATE, event.getUser().getIdLong(), (moderator, reason) -> {

            UserRoleAddEvent customEvent = new UserRoleAddEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().modifyMemberRoles(event.getMember(), null, event.getRoles())
                        .reason(customEvent.getCancelReason())
                        .queue(null, ex -> {});
                return;
            }

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = event.getRoles().stream().map(Role::getId).collect(Collectors.joining(","));

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getUser().getIdLong(),
                    modId,
                    "ROLE_ADD",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onRoleRemove(GuildMemberRoleRemoveEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.MEMBER_ROLE_UPDATE, event.getUser().getIdLong(), (moderator, reason) -> {

            UserRoleRemoveEvent customEvent = new UserRoleRemoveEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().modifyMemberRoles(event.getMember(), event.getRoles(), null)
                        .reason(customEvent.getCancelReason())
                        .queue(null, ex -> {});
                return;
            }

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = event.getRoles().stream().map(Role::getId).collect(Collectors.joining(","));

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getUser().getIdLong(),
                    modId,
                    "ROLE_REMOVE",
                    customEvent.getReason(),
                    payload
            );
        });
    }
}