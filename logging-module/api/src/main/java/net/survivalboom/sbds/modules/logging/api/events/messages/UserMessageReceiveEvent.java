package net.survivalboom.sbds.modules.logging.api.events.messages;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.logging.api.storage.ILoggedMessageData;
import org.jetbrains.annotations.NotNull;

public class UserMessageReceiveEvent extends EventCancellableBase {

    private final MessageReceivedEvent jdaEvent;

    private final ILoggedMessageData logRecord;

    public UserMessageReceiveEvent(
            @NotNull ModuleMain module,
            @NotNull MessageReceivedEvent jdaEvent,
            @NotNull ILoggedMessageData logRecord
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.logRecord = logRecord;
    }


    public @NotNull MessageReceivedEvent getJdaEvent() {
        return jdaEvent;
    }

    public @NotNull ILoggedMessageData getLogRecord() {
        return logRecord;
    }

}