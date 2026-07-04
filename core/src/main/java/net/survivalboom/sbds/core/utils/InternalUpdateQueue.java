package net.survivalboom.sbds.core.utils;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.queue.AbstractUpdateQueue;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InternalUpdateQueue extends AbstractUpdateQueue {

    private final Scheduler scheduler;

    public InternalUpdateQueue(
            @NotNull Runnable runnable,
            @NotNull String name,
            int delay,
            @NotNull Scheduler scheduler
    ) {
        super(runnable, name, delay);
        Objects.requireNonNull(scheduler, "scheduler == null");
        this.scheduler = scheduler;
    }

    public InternalUpdateQueue(
            @NotNull Runnable runnable,
            @NotNull String name,
            int delay,
            @NotNull SBDS sbds
    ) {
        this(runnable, name, delay, sbds.getScheduler());
    }

    @Override
    protected ISchedulerTask schedule0() {
        return scheduler.schedule0(null, name, task -> task(), 1000, 500);
    }

}
