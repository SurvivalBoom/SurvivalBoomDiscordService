package net.survivalboom.sbds.modules.logging.module.storage;

import jakarta.persistence.*;
import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.modules.logging.api.storage.ILoggedMessageData;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "logging_messages")
public class LoggedMessageRecord extends DataRecord implements ILoggedMessageData {

    @Id
    private long messageId;

    @Column(nullable = false)
    private long guildId;

    @Column(nullable = false)
    private long channelId;

    @Column(nullable = false)
    private long authorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private long timestamp;

    @Transient
    private boolean valid = true;

    protected LoggedMessageRecord() {}

    public LoggedMessageRecord(long messageId, long guildId, long channelId, long authorId, @NotNull String content, long timestamp) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.authorId = authorId;
        this.content = content;
        this.timestamp = timestamp;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public long getMessageId() {
        return messageId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    @Override
    public long getAuthorId() {
        return authorId;
    }


    @Override
    public @NotNull String getContent() {
        return content;
    }


    @Override public void setContent(@NotNull String content) {
        this.content = content;
    }

    @Override public long getTimestamp() {
        return timestamp;
    }

}