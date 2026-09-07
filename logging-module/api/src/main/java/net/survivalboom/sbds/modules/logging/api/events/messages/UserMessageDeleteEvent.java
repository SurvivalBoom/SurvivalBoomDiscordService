package net.survivalboom.sbds.modules.logging.api.events.messages;

import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.logging.api.storage.ILoggedMessageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserMessageDeleteEvent extends EventBase {

    private final MessageDeleteEvent jdaEvent;
    private final ILoggedMessageData deletedRecord;

    public UserMessageDeleteEvent(
            @NotNull ModuleMain module,
            @NotNull MessageDeleteEvent jdaEvent,
            @Nullable ILoggedMessageData deletedRecord
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.deletedRecord = deletedRecord;
    }

    public @NotNull MessageDeleteEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable ILoggedMessageData getDeletedRecord() {
        return deletedRecord;
    }

}