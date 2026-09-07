package net.survivalboom.sbds.modules.logging.api.events.members;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserRoleAddEvent extends EventCancellableBase {

    private final GuildMemberRoleAddEvent jdaEvent;
    private final User moderator;
    private String reason;
    private String cancelReason = "Role addition cancelled by system";

    public UserRoleAddEvent(@NotNull ModuleMain module, @NotNull GuildMemberRoleAddEvent jdaEvent, @Nullable User moderator, @Nullable String reason) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }

    public @NotNull GuildMemberRoleAddEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable User getModerator() {
        return moderator;
    }

    public @Nullable String getReason() {
        return reason;
    }

    public void setReason(@Nullable String reason) {
        this.reason = reason;
    }

    public @NotNull String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(@NotNull String cancelReason) {
        this.cancelReason = cancelReason;
    }
}