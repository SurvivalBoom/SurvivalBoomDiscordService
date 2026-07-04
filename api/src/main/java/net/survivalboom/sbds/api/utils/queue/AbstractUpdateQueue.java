package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.valid.Manager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public abstract class AbstractUpdateQueue extends Manager {

    protected final String name;

    protected final Runnable runnable;

    protected final Logger log;


    protected final int delay;

    protected ISchedulerTask task;

    protected boolean updateRequested = false;

    protected long lastAppend = 0;


    public AbstractUpdateQueue(
            @NotNull Runnable runnable,
            @NotNull String name,
            int delay
    ) {

        Objects.requireNonNull(runnable, "runnable == null");
        Objects.requireNonNull(name, "name == null");

        this.runnable = runnable;
        this.name = name;
        this.delay = delay;

        this.log = LoggerFactory.getLogger(name + "-UpdateQueue");

    }

    @Override
    protected void init0() {
        task = schedule0();
    }

    @Override
    protected void shutdown0() {
        task.tryCancel();
        task = null;
    }

    protected abstract ISchedulerTask schedule0();

    protected void task() {

        if (!updateRequested) {
            return;
        }

        long time = System.currentTimeMillis();
        if (lastAppend + delay > time) {
            return;
        }

        this.updateRequested = false;

        try {
            runnable.run();
        }

        catch (Throwable t) {
            log.error("Failed to execute an update.", t);
        }

    }

    public void requestUpdate() {
        checkValid();
        this.updateRequested = true;
        this.lastAppend = System.currentTimeMillis();
    }

}
