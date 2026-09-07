package net.survivalboom.sbds.modules.logging.api.events.voices;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoiceChannelStatusUpdateLogEvent extends EventCancellableBase {

    private final ChannelUpdateVoiceStatusEvent jdaEvent;
    private final User moderator;
    private String reason;


    public VoiceChannelStatusUpdateLogEvent(
            @NotNull ModuleMain module,
            @NotNull ChannelUpdateVoiceStatusEvent jdaEvent,
            @Nullable User moderator,
            @Nullable String reason
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }


    public @NotNull ChannelUpdateVoiceStatusEvent getJdaEvent() {
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
}