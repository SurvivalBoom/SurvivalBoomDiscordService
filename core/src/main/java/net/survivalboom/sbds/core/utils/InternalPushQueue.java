package net.survivalboom.sbds.core.utils;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.queue.AbstractPushQueue;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class InternalPushQueue<obj> extends AbstractPushQueue<obj, InternalPushQueue<obj>> {

    private final Scheduler scheduler;

    public InternalPushQueue(
            @NotNull Consumer<InternalPushQueue<obj>> consumer,
            @NotNull String name,
            int delay,
            @NotNull Scheduler scheduler
    ) {
        super(consumer, name, delay);
        Objects.requireNonNull(scheduler, "scheduler == null");
        this.scheduler = scheduler;
    }

    public InternalPushQueue(
            @NotNull Consumer<InternalPushQueue<obj>> consumer,
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
