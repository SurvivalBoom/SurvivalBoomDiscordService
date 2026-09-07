package net.survivalboom.sbds.modules.logging.module.storage;

import net.survivalboom.sbds.api.utils.valid.Valid;
import net.survivalboom.sbds.modules.logging.api.storage.ILogRecordData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class LogData extends Valid implements ILogRecordData {

    private final LogDataManager manager;
    private final LogDataRecord record;

    public LogData(@NotNull LogDataRecord record, @NotNull LogDataManager manager) {
        this.record = record;
        this.manager = manager;
    }


    @Override
    public long getId() {
        return record.getId();
    }

    @Override
    public long getGuildId() {
        return record.getGuildId();
    }

    @Override
    public long getUserId() {
        return record.getUserId();
    }

    @Override
    public @Nullable Long getModeratorId() {
        return record.getModeratorId();
    }


    @Override
    public @NotNull String getAction() {
        return record.getAction();
    }

    @Override
    public @Nullable String getReason() {
        return record.getReason();
    }

    @Override
    public @Nullable String getPayload() {
        return record.getPayload();
    }

    @Override
    public @NotNull Instant getTime() {
        return record.getTime();
    }

    public @NotNull LogDataRecord getRecord() {
        return record;
    }

    public @NotNull LogDataManager getManager() {
        return manager;
    }

    @Override
    protected void setValid(boolean v) {
        super.setValid(v);
    }
}