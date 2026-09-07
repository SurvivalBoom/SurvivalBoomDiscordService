package net.survivalboom.sbds.modules.logging.api.events.members;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserRoleRemoveEvent extends EventCancellableBase {

    private final GuildMemberRoleRemoveEvent jdaEvent;
    private final User moderator;
    private String reason;
    private String cancelReason = "Role removal cancelled by system";

    public UserRoleRemoveEvent(@NotNull ModuleMain module, @NotNull GuildMemberRoleRemoveEvent jdaEvent, @Nullable User moderator, @Nullable String reason) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }

    public @NotNull GuildMemberRoleRemoveEvent getJdaEvent() {
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