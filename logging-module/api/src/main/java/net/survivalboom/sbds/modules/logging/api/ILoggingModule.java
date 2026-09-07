package net.survivalboom.sbds.modules.logging.api;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.modules.logging.api.storage.ILogDataManager;
import net.survivalboom.sbds.modules.logging.api.storage.ILogRecordData;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ILoggingModule {

    ILogDataManager getLogDataManager();

    @NotNull CompletableFuture<@NotNull List<ILogRecordData>> getUserHistory(@NotNull Guild guild, @NotNull User user);

}