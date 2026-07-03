package net.survivalboom.sbds.modules.logging.api;

public interface ILoggedMessage {

    long getMessageId();

    long getGuildId();

    long getChannelId();

    long getAuthorId();

    String getContent();

    long getTimestamp();

}