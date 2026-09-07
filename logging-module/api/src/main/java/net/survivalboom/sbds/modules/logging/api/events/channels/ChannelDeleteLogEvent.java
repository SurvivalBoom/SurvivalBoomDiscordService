package net.survivalboom.sbds.modules.logging.api.events.channels;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChannelDeleteLogEvent extends EventBase {

    private final ChannelDeleteEvent jdaEvent;
    private final User moderator;
    private final String reason;

    public ChannelDeleteLogEvent(
            @NotNull ModuleMain module,
            @NotNull ChannelDeleteEvent jdaEvent,
            @Nullable User moderator,
            @Nullable String reason
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }

    public @NotNull ChannelDeleteEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable User getModerator() {
        return moderator;
    }

    public @Nullable String getReason() {
        return reason;
    }
}