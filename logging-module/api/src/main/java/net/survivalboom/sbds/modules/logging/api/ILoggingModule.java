package net.survivalboom.sbds.modules.logging.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface ILoggingModule {

    @NotNull CompletableFuture<@Nullable ILoggedMessage> getCachedMessage(long messageId);

}