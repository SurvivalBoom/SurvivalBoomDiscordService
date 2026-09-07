package net.survivalboom.sbds.modules.logging.api.events.members;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserNicknameUpdateEvent extends EventCancellableBase {

    private final GuildMemberUpdateNicknameEvent jdaEvent;
    private final User moderator;
    private String reason;
    private String cancelReason = "Nickname change cancelled by system";

    public UserNicknameUpdateEvent(@NotNull ModuleMain module, @NotNull GuildMemberUpdateNicknameEvent jdaEvent, @Nullable User moderator, @Nullable String reason) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }

    public @NotNull GuildMemberUpdateNicknameEvent getJdaEvent() {
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