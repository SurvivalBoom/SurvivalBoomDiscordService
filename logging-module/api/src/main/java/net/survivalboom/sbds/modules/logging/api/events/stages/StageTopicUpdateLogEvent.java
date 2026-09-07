package net.survivalboom.sbds.modules.logging.api.events.stages;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.stage.update.StageInstanceUpdateTopicEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StageTopicUpdateLogEvent extends EventCancellableBase {

    private final StageInstanceUpdateTopicEvent jdaEvent;
    private final User moderator;
    private String reason;


    public StageTopicUpdateLogEvent(@NotNull ModuleMain module, @NotNull StageInstanceUpdateTopicEvent jdaEvent, @Nullable User moderator, @Nullable String reason) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }


    public @NotNull StageInstanceUpdateTopicEvent getJdaEvent() {
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