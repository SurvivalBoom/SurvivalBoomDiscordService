package net.survivalboom.sbds.api.commands.context;

import net.survivalboom.sbds.api.commands.CommandExecutor;
import org.jetbrains.annotations.NotNull;

public interface ContextCommandExecutor<E extends ContextInteractionInfo<?>> extends CommandExecutor {

    void executes(@NotNull E info) throws Throwable;

}
