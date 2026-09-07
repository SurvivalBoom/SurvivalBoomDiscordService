package net.survivalboom.sbds.modules.logging.api.events.stages;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.stage.StageInstanceDeleteEvent;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StageEndLogEvent extends EventBase {

    private final StageInstanceDeleteEvent jdaEvent;
    private final User moderator;
    private final String reason;


    public StageEndLogEvent(@NotNull ModuleMain module, @NotNull StageInstanceDeleteEvent jdaEvent, @Nullable User moderator, @Nullable String reason) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.moderator = moderator;
        this.reason = reason;
    }


    public @NotNull StageInstanceDeleteEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable User getModerator() {
        return moderator;
    }

    public @Nullable String getReason() {
        return reason;
    }
}