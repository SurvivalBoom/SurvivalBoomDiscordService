package net.survivalboom.sbds.modules.logging.module.storage;

import jakarta.persistence.*;
import net.survivalboom.sbds.api.database.DataRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "logging_records")
public class LogDataRecord extends DataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long guildId;

    @Column(nullable = false)
    private long userId;

    @Column
    private Long moderatorId;

    @Column(nullable = false)
    private String action;

    @Column
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant time;

    protected LogDataRecord() {}

    public LogDataRecord(
            long guildId,
            long userId,
            @Nullable Long moderatorId,
            @NotNull String action,
            @Nullable String reason,
            @Nullable String payload
    ) {
        this.guildId = guildId;
        this.userId = userId;
        this.moderatorId = moderatorId;
        this.action = action;
        this.reason = reason;
        this.payload = payload;
        this.time = Instant.now();
    }


    public long getId() {
        return id;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getUserId() {
        return userId;
    }

    public @Nullable Long getModeratorId() {
        return moderatorId;
    }


    public @NotNull String getAction() {
        return action;
    }

    public @Nullable String getReason() {
        return reason;
    }


    public @Nullable String getPayload() {
        return payload;
    }

    public @NotNull Instant getTime() {
        return time;
    }

    public void setPayload(@Nullable String payload) { this.payload = payload; }
}