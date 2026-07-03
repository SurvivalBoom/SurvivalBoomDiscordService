package net.survivalboom.sbds.modules.logging.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;

@Entity
@Table(name = "sbds_logged_messages")
public class MessageRecord extends DataRecord implements ILoggedMessage {

    @Id
    @Column(nullable = false)
    private long messageId;

    @Column(nullable = false)
    private long guildId;

    @Column(nullable = false)
    private long channelId;

    @Column(nullable = false)
    private long authorId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long timestamp;

    protected MessageRecord() {}

    public MessageRecord(long messageId, long guildId, long channelId, long authorId, String content, long timestamp) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.authorId = authorId;
        this.content = content;
        this.timestamp = timestamp;
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
    public String getContent() {
        return content;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setContent(String content) {
        this.content = content;
    }
}