package net.survivalboom.sbds.modules.logging.api.events.channels;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChannelUpdateNameLogEvent extends EventCancellableBase {

    private String cancelReason = "Cancelled by internal system";

    private final ChannelUpdateNameEvent jdaEvent;
    private final User moderator;
    private String reason;

    public ChannelUpdateNameLogEvent(
            @NotNull ModuleMain module,
            @NotNull ChannelUpdateNameEvent jdaEvent,
            @Nullable User moderator,
            @Nullable String reason
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }

    public @NotNull ChannelUpdateNameEvent getJdaEvent() {
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