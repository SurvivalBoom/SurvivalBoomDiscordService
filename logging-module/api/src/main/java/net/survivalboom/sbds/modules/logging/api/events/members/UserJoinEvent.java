package net.survivalboom.sbds.modules.logging.api.events.members;

import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserJoinEvent extends EventCancellableBase {

    private final GuildMemberJoinEvent jdaEvent;
    private final Invite inviter;
    private String cancelReason = "Join cancelled by internal system";

    public UserJoinEvent(@NotNull ModuleMain module, @NotNull GuildMemberJoinEvent jdaEvent, @Nullable Invite inviter) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.inviter = inviter;
    }

    public @NotNull GuildMemberJoinEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable Invite getInvite() {
        return inviter;
    }

    public @NotNull String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(@NotNull String cancelReason) {
        this.cancelReason = cancelReason;
    }
}