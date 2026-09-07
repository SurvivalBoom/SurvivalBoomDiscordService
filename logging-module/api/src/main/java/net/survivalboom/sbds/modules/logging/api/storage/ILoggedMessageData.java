package net.survivalboom.sbds.modules.logging.api.storage;

import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;

public interface ILoggedMessageData extends IValid {
    long getMessageId();

    long getGuildId();

    long getChannelId();

    long getAuthorId();


    @NotNull String getContent();

    void setContent(@NotNull String content);


    long getTimestamp();

}