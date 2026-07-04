package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.valid.Manager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractPushQueue<obj, it extends AbstractPushQueue<obj, it>> extends Manager {

    protected final String name;

    protected final Consumer<it> consumer;

    protected final Logger log;


    protected final int delay;

    protected ISchedulerTask task;

    protected final List<obj> queue = new ArrayList<>();

    protected long lastAppend = 0;


    public AbstractPushQueue(
            @NotNull Consumer<it> consumer,
            @NotNull String name,
            int delay
    ) {

        Objects.requireNonNull(consumer, "consumer == null");
        Objects.requireNonNull(name, "name == null");

        this.consumer = consumer;
        this.name = name;
        this.delay = delay;

        this.log = LoggerFactory.getLogger(name + "-PushQueue");

    }

    //
    // MANAGER
    //

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

    @SuppressWarnings("unchecked")
    protected void task() {

        if (queue.isEmpty()) {
            return;
        }

        long time = System.currentTimeMillis();
        if (lastAppend + delay > time) {
            return;
        }

        List<obj> queue = new ArrayList<>(this.queue);

        try {
            consumer.accept((it) this);
        }

        catch (Throwable t) {
            log.error("Failed to push the queue of {} objects!", queue.size(), t);
        }

        this.queue.clear();

    }

    //
    // QUEUE
    //

    public void append(@NotNull obj obj) {

        Objects.requireNonNull(obj, "obj == null");
        checkValid();

        if (queue.contains(obj)) {
            return;
        }

        this.queue.add(obj);
        this.lastAppend = System.currentTimeMillis();

    }

    public @NotNull List<obj> getQueue() {
        checkValid();
        return new ArrayList<>(queue);
    }

}
