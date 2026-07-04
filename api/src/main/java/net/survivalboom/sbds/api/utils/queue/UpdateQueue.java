package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UpdateQueue extends AbstractUpdateQueue {

    private final IModule module;

    public UpdateQueue(
            @NotNull IModule module,
            @NotNull Runnable runnable,
            @NotNull String name,
            int delay
    ) {
        super(runnable, name, delay);
        Objects.requireNonNull(module, "module == null");
        this.module = module;
    }

    public UpdateQueue(
            @NotNull ModuleMain module,
            @NotNull Runnable runnable,
            @NotNull String name,
            int delay
    ) {
        this(module.getModule(), runnable, name, delay);
    }

    @Override
    protected ISchedulerTask schedule0() {
        return module.getManager().getSbds().getScheduler().schedule(module, this::task, 1000, 500);
    }

}
