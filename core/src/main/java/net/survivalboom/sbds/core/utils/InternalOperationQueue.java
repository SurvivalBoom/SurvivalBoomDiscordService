package net.survivalboom.sbds.core.utils;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.queue.AbstractOperationQueue;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class InternalOperationQueue<obj> extends AbstractOperationQueue<obj, InternalOperationQueue<obj>> {

    private final Scheduler scheduler;

    public InternalOperationQueue(
            @NotNull Consumer<InternalOperationQueue<obj>> consumer,
            @NotNull String name,
            int delay,
            @NotNull Scheduler scheduler
    ) {
        super(consumer, name, delay);
        Objects.requireNonNull(scheduler, "scheduler == null");
        this.scheduler = scheduler;
    }

    public InternalOperationQueue(
            @NotNull Consumer<InternalOperationQueue<obj>> consumer,
            @NotNull String name,
            int delay,
            @NotNull SBDS sbds
    ) {
        this(consumer, name, delay, sbds.getScheduler());
    }

    @Override
    protected ISchedulerTask schedule0() {
        return scheduler.schedule0(null, name, task -> task(), 1000, 500);
    }

}
