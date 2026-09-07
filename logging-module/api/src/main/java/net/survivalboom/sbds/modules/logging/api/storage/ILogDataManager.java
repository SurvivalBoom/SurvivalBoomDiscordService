package net.survivalboom.sbds.modules.logging.api.storage;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.utils.valid.IManager;
import net.survivalboom.sbds.modules.logging.api.ILoggingModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ILogDataManager extends IManager {

    @NotNull ILoggingModule getModule();


    @NotNull CompletableFuture<@NotNull ILogRecordData> create(
            long guildId,
            long userId,
            @Nullable Long moderatorId,
            @NotNull String action,
            @Nullable String reason,
            @Nullable String payload
    );


    @NotNull CompletableFuture<@NotNull List<ILogRecordData>> getHistory(long guildId, long userId);

    default @NotNull CompletableFuture<@NotNull List<ILogRecordData>> getHistory(@NotNull Guild guild, @NotNull User user) {
        return getHistory(guild.getIdLong(), user.getIdLong());
    }


    @NotNull CompletableFuture<Void> delete(long id);

    @NotNull CompletableFuture<Void> save(@NotNull ILogRecordData logData);


    @NotNull CompletableFuture<Void> saveMessage(@NotNull ILoggedMessageData record);

    @NotNull CompletableFuture<@Nullable ILoggedMessageData> getMessage(long messageId);
}