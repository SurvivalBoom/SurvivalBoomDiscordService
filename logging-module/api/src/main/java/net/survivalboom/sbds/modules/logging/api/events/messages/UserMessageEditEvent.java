package net.survivalboom.sbds.modules.logging.api.events.messages;

import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.logging.api.storage.ILogRecordData;
import net.survivalboom.sbds.modules.logging.api.storage.ILoggedMessageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserMessageEditEvent extends EventBase {

    private final MessageUpdateEvent jdaEvent;

    private final ILoggedMessageData oldLogRecord;

    private final String newContent;


    public UserMessageEditEvent(
            @NotNull ModuleMain module,
            @NotNull MessageUpdateEvent jdaEvent,
            @Nullable ILoggedMessageData oldLogRecord,
            @NotNull String newContent
    ) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.oldLogRecord = oldLogRecord;
        this.newContent = newContent;
    }

    public @NotNull MessageUpdateEvent getJdaEvent() {
        return jdaEvent;
    }

    public @Nullable ILoggedMessageData getOldLogRecord() {
        return oldLogRecord;
    }

    public @NotNull String getNewContent() {
        return newContent;
    }
}